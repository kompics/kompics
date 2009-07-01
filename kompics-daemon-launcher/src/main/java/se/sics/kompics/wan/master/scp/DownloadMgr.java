package se.sics.kompics.wan.master.scp;

import java.io.IOException;
import java.util.List;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.wan.master.scp.download.DownloadMD5Request;
import se.sics.kompics.wan.master.scp.upload.UploadMD5Request;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.Session;

public class DownloadMgr extends ComponentDefinition {

	private Negative<DownloadMgrPort> checker = negative(DownloadMgrPort.class);

	private Positive<ScpPort> scpPort = positive(ScpPort.class);

	public DownloadMgr() {

		subscribe(handleDownloadMD5Request, checker);
		subscribe(handleUploadMD5Request, checker);

		subscribe(handleScpCopyFinished, scpPort);
		subscribe(handleScpCopyFileResponse, scpPort);
	}

	/**
	 * Does the work done by DownloadMD5CheckThread.run
	 */
	public Handler<DownloadMD5Request> handleDownloadMD5Request = new Handler<DownloadMD5Request>() {
		public void handle(DownloadMD5Request event) {

			CommandSpec commandSpec = event.getCommandSpec();
			// Session session = event.getSession();
			List<FileInfo> fileMD5Hashes = event.getFileMD5Hashes();

			// int copyCount = 0;

			commandSpec.started();

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

			boolean md5match = remoteMD5.equals(localMD5);

			// localMD5 is null if file not found
			if (localMD5 != null) {
				checkDownloadedFile(file, commandSpec);
			} else {
				commandSpec.receivedControlErr("copying (missing): "
						+ file.getFullRemotePath() + " -> "
						+ file.getLocalFile().getCanonicalPath());
			}
			System.out.println(commandSpec.toString());

			if (!md5match) {

				// TODO create dir, but maybe not always
				file.getLocalFile();
				if (file.getCopyCount() < 3) {
					// System.out.println("sending to copy thread: "
					// + file.getFullRemotePath());

					// XXX send the file to the ScpComponent
					// scpCopyThread.copyFile(file);

					trigger(new ScpCopyFileRequest(file, commandSpec, scpClient), scpPort);
					// copyCount++;
				}
				else
				{
					System.err.println("Execeed max file copy count. Not retrying copying file");
				}
			}
			// currentFile++;

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

	private boolean checkDownloadedFile(FileInfo file, CommandSpec commandSpec) {
		LocalDirMD5Info localMD5checker = LocalDirMD5Info.getInstance();
		String localMD5 = localMD5checker.getFileMD5(file.getLocalFile());

		String remoteMD5 = file.getMd5();
		System.out.println("local_: " + localMD5);
		System.out.println("remote: " + remoteMD5);

		boolean md5match = remoteMD5.equals(localMD5);

		// localMD5 is null if file not found
		if (localMD5 != null) {
			return false;
		}
			// does the md5 match?
			if (md5match) {
				// System.out.println("passed");
				commandSpec.receivedControlData("passed: "
						+ file.getFullRemotePath() + " -> "
						+ file.getLocalFile().getCanonicalPath());
			} else {
				commandSpec.receivedControlErr("copying (md5 failed):"
						+ file.getFullRemotePath() + " -> "
						+ file.getLocalFile().getCanonicalPath());
			}
			 System.out.println("size: " + commandSpec.getProcOutput(0).size());

		
		return true;
	}
	
	public Handler<UploadMD5Request> handleUploadMD5Request = new Handler<UploadMD5Request>() {
		public void handle(UploadMD5Request event) {

		}
	};

	
	public Handler<ScpCopyFileResponse> handleScpCopyFileResponse = new Handler<ScpCopyFileResponse>() {
		public void handle(ScpCopyFileResponse event) {
			
			checkFile(event.getFileInfo(), event.getCommandSpec(), event.getScpClient());
		}
	};
	
	public Handler<ScpCopyFinished> handleScpCopyFinished = new Handler<ScpCopyFinished>() {
		public void handle(ScpCopyFinished event) {

			checkFile(event.)
			
			// check if we are done (all queues are empty)
			boolean scpDone = scpCopyThread.isDone();
			boolean md5Done = fileMD5Hashes.isEmpty();
			if (scpDone && md5Done) {
				System.out.println("all done");
				quit = true;
				scpCopyThread.halt();
				scpCopyThreadThread.interrupt();
			}
		}
	};

}
