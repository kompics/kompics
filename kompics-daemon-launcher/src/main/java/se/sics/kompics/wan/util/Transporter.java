package se.sics.kompics.wan.util;

import java.io.File;
import java.io.InputStream;

public interface Transporter {

	/** The Plexus role identifier. */
	String ROLE = Transporter.class.getName();

	/**
	 * 
	 * @param url
	 *            The url where to get data from
	 * @param file
	 *            The file to save date to
	 * @throws TransporterException
	 */
	public void get(String url, File file) throws TransporterException;

	public InputStream getAsStream(String url) throws TransporterException;
	
	public InputStream getAsStream(String url, int bufferSize) throws TransporterException;
	
	/**
	 * 
	 * @param file
	 *            The file containing data to put
	 * @param url
	 *            The url where to put data to
	 * @throws TransporterException
	 */
	public void put(File file, String url) throws TransporterException;

}
