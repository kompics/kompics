
package se.sics.kompics.kdld.main;

import java.io.FileNotFoundException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.configuration.ConfigurationException;

import se.sics.kompics.kdld.util.ChordConfiguration;
import se.sics.kompics.kdld.util.Configuration;
import se.sics.kompics.kdld.util.CyclonConfiguration;
import se.sics.kompics.kdld.util.DaemonConfiguration;
import se.sics.kompics.kdld.util.HostsParserException;
import se.sics.kompics.kdld.util.MasterConfiguration;

/**
 * Unit test for simple App.
 */
public class ConfigTest extends TestCase {

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public ConfigTest(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(ConfigTest.class);
	}

	public void testConfig() {

		String[] args = {""};
		try {
			MasterConfiguration mc = 
				(MasterConfiguration) Configuration.init(args, MasterConfiguration.class);
			mc.getHosts();
			
			ChordConfiguration ch = 
				(ChordConfiguration) Configuration.init(args, ChordConfiguration.class);
			ch.getMonitorConfiguration();
			ch.getSuspectedPeriod();
			ch.getMinRTo();
			ch.getMonitorConfiguration();
			ch.getFdConfiguration();
			
			
			CyclonConfiguration cc = (CyclonConfiguration) Configuration.init(args, CyclonConfiguration.class);
			cc.getHomepage();
			cc.getIp();
			cc.getPeer0Address();
			cc.getPort();

			DaemonConfiguration dc = (DaemonConfiguration) Configuration.init(args, DaemonConfiguration.class);
			dc.getDaemonRetryPeriod();
			dc.getDaemonRetryCount();
			
			assertTrue(true);
		} catch (ConfigurationException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (HostsParserException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();			
			assertTrue(false);
		}
		
	}
}
