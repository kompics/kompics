package se.sics.kompics.kdld.main;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import se.sics.kompics.Kompics;
import se.sics.kompics.address.Address;
import se.sics.kompics.kdld.job.DummyPomConstructionException;
import se.sics.kompics.kdld.job.JobToDummyPom;

/**
 * Unit test for simple App.
 */
public class IndexerTest extends TestCase {

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public IndexerTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(IndexerTest.class);
	}

	public void testIndexer() {

		Kompics.createAndStart(TestIndexerComponent.class,2);

	}
	


}
