package se.sics.kompics.wan.daemon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.wan.daemon.indexer.IndexPort;
import se.sics.kompics.wan.daemon.indexer.Indexer;
import se.sics.kompics.wan.daemon.indexer.IndexerInit;
import se.sics.kompics.wan.job.JobFound;
import se.sics.kompics.wan.daemon.indexer.ListJobsLoadedResponse;
import se.sics.kompics.wan.daemon.masterclient.MasterClient;
import se.sics.kompics.wan.daemon.masterclient.MasterClientConfig;
import se.sics.kompics.wan.daemon.masterclient.MasterClientInit;
import se.sics.kompics.wan.daemon.masterclient.MasterClientPort;
import se.sics.kompics.wan.daemon.maven.Maven;
import se.sics.kompics.wan.daemon.maven.MavenPort;
import se.sics.kompics.wan.daemonmaster.events.ConnectMasterRequest;
import se.sics.kompics.wan.daemonmaster.events.ConnectMasterResponse;
import se.sics.kompics.wan.daemonmaster.events.DisconnectMasterRequest;
import se.sics.kompics.wan.daemonmaster.events.JobFoundMsg;
import se.sics.kompics.wan.daemonmaster.events.ShutdownDaemonRequestMsg;
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
import se.sics.kompics.wan.masterdaemon.events.DaemonAddress;
import se.sics.kompics.wan.masterdaemon.events.JobExecRequestMsg;
import se.sics.kompics.wan.masterdaemon.events.JobExecResponseMsg;
import se.sics.kompics.wan.masterdaemon.events.JobExitedMsg;
import se.sics.kompics.wan.masterdaemon.events.JobLoadRequestMsg;
import se.sics.kompics.wan.masterdaemon.events.JobLoadResponseMsg;
import se.sics.kompics.wan.masterdaemon.events.JobReadFromExecutingRequestMsg;
import se.sics.kompics.wan.masterdaemon.events.JobReadFromExecutingResponseMsg;
import se.sics.kompics.wan.masterdaemon.events.JobRemoveRequestMsg;
import se.sics.kompics.wan.masterdaemon.events.JobRemoveResponseMsg;
import se.sics.kompics.wan.masterdaemon.events.JobStartRequestMsg;
import se.sics.kompics.wan.masterdaemon.events.JobStartResponseMsg;
import se.sics.kompics.wan.masterdaemon.events.JobStopRequestMsg;
import se.sics.kompics.wan.masterdaemon.events.JobStopResponseMsg;
import se.sics.kompics.wan.masterdaemon.events.JobWriteToExecutingRequestMsg;
import se.sics.kompics.wan.masterdaemon.events.ListJobsLoadedRequestMsg;

public class Daemon extends ComponentDefinition {

    public static final String KOMPICS_HOME;
    public static final String MAVEN_REPO_LOCAL;
    public static final String SCENARIO_FILENAME = "scenario";
    public static final String POM_FILENAME = "pom.xml";
    private static final Logger logger = LoggerFactory.getLogger(Daemon.class);

    static {
        String kHome = System.getProperty("kompics.home");
        String userHome = System.getProperty("user.home");
        if (userHome != null && kHome == null) {
            System.setProperty("kompics.home", new File(userHome + "/.kompics/").getAbsolutePath());
        } else if (userHome == null && kHome == null) {
            throw new IllegalStateException(
                    "kompics.home and user.home environment variables not set.");
        }
        KOMPICS_HOME = System.getProperty("kompics.home");

        if (new File(Daemon.KOMPICS_HOME).exists() == false) {
            if (new File(Daemon.KOMPICS_HOME).mkdirs() == false) {
                logger.warn("Could not create directory: " + Daemon.KOMPICS_HOME);
                logger.warn("You need to log on to the machine with an account that create a '.kompics' directory in the account's home directory");
                System.exit(-1);
            }
        }

        if (System.getProperty("maven.repo.local") == null) {
            System.setProperty("maven.repo.local", new File(userHome + "/.m2/repository").getAbsolutePath());

        }
        MAVEN_REPO_LOCAL = System.getProperty("maven.repo.local");

        if (new File(Daemon.MAVEN_REPO_LOCAL).exists() == false) {
            if ((new File(Daemon.MAVEN_REPO_LOCAL).mkdirs()) == false) {
                logger.warn("Couldn't set directory for Maven Local Repository: " + Daemon.MAVEN_REPO_LOCAL + "\nCheck file permissions for this directory.");
                System.exit(-1);
            }
        }

    }
    private Positive<Network> net = positive(Network.class);
    private Positive<Timer> timer = positive(Timer.class);
    private Component mavenLauncher;
    private Component indexer;
    private Component masterClient;
    private DaemonAddress self;
    private Address masterAddress;
    private Map<Integer, Job> loadingJobs = new HashMap<Integer, Job>();
    private Map<Integer, Job> loadedJobs = new HashMap<Integer, Job>();
    private Map<Integer, JobStartRequest> executingJobs = new HashMap<Integer, JobStartRequest>();
    private Map<Integer, JobStartRequest> completedJobs = new HashMap<Integer, JobStartRequest>();
    private Map<Integer, JobExecRequest> runningJobs = new HashMap<Integer, JobExecRequest>();
    private Map<Integer, JobExecRequest> finishedJobs = new HashMap<Integer, JobExecRequest>();
//	private Map<Integer, MavenLauncher.ProcessWrapper> executingProcesses = new ConcurrentHashMap<Integer, MavenLauncher.ProcessWrapper>();
    private boolean connectedToMaster = false;

