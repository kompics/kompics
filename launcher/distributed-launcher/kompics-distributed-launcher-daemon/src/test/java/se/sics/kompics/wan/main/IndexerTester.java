package se.sics.kompics.wan.main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import java.util.logging.Level;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.DaemonConfiguration;
import se.sics.kompics.wan.daemon.indexer.IndexPort;
import se.sics.kompics.wan.daemon.indexer.IndexShutdown;
import se.sics.kompics.wan.daemon.indexer.Indexer;
import se.sics.kompics.wan.daemon.indexer.IndexerInit;
import se.sics.kompics.wan.daemon.maven.Maven;
import se.sics.kompics.wan.daemon.maven.MavenPort;
import se.sics.kompics.wan.job.DummyPomConstructionException;
import se.sics.kompics.wan.job.Job;
import se.sics.kompics.wan.job.JobExited;
import se.sics.kompics.wan.job.JobFound;
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
import se.sics.kompics.wan.job.JobToDummyPom;

/**
 * Unit test for simple App.
 */
public class IndexerTester implements Serializable {

    private static final long serialVersionUID = -8704832589507459009L;
    public static Semaphore semaphore = new Semaphore(0);
    public static final int EVENT_COUNT = 1;
    public static SimulationScenario scenario = new SimulationScenario() {

        private static final long serialVersionUID = -5355642917108165919L;
    };

    private boolean result=false;

    public IndexerTester() {
    }

    public static class JobStopTimeout extends Timeout {

        private final int jobId;

        public JobStopTimeout(int jobId, ScheduleTimeout request) {
            super(request);
            this.jobId = jobId;
        }

        public int getJobId() {
            return jobId;
        }
    }

    public static class ExecReadTimeout extends Timeout {

        private final int jobId;

        public ExecReadTimeout(int jobId, ScheduleTimeout request) {
            super(request);
            this.jobId = jobId;
        }

        public int getJobId() {
            return jobId;
        }
    }

    public static class TestIndexerComponent extends ComponentDefinition implements Serializable {

        private static final long serialVersionUID = -5967918118211382L;
        public Logger logger = LoggerFactory.getLogger(TestIndexerComponent.class);
        public Component indexer;
        public Component timer;
        private Component mavenLauncher;
        public Map<Integer, Job> loadedJobs = new HashMap<Integer, Job>();
        private static IndexerTester testObj = null;
        private JobToDummyPom dummy;
        private final HashSet<UUID> outstandingTimeouts;

        public TestIndexerComponent() {

            if (testObj == null) {
                throw new IllegalStateException(
                        "Test object should be set before calling component");
            }

            outstandingTimeouts = new HashSet<UUID>();

            indexer = create(Indexer.class);
            timer = create(JavaTimer.class);
            mavenLauncher = create(Maven.class);

            connect(indexer.getNegative(Timer.class), timer.getPositive(Timer.class));


            subscribe(handleListJobsLoadedResponse, indexer.getPositive(IndexPort.class));
//            subscribe(handleJobFoundLocally, indexer.getPositive(IndexPort.class));

            subscribe(handleJobAssemblyResponse, mavenLauncher.getPositive(MavenPort.class));
            subscribe(handleJobExecResponse, mavenLauncher.getPositive(MavenPort.class));
            subscribe(handleJobReadFromExecutingResponse, mavenLauncher.getPositive(MavenPort.class));

            subscribe(handleExecReadTimeout, timer.getPositive(Timer.class));
            subscribe(handleJobStopTimeout, timer.getPositive(Timer.class));
            subscribe(handleJobStopResponse, mavenLauncher.getPositive(MavenPort.class));
            subscribe(handleJobExited, mavenLauncher.getPositive(MavenPort.class));

            subscribe(handleStart, control);

            trigger(new IndexerInit(5000), indexer.getControl());

            logger.info("Initializing the Indexer");
        }

