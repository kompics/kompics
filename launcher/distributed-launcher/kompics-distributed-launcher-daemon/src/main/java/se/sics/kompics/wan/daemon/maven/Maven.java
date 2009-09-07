package se.sics.kompics.wan.daemon.maven;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.wan.config.ChordSystemConfiguration;
import se.sics.kompics.wan.config.CyclonSystemConfiguration;
import se.sics.kompics.wan.config.SystemConfiguration;
import se.sics.kompics.wan.daemon.Daemon;
import se.sics.kompics.wan.job.DummyPomConstructionException;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.job.JobExecRequest;
import se.sics.kompics.wan.job.JobExecResponse;
import se.sics.kompics.wan.job.JobExited;
import se.sics.kompics.wan.job.JobLoadRequest;
import se.sics.kompics.wan.job.JobLoadResponse;
import se.sics.kompics.wan.job.JobReadFromExecutingRequest;
import se.sics.kompics.wan.job.JobReadFromExecutingResponse;
import se.sics.kompics.wan.job.JobRemoveRequest;
import se.sics.kompics.wan.job.JobRemoveResponse;
import se.sics.kompics.wan.job.JobStartRequest;
import se.sics.kompics.wan.job.JobStartResponse;
import se.sics.kompics.wan.job.JobStopRequest;
import se.sics.kompics.wan.job.JobStopResponse;
import se.sics.kompics.wan.job.JobWriteToExecutingRequest;
import se.sics.kompics.wan.masterdaemon.events.JobRemoveResponseMsg;
import se.sics.kompics.wan.util.PomUtils;

public class Maven extends ComponentDefinition {

    public static final String SCENARIO_FILENAME = "scenario";
    private static final Logger logger = LoggerFactory.getLogger(Maven.class);
    private Negative<MavenPort> maven = negative(MavenPort.class);
    private Map<Integer, Job> executingJobs = new HashMap<Integer, Job>();
    private Map<Integer, ProcessWrapper> executingProcesses = new ConcurrentHashMap<Integer, ProcessWrapper>();

    public class ProcessWrapper implements Runnable {

        private final int jobId;
        private Process process = null;
        private BufferedWriter writeToInput = null;
        private BufferedReader readFromOutput = null;
        private final ProcessBuilder processBuilder;
        private AtomicBoolean started = new AtomicBoolean(false);
        private int exitValue = -111;

        public ProcessWrapper(int jobId, ProcessBuilder processBuilder) {
            this.jobId = jobId;
            if (processBuilder == null) {
                throw new IllegalArgumentException("ProcessBuilder was null");
            }
            this.processBuilder = processBuilder;
        }

        public void run() {

            String exitMsg = jobId + ": Process exited successfully";
            try {
                // This can throw exceptions
                process = processBuilder.start();

                if (process == null) {
                    return;
                }


                writeToInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
                // XXX configure output to write to Socket for log4j at Master
                readFromOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

                started.set(true);



                try {
                    BufferedReader out = new BufferedReader(new InputStreamReader(
                            process.getInputStream()));
                    Writer input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

                    String line;
                    do {
                        line = out.readLine();
                        if (line != null) {
                            if (line.equals("2DIE")) {
                                if (process != null) {
                                    process.destroy();
                                    process = null;
                                }
                                break;
                            }
                            logger.info(line + "\n");
                        }
                    } while (line != null);
                } catch (Throwable e) {
                    logger.warn(e.getMessage());
                }



                exitValue = process.waitFor();
            } catch (IOException e) {
                e.printStackTrace();
                exitMsg = e.getMessage();
            } catch (InterruptedException e) {
                e.printStackTrace();
                exitMsg = e.getMessage();
            } finally {
                executingJobs.remove(jobId);
                executingProcesses.remove(jobId);
            }

            // XXX notify MavenLauncher that process is exiting with event
            notifyProcessExiting(jobId, exitValue, exitMsg);
        }

        public boolean started() {
            return started.get();
        }

        public int getExitValue() {
            return exitValue;
        }

        /**
         * @return true : stopped, false : already stopped
         */
        public synchronized boolean destroy() {
//            if (isAlive() == false) {
//                return false;
//            }
            process.destroy();
            return true;
        }

        public int getJobId() {
            return jobId;
        }