    public Daemon() {

        mavenLauncher = create(Maven.class);
        indexer = create(Indexer.class);
        masterClient = create(MasterClient.class);

        subscribe(handleInit, control);
        subscribe(handleStart, control);

        subscribe(handleJobLoadRequest, net);
        subscribe(handleJobStartRequest, net);
        subscribe(handleJobExecRequest, net);
        subscribe(handleShutdownDaemonRequestMsg, net);
        subscribe(handleJobStopRequestMsg, net);
        subscribe(handleListJobsLoadedRequest, net);
        subscribe(handleJobRemoveRequestMsg, net);
        subscribe(handleJobReadFromExecutingRequestMsg, net);
        subscribe(handleJobWriteToExecutingRequestMsg, net);

        subscribe(handleJobLoadResponse, mavenLauncher.getPositive(MavenPort.class));
        subscribe(handleJobStartResponse, mavenLauncher.getPositive(MavenPort.class));
        subscribe(handleJobRemoveResponse, mavenLauncher.getPositive(MavenPort.class));
        subscribe(handleJobExited, mavenLauncher.getPositive(MavenPort.class));
        subscribe(handleJobReadFromExecutingResponse, mavenLauncher.getPositive(MavenPort.class));
        subscribe(handleJobStopResponse, mavenLauncher.getPositive(MavenPort.class));
        subscribe(handleJobReadFromExecutingResponse, mavenLauncher.getPositive(MavenPort.class));


        subscribe(handleShutdownTimeout, timer);
        subscribe(handleTimerDaemonShutdown, timer);

        subscribe(handleJobFound, indexer.getPositive(IndexPort.class));
        subscribe(handleListJobsLoadedResponse, indexer.getPositive(IndexPort.class));
    }
    public Handler<Start> handleStart = new Handler<Start>() {

        public void handle(Start event) {
            trigger(new ConnectMasterRequest(), masterClient.getPositive(MasterClientPort.class));

            trigger(new Start(), indexer.getControl());
        }
    };
    public Handler<JobReadFromExecutingResponse> handleJobReadFromExecutingResponse =
            new Handler<JobReadFromExecutingResponse>() {

                public void handle(JobReadFromExecutingResponse event) {

                    logger.info("Read from job {} :", event.getJobId());
                    trigger(new JobReadFromExecutingResponseMsg(event, self, masterAddress), net);
                }
            };
    public Handler<ConnectMasterResponse> handleConnectMasterResponse = new Handler<ConnectMasterResponse>() {

        public void handle(ConnectMasterResponse event) {

            if (event.isSucceeded() == true) {
                logger.info("Successful connection to master from {}", self);
                connectedToMaster = true;
            } else {
                logger.warn("Failed connection to master from {}. Exiting ...", self);
                System.exit(-1);
            }
        }
    };
    public Handler<DisconnectMasterRequest> handleDisconnectMasterRequest =
            new Handler<DisconnectMasterRequest>() {

                public void handle(DisconnectMasterRequest event) {

                    logger.info("Disconnect request sending to master...");

                    trigger(event, masterClient.getPositive(MasterClientPort.class));
                }
            };
    public Handler<DaemonInit> handleInit = new Handler<DaemonInit>() {

        public void handle(DaemonInit event) {
            self = new DaemonAddress(event.getId(), event.getSelf());
            masterAddress = event.getMasterAddr();

            MasterClientConfig mc = new MasterClientConfig(
                    masterAddress,
                    event.getMasterRetryPeriod(),
                    event.getMasterRetryCount(),
                    event.getClientKeepAlivePeriod());

            trigger(new IndexerInit(event.getIndexingPeriod()), indexer.getControl());

            trigger(new MasterClientInit(self, mc),
                    masterClient.getControl());

            connect(timer, indexer.getNegative(Timer.class));

            connect(timer, masterClient.getNegative(Timer.class));
            connect(net, masterClient.getNegative(Network.class));
        }
    };
    public Handler<JobLoadResponse> handleJobLoadResponse = new Handler<JobLoadResponse>() {

        public void handle(JobLoadResponse event) {
            logger.info("JobLoadResponse {} received from Maven component: {}", event.getJobId(),
                    event.getStatus());

            // if success then remove from loadingJobs, add to loadedJobs
            if (event.getStatus() == JobLoadResponse.Status.ASSEMBLED) {
                Job job = loadingJobs.get(event.getJobId());
                loadedJobs.put(event.getJobId(), job);
            }
            // remove job from loadingJobs whatever the result.
            loadingJobs.remove(event.getJobId());

            trigger(new JobLoadResponseMsg(event, self, masterAddress), net);
        }
    };
    public Handler<JobExecResponse> handleJobExecResponse = new Handler<JobExecResponse>() {

        public void handle(JobExecResponse event) {
            if (event.getStatus() != JobExecResponse.Status.SUCCESS) {
                executingJobs.remove(event.getJobId());
            }
            trigger(new JobExecResponseMsg(event, self, masterAddress), net);
        }
    };
    public Handler<JobStartResponse> handleJobStartResponse = new Handler<JobStartResponse>() {

        public void handle(JobStartResponse event) {
            // if success, add to executingJobs
            if (event.getStatus() != JobStartResponse.Status.SUCCESS) {
                executingJobs.remove(event.getJobId());
            }

            trigger(new JobStartResponseMsg(event, self, masterAddress), net);
        }
    };
    public Handler<JobLoadRequestMsg> handleJobLoadRequest = new Handler<JobLoadRequestMsg>() {

        public void handle(JobLoadRequestMsg event) {
            logger.info("Job {} Load Request Received for " + event.getGroupId() + "." + event.getArtifactId() + "-" + event.getVersion(), event.getJobId());

            int id = event.getJobId();
            JobLoadResponse.Status status;

            // If msg not a duplicate
            if (loadingJobs.containsKey(id) == false) {
                // if job not already loaded
                if (loadedJobs.containsKey(id) == false) {
                    JobLoadRequest job;
                    try {
                        job = new JobLoadRequest(event.getGroupId(), event.getArtifactId(), event.getVersion(), event.getMainClass(), event.getArgs(), event.getRepoId(), event.getRepoUrl(),
                                event.isHideMavenOutput());

                        job.createDummyPomFile();
                        status = JobLoadResponse.Status.POM_CREATED;
                        loadingJobs.put(id, job);
                        trigger(job, mavenLauncher.getPositive(MavenPort.class));

                    } catch (DummyPomConstructionException e) {
                        // XXX store stack trace and send back in msg
//						String msg = e.printStackTrace();
                        status = JobLoadResponse.Status.FAIL;
                        trigger(new JobLoadResponseMsg(id, status, self, event.getSource()), net);
                    }
                } else {
                    logger.info("Job already loaded {}", id);
                    status = JobLoadResponse.Status.ASSEMBLED;
                    trigger(new JobLoadResponseMsg(id, status, self, event.getSource()), net);
                }
            } else {
                logger.info("Duplicate request, job already loading {}", id);
                status = JobLoadResponse.Status.DUPLICATE;
                trigger(new JobLoadResponseMsg(id, status, self, event.getSource()), net);
            }


        }
    };
    public Handler<JobExecRequestMsg> handleJobExecRequest = new Handler<JobExecRequestMsg>() {

        public void handle(JobExecRequestMsg event) {
            int jobId = event.getJobId();
            logger.info("Executing job {} at Daemon.", jobId);

            
            Job job = loadedJobs.get(jobId);

            JobExecResponse.Status status = JobExecResponse.Status.SUCCESS;
            if (job == null) {
                // see if job is loading, if yes, then wait
                job = loadingJobs.get(jobId);
                if (job == null) {
                    // need to load job first
                    status = JobExecResponse.Status.NOT_LOADED;
                }
                logger.warn("Couldn't execute Job {} . Not found at Daemon.", jobId);
                JobExecResponseMsg response =
                        new JobExecResponseMsg(jobId, status, self, event.getSource());
                trigger(response, net);
            } else {
                logger.info("Found Job {} at Daemon. Now executing.", jobId);
                JobExecRequest jobExec = new JobExecRequest(job,event);
                trigger(jobExec, mavenLauncher.getPositive(MavenPort.class));
                runningJobs.put(jobId, jobExec);
            }
        }
    };
    public Handler<JobStartRequestMsg> handleJobStartRequest = new Handler<JobStartRequestMsg>() {

        public void handle(JobStartRequestMsg event) {
            int jobId = event.getJobId();
            logger.info("Starting job {} at Daemon.", jobId);

            Job job = loadedJobs.get(jobId);

            JobStartResponse.Status status = JobStartResponse.Status.SUCCESS;
            if (job == null) {
                // see if job is loading, if yes, then wait
                job = loadingJobs.get(jobId);
                if (job == null) {
                    // need to load job first
                    status = JobStartResponse.Status.NOT_LOADED;
                }
                JobStartResponseMsg response =
                        new JobStartResponseMsg(jobId, status, self, event.getSource());
                trigger(response, net);
            } else {
                JobStartRequest jobExec = new JobStartRequest(
                        event.getNumPeers(), job, event.getSimulationScenario());
                trigger(jobExec, mavenLauncher.getPositive(MavenPort.class));
                executingJobs.put(jobId, jobExec);
            }

        }
    };
    public Handler<ShutdownDaemonRequestMsg> handleShutdownDaemonRequestMsg = new Handler<ShutdownDaemonRequestMsg>() {

        public void handle(ShutdownDaemonRequestMsg event) {

            int timeout = event.getTimeout();
            ScheduleTimeout st = new ScheduleTimeout(timeout);
            st.setTimeoutEvent(new TimerDaemonShutdown(st));
            trigger(st, timer);
        }
    };
    public Handler<JobExited> handleJobExited = new Handler<JobExited>() {

        public void handle(JobExited event) {

            logger.info("Job {} has exited with exit value {}", event.getJobId(), event.getExitValue());

            JobStartRequest job = executingJobs.remove(event.getJobId());
            JobStartRequest job2 = executingJobs.remove(event.getJobId());

            if (job != null) {
                completedJobs.put(job.getId(), job);
            } else if (job2 != null) {
                completedJobs.put(job.getId(), job);
            }

            JobExitedMsg e = new JobExitedMsg(event, self, masterAddress);
            trigger(e, net);
        }
    };
    public Handler<TimerDaemonShutdown> handleTimerDaemonShutdown = new Handler<TimerDaemonShutdown>() {

        public void handle(TimerDaemonShutdown event) {

            destroy(masterClient);
            destroy(indexer);
            destroy(mavenLauncher);

            System.exit(-1);
        }
    };
    public Handler<JobRemoveRequestMsg> handleJobRemoveRequestMsg = new Handler<JobRemoveRequestMsg>() {

        public void handle(JobRemoveRequestMsg event) {

            Job job = loadedJobs.get(event.getJobId());
            if (job == null) {
                trigger(new JobRemoveResponseMsg(event.getJobId(), JobRemoveResponseMsg.Status.NOT_LOADED,
                        self, masterAddress), net);
            } else {
                trigger(new JobRemoveRequest(job), mavenLauncher.getNegative(MavenPort.class));
            }
        }
    };
    public Handler<JobRemoveResponse> handleJobRemoveResponse = new Handler<JobRemoveResponse>() {

        public void handle(JobRemoveResponse event) {
            // Assuming that the reply is sent to the Master here.
            trigger(new JobRemoveResponseMsg(event.getJobId(), event.getStatus(),
                    event.getMsg(), self, masterAddress), net);
        }
    };
    public Handler<TimerDaemonShutdown> handleShutdownTimeout = new Handler<TimerDaemonShutdown>() {

        public void handle(TimerDaemonShutdown event) {
            System.exit(0);
        }
    };

