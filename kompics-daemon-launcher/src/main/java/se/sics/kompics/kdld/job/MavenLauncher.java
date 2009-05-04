package se.sics.kompics.kdld.job;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.kdld.daemon.DaemonAddress;
import se.sics.kompics.kdld.daemon.JobStopRequest;
import se.sics.kompics.kdld.daemon.JobStopResponse;
import se.sics.kompics.kdld.daemon.ProcessWrapper;
import se.sics.kompics.network.Network;
import se.sics.kompics.simulator.SimulationScenario;
import se.sics.kompics.timer.Timer;

public class MavenLauncher extends ComponentDefinition {

	public static final String KOMPICS_HOME;
	
	public static final String SCENARIO_FILENAME = "scenario";
	
	static {
		String kHome = System.getProperty("kompics.home");
		String userHome = System.getProperty("user.home");
		if (userHome != null && kHome == null) {
			System.setProperty("kompics.home", new File(userHome + "/.kompics/")
					.getAbsolutePath());
		} 
		KOMPICS_HOME = System.getProperty("kompics.home");
		PropertyConfigurator.configureAndWatch("log4j.properties");
	}
	
	private static final Logger logger = LoggerFactory.getLogger(MavenLauncher.class);

	private Positive<Network> net = positive(Network.class);

	private Negative<Maven> maven = negative(Maven.class);
	
	private Map<Integer,Job> waitingJobs = new HashMap<Integer,Job>();
	private Map<Integer,Job> executingJobs = new HashMap<Integer,Job>();

	private Map<Integer,ProcessWrapper> executingProcesses = 
		new ConcurrentHashMap<Integer, ProcessWrapper>();
	
	public MavenLauncher() {

		subscribe(handleJobAssembleRequest, maven);
		subscribe(handleJobExecRequest, maven);
		subscribe(handleJobStopRequest, maven);
	
	}
	
	public Handler<JobAssembly> handleJobAssembleRequest = new Handler<JobAssembly>() {
		public void handle(JobAssembly event) {

			int id = event.getId();
			JobAssemblyResponse.Status status;

			// If msg not a duplicate
			if (waitingJobs.containsKey(id) == false)
			{
				try {
					event.createDummyPomFile(); 
					status = JobAssemblyResponse.Status.POM_CREATED;
					waitingJobs.put(id, event);
					status = mvnAssemblyAssembly(event);
				} catch (DummyPomConstructionException e) {
					e.printStackTrace();
					status = JobAssemblyResponse.Status.FAIL;
				}
			}
			else
			{
				status = JobAssemblyResponse.Status.DUPLICATE;
			}

			trigger(new JobAssemblyResponse(event, id, status),maven);
		}
	};
	
	public Handler<JobExec> handleJobExecRequest = new Handler<JobExec>() {
		public void handle(JobExec event) {
			int id = event.getId();
			Job job = waitingJobs.get(id);
			
			JobExecResponse.Status status = JobExecResponse.Status.SUCCESS;
			if (job == null)
			{
				status = JobExecResponse.Status.FAIL;
			}
			else
			{
				status = mvnExecExec(event, event.getScenario());
			}
			
			
			JobExecResponse response = new JobExecResponse(event, id,status);
			trigger(response,net);
		}
	};

	private JobAssemblyResponse.Status mvnAssemblyAssembly(JobAssembly job)
	{
		JobAssemblyResponse.Status status;
		int res = forkProcess(job, null, true);
		switch (res)
		{
		case 0: status = JobAssemblyResponse.Status.ASSEMBLED; break;
		case -1: status = JobAssemblyResponse.Status.FAIL; break;
		default:
			status = JobAssemblyResponse.Status.FAIL; break;
		}
		return status;
	}

