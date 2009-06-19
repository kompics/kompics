package se.sics.kompics.wan.master.scp;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.scp.download.DownloadMD5Request;
import se.sics.kompics.wan.master.scp.download.DownloadMD5Response;
import se.sics.kompics.wan.master.scp.upload.UploadMD5Request;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;

public class ScpComponent extends ComponentDefinition {

	private Negative<ScpPort> scpPort;

	private Positive<DownloadMgrPort> downloadMgrPort;

	private ConcurrentLinkedQueue<CopyThread> executingCopyThreads = new ConcurrentLinkedQueue<CopyThread>();

	public class CopyThread extends Thread {
		private AtomicBoolean quit = new AtomicBoolean(false);
		private AtomicBoolean working = new AtomicBoolean(false);
		private SCPClient scpClient;
		private final String hostname;

		private final LinkedBlockingQueue<FileInfo> fileQueue = new LinkedBlockingQueue<FileInfo>();

		public CopyThread(SCPClient scpClient, String hostname) {
			this.scpClient = scpClient;
			this.hostname = hostname;
		}

		public void run() {

			while (!quit.get()) {

				try {

					FileInfo fileInfo = fileQueue.take();
					working.set(true);

					// get a ticket from the RPC server to make sure that
					// there
					// are a limited number of concurrent copy operations
					// RpcServer.getInstance().getNetworkIntensiveTicket();
					PlanetLabConfiguration.getNetworkIntensiveTicket();

					if (fileInfo.isLocalFile()) {
						// System.out.println("uploading(" + available + "): "
						// + fileInfo.toString());
						try {
							scpClient.put(fileInfo.getLocalFile()
									.getCanonicalPath(), fileInfo
									.getRemoteDir(), "0644");
						} catch (IOException e1) {
							// some problem with the scp client
							if (e1.getMessage().contains(
									"Error during SCP transfer")) {
								System.out.println(hostname
										+ ": disconnected during SCP transfer");
								System.err.println(e1.getCause().getMessage());
							}
						}
						fileInfo.increaseCopyCount();

						// send fileInfo event to MD5Check component
						// checkThread.checkFile(fileInfo);

						trigger(new DownloadMD5Response(fileInfo, true),
								md5CheckerPort);
					} else {
						// this is a remote file
						// System.out.println("downloading(" + available + "):"
						// + fileInfo.toString());
						// System.out.println("using localfile: "
						// + fileInfo.getLocalFile().getCanonicalPath());
						if (fileInfo.createLocalDirectoryStructure()) {

							BufferedOutputStream localFile = new BufferedOutputStream(
									new FileOutputStream(fileInfo
											.getLocalFile()));
							// System.out.println("created file");
							try {
								scpClient.get(fileInfo.getFullRemotePath(),
										localFile);
							} catch (IOException e1) {
								// some problem with the scp client
								if (e1.getMessage().contains(
										"Error during SCP transfer")) {
									System.out
											.println(hostname
													+ ": disconnected during SCP transfer");
									System.err.println(e1.getCause()
											.getMessage());
								}
							}
							fileInfo.increaseCopyCount();
							localFile.close();

							// XXX tell component that file is now downloaded
							// checkThread.checkFile(fileInfo);
							fileDownloaded(this, fileInfo);

						} else {
							System.out
									.println("could not create local directory: "
											+ fileInfo.getLocalDirectory());
						}

					}
					working.set(false);

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

		public void copyFile(FileInfo fileInfo) {
			fileQueue.add(fileInfo);
		}
	}

	public ScpComponent() {
		subscribe(handleScpCopyFileTo, scpPort);
		
		subscribe(handleDownloadMD5Request, downloadMgrPort);
		subscribe(handleUploadMD5Request, downloadMgrPort);

	}

	public Handler<DownloadMD5Request> handleDownloadMD5Request = new Handler<DownloadMD5Request>() {
		public void handle(DownloadMD5Request event) {

			CommandSpec commandSpec = event.getCommandSpec();
			Session session = event.getSession();
			List<FileInfo> fileMD5Hashes = event.getFileMD5Hashes();
			

			int copyCount = 0;

			commandSpec.started();
			
			
			CopyThread scpCopyThread = new CopyThread(, this);
			scpCopyThreadThread = new Thread(scpCopyThread);
			scpCopyThreadThread.setName("ScpThread: " + sshConn.getExpHost().getHostname());
			scpCopyThreadThread.start();
			
			
		}
	};
	
	
			while (!quit.get()) {
				FileInfo file;
				try {

					// XXX change this
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
							commandSpec.recievedControllData("passed: "
									+ file.getFullRemotePath() + " -> "
									+ file.getLocalFile().getCanonicalPath());
						} else {
							commandSpec
									.recievedControllErr("copying (md5 failed):"
											+ file.getFullRemotePath()
											+ " -> "
											+ file.getLocalFile()
													.getCanonicalPath());
						}
						// System.out.println("size: "
						// + commandSpec.getProcOutput(0).size());
					} else {
						commandSpec.recievedControllErr("copying (missing): "
								+ file.getFullRemotePath() + " -> "
								+ file.getLocalFile().getCanonicalPath());
					}
					// System.out.println(commandSpec.toString());

					if (!md5match) {

						// TODO create dir, but maybe not always
						file.getLocalFile();
						if (file.getCopyCount() < 3) {
							// System.out.println("sending to copy thread: "
							// + file.getFullRemotePath());

							// XXX send the file to the ScpComponent
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
			// }
			session.close();
			// try {
			// scpCopyThreadThread.join();
			// success = true;
			// commandResults.setExitCode(0, "copied " + copyCount);
			// System.out.print(sshConn.getExpHost().getHostname() +
			// ": done MD5");
			// } catch (InterruptedException e) {

		}

	};

	public Handler<UploadMD5Request> handleUploadMD5Request = new Handler<UploadMD5Request>() {
		public void handle(UploadMD5Request event) {

		}
	};

	private Handler<ScpCopyFileTo> handleScpCopyFileTo = new Handler<ScpCopyFileTo>() {
		public void handle(ScpCopyFileTo event) {

			SCPClient scpClient = event.getScpClient();
			String hostname = event.getHostname();
			FileInfo fileInfo = event.getFileInfo();

			CopyThread copyThread = new CopyThread(scpClient, hostname);
			copyThread.run();

			executingCopyThreads.add(copyThread);

			copyThread.copyFile(fileInfo);
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

	private void fileDownloaded(CopyThread copyThread, FileInfo fileInfo) {

		trigger(new DownloadMD5Response(fileInfo), scpPort);
		executingCopyThreads.remove(copyThread);

	}
}
