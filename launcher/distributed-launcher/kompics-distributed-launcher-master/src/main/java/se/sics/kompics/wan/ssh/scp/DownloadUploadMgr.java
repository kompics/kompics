package se.sics.kompics.wan.ssh.scp;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.wan.ssh.CommandSpec;
import se.sics.kompics.wan.ssh.scp.events.DownloadMD5Request;
import se.sics.kompics.wan.ssh.scp.events.DownloadMD5Response;
import se.sics.kompics.wan.ssh.scp.events.ScpGetFileRequest;
import se.sics.kompics.wan.ssh.scp.events.ScpGetFileResponse;
import se.sics.kompics.wan.ssh.scp.events.ScpPutFileRequest;
import se.sics.kompics.wan.ssh.scp.events.ScpPutFileResponse;
import se.sics.kompics.wan.ssh.scp.events.UploadMD5Request;
import se.sics.kompics.wan.ssh.scp.events.UploadMD5Response;
import ch.ethz.ssh2.SCPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadUploadMgr extends ComponentDefinition {

    private final Logger logger = LoggerFactory.getLogger(DownloadUploadMgr.class);

	private Negative<DownloadUploadPort> checker = negative(DownloadUploadPort.class);

	private Positive<ScpPort> scpPort = positive(ScpPort.class);

	private Map<Integer, Integer> commandOutstandingFiles = new HashMap<Integer, Integer>();

	private Map<Integer, DownloadMD5Request> downloadRequests = new HashMap<Integer, DownloadMD5Request>();

	private Map<Integer, UploadMD5Request> uploadRequests = new HashMap<Integer, UploadMD5Request>();

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
			int commandId = commandSpec.getCommandId();

			List<FileInfo> fileMD5Hashes = event.getFileMD5Hashes();

			commandSpec.started();

			int filesOutstanding = fileMD5Hashes.size();
			commandOutstandingFiles.put(commandId, filesOutstanding);

			downloadRequests.put(commandId, event);

			boolean filesAlreadyDownloaded = true;
			for (FileInfo file : fileMD5Hashes) {
				try {
					if (checkFile(file, commandSpec, event.getScpClient()) == false) {
						filesAlreadyDownloaded = false;
					}
				} catch (ScpRetryExceeded e) {
					e.printStackTrace();
					trigger(new DownloadMD5Response(event, commandId, false), checker);
				}
			}
			if (filesAlreadyDownloaded == true) {
				trigger(new DownloadMD5Response(event, commandId, true), checker);
			}
		}

	};

	private boolean checkFile(FileInfo file, CommandSpec commandSpec, SCPClient scpClient) 
		throws ScpRetryExceeded {

		try {

			// check if the file exists locally
			LocalDirMD5Info localMD5checker = LocalDirMD5Info.getInstance();
			String localMD5 = localMD5checker.getFileMD5(file.getLocalFile());

			String remoteMD5 = file.getMd5();
			logger.debug("local_: " + localMD5);
			logger.debug("remote: " + remoteMD5);

			boolean md5match;

			// localMD5 is null if file not found
			if (localMD5 != null) {

				md5match = (remoteMD5.compareTo(localMD5) == 0) ? true : false;

				if (md5match) {
					 logger.debug("MD5 ok");
					commandSpec.receivedControlData("passed: " + file.getFullRemotePath() + " -> "
							+ file.getLocalFile().getCanonicalPath());
					return true;
				} else {
					commandSpec.receivedControlErr("copying (md5 failed):"
							+ file.getFullRemotePath() + " -> "
							+ file.getLocalFile().getCanonicalPath());
				}
			} else {
				commandSpec.receivedControlErr("copying (missing): " + file.getFullRemotePath()
						+ " -> " + file.getLocalFile().getCanonicalPath());
				md5match = false;
			}
			logger.debug(commandSpec.toString());

			if (!md5match) { // get the remote file

				if (file.getCopyCount() < 3) {
					trigger(new ScpGetFileRequest(file, commandSpec, scpClient), scpPort);
				} else {
					throw new ScpRetryExceeded( commandSpec.getCommand(), 
							"Scp Timeout! Retried copying count exceeeded.");
				}
			}

		} catch (InterruptedException e) {
			// ignore, application shutdown
		} catch (IOException e) {
			if (e.getMessage().contains("SSH channel closed")) {
				// ignore, probably happened in the shell session to...
			}
		}
		return false;
	}


	public Handler<UploadMD5Request> handleUploadMD5Request = new Handler<UploadMD5Request>() {
		public void handle(UploadMD5Request event) {

			CommandSpec commandSpec = event.getCommandSpec();
			int commandId = commandSpec.getCommandId();

			List<FileInfo> fileMD5Hashes = event.getFileMD5Hashes();

			commandSpec.started();

			int filesOutstanding = fileMD5Hashes.size();
			commandOutstandingFiles.put(commandId, filesOutstanding);

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
			try {
				if (checkFile(event.getFileInfo(), event.getCommandSpec(), event.getScpClient()) == true)
				{
					int filesOutstanding = commandOutstandingFiles.get(commandId);
					filesOutstanding--;
					if (filesOutstanding == 0) {
						DownloadMD5Request request = downloadRequests.get(commandId);
						trigger(new DownloadMD5Response(request, commandId, true), checker);
						commandOutstandingFiles.remove(commandId);
					} else {
						commandOutstandingFiles.put(commandId, filesOutstanding);
					}				
				}
			} catch (ScpRetryExceeded e) {
				System.err.println(e.getMessage());
				DownloadMD5Request request = downloadRequests.get(commandId);
				trigger(new DownloadMD5Response(request, commandId, false), checker);
			}


		}
	};

	public Handler<ScpPutFileResponse> handleScpPutFileResponse = new Handler<ScpPutFileResponse>() {
		public void handle(ScpPutFileResponse event) {

			int commandId = event.getCommandSpec().getCommandId();

			// checkFile(sessionId, event.getFileInfo(), event.getCommandSpec(),
			// event.getScpClient());

			int filesOutstanding = commandOutstandingFiles.get(commandId);
			filesOutstanding--;
			if (filesOutstanding == 0) {
				UploadMD5Request request = uploadRequests.get(commandId);
				FileInfo file = event.getFileInfo();
				trigger(new UploadMD5Response(request, commandId, file, true), checker);
				commandOutstandingFiles.remove(commandId);
			} else {
				commandOutstandingFiles.put(commandId, filesOutstanding);
			}

			
			// checkRemoteFile(session, commandResults, file);

			// checkFile(event.)
			//			
			// // check if we are done (all queues are empty)
			// boolean scpDone = scpCopyThread.isDone();
			// boolean md5Done = fileMD5Hashes.isEmpty();
			// if (scpDone && md5Done) {
			// System.out.println("all done");
			// quit = true;
			// scpCopyThread.halt();
			// scpCopyThreadThread.interrupt();
			// }
		}
	};

}
