package se.sics.kompics.wan.master.scp;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.wan.master.scp.download.DownloadMD5Request;
import se.sics.kompics.wan.master.scp.download.DownloadMD5Response;
import se.sics.kompics.wan.master.scp.upload.UploadMD5Request;
import se.sics.kompics.wan.master.scp.upload.UploadMD5Response;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import se.sics.kompics.wan.master.ssh.SshComponent;
import se.sics.kompics.wan.master.ssh.SshComponent.SshConn;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;

public class DownloadUploadMgr extends ComponentDefinition {

	private Negative<DownloadUploadPort> checker = negative(DownloadUploadPort.class);

	private Positive<ScpPort> scpPort = positive(ScpPort.class);

	private Map<Integer, Integer> commandOutstandingFiles = new HashMap<Integer, Integer>();
	
	private Map<Integer, DownloadMD5Request> downloadRequests = 
		new HashMap<Integer, DownloadMD5Request>();

	private Map<Integer, UploadMD5Request> uploadRequests = 
		new HashMap<Integer, UploadMD5Request>();

	public DownloadUploadMgr() {

		subscribe(handleDownloadMD5Request, checker);
		subscribe(handleUploadMD5Request, checker);

		subscribe(handleScpGetFileResponse, scpPort);
		subscribe(handleScpPutFileResponse, scpPort);
	}

	/**
	 * Does the work done by DownloadMD5CheckThread.run
	 */
	public Handler<DownloadMD5Request> handleDownloadMD5Request = new Handler<DownloadMD5Request>() {
		public void handle(DownloadMD5Request event) {

			CommandSpec commandSpec = event.getCommandSpec();
			int sessionId = event.getSessionId();
			int commandId = commandSpec.getCommandId();
			
			List<FileInfo> fileMD5Hashes = event.getFileMD5Hashes();

			commandSpec.started();
			
			int filesOutstanding = fileMD5Hashes.size();
			commandOutstandingFiles.put(commandId , filesOutstanding);

			downloadRequests.put(sessionId, event);
			
			for (FileInfo file : fileMD5Hashes) {
				checkFile(file, commandSpec, event.getScpClient());
			}
		}
		// }
		// session.close();
		// try {
		// scpCopyThreadThread.join();
		// success = true;
		// commandResults.setExitCode(0, "copied " + copyCount);
		// System.out.print(sshConn.getExpHost().getHostname() + ": done MD5");
		// } catch (InterruptedException e) {

		// }

	};

	private void checkFile(FileInfo file, CommandSpec commandSpec,
			SCPClient scpClient) {

		try {

			// check if the file exists locally
			LocalDirMD5Info localMD5checker = LocalDirMD5Info.getInstance();
			String localMD5 = localMD5checker.getFileMD5(file.getLocalFile());

			String remoteMD5 = file.getMd5();
			System.out.println("local_: " + localMD5);
			System.out.println("remote: " + remoteMD5);

			boolean md5match = (remoteMD5.compareTo(localMD5) == 0) ? true : false;

			// localMD5 is null if file not found
			if (localMD5 != null) {

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
//				if (checkDownloadedFile(file, commandSpec) == false) {
//					md5match = false;
//				}
			} else {
				commandSpec.receivedControlErr("copying (missing): "
						+ file.getFullRemotePath() + " -> "
						+ file.getLocalFile().getCanonicalPath());
			}
			System.out.println(commandSpec.toString());

			if (!md5match) { // get the remote file 

				// TODO create dir, but maybe not always
				if (file.getCopyCount() < 3) {
					trigger(new ScpGetFileRequest(file, commandSpec, scpClient), scpPort);
				}
				else
				{
					System.err.println("Execeed max file copy count. Not retrying copying file");
				}
			}

			// check if we are done (all queues are empty)
			// boolean scpDone = scpCopyThread.isDone();
			// boolean md5Done = fileMD5Hashes.isEmpty();
			// if (scpDone && md5Done) {
			// System.out.println("all done");
			// quit = true;
			// scpCopyThread.halt();
			// scpCopyThreadThread.interrupt();
			// }
		} catch (InterruptedException e) {
			// ignore, application shutdown
		} catch (IOException e) {
			if (e.getMessage().contains("SSH channel closed")) {
				// ignore, probably happened in the shell session to...

			}
		}
	}

//	private boolean checkDownloadedFile(FileInfo file, CommandSpec commandSpec) 
//	throws IOException, InterruptedException {
//		LocalDirMD5Info localMD5checker = LocalDirMD5Info.getInstance();
//		String localMD5 = localMD5checker.getFileMD5(file.getLocalFile());
//
//		String remoteMD5 = file.getMd5();
//		System.out.println("local_: " + localMD5);
//		System.out.println("remote: " + remoteMD5);
//
//		boolean md5match = remoteMD5.equals(localMD5);
//
//		// localMD5 is null if file not found
//		if (localMD5 != null) {
//			return false;
//		}
//			// does the md5 match?
//			if (md5match) {
//				// System.out.println("passed");
//				commandSpec.receivedControlData("passed: "
//						+ file.getFullRemotePath() + " -> "
//						+ file.getLocalFile().getCanonicalPath());
//			} else {
//				commandSpec.receivedControlErr("copying (md5 failed):"
//						+ file.getFullRemotePath() + " -> "
//						+ file.getLocalFile().getCanonicalPath());
//			}
//			 System.out.println("size: " + commandSpec.getProcOutput(0).size());
//
//		
//		return true;
//	}
	
