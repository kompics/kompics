package se.sics.kompics.wan.master.scp;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.wan.master.ssh.SshComponent;

public class ScpComponent extends ComponentDefinition {

	private Component sshComponent;
	
	private Negative<ScpPort> scpPort;
	
	public ScpComponent() {
		this.sshComponent = create(SshComponent.class);
		subscribe(handleScpCopyFileTo, scpPort);
	}
	

	private Handler<ScpCopyFileTo> handleScpCopyFileTo = new Handler<ScpCopyFileTo>() {
		public void handle(ScpCopyFileTo event) {
			
			// check MD5 hash of all files in dir, compare with remote dir
			
			// trigger event to sshComponent
			
			
		}
	};

}
