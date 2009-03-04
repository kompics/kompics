
package se.sics.kompics.kdld.main.event;

import se.sics.kompics.Event;

/**
 * @author jdowling
 *
 */
public class MavenCommandRequest extends Event {

	public final String[] mvnCommands = { "compile", "install", "install:install-file"};
	
	private final String params;
	
	private final String command;
	
	public MavenCommandRequest(String command, String params) {
		this.command = command;
		
		boolean validCommand = false;
		for (String mvn : mvnCommands)
		{
			if (command.compareTo(mvn) == 0)
			{
				validCommand = true;
				break;
			}
		}
		if (validCommand == false)	
		{
			throw new IllegalArgumentException("Invalid maven command: " + command);
		}
		
		this.params = params;
	}

	public String getCommand() {
		return command;
	}
	
	public String getParams() {
		return params;
	}
}