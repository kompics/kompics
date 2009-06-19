package se.sics.kompics.wan.master.scp.upload;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import se.sics.kompics.wan.master.scp.FileInfo;
import se.sics.kompics.wan.master.scp.MD5Check;
import se.sics.kompics.wan.master.scp.RemoteDirMD5Info;
import se.sics.kompics.wan.master.scp.ScpCopyThread;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import se.sics.kompics.wan.master.ssh.SshComponent;
import se.sics.kompics.wan.master.ssh.SshConnection;
import ch.ethz.ssh2.Session;


/**
 * Class that takes a hostname and a file => md5 map and compares it to the
 * files on the hostname
 * 
 * @author isdal
 * 
 */

public class UploadMD5CheckThread implements MD5Check {

	private final ScpCopyThread scpCopyThread;

	private final CommandSpec commandResults;

	private final Thread scpCopyThreadThread;

	private final SshComponent.SshConn sshConn;

	private final LinkedBlockingQueue<FileInfo> fileMD5Hashes = new LinkedBlockingQueue<FileInfo>();

	// private final Vector<CommandSpec> results = new
	// Vector<CommandSpec>();

	private int currentFile = 0;

	private boolean success = false;

	private boolean quit = false;

	public UploadMD5CheckThread(SshComponent.SshConn sshConn,
			List<FileInfo> fileMD5Hashes, CommandSpec commandSpec) {
		this.sshConn = sshConn;
		this.commandResults = commandSpec;

		for (FileInfo info : fileMD5Hashes) {
			this.fileMD5Hashes.add(info.clone());
		}

		scpCopyThread = new ScpCopyThread(sshConn, this);
		scpCopyThreadThread = new Thread(scpCopyThread);
		scpCopyThreadThread.setName("ScpThread: " + sshConn.getExpHost());
		scpCopyThreadThread.start();

	}

	public void run() {
		int copyCount = 0;
		Session session = null;
		if (null != (session = sshConn.startShell())) {
			commandResults.started();
			RemoteDirMD5Info remoteMD5Checker = new RemoteDirMD5Info(sshConn);
			while (!quit) {
				FileInfo file;
				try {
					file = fileMD5Hashes.take();

					boolean md5match = remoteMD5Checker.checkRemoteFile(session,
							commandResults, file);
					// System.out.println(commandSpec.toString());

					if (!md5match) {

						// TODO create dir, but maybe not always
						sshConn.runCommand(new CommandSpec("mkdir -p "
								+ file.getRemoteDir(), 0, -1, false), session);

						if (file.getCopyCount() < 3) {
							scpCopyThread.copyFile(file);
							copyCount++;
						}
					}
					currentFile++;

					// check if we are done (all queues are empty)
					boolean scpDone = scpCopyThread.isDone();
					boolean md5Done = fileMD5Hashes.isEmpty();
					if (scpDone && md5Done) {
						System.out.println("all done");
						quit = true;
						scpCopyThread.halt();
						scpCopyThreadThread.interrupt();
					}
				} catch (InterruptedException e) {
					// ignore, probably shutdown
				} catch (IOException e) {
					if (e.getMessage().contains("SSH channel closed")) {
						// ignore, probably happened in the shell session to...

					}
				}
			}
		}
		session.close();
		try {
			scpCopyThreadThread.join();
			success = true;
			commandResults.setExitCode(0, "copied " + copyCount);
			System.out.print(sshConn.getExpHost() + ": done MD5");
		} catch (InterruptedException e) {

		}
	}

	public void checkFile(FileInfo file) {
		try {
			fileMD5Hashes.put(file);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean wasSuccess() {
		return success;
	}

}