/**
 * Class that calculates the md5 of all files in a remote directory, recursive
 */

package se.sics.kompics.wan.master.scp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import se.sics.kompics.wan.master.ssh.CommandSpec;
import se.sics.kompics.wan.master.ssh.SshComponent;
import ch.ethz.ssh2.Session;

public class RemoteDirMD5Info {

	private final SshComponent.SshConn sshConn;

	public RemoteDirMD5Info(SshComponent.SshConn sshConn) {
		this.sshConn = sshConn;

	}

	public List<FileInfo> getRemoteFileList(String remoteDir, String filter,
			CommandSpec baseCommand) throws IOException, InterruptedException {
		baseCommand.started();
		baseCommand.receivedData("getting remote file list");
		CommandSpec command = this.generateCommand(remoteDir, filter);
		Session session = null;
		ArrayList<FileInfo> remoteFiles = new ArrayList<FileInfo>();
		// System.out.println("Starting shell");
		
		
//		if (null != (session = sshConn.startShell())) {
//			// System.out.println("Running command: " + command.getCommand());
//			sshConn.runCommand(command, session);
//			int numFiles = command.getLineNum();
//			// System.out.println("got " + numFiles + " lines");
//			for (int i = 1; i < numFiles; i++) {
//				String line = command.getProcLine(i);
//				int index = line.indexOf(" ");
//
//				if (index > 0) {
//					String md5 = line.substring(0, index);
//					String path = line.substring(index + 2);
//					// System.out.println(line);
//					// System.out.println(md5 + "." + path);
//					remoteFiles.add(new FileInfo(path, md5, sshConn
//							.getExpHost().getHostname()));
//				}
//
//			}
//			session.close();
//		}
		baseCommand.receivedData("calculated md5 of " + remoteFiles.size()
				+ " files");
		baseCommand.setExitCode(0);

		return remoteFiles;
	}

	public boolean checkRemoteFile(Session session, CommandSpec commandResults,
			FileInfo file) throws IOException, InterruptedException {

		CommandSpec commandSpec = this.md5CheckCommand(file);
//		if (sshConn.runCommand(commandSpec, session) < 0) {
//			// timeout or killed...
//
//			return false;
//		}
		boolean md5match = false;
		// does the file exists? md5sum returns 0 on success
		if (commandSpec.getExitCode() == 0) {
			// does the md5 match?
			String localMD5 = file.getMd5();
			String remoteMD5 = commandSpec.getProcLine(1).split(" ")[0];
			// System.out.println("checking "
			// + file.getRemoteFileName());
			if (localMD5.equals(remoteMD5)) {
				md5match = true;
				// System.out.println("passed");
				commandResults.receivedControlData("passed: "
						+ file.getFullRemotePath());
			} else {
				commandResults.receivedControlErr("copying (md5 failed):"
						+ file.getFullRemotePath());
			}
			// System.out.println("size: "
			// + commandSpec.getProcOutput(0).size());
		} else {
			commandResults.receivedControlErr("copying (missing):"
					+ file.getFullRemotePath());
		}
		return md5match;

	}

	private CommandSpec md5CheckCommand(FileInfo file) {
		return new CommandSpec("md5sum " + file.getFullRemotePath(), 0, 0,
				false);
	}

	private CommandSpec generateCommand(String remoteDir, String filter) {
		if (filter != null && filter != "") {
			return new CommandSpec("md5sum `find " + remoteDir + " | grep "
					+ filter + "` 2> /dev/null", 0, -1, false);
		} else {
			return new CommandSpec("md5sum `find " + remoteDir
					+ "` 2> /dev/null", 0, -1, false);
		}

	}

}
