package se.sics.kompics.wan.master.ssh;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import se.sics.kompics.wan.master.plab.rpc.RpcServer;


public class CommandSpec implements Serializable {

	// private Integer outputReadPos = 0;

	private static final long serialVersionUID = -6608381371840392678L;

	public static final int RETURN_KILLED = -1;

	public static final int RETURN_TIMEDOUT = -2;

	private double startTime = 0.0;

	private double lastDataTime = -1;

	private double completionTime = -1;

	private int exitCode;

	private String exitCodeString = "";

	private Vector<OutputLine> procOutput;

	private final String command;

	private final double timeout;

	private final int commandId;

	private final boolean stopOnError;

	private boolean started = false;

	private boolean killed = false;

	public CommandSpec(String command, double timeout, int commandId,
			boolean stopOnError) {
		// this.startTime=System.currentTimeMillis();
		this.command = command;
		this.timeout = timeout;
		this.procOutput = new Vector<OutputLine>();
		this.commandId = commandId;
		
		this.exitCode = -1;
		this.stopOnError = stopOnError;
	}

	public void started() {
		this.startTime = RpcServer.getTime();
		this.lastDataTime = RpcServer.getTime();
		this.started = true;
		// add the command (to simulate console echo
		procOutput.add(new OutputLine(command, OutputLine.TYPE_COMMAND, this));
	}

	public boolean isKilled() {
		return killed;
	}

	public void kill() {
		killed = true;
	}

	public boolean isStarted() {
		return started;
	}

	public void recievedControllData(String line) {
		synchronized (procOutput) {
			this.procOutput.add(new OutputLine(line, OutputLine.TYPE_CONTROLL,
					this));
			this.lastDataTime = RpcServer.getTime();
		}
	}

	public void recievedControllErr(String line) {
		synchronized (procOutput) {
			this.procOutput.add(new OutputLine(line,
					OutputLine.TYPE_CONTROLL_ERR, this));
			this.lastDataTime = RpcServer.getTime();
		}
	}

	public void receivedData(String line) {
		synchronized (procOutput) {
			this.procOutput.add(new OutputLine(line, OutputLine.TYPE_STDOUT,
					this));
			this.lastDataTime = RpcServer.getTime();
		}
	}

	public void receivedErr(String line) {
		synchronized (procOutput) {
			this.procOutput.add(new OutputLine(line, OutputLine.TYPE_STDERR,
					this));
			this.lastDataTime = RpcServer.getTime();
		}
	}

	public String getCommand() {
		return command;
	}

	public double getTimeout() {
		return timeout;
	}

	public Map<String, Object> getCommandStats() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("command", this.getCommand());
		map.put("exit", this.getExitCodeString());
		map.put("time", this.getExecutionTime());
		return map;
	}

	public boolean isTimedOut() {

		if (isCompleted() || timeout == 0) {
			return false;
		} else {
			return getExecutionTime() > timeout;
		}

	}

	public boolean isCompleted() {
		return completionTime > 0;
	}

	public double getExecutionTime() {
		if (!isStarted()) {
			return 0.0;
		}
		if (isCompleted()) {
			return completionTime - startTime;
		} else {
			return Math.max(0, RpcServer.getTime() - startTime);
		}
	}

	public int getExitCode() {
		return exitCode;
	}

	public String getExitCodeString() {
		String ret = "";
		if (exitCode >= 0) {
			if (exitCodeString.equals("")) {
				ret = exitCode + "";
			} else {
				ret = exitCode + " '" + exitCodeString + "'";
			}
		}
		return ret;
	}

	public void setExitCode(int code, String exitCodeString) {
		this.completionTime = RpcServer.getTime();
		this.exitCode = code;
		this.exitCodeString = exitCodeString;
	}

	public void setExitCode(int code) {
		setExitCode(code, "");
	}

	public String getProcLine(int lineNum) {
		if (lineNum < 0 || lineNum >= procOutput.size()) {
			return null;
		}
		return procOutput.get(lineNum).getLine();
	}

	public String getLastLine() {
		if (procOutput.size() < 1) {
			return "";
		}
		return procOutput.get(procOutput.size() - 1).getLine();
	}

	public List<Map> getProcOutput(int startPos) {

		List<Map> outputLines = new Vector<Map>();
		for (int i = startPos; i < procOutput.size(); i++) {
			outputLines.add(procOutput.get(i).toMap());
		}
		return outputLines;
	}

	public int getLineNum() {
		return procOutput.size();
	}

	public int getCommandId() {
		return commandId;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(command + ":" + exitCode + ":" + getProcLine(1));

		return buf.toString();

	}

	public boolean isStopOnError() {
		return stopOnError;
	}

	public double getLastDataTime() {
		return lastDataTime;
	}
}