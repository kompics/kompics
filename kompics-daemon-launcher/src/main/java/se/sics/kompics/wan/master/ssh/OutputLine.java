package se.sics.kompics.wan.master.ssh;

import java.util.HashMap;
import java.util.Map;

import se.sics.kompics.wan.master.plab.rpc.RpcServer;


public class OutputLine {

	public static final String TYPE_STDERR = "stderr";

	public static final String TYPE_STDOUT = "stdout";

	public static final String TYPE_CONTROLL = "controll";
	public static final String TYPE_CONTROLL_ERR = "controll_err";
	public static final String TYPE_COMMAND = "command";
	
	private String line;

	private double time;

	private String type;

	private CommandSpec command;

	public OutputLine(String line_, String type, CommandSpec command_) {
		super();
		this.line = line_;
		this.type = type;
		this.time = RpcServer.getTime();
		this.command = command_;
	}

	public String getLine() {
		return line;
	}

	public boolean isStderr() {
		return type.equals(OutputLine.TYPE_STDERR);
	}

	public double getTime() {
		return time;
	}

	public CommandSpec getCommand() {
		return command;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("command_id", command.getCommandId());
		map.put("command", command.getCommand());
		map.put("time", time);
		map.put("type", type);
		map.put("exit_code", command.getExitCodeString());
		map.put("execution_time", command.getExecutionTime());
		//System.out.println("line=" + line);
		map.put("line", this.getLine());
		return map;
	}
}