    public final void execute(Class<? extends ComponentDefinition> main) {
        File file = null;
        try {
            file = File.createTempFile("scenario", ".bin");
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(this);
            oos.flush();
            oos.close();
            System.setProperty("scenario", file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            // cl.run(main.getCanonicalName(), null);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
    public Handler<JobStopResponse> handleJobStopResponse = new Handler<JobStopResponse>() {

        public void handle(JobStopResponse event) {


            JobStopResponseMsg response = new JobStopResponseMsg(event, self, masterAddress);
            trigger(response, net);
        }
    };
    public Handler<JobStopRequestMsg> handleJobStopRequestMsg = new Handler<JobStopRequestMsg>() {

        public void handle(JobStopRequestMsg event) {

            int id = event.getJobId();
//			JobStopResponse.Status status;
//
//			MavenLauncher.ProcessWrapper pw = executingProcesses.get(id);
//			if (pw == null) {
//				status = JobStopResponse.Status.COULD_NOT_FIND_PROCESS_HANDLE_TO_STOP_JOB;
//			} else {
//				if (pw.destroy() == true) {
//					status = JobStopResponse.Status.STOPPED;
//				} else {
//					status = JobStopResponse.Status.ALREADY_STOPPED;
//				}
//			}

//			JobStopResponseMsg response = new JobStopResponseMsg(id, status, self, event.getSource());
//			trigger(response, net);

            JobStopRequest stopJob = new JobStopRequest(id);
            trigger(stopJob, mavenLauncher.getPositive(MavenPort.class));
        }
    };
    public Handler<ListJobsLoadedRequestMsg> handleListJobsLoadedRequest = new Handler<ListJobsLoadedRequestMsg>() {

        public void handle(ListJobsLoadedRequestMsg event) {

            Set<Job> jobsLoaded = new HashSet<Job>(loadedJobs.values());
            storeJobs(jobsLoaded);

            for (Job job : jobsLoaded) {
                trigger(new JobFoundMsg(job, self, masterAddress), net);
            }
        }
    };

    private void storeJobs(Set<Job> jobsLoaded) {
        for (Job job : jobsLoaded) {
            loadedJobs.put(job.getId(), job);
        }
    }
    public Handler<ListJobsLoadedResponse> handleListJobsLoadedResponse = new Handler<ListJobsLoadedResponse>() {

        public void handle(ListJobsLoadedResponse event) {
            logger.debug("ListJobsLoadedResponse received");

            Set<Job> jobsLoaded = event.getSetJobs();
            storeJobs(jobsLoaded);

            for (Job job : jobsLoaded) {
                trigger(new JobFoundMsg(job, self, masterAddress), net);
            }
        }
    };
    public Handler<JobFound> handleJobFound = new Handler<JobFound>() {

        public void handle(JobFound event) {
            logger.debug("Job Found by Daemon.");


            loadedJobs.put(event.getId(), event);

            // Could send event to Master indicating job found here
            // Fire-and-forget, as master can query list of jobs
            trigger(new JobFoundMsg(event, self, masterAddress), net);
        }
    };
    public Handler<JobReadFromExecutingRequestMsg> handleJobReadFromExecutingRequestMsg = new Handler<JobReadFromExecutingRequestMsg>() {

        public void handle(JobReadFromExecutingRequestMsg event) {
            JobReadFromExecutingRequest req = new JobReadFromExecutingRequest(event);
            trigger(req, mavenLauncher.getPositive(MavenPort.class));
        }
    };
    public Handler<JobWriteToExecutingRequestMsg> handleJobWriteToExecutingRequestMsg =
            new Handler<JobWriteToExecutingRequestMsg>() {

                public void handle(JobWriteToExecutingRequestMsg event) {
                    JobWriteToExecutingRequest req = new JobWriteToExecutingRequest(event);
                    trigger(req, mavenLauncher.getPositive(MavenPort.class));
                }
            };
    public Handler<JobReadFromExecutingResponse> handleJobMessageResponse =
            new Handler<JobReadFromExecutingResponse>() {

                public void handle(JobReadFromExecutingResponse event) {

                    trigger(new JobReadFromExecutingResponseMsg(event, self, masterAddress), net);
                }
            };
}