	public Handler<UploadMD5Request> handleUploadMD5Request = new Handler<UploadMD5Request>() {
		public void handle(UploadMD5Request event) {

			CommandSpec commandSpec = event.getCommandSpec();
			int sessionId = event.getSessionId();
			int commandId = commandSpec.getCommandId();
			
			List<FileInfo> fileMD5Hashes = event.getFileMD5Hashes();

			commandSpec.started();
			
			int filesOutstanding = fileMD5Hashes.size();
			commandOutstandingFiles.put(sessionId , filesOutstanding);

			uploadRequests.put(commandId, event);
			
			SCPClient scpClient = event.getScpClient();
			
			for (FileInfo file : fileMD5Hashes) {
				trigger(new ScpPutFileRequest(file, commandSpec, scpClient), scpPort);
			}
			// wait for ScpPutResponse before doing MD5 check of file
			
		}
	};

	
	public Handler<ScpGetFileResponse> handleScpGetFileResponse = new Handler<ScpGetFileResponse>() {
		public void handle(ScpGetFileResponse event) {
			int commandId = event.getCommandSpec().getCommandId();
			checkFile(event.getFileInfo(), event.getCommandSpec(), event.getScpClient());
			
			int filesOutstanding = commandOutstandingFiles.get(commandId);
			filesOutstanding--;
			if (filesOutstanding == 0)
			{
				DownloadMD5Request request = downloadRequests.get(commandId);	
				trigger(new DownloadMD5Response(request, commandId, true), checker);
				commandOutstandingFiles.remove(commandId);
			}
			else {
				commandOutstandingFiles.put(commandId, filesOutstanding);
			}
		}
	};
	
	public Handler<ScpPutFileResponse> handleScpPutFileResponse = new Handler<ScpPutFileResponse>() {
		public void handle(ScpPutFileResponse event) {

			int commandId = event.getCommandSpec().getCommandId();
			
//			checkFile(sessionId, event.getFileInfo(), event.getCommandSpec(), event.getScpClient());
			
			int filesOutstanding = commandOutstandingFiles.get(commandId);
			filesOutstanding--;
			if (filesOutstanding == 0)
			{
				UploadMD5Request request = uploadRequests.get(commandId );
				FileInfo file = event.getFileInfo();
				trigger(new UploadMD5Response(request, commandId , file, true), checker);
				commandOutstandingFiles.remove(commandId );
			}
			else {
				commandOutstandingFiles.put(commandId , filesOutstanding);
			}

//			checkRemoteFile(session, commandResults, file);
			

			
//			checkFile(event.)
//			
//			// check if we are done (all queues are empty)
//			boolean scpDone = scpCopyThread.isDone();
//			boolean md5Done = fileMD5Hashes.isEmpty();
//			if (scpDone && md5Done) {
//				System.out.println("all done");
//				quit = true;
//				scpCopyThread.halt();
//				scpCopyThreadThread.interrupt();
//			}
		}
	};
	
	

}