        public boolean isAlive() {
            try {
                // XXX hack here. Shouldn't use IllegalThreadStateException
                // to determine if process hasn't failed.
                int exitValue = process.exitValue();
            } catch (IllegalThreadStateException e) {
                return true;
            }
            return false;
        }

        /**
         * Input.
         *
         * @param string
         *            the string
         *
         * @throws IOException
         *             Signals that an I/O exception has occurred.
         */
        public final void writeBufferedInput(String string) throws IOException {
            if (writeToInput == null) {
                return;
            }
            writeToInput.write(string);
            writeToInput.write("\n");
            writeToInput.flush();
        }

        public final String readLineFromOutput() throws IOException {
            if (readFromOutput == null) {
                return "Process not initialized yet";
            }
            return readFromOutput.readLine();
        }

        public final CharBuffer readBufferedOutput(CharBuffer cb) throws IOException {
            if (readFromOutput == null) {
                return cb.append("Process not initialized yet");
            }
            if (readFromOutput.ready() == false) {
                return cb.append("Process not ready yet");
            }
            readFromOutput.read(cb);
            return cb;
        }
    }

    public Maven() {

        subscribe(handleJobLoadRequest, maven);
        subscribe(handleJobStartRequest, maven);
        subscribe(handleJobStopRequest, maven);
        subscribe(handleJobRemoveRequest, maven);
        subscribe(handleJobWriteToExecuting, maven);
        subscribe(handleJobReadFromExecuting, maven);

        subscribe(handleJobExecRequest, maven);
    }
    public Handler<JobReadFromExecutingRequest> handleJobReadFromExecuting = new Handler<JobReadFromExecutingRequest>() {

        public void handle(JobReadFromExecutingRequest event) {

            int jobId = event.getJobId();
            ProcessWrapper p = executingProcesses.get(jobId);
            if (p == null) {
                throw new IllegalStateException("Process p not found for jobId: " + jobId);
            }

            String msg;
            CharBuffer cb = CharBuffer.allocate(1000);
            try {
                cb = p.readBufferedOutput(cb);
                // msg = p.readLineFromOutput();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                cb = CharBuffer.allocate(e.getMessage().length());
                cb.put(e.getMessage());
            }
            cb.rewind();
            msg = cb.toString();
            JobReadFromExecutingResponse resp = new JobReadFromExecutingResponse(event, event.getJobId(), msg);
            trigger(resp, maven);
        }
    };
    public Handler<JobWriteToExecutingRequest> handleJobWriteToExecuting = new Handler<JobWriteToExecutingRequest>() {

        public void handle(JobWriteToExecutingRequest event) {

            int jobId = event.getJobId();
            ProcessWrapper p = executingProcesses.get(jobId);
            if (p == null) {
                throw new IllegalStateException("Process p not found for jobId: " + jobId);
            }

            String msg;
            try {
                p.writeBufferedInput(event.getMsg());
                CharBuffer cb = CharBuffer.allocate(1000);
                try {
                    cb = p.readBufferedOutput(cb);
                } catch (IOException e) {
                    e.printStackTrace();
                    cb = CharBuffer.allocate(e.getMessage().length());
                    cb.put(e.getMessage());
                }
                cb.rewind();
                msg = cb.toString();
            } catch (IOException e1) {
                msg = e1.toString();
            }

            JobReadFromExecutingResponse resp = new JobReadFromExecutingResponse(event, event.getJobId(), msg);
            trigger(resp, maven);
        }
    };
    public Handler<JobRemoveRequest> handleJobRemoveRequest = new Handler<JobRemoveRequest>() {

        public void handle(JobRemoveRequest event) {
            String msg = "Success";
            JobRemoveResponseMsg.Status status = JobRemoveResponseMsg.Status.SUCCESS;
            Job job = event.getJob();
            File pomFile = job.getPomFile();

            try {
                removeFileRecurse(pomFile.getParentFile(), true);
            } catch (FileRemovalException e) {
                msg = e.getMessage();
                status = JobRemoveResponseMsg.Status.ERROR;
            }
            trigger(new JobRemoveResponse(event, status, event.getJob().getId(), msg), maven);
        }
    };

