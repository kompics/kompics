package se.sics.kompics.wan.master.scp;

import java.io.IOException;
import java.util.List;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.wan.master.scp.download.DownloadMD5Request;
import se.sics.kompics.wan.master.scp.upload.UploadMD5Request;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import ch.ethz.ssh2.Session;

public class DownloadMgr extends ComponentDefinition {

	private Negative<DownloadMgrPort> checker = negative(DownloadMgrPort.class);
	
	public DownloadMgr() {
		
		subscribe(handleDownloadMD5Request, checker);
		subscribe(handleUploadMD5Request, checker);
	}
	
	
	public Handler<DownloadMD5Request> handleDownloadMD5Request = new Handler<DownloadMD5Request>()
	{
		public void handle(DownloadMD5Request event) {
			
			CommandSpec commandSpec = event.getCommandSpec();
			Session session = event.getSession();
			List<FileInfo> fileMD5Hashes = event.getFileMD5Hashes();
			
			int copyCount = 0;

				commandSpec.started();
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
								commandSpec.receivedControlData("passed: "
										+ file.getFullRemotePath() + " -> "
										+ file.getLocalFile().getCanonicalPath());
							} else {
								commandSpec
										.receivedControlErr("copying (md5 failed):"
												+ file.getFullRemotePath()
												+ " -> "
												+ file.getLocalFile()
														.getCanonicalPath());
							}
							// System.out.println("size: "
							// + commandSpec.getProcOutput(0).size());
						} else {
							commandSpec.receivedControlErr("copying (missing): "
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
//			}
			session.close();
//			try {
//				scpCopyThreadThread.join();
//				success = true;
//				commandResults.setExitCode(0, "copied " + copyCount);
//				System.out.print(sshConn.getExpHost().getHostname() + ": done MD5");
//			} catch (InterruptedException e) {

			}

			

	};
	
	public Handler<UploadMD5Request> handleUploadMD5Request = new Handler<UploadMD5Request>()
	{
		public void handle(UploadMD5Request event) {
			
			
		}
	};
}
