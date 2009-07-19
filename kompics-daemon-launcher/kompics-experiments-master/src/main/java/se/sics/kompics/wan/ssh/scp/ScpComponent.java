package se.sics.kompics.wan.ssh.scp;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.ssh.scp.events.ScpGetFileRequest;
import se.sics.kompics.wan.ssh.scp.events.ScpGetFileResponse;
import se.sics.kompics.wan.ssh.scp.events.ScpPutFileRequest;
import se.sics.kompics.wan.ssh.scp.events.ScpPutFileResponse;
import ch.ethz.ssh2.SCPClient;

public class ScpComponent extends ComponentDefinition {

	private Negative<ScpPort> scpPort = negative(ScpPort.class);

	private Negative<DownloadUploadPort> downloadUploadPort = negative(DownloadUploadPort.class);

	private ConcurrentLinkedQueue<Thread> executingThreads = new ConcurrentLinkedQueue<Thread>();

//	private final List<FileInfo> fileMD5Hashes = new ArrayList<FileInfo>();

	private class ScpPutThread extends Thread {

		private final ScpPutFileRequest event;

		public ScpPutThread(ScpPutFileRequest event) {
			this.event = event;
		}

		@Override
		public void run() {
			// get a ticket from the RPC server to make sure that there
			// are a limited number of concurrent copy operations
			// RpcServer.getInstance().getNetworkIntensiveTicket();
			try {
				PlanetLabConfiguration.getNetworkIntensiveTicket();

				FileInfo fileInfo = event.getFileInfo();
				SCPClient scpClient = event.getScpClient();

				try {
					scpClient.put(fileInfo.getLocalFile().getCanonicalPath(), fileInfo
							.getRemoteDir(), "0644");
				} catch (IOException e1) {
					// some problem with the scp client
					if (e1.getMessage().contains("Error during SCP transfer")) {
						System.err.println(e1.getCause().getMessage());
					}
				}
				fileInfo.increaseCopyCount();

				trigger(new ScpPutFileResponse(event, fileInfo, event.getCommandSpec(), scpClient),
						scpPort);

				// release the copy ticket
				PlanetLabConfiguration.releaseNetworkIntensiveTicket();

			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				executingThreads.remove(this);
			}

		}
	}

	public class ScpGetThread extends Thread {

		private ScpGetFileRequest event;

		public ScpGetThread(ScpGetFileRequest event) {
			this.event = event;

		}

		public void run() {

			SCPClient scpClient = event.getScpClient();
			FileInfo fileInfo = event.getFileInfo();
			try {

				// get a ticket from the RPC server to make sure that there
				// are a limited number of concurrent copy operations
				// RpcServer.getInstance().getNetworkIntensiveTicket();
				PlanetLabConfiguration.getNetworkIntensiveTicket();

				// this is a remote file
				// System.out.println("downloading(" + available + "):"
				// + fileInfo.toString());
				// System.out.println("using localfile: "
				// + fileInfo.getLocalFile().getCanonicalPath());
				if (fileInfo.createLocalDirectoryStructure()) {

					BufferedOutputStream localFile = new BufferedOutputStream(new FileOutputStream(
							fileInfo.getLocalFile()));
					// System.out.println("created file");
					try {
						scpClient.get(fileInfo.getFullRemotePath(), localFile);
					} catch (IOException e1) {
						// some problem with the scp client
						if (e1.getMessage().contains("Error during SCP transfer")) {
							// System.out
							// .println(hostname
							// + ": disconnected during SCP transfer");
							System.err.println(e1.getCause().getMessage());
						}
					}
					fileInfo.increaseCopyCount();
					localFile.close();

					trigger(new ScpGetFileResponse(event, fileInfo, 
							event.getCommandSpec(), scpClient),
							scpPort);


				} else {
					System.out.println("could not create local directory: "
							+ fileInfo.getLocalDirectory());
				}

				// release the copy ticket
				PlanetLabConfiguration.releaseNetworkIntensiveTicket();

			} catch (InterruptedException e) {
				// ignore interupts... probably means that we should
				// quit
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				executingThreads.remove(this);
			}

		}

	}

	public ScpComponent() {
		subscribe(handleScpGetFileRequest, scpPort);
		subscribe(handleScpPutFileRequest, scpPort);
		
	}


	private Handler<ScpPutFileRequest> handleScpPutFileRequest = new Handler<ScpPutFileRequest>() {
		public void handle(ScpPutFileRequest event) {

			ScpPutThread t = new ScpPutThread(event);
			t.start();

			executingThreads.add(t);
		}
	};

	private Handler<ScpGetFileRequest> handleScpGetFileRequest = new Handler<ScpGetFileRequest>() {
		public void handle(ScpGetFileRequest event) {

			SCPClient scpClient = event.getScpClient();
			// String hostname = event.getHostname();
			FileInfo fileInfo = event.getFileInfo();

			ScpGetThread t = new ScpGetThread(event);
			t.run();

			executingThreads.add(t);

			// copyThread.copyFile(fileInfo);
			// no need to wait for thread to complete

			// try {
			// scpClient = sshConn.getConnection().createSCPClient();
			// } catch (IOException e2) {
			// System.err.println(sshConn.getExpHost().getHostname()
			// + ": could not create ssh client");
			// return;
			// }

		}
	};

//	private void fileDownloaded(ScpGetThread copyThread, FileInfo fileInfo) {
//
//		trigger(new DownloadMD5Response(fileInfo), scpPort);
//
//	}

}
