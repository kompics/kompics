package se.sics.kompics.wan.master.scp;

public interface MD5Check extends Runnable {

	
	public void checkFile(FileInfo file);
	
	public boolean wasSuccess();
}
