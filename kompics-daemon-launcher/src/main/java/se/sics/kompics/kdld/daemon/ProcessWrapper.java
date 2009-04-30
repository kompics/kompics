package se.sics.kompics.kdld.daemon;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class ProcessWrapper implements Runnable
{
	private final int jobId;
	
	private Process process;
	
	private BufferedWriter writeToInput;
	
	private BufferedReader readFromOutput;
	
	public ProcessWrapper(int jobId, Process process) {
		this.jobId = jobId;
		if (process == null)
		{
			throw new IllegalArgumentException("Process was null");
		}
		this.process = process;
//		executingProcesses.put(jobId, this);
	}
	
	public void run() {

		writeToInput = new BufferedWriter(new OutputStreamWriter(process
				.getOutputStream()));
		// XXX configure output to write to Socket for log4j at Master
		
		readFromOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

		
		
	}
	
	/**
	 * @return true : stopped, false : already stopped
	 */
	public synchronized boolean destroy()
	{
		if (isAlive() == false)
		{
			return false;
		}
		process.destroy();
		return true;
	}
	
	public int getJobId() {
		return jobId;
	}
	
	public boolean isAlive()
	{
		try
		{
			// XXX hack here. Shouldn't use RuntimeException 
			// to determine if process hasn't failed.
			int exitValue = process.exitValue();
		}
		catch (IllegalThreadStateException e)
		{
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
	final void input(String string) throws IOException {
		writeToInput.write(string);
		writeToInput.write("\n");
		writeToInput.flush();
	}
	
	final String readLineFromOutput() throws IOException
	{
		return readFromOutput.readLine();
	}

	final int readCharsFromOutput(char[] cbuf) throws IOException
	{
		return readFromOutput.read(cbuf);
	}
	
}