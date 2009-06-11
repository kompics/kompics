package se.sics.kompics.wan.master.scp.upload;

import java.io.File;

import se.sics.kompics.wan.master.scp.LocalDirMD5Info;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import se.sics.kompics.wan.master.ssh.SshConnection;


public class UploadManager {

	private static UploadManager instance = null;

	protected UploadManager() {

	}

	public static UploadManager getInstance() {
		if (instance == null) {
			instance = new UploadManager();
		}
		return instance;
	}

	public boolean uploadDir(SshConnection conn, File baseDir,
			CommandSpec commandSpec) {
		try {
			UploadMD5CheckThread md5Check;

			md5Check = new UploadMD5CheckThread(conn, LocalDirMD5Info
					.getInstance().getFileInfo(baseDir), commandSpec);

			// no need to fire it up as a thread, just call the run method
			md5Check.run();

			return true;
			
		} catch (InterruptedException e) {
			commandSpec.receivedErr("local i/o error, " + e.getMessage());
		}
		return false;
	}
}