	private JobExecResponse.Status mvnExecExec(JobExec job, SimulationScenario scenario)
	{
		JobExecResponse.Status status;
		int res = forkProcess(job, scenario, false);
		switch (res)
		{
		case 0: status = JobExecResponse.Status.SUCCESS; break;
		case -1: status = JobExecResponse.Status.FAIL; break;
		default: status = JobExecResponse.Status.FAIL; break;
		}
		return status;
	}
	
	/**
	 * @param id
	 * @param job
	 * @param scenario
	 * @param assembly
	 * @return 0 on success, -1 on failure.
	 */
	private int forkProcess(Job job, SimulationScenario scenario,
			boolean assembly)
	{
		int res=0;
		String[] args = {};
		
	
		String classPath = System.getProperty("java.class.path");
		java.util.List<String> command = new ArrayList<String>();
		command.add("java");
		command.add("-classpath");
		command.add(classPath); // the Slave jar file should be on this path
		command.add("-Dlog4j.properties=log4j.properties");
		command.add("-DKOMPICS_HOME="
				+ KOMPICS_HOME);
		command.add(job.getMainClass());
		command.add(job.getGroupId());
		command.add(job.getArtifactId());
		String assembleC;
		assembleC = assembly ? "assemble=true" : "assemble=false"; 
		command.add(assembleC);
		command.addAll(job.getArgs());


		ProcessBuilder processBuilder = new ProcessBuilder(command);
		
		processBuilder.redirectErrorStream(true);
		Map<String,String> env = processBuilder.environment();
		env.put("KOMPICS_HOME", KOMPICS_HOME);
		
		if (scenario != null)
		{
			try {
				File file = File.createTempFile(SCENARIO_FILENAME, ".bin");
				ObjectOutputStream oos = new ObjectOutputStream(
						new FileOutputStream(file));
				oos.writeObject(this);
				oos.flush();
				oos.close();
				env.put(SCENARIO_FILENAME, file.getAbsolutePath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try {
			Process p = processBuilder.start();
			ProcessWrapper pw = new ProcessWrapper(job.getId(), p);
			executingProcesses.put(job.getId(), pw);
			new Thread(pw).start();
			
		} catch (IOException e1) {
			e1.printStackTrace();
			res = -1;
		}
		
		waitingJobs.remove(job.getId());
		executingJobs.put(job.getId(), job);

		return res;
	}
	
	
	public Handler<JobStopRequest> handleJobStopRequest = new Handler<JobStopRequest>() {
		public void handle(JobStopRequest event) {

			int id = event.getJobId();
			JobStopResponse.Status status;
			
			ProcessWrapper pw = executingProcesses.get(id);
			if (pw == null)
			{
				status = JobStopResponse.Status.FAILED_TO_STOP;
			}
			else
			{
				if (pw.destroy() == true) {
					status = JobStopResponse.Status.STOPPED;
				}
				else {
					status = JobStopResponse.Status.ALREADY_STOPPED;
				}
			}
			
			JobStopResponse response = new JobStopResponse(id,status, 
					new DaemonAddress(event.getDaemonId(), event.getDestination()),
					event.getSource());
			trigger(response,net);
		}
	};
	
	
//	public Handler<JobMessageRequest> handleJobMessageRequest = new Handler<JobMessageRequest>() {
//		public void handle(JobMessageRequest event) {
//			int id = event.getJobId();
//			JobMessageResponse.Status status;
//			ProcessWrapper pw = executingProcesses.get(id);
//			if (pw == null)
//			{
//				status = JobMessageResponse.Status.STOPPED;
//			}
//			else
//			{
//				try {
//					pw.input(event.getMsg());
//				
//					status = JobMessageResponse.Status.SUCCESS;
//				}
//				catch (IOException e)
//				{
//					status = JobMessageResponse.Status.FAIL;
//				}
//			}
//			
//			JobMessageResponse response = new JobMessageResponse(id,status, 
//					new DaemonAddress(daemonId, event.getDestination()),
//					event.getSource());
//			trigger(response,net);
//		}
//	};
	
	
}
