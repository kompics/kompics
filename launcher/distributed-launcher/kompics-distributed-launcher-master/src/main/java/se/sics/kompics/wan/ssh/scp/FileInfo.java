package se.sics.kompics.wan.ssh.scp;

import java.io.File;
import java.io.IOException;

public class FileInfo {

	private int copyCount = 1;

	private File localFile;

	private final boolean isLocalFile;

	private final String md5;

	private String remotePath;

	private String remoteHostname;

        /**
	 * Contructor used when getting md5 info of local files
	 * 
	 * @param file
	 * @param md5
	 * @throws IOException
	 */
	public FileInfo(File file, String md5) throws IOException {
		this.localFile = file;
		this.md5 = md5;
		this.isLocalFile = true;

		// String remotePath = getRemotePath(file, baseDir);
		// this.remoteDir = getRemoteRelativeDir(remotePath);
		// this.remoteFileName = extractRemoteFilename(remotePath);
	}

	/**
	 * constructor used when downloading
	 * 
	 * @param remotePath
	 * @param md5
	 * @param hostname
	 */
	public FileInfo(String remotePath, String md5, String hostname) {
		this.remotePath = remotePath;
		this.md5 = md5;
		this.isLocalFile = false;
		this.remoteHostname = hostname;
	}

	private FileInfo(File file, String remotePath, String md5, boolean localFile) {
		this.localFile = file;
		this.remotePath = remotePath;
		this.md5 = md5;
		this.isLocalFile = localFile;
	}

	/**
	 * returns a clone of the current FileInfo
	 */
	public FileInfo clone() {
		return new FileInfo(localFile, remotePath, md5, isLocalFile);
	}

	/**
	 * create the directory structure needed to access this file
	 * 
	 * @return true i successful
	 */

	public boolean createLocalDirectoryStructure() {
		if (!localFile.isFile()) {
			try {
				String dir = this.getLocalDirectory();
				File fileDir = new File(dir);
				if (fileDir.isDirectory()) {
					return true;
				} else {
					return fileDir.mkdirs();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}

	public int getCopyCount() {
		return copyCount;
	}

	public String getFullRemotePath() {
		return remotePath;
	}

	public String getLocalDirectory() throws IOException {
		String path = localFile.getCanonicalPath();

		int lastIndex = path.lastIndexOf(File.separatorChar);
		String dir = path.substring(0, lastIndex + 1);
		System.out.println("path='" + path + "'");
		System.out.println("path='" + dir + "'");

		return dir;
	}

	public File getLocalFile() {
		return localFile;
	}

	public String getMd5() {
		return md5;
	}

	public String getRemoteDir() {
		int lastSlash = this.remotePath.lastIndexOf('/');
		return this.remotePath.substring(0, lastSlash + 1);
	}

	public void increaseCopyCount() {
		copyCount++;
	}

	public boolean isLocalFile() {
		return isLocalFile;
	}

	// public String getLastDir() {
	// if (localFile != null && localFile.exists() && isLocalFile) {
	// if (localFile.isFile()) {
	// return "";
	// } else if (localFile.isDirectory()) {
	// if (localFile.isDirectory()) {
	// return localFile.getName();
	// }
	// }
	// }
	// return "";
	// }

	public void setRemotePath(File localBasePath, String remoteBasePath)
			throws IOException {
		
		// fix remotebase path so slashes are added if needed
		remoteBasePath = this.stripSlashes(remoteBasePath);
		if (remoteBasePath.charAt(0) != '~') {
			remoteBasePath = "/" + remoteBasePath;
		}

		String localBaseString = localBasePath.getCanonicalPath();
		// System.out.println("base='" + localBaseString+"'");
		String localFileString = localFile.getCanonicalPath();
		// System.out.println("file='" + localFileString+"'");
		if (!localFileString.startsWith(localBaseString)) {
			throw new IOException("file not in base dir(!?)");
		}
		// if basepath is a file, just add basepath to the filename
		if (localBasePath.isFile()) {
			this.remotePath = remoteBasePath + "/" + localFile.getName();
		} else if (localBasePath.isDirectory()) {
			// take away the base path from the local file path
			String relativePath = localFileString.substring(localBaseString
					.length());
			relativePath = this.fixFileSeparator(relativePath, '/');

			this.remotePath = remoteBasePath + "/"
					+ this.stripSlashes(relativePath);
		} else {
			throw new IOException("base not file or dir");
		}
	}

//	public void setLocalFlatFile(File localBaseDir) throws IOException {
//		String remoteFlatPath = this.stripSlashes(remotePath);
//		remoteFlatPath = remoteFlatPath.replaceAll("/", "_");
//		String localPath = localBaseDir.getCanonicalPath() remotePath
//				+ remoteHostname + "_" + remoteFlatPath;
//
//		// System.out.println("localPath='" + localPath + "'");
//		localPath = this.fixFileSeparator(localPath, File.separatorChar);
//		// System.out.println("localPath='" + localPath + "'");
//
//		localFile = new File(localPath);
//	}
	
	public void setLocalFlatFile(File localBaseDir) throws IOException {
		int pos = remotePath.lastIndexOf('/');
		String remoteFile = remotePath.substring(pos+1, remotePath.length()); 
		
		String localPath = localBaseDir.getCanonicalPath() + File.separatorChar + remoteFile;
		localPath = this.fixFileSeparator(localPath, File.separatorChar);
		localFile = new File(localPath);
	}

	public void setLocalHierarchicalFile(File localBaseDir) throws IOException {
		String localPath = localBaseDir.getCanonicalPath() + File.separator
				+ remoteHostname + File.separator + remotePath;
		// System.out.println("localPath='" + localPath + "'");
		localPath = this.fixFileSeparator(localPath, File.separatorChar);
		// System.out.println("localPath='" + localPath + "'");
		localFile = new File(localPath);

	}

	public String toString() {
		return md5 + " " + localFile + " " + remotePath;
	}

	private String stripSlashes(String s) {
		if (s.charAt(0) == '/') {
			s = s.substring(1);
		}
		if (s.charAt(s.length() - 1) == '/') {
			s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private String fixFileSeparator(String path, char separator) {
		StringBuffer newString = new StringBuffer();

		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (c == '\\') {
				newString.append(separator);
			} else if (c == '/') {
				newString.append(separator);
			} else {
				newString.append(c);
			}
		}

		// TODO the following code WILL generate a string index out of bounds
		// exception
		// localPath = localPath.replaceAll("\\\\", File.separator);
		// localPath = localPath.replaceAll("/", File.separator);
		return newString.toString();
	}
	
	@Override
	public int hashCode() {
		
		int id=0;
		if (localFile != null)
		{
			id += localFile.hashCode();
		}
		if (remotePath != null)
		{
			id += remotePath.hashCode();
		}
		if (remoteHostname != null) {
			id += remoteHostname.hashCode();
		}
		
		return super.hashCode();
	}
}
