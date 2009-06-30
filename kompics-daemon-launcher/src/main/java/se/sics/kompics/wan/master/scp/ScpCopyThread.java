package se.sics.kompics.wan.master.scp;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.ssh.SshComponent;
import ch.ethz.ssh2.SCPClient;


/**
 * class that copies files to a hostname using scp, one file at the time
 * 
 * @author isdal
 * 
 */
public class ScpCopyThread implements Runnable {

	private final SshComponent.SshConn sshConn;

	private final LinkedBlockingQueue<FileInfo> fileQueue = new LinkedBlockingQueue<FileInfo>();

	private MD5Check checkThread;

	private volatile boolean quit = false;

	private volatile boolean working = false;

	public ScpCopyThread(SshComponent.SshConn sshConn, MD5Check checkThread) {
		this.sshConn = sshConn;
		this.checkThread = checkThread;

	}

	public void run() {

		SCPClient scpClient = null;
		try {
			scpClient = sshConn.getConnection().createSCPClient();
		} catch (IOException e2) {
			System.err.println(sshConn.getExpHost().getHostname()
					+ ": could not create ssh client");
			return;
		}

		while (!quit) {

			try {
				
				FileInfo fileInfo = fileQueue.take();
				working = true;

				PlanetLabConfiguration.getNetworkIntensiveTicket();

				if (fileInfo.isLocalFile()) {
					// System.out.println("uploading(" + available + "): "
					// + fileInfo.toString());
					try {
						if(!sshConn.isConnected()){
							System.out.println("connection problem to: '" + sshConn.getExpHost().getHostname() + "' aborting copy");
							PlanetLabConfiguration.releaseNetworkIntensiveTicket();
							return;
						}
						scpClient.put(fileInfo.getLocalFile()
								.getCanonicalPath(), fileInfo.getRemoteDir(),
								"0644");
					} catch (IOException e1) {
						// some problem with the scp client
						if (e1.getMessage().contains(
								"Error during SCP transfer")) {
							System.out.println(sshConn.getExpHost().getHostname()
									+ ": disconnected during SCP transfer");
							System.err.println(e1.getCause().getMessage());
						}
					}
					fileInfo.increaseCopyCount();
					checkThread.checkFile(fileInfo);
				} else {
					// this is a remote file
					// System.out.println("downloading(" + available + "):"
					// + fileInfo.toString());
					// System.out.println("using localfile: "
					// + fileInfo.getLocalFile().getCanonicalPath());
					if (fileInfo.createLocalDirectoryStructure()) {
						if(!sshConn.isConnected()){
							System.out.println("connection problem to: '" + sshConn.getExpHost().getHostname() + "' aborting copy");
							PlanetLabConfiguration.releaseNetworkIntensiveTicket();
							return;
						}
						BufferedOutputStream localFile = new BufferedOutputStream(
								new FileOutputStream(fileInfo.getLocalFile()));
						// System.out.println("created file");
						try {
							scpClient.get(fileInfo.getFullRemotePath(),
									localFile);
						} catch (IOException e1) {
							// some problem with the scp client
							if (e1.getMessage().contains(
									"Error during SCP transfer")) {
								System.out.println(sshConn.getExpHost().getHostname()
										+ ": disconnected during SCP transfer");
								System.err.println(e1.getCause().getMessage());
							}
						}
						fileInfo.increaseCopyCount();
						localFile.close();
						checkThread.checkFile(fileInfo);
					} else {
						System.out.println("could not create local directory: "
								+ fileInfo.getLocalDirectory());
					}

				}
				working = false;

				// release the copy ticket
				PlanetLabConfiguration.releaseNetworkIntensiveTicket();

			} catch (InterruptedException e) {
				// ignore interupts... probably means that we should
				// quit
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void halt() {
		this.quit = true;
	}

	public void copyFile(FileInfo fileInfo) {
		fileQueue.add(fileInfo);
	}

	public boolean isDone() {
		return fileQueue.size() == 0 && !working;
	}

}
