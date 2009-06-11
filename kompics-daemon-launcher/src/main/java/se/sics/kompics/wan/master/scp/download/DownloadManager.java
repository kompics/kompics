package se.sics.kompics.wan.master.scp.download;

import java.io.File;
import java.io.IOException;
import java.util.List;

import se.sics.kompics.wan.master.scp.FileInfo;
import se.sics.kompics.wan.master.scp.MD5Check;
import se.sics.kompics.wan.master.scp.RemoteDirMD5Info;
import se.sics.kompics.wan.master.ssh.CommandSpec;
import se.sics.kompics.wan.master.ssh.SshConnection;


public class DownloadManager {
	private static DownloadManager instance = null;

	private File baseDir;

	public static final String FLAT = "flat";

	public static final String HIERARCHY = "hierarchy";

	public static final String[] NAMING_TYPES = { HIERARCHY, FLAT };

	private volatile String downloadDirectoryType = HIERARCHY;

	protected DownloadManager() {

	}

	public static DownloadManager getInstance() {
		if (instance == null) {
			instance = new DownloadManager();
		}
		return instance;
	}

	private boolean setBaseDir(File baseDir) {
		if (baseDir.isDirectory()) {
			this.baseDir = baseDir;
		} else if (baseDir.mkdirs()) {
			this.baseDir = baseDir;
		} else {
			System.err
					.println("could not create local directory for downloads: "
							+ baseDir);
			return false;
		}
		return true;
	}

	public boolean setLocalFilenameType(String type) {
		if (FLAT.equals(type)) {
			this.downloadDirectoryType = FLAT;
		} else if (HIERARCHY.equals(type)) {
			this.downloadDirectoryType = HIERARCHY;
		} else {
			System.out.println("unknown local naming type: '" + type
					+ "', using default '" + DownloadManager.HIERARCHY + "'");
			this.downloadDirectoryType = HIERARCHY;
			return false;
		}
		return true;
	}

	public boolean downloadDir(SshConnection conn, String remotePath,
			File localBaseDir, String fileFilter, CommandSpec commandSpec) {

		this.setBaseDir(localBaseDir);

		RemoteDirMD5Info remoteMD5 = new RemoteDirMD5Info(conn);
		System.out.println("Getting file list");
		List<FileInfo> fileList;
		try {
			fileList = remoteMD5.getRemoteFileList(remotePath, fileFilter,
					commandSpec);
			for (FileInfo info : fileList) {
				if (this.downloadDirectoryType.equals(FLAT)) {
					info.setLocalFlatFile(baseDir);
				} else if (this.downloadDirectoryType.equals(HIERARCHY)) {
					info.setLocalHierarchicalFile(baseDir);
				}
			}
			System.out.println("starting md5 check thread");
			MD5Check md5Check = new DownloadMD5CheckThread(conn, fileList,
					commandSpec);

			// no need to start it as a separate thread
			md5Check.run();

			return true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

}