        public static void setTestObj(IndexerTester testObj) {
            TestIndexerComponent.testObj = testObj;
        }
        public Handler<Start> handleStart = new Handler<Start>() {

            public void handle(Start event) {
                logger.info("Starting TestIndexer");

                try {

                    List<String> args = new ArrayList<String>();
                    args.add("0");

//                    dummy = new JobToDummyPom("se.kth.id2210",
//                            "lab1", "0.0.1-SNAPSHOT",
//                            "main.ChordExperiments", args,
//                            "sics-snapshot", "http://kompics.sics.se/maven/snapshotrepository");

                    dummy = new JobToDummyPom("se.sics.kompics",
                            "kompics-manual", "0.4.0",
                            "se.sics.kompics.manual.example1.Root", args,
                            "sics-release", "http://kompics.sics.se/maven/repository");

                    dummy.createDummyPomFile();

                    logger.info("Creating a dummy pom: {}", dummy.getPomFilename());

                    trigger(new JobLoadRequest(dummy, false), mavenLauncher.getPositive(MavenPort.class));

                } catch (DummyPomConstructionException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }
        };
        public Handler<JobStartResponse> handleJobExecResponse = new Handler<JobStartResponse>() {

            public void handle(JobStartResponse event) {

                logger.info("Received job execResponse from job-id: {} ", event.getJobId());


                ScheduleTimeout st = new ScheduleTimeout(5000);
                ExecReadTimeout execReadTimeout = new ExecReadTimeout(event.getJobId(), st);
                st.setTimeoutEvent(execReadTimeout);

                UUID timerId = execReadTimeout.getTimeoutId();
                outstandingTimeouts.add(timerId);

                trigger(st, timer.getPositive(Timer.class));

            }
        };
        public Handler<ExecReadTimeout> handleExecReadTimeout = new Handler<ExecReadTimeout>() {

            public void handle(ExecReadTimeout event) {

                if (!outstandingTimeouts.contains(event.getTimeoutId())) {
                    return;
                }
                outstandingTimeouts.remove(event.getTimeoutId());


                logger.debug("Retrying to read from job: " + event.getJobId());
                trigger(new JobReadFromExecutingRequest(event.getJobId()), mavenLauncher.getPositive(MavenPort.class));

                ScheduleTimeout st = new ScheduleTimeout(2000);
                st.setTimeoutEvent(new JobStopTimeout(event.getJobId(), st));
                trigger(st, timer.getPositive(Timer.class));
            }
        };
        public Handler<JobStopTimeout> handleJobStopTimeout = new Handler<JobStopTimeout>() {

            public void handle(JobStopTimeout event) {
                logger.warn("Trying to stop a job: " + event.getJobId());
                trigger(new JobStopRequest(event.getJobId()), mavenLauncher.getPositive(MavenPort.class));


            }
        };
        public Handler<JobExited> handleJobExited = new Handler<JobExited>() {

            public void handle(JobExited event) {
                logger.debug("Job exited: " + event.getJobId());

                trigger(new JobRemoveRequest(dummy), mavenLauncher.getPositive(MavenPort.class));
            }
        };
        public Handler<JobStopResponse> handleJobStopResponse = new Handler<JobStopResponse>() {

            public void handle(JobStopResponse event) {
                logger.debug("Job stopped : " + event.getJobId() + " : " + event.getStatus() +
                        " : " + event.getMsg());

                trigger(new IndexShutdown(), indexer.getPositive(IndexPort.class));
                trigger(new Stop(), indexer.getControl());

                TestIndexerComponent.testObj.resultOfTest(true, true);
            }
        };
        public Handler<JobReadFromExecutingResponse> handleJobReadFromExecutingResponse = new Handler<JobReadFromExecutingResponse>() {

            public void handle(JobReadFromExecutingResponse event) {
                logger.info("Read from job id:" + event.getJobId() + " - " + event.getMsg());
            }
        };
        public Handler<JobLoadResponse> handleJobAssemblyResponse = new Handler<JobLoadResponse>() {

            public void handle(JobLoadResponse event) {

                logger.debug("assembly:assembly response for job: " + event.getJobId());

                // if success then remove from loadingJobs, add to loadedJobs
                if (event.getStatus() != JobLoadResponse.Status.ASSEMBLED) {
                    logger.warn("Failed to assemble job: " + event.getJobId());
                    trigger(new IndexShutdown(), indexer.getPositive(IndexPort.class));
                    testObj.resultOfTest(false, true);
                }

            }
        };
        public Handler<JobFound> handleListJobsLoadedResponse = new Handler<JobFound>() {

            public void handle(JobFound event) {

                    if (loadedJobs.containsKey(event.getId()) == true) {
                        logger.info("Found already loaded job: {}", event.getId());
                    } else {
                        logger.info("Found new job {}", event.getId());

                    }
                    trigger(new JobStartRequest(1, event, scenario), mavenLauncher.getPositive(MavenPort.class));


            }
        };
//        public Handler<JobsFound> handleJobFoundLocally = new Handler<JobsFound>() {
//
//            public void handle(JobsFound event) {
//
//                for (Job job : event.getSetJobs()) {
//                    int id = job.getId();
//
//                    logger.info("Received job {} found locally.", id);
//                    try {
//                        Thread.currentThread().sleep(10 * 1000);
//                    } catch (InterruptedException ex) {
//                        java.util.logging.Logger.getLogger(IndexerTester.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//
//                    if (loadedJobs.containsKey(id) == false) {
//                        loadedJobs.put(id, job);
//                        logger.info("Added job {} to loaded jobs set.", id);
//                        trigger(new ListJobsLoadedRequest(),
//                                indexer.getPositive(IndexPort.class));
//                    }
//                }
//
//            }
//        };

        private void removeJob(int jobId) {
            Job job = loadedJobs.get(jobId);
            trigger(new JobRemoveRequest(job), mavenLauncher.getNegative(MavenPort.class));
        }
        public Handler<JobRemoveResponse> handleJobRemoveResponse = new Handler<JobRemoveResponse>() {

            public void handle(JobRemoveResponse event) {

                logger.info("Job remove response was:" + event.getMsg() + " - " + event.getStatus());

                trigger(new IndexShutdown(), indexer.getNegative(IndexPort.class));

                trigger(new Stop(), indexer.getControl());
            }
        };
    }

    @org.junit.Test
    public void testIndexer() {

        TestIndexerComponent.setTestObj(this);

        String[] args = {"-indexingPeriod", "2"};
        try {
            Configuration.init(args, DaemonConfiguration.class);
            Kompics.createAndStart(TestIndexerComponent.class, 2);
        } catch (ConfigurationException ex) {
            java.util.logging.Logger.getLogger(IndexerTester.class.getName()).log(Level.SEVERE, null, ex);
        }

        try {
            IndexerTester.semaphore.acquire(EVENT_COUNT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assert(result);

    }

    public void resultOfTest(boolean res, boolean exitTest) {
//        org.junit.Assert.assertTrue(res);

        setResult(res);

        if (exitTest == true) {
            IndexerTester.semaphore.release();
        }
    }

    private void setResult(boolean res)
    {
        result = res;
    }

}
