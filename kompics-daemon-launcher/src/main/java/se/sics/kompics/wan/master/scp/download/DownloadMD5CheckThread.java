package se.sics.kompics.wan.master.scp.download;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import se.sics.kompics.wan.master.scp.FileInfo;
import se.sics.kompics.wan.master.scp.LocalDirMD5Info;
import se.sics.kompics.wan.master.scp.MD5Check;
import se.sics.kompics.wan.master.scp.ScpCopyThread;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import se.sics.kompics.wan.master.ssh.SshConnection;
import ch.ethz.ssh2.Session;


/**
 * Class that takes a hostname and a file => md5 map and compares it to the
 * files on the hostname
 * 
 * @author isdal
 * 
 */

public class DownloadMD5CheckThread implements MD5Check {

	private final ScpCopyThread scpCopyThread;

	private final CommandSpec commandResults;

	private final Thread scpCopyThreadThread;

	private final SshConnection sshConn;

	private final LinkedBlockingQueue<FileInfo> fileMD5Hashes = new LinkedBlockingQueue<FileInfo>();

	private int currentFile = 0;

	private boolean success = false;

	private boolean quit = false;

	public DownloadMD5CheckThread(SshConnection sshConn,
			List<FileInfo> fileMD5Hashes, CommandSpec commandSpec) {
		this.sshConn = sshConn;
		this.commandResults = commandSpec;

		for (FileInfo info : fileMD5Hashes) {
			this.fileMD5Hashes.add(info.clone());
		}

		scpCopyThread = new ScpCopyThread(sshConn, this);
		scpCopyThreadThread = new Thread(scpCopyThread);
		scpCopyThreadThread.setName("ScpThread: " + sshConn.getHostname());
		scpCopyThreadThread.start();

	}

	public void run() {
		int copyCount = 0;
		Session session = null;
		if (null != (session = sshConn.startShell())) {
			commandResults.started();
			while (!quit) {
				FileInfo file;
				try {
					file = fileMD5Hashes.take();

					// check if the file exists locally
					LocalDirMD5Info localMD5checker = LocalDirMD5Info
							.getInstance();
					String localMD5 = localMD5checker.getFileMD5(file
							.getLocalFile());

					String remoteMD5 = file.getMd5();
					 System.out.println("local_: " + localMD5);
										System.out.println("remote: " + remoteMD5);
					
					boolean md5match = remoteMD5.equals(localMD5);

					// localMD5 is null if file not found
					if (localMD5 != null) {
						// does the md5 match?
						if (md5match) {
							// System.out.println("passed");
							commandResults.recievedControllData("passed: "
									+ file.getFullRemotePath() + " -> "
									+ file.getLocalFile().getCanonicalPath());
						} else {
							commandResults
									.recievedControllErr("copying (md5 failed):"
											+ file.getFullRemotePath()
											+ " -> "
											+ file.getLocalFile()
													.getCanonicalPath());
						}
						// System.out.println("size: "
						// + commandSpec.getProcOutput(0).size());
					} else {
						commandResults.recievedControllErr("copying (missing): "
								+ file.getFullRemotePath()
								+ " -> "
								+ file.getLocalFile()
										.getCanonicalPath());
					}
					// System.out.println(commandSpec.toString());

					if (!md5match) {

						// TODO create dir, but maybe not always
						file.getLocalFile();
						if (file.getCopyCount() < 3) {
							// System.out.println("sending to copy thread: "
							//									+ file.getFullRemotePath());
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
					// ignore, application shutdown 
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
			System.out.print(sshConn.getHostname() + ": done MD5");
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