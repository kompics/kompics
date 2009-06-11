/**
 * get the md5 checksums of a local dir or file
 */

package se.sics.kompics.wan.master.scp;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import org.apache.commons.codec.digest.DigestUtils;

//import com.twmacinta.util.MD5;

public class LocalDirMD5Info {
	// on minute
	private static final long MAX_AGE = 60 * 1000;

	private ConcurrentHashMap<String, Long> lastUpdate;

	private ConcurrentHashMap<String, FileInfo> pathMD5cache;

	private ConcurrentHashMap<String, Vector<FileInfo>> fileLists;

	private Semaphore semaphore = new Semaphore(1);

	private static LocalDirMD5Info instance = null;

	/**
	 * @param args
	 * @throws IOException
	 */

	protected LocalDirMD5Info() {
		lastUpdate = new ConcurrentHashMap<String, Long>();
		fileLists = new ConcurrentHashMap<String, Vector<FileInfo>>();
		pathMD5cache = new ConcurrentHashMap<String, FileInfo>();
	}

	public static LocalDirMD5Info getInstance() {

		if (instance == null) {
			instance = new LocalDirMD5Info();

		}
		return instance;
	}

	public static File[] listFilesAsArray(File directory,
			FilenameFilter filter, boolean recurse) {
		Collection<File> files = listFiles(directory, filter, recurse);
		// Java4: Collection files = listFiles(directory, filter, recurse);

		File[] arr = new File[files.size()];
		return files.toArray(arr);
	}

	public static Collection<File> listFiles(
	// Java4: public static Collection listFiles(
			File directory, FilenameFilter filter, boolean recurse) {
		// List of files / directories
		Vector<File> files = new Vector<File>();
		// Java4: Vector files = new Vector();

		// Get files / directories in the directory
		File[] entries = directory.listFiles();

		// Go over entries
		if (entries != null) {
			for (File entry : entries) {
				// Java4: for (int f = 0; f < files.length; f++) {
				// Java4: File entry = (File) files[f];

				// If there is no filter or the filter accepts the
				// file / directory, add it to the list
				if (filter == null || filter.accept(directory, entry.getName())) {
					files.add(entry);
				}

				// If the file is a directory and the recurse flag
				// is set, recurse into the directory
				if (recurse && entry.isDirectory()) {
					files.addAll(listFiles(entry, filter, recurse));
				}
			}

			// Return collection of files

		}
		return files;
	}

	public Vector<FileInfo> getFileInfo(File baseDirOrFile)
			throws InterruptedException {

		// make sure that only one instance is running
		semaphore.acquire();
		Vector<FileInfo> fileList;

		try {
			// check if we already have a resent copy of if
			String baseDirPath = baseDirOrFile.getCanonicalPath();
			if (lastUpdate.containsKey(baseDirPath)) {
				long currentTime = System.currentTimeMillis();
				long age = currentTime - lastUpdate.get(baseDirPath);
				if (age < MAX_AGE && fileLists.containsKey(baseDirPath)) {
					semaphore.release();
					return fileLists.get(baseDirPath);
				}
			}
			// else, update the md5 hashes and add it to the hashmap

			FilenameFilter fileFilter = new FilenameFilter() {
				public boolean accept(File file, String name) {
					return true;

				}
			};
			File[] files = new File[0];
			if (baseDirOrFile.isFile()) {
				files = new File[1];
				files[0] = baseDirOrFile;
			} else if (baseDirOrFile.isDirectory()) {
				files = listFilesAsArray(baseDirOrFile, fileFilter, true);
			}
			long startTime = System.nanoTime();
			fileList = new Vector<FileInfo>();
			// MD5.initNativeLibrary(false);
			for (int i = 0; i < files.length; i++) {
				if (files[i].isFile()) {

					byte[] md5;

					try {
						// XXX check if code that replaces below is valid
						// check if stream is character or binary?
						// md5 = MD5.getHash(files[i]);
						MessageDigest md = MessageDigest.getInstance("MD5");
						InputStream is = new FileInputStream(files[i]);
						
						try {
							is = new DigestInputStream(is, md);
							BufferedInputStream bufIn = new BufferedInputStream(is);

							int c;
							ByteArrayOutputStream bAOut = new ByteArrayOutputStream();
							while ((c = bufIn.read()) != -1) {
								bAOut.write((char) c);
							}
							md5 = bAOut.toByteArray();
						} finally {
							is.close();
						}
						byte[] digest = md.digest();

						FileInfo fileInfo = new FileInfo(files[i], DigestUtils.md5Hex(md5)); 

						String dirName = "";
						if (baseDirOrFile.isDirectory()) {
							dirName = baseDirOrFile.getName();
						}

						fileInfo.setRemotePath(baseDirOrFile, "~/" + dirName
								+ "/");
						// System.out.println(fileInfo.toString());
						fileList.add(fileInfo);
						pathMD5cache.put(fileInfo.getLocalFile()
								.getCanonicalPath(), fileInfo);
						lastUpdate
								.put(
										fileInfo.getLocalFile()
												.getCanonicalPath(), System
												.currentTimeMillis());
					} catch (IOException e) {

						System.err.println("problem with file '"
								+ files[i].getCanonicalPath() + "' :"
								+ e.getMessage());
					} catch (NoSuchAlgorithmException e) {
						e.printStackTrace();
						System.err.println(e.getMessage());						
					}
				}
			}
			double runTime = ((double) System.nanoTime() - startTime) / 1000000000.0;
			System.out.println("MD5 check runtime:" + runTime);
			fileLists.put(baseDirPath, fileList);
			lastUpdate.put(baseDirPath, System.currentTimeMillis());

		} catch (IOException e1) {
			// problem with the basedir
			fileList = null;
		}
		// release
		semaphore.release();
		return fileList;
	}

	public String getFileMD5(File localFile) throws IOException,
			InterruptedException {
		// check if this file already exists in our cache
		if (localFile.exists() && localFile.isFile()) {
			if (lastUpdate.containsKey(localFile.getCanonicalPath())) {
				// check if we have this one in our cache
				if (lastUpdate.get((localFile.getCanonicalPath())) + MAX_AGE > System
						.currentTimeMillis()) {
					FileInfo info = pathMD5cache.get(localFile
							.getCanonicalPath());
					if (info != null) {
						return info.getMd5();
					}
				}
			}
			// not in cache, but exists
			// we need to get the md5 of this file
			List<FileInfo> fileList = this.getFileInfo(localFile);
			if (fileList.size() == 1) {
				return fileList.get(0).getMd5();
			}

		}
		// file does not exists, return null
		return null;
	}
}