    /**
     * @param pathToPomDir
     *            directory containing pom.xml file.
     * @param isInPomDir
     * @return true if success, false is only used in recursion not in public
     *         API.
     * @throws FileRemovalException
     *             if the file wasn't successfully deleted.
     */
    private boolean removeFileRecurse(File pathToPomDir, boolean isInPomDir)
            throws FileRemovalException {
        if (isInPomDir == true) {
            if (pathToPomDir.exists()) {
                if (pathToPomDir.isDirectory()) {
                    File[] files = pathToPomDir.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].isDirectory()) {
                            removeFileRecurse(files[i], true);
                        } else {
                            if (files[i].delete() == false) {
                                throw new FileRemovalException("Problem when deleting file: " + files[i].getAbsolutePath());
                            }
                        }

                    }
                }
            }
        }
        File parent = pathToPomDir.getParentFile();

        if (pathToPomDir.delete() == false) {
            // if I couldn't delete the pom dir, throw exception
            // don't throw exception if couldn't delete a parent directory, as
            // this is
            // expected behaviour if the parent directory contains existing
            // files/dirs.
            if (isInPomDir == true) {
                return false;
            }

        }

        // recurse deleting dirs and stop when Daemon.KOMPICS_HOME is reached.
        if (parent.getAbsolutePath().compareTo(Daemon.KOMPICS_HOME) != 0) {
            removeFileRecurse(parent, false);
        }

        return true;
    }
    public Handler<JobLoadRequest> handleJobLoadRequest = new Handler<JobLoadRequest>() {

        public void handle(JobLoadRequest event) {

            int id = event.getId();

            JobLoadResponse.Status status = JobLoadResponse.Status.ASSEMBLED;
            // If msg not a duplicate
            // if (assembledJobs.containsKey(id) == false &&
            // executingJobs.containsKey(id) == false) {
            try {
                if (event.createDummyPomFile() == false) {
                    status = JobLoadResponse.Status.MAVEN_EXECPTION;
                } else {
                    status = JobLoadResponse.Status.POM_CREATED;
                    String pomFilename = event.getPomFile().getAbsolutePath();
                    mvnAssemblyAssembly(pomFilename, event.isHideMavenOutput());
                }
                // assembledJobs.put(id, event);
            } catch (DummyPomConstructionException e) {
                e.printStackTrace();
                status = JobLoadResponse.Status.FAIL;
            } catch (MavenExecException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                status = JobLoadResponse.Status.MAVEN_EXECPTION;
            }


            trigger(new JobLoadResponse(event, id, status), maven);
        }
    };
    public Handler<JobExecRequest> handleJobExecRequest = new Handler<JobExecRequest>() {

        public void handle(JobExecRequest event) {

            int jobId = event.getId();
            int port = event.getPort();
            int webPort = event.getWebPort();
            String mainClass = event.getMainClass();
            String bootHost = event.getBootHost();
            String monitorHost = event.getMonitorHost();
            int numPeers = event.getNumPeers();

            logger.info("Job exec received for :" + event.getMainClass());
            logger.info("View at web page: http://{}:{}", event.getHostname(), webPort);
            logger.info("Bootstrap server: http://{}:{}", bootHost, event.getBootPort() );
            logger.info("Monitor server: http://{}:{}", monitorHost, event.getMonitorPort() );
            logger.info("Number of peers being launched: {}", event.getNumPeers());
            
            for (int i = 0; i < numPeers; i++) {

                SystemConfiguration config = null;


                // TODO replace this hack
                if (mainClass.compareTo("se.sics.kompics.p2p.systems.cyclon.CyclonPeerMain")==0)
                {
                    config = new CyclonSystemConfiguration(port+(i*100), bootHost, monitorHost);
                }
                else if (mainClass.compareTo("se.sics.kompics.p2p.systems.chord.ChordPeerMain")==0)
                {
                    config = new ChordSystemConfiguration(port+(i*100), bootHost, monitorHost);
                }
                else if (mainClass.compareTo("se.sics.kompics.p2p.systems.cyclon.monitor.server.CyclonMonitorServerMain")==0)
                {
                    config = new CyclonSystemConfiguration(port+(i*100), bootHost, monitorHost);
                }
                else if (mainClass.compareTo("se.sics.kompics.p2p.systems.chord.monitor.server.ChordMonitorServerMain")==0)
                {
                    config = new ChordSystemConfiguration(port+(i*100), bootHost, monitorHost);
                }
                else if (mainClass.compareTo("se.sics.kompics.p2p.systems.bootstrap.server.BootstrapServerMain")==0)
                {
                    config = new ChordSystemConfiguration(port+(i*100), bootHost, monitorHost);
                }

                else {
                    config = new ChordSystemConfiguration(port+(i*100), bootHost, monitorHost);
                }

                JobExecResponse.Status status = JobExecResponse.Status.SUCCESS;
                if (event == null) {
                    status = JobExecResponse.Status.FAIL;
                } else {

//                    String pomFilename = event.getPomFilename();
//                    mvnExecExec(event, pomFilename);

                        String jarName = event.getDummyJarWithDependenciesName();
                        String hostname = event.getHostname();
                        int id = i + Math.abs(hostname.hashCode() % 4000);
                        int res = forkProcess(event, jarName, id, config,  null);

                        if (res == 0) {
                            status = JobExecResponse.Status.SUCCESS;
                        } else {
                            status = JobExecResponse.Status.FAIL;
                        }

                }

                // ProcessWrapper p = executingProcesses.get(id);
                JobExecResponse response = new JobExecResponse(event, jobId, status);

                trigger(response, maven);
            }
        }
    };
    public Handler<JobStartRequest> handleJobStartRequest = new Handler<JobStartRequest>() {

        public void handle(JobStartRequest event) {
            int jobId = event.getId();

            logger.info("Job start received for :" + event.getMainClass());

            JobStartResponse.Status status = JobStartResponse.Status.SUCCESS;
            if (event == null) {
                status = JobStartResponse.Status.FAIL;
            } else {
//				status = mvnExecExec(event, event.getScenario());
//                int res = forkProcess(event.getNumPeers(), event.getMainClass(),
//                        event.getArgs(), event.getDummyJarWithDependenciesName(),
//                        null, event.getScenario());
//                switch (res) {
//                    case 0:
//                        status = JobStartResponse.Status.SUCCESS;
//                        break;
//                    case -1:
//                        status = JobStartResponse.Status.FAIL;
//                        break;
//                    default:
//                        status = JobStartResponse.Status.FAIL;
//                        break;
//                }
            }

            // ProcessWrapper p = executingProcesses.get(id);
            JobStartResponse response = new JobStartResponse(event, jobId, status);
            trigger(response, maven);
        }
    };


    private void mvnAssemblyAssembly(String pomFilename, boolean hideMavenOutput)
            throws MavenExecException {

//        String pomFilename = job.getPomFile().getAbsolutePath();
        logger.info("Assembling pom file:" + pomFilename);
        MavenWrapper mw = new MavenWrapper(pomFilename);

        // XXX redirecting stdin and stderr for this command, due to much
        // crap being printed. <pluginmanagement> not recongnized in assembly
        // artifact
        // by maven embedder.
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;

        if (hideMavenOutput == true) {
            System.setOut(new java.io.PrintStream(new java.io.OutputStream() {

                public void write(int devNull) { // dump output (like writing to
                    // /dev/null)
                }
            }));
            System.setErr(new java.io.PrintStream(new java.io.OutputStream() {

                public void write(int devNull) { // dump output (like writing to
                    // /dev/null)
                }
            }));
        }

        mw.execute("assembly:assembly", null);

        System.setOut(origOut);
        System.setErr(origErr);

        logger.info("maven assembly:assembly completed...");
    }

    /**
     * XXX Currently executing using the assembled jar, not 'maven exec:exec'
     */
    private void mvnExecExec(Job job, String pomFilename) throws MavenExecException {
//        JobExecResponse.Status status = JobExecResponse.Status.SUCCESS;


        executingJobs.put(job.getId(), job);
        final String filename = job.getPomFile().getAbsolutePath();
//            Runnable launchMvnExec = new Runnable() {
//
//                @Override
//                public void run() {
//                    MavenWrapper mw;
//                    try {
//                        mw = new MavenWrapper(filename);
//                        mw.execute("exec:exec");
//                    } catch (MavenExecException ex) {
//                        java.util.logging.Logger.getLogger(Maven.class.getName()).log(Level.SEVERE, null, ex);
//                        logger.warn("maven exec:exec failed: " + e.getMessage());
//                        status = JobExecResponse.Status.FAIL;
//                    }
//                }
//            };
//            new Thread(launchMvnExec).run();

        MavenWrapper mw;

        mw =
                new MavenWrapper(filename);
        mw.execute("exec:exec", null);


        logger.info("maven exec:exec completed...");
    }

    /**
     * @param nodeId
     * @param job
     * @param scenario
     * @param assembly
     * @return 0 on success, -1 on failure.
     */
    private int forkProcess(Job job, String jarName, int peerId, SystemConfiguration config, SimulationScenario scenario) {
        int res = 0;
        String classPath = System.getProperty("java.class.path");
        java.util.List<String> command = new ArrayList<String>();
        command.add("java");
        command.add("-classpath");
        classPath =
                classPath + File.pathSeparatorChar + jarName; //job.getDummyJarWithDependenciesName()
        command.add(classPath);
//        command.add("-D" + Configuration.OPT_PEERS + " " + Integer.toString(numPeers));
        command.addAll(job.getArgs());
//        command.add("-Dlog4j.properties=log4j.properties");
//        command.add("-DKOMPICS_HOME=" + Daemon.KOMPICS_HOME);
//        command.add("-DMAVEN_REPO_LOCAL=" + Daemon.MAVEN_REPO_LOCAL);
        command.add("-Dpeer.id=" + peerId);
        Properties p;

        try {
            p = config.set();
            Enumeration<String> keys = (Enumeration<String>) p.propertyNames();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String value = p.getProperty(key);
                command.add("-D" + key + "=" + value);
            }
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Maven.class.getName()).log(Level.SEVERE, null, ex);
        }

        command.add(job.getMainClass());


        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Map<String, String> env = processBuilder.environment();
        env.put("KOMPICS_HOME", Daemon.KOMPICS_HOME);
        env.put("MAVEN_REPO_LOCAL", Daemon.MAVEN_REPO_LOCAL);

        if (scenario != null) {
            File file = null;
            ObjectOutputStream oos = null;
            try {
                file = File.createTempFile(SCENARIO_FILENAME, ".bin");
                oos =
                        new ObjectOutputStream(new FileOutputStream(file));
                oos.writeObject(scenario);
                oos.flush();
                oos.close();
                env.put(SCENARIO_FILENAME, file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                logger.debug(e.getMessage());
            } finally {
                if (oos != null) {
                    try {
                        oos.close();
                    } catch (IOException ignoreException) {
                    }
                }
            }
        }

        StringBuffer sb = new StringBuffer();
        for (String s : command) {
            sb.append(s);
            sb.append(" ");
        }

        logger.info("Executing: " + sb.toString());
        logger.info("Short version: java -jar " + jarName + PomUtils.sepStr() + job.getMainClass());

        ProcessWrapper pw = new ProcessWrapper(job.getId(), processBuilder);
        executingProcesses.put(job.getId(), pw);
        executingJobs.put(job.getId(), job);
        new Thread(pw).start();
        return res;
    }
    public Handler<JobStopRequest> handleJobStopRequest = new Handler<JobStopRequest>() {

        public void handle(JobStopRequest event) {

            int id = event.getJobId();
            JobStopResponse.Status status;

            String msg = "Stopped job: " + id;
            ProcessWrapper pw = executingProcesses.get(id);
            if (pw == null) {
                status = JobStopResponse.Status.COULD_NOT_FIND_PROCESS_HANDLE_TO_STOP_JOB;
                msg =
                        "Failed to stop " + id;
            } else {
                if (pw.destroy() == true) {
                    status = JobStopResponse.Status.STOPPED;
                } else {
                    status = JobStopResponse.Status.ALREADY_STOPPED;
                    msg =
                            "Already stopped " + id;
                }

            }

            logger.info("Job stopping res: {}", msg);

            JobStopResponse response = new JobStopResponse(event, id, status, msg);
            trigger(response, maven);
        }
    };

    /**
     * This method should only be called from WrappedThread instances...
     *
     * @param jobId
     * @param exitValue
     * @param exitMsg
     */
    protected synchronized void notifyProcessExiting(int jobId, int exitValue, String exitMsg) {
        trigger(new JobExited(jobId, exitValue, exitMsg), maven);
    }

}
