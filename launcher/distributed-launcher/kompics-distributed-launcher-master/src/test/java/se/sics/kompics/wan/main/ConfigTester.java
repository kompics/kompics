
package se.sics.kompics.wan.main;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.configuration.ConfigurationException;

import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;

/**
 * Unit test for simple App.
 */
public class ConfigTester extends TestCase {

	/**
	 * Create the test case
	 * 
	 * @param testName
	 *            name of the test case
	 */
	public ConfigTester(String testName) {
		super(testName);
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite() {
		return new TestSuite(ConfigTester.class);
	}

	public void config() {

		String[] args = {""};
		try {
			MasterConfiguration mc = 
				(MasterConfiguration) Configuration.init(args, MasterConfiguration.class);
			mc.getHosts();
//
//			ChordSystemConfiguration ch =
//				(ChordSystemConfiguration) Configuration.init(args, ChordSystemConfiguration.class);
//			ch.getMonitorConfiguration();
//			ch.getSuspectedPeriod();
//			ch.getMinRTo();
//			ch.getMonitorConfiguration();
//			ch.getFdConfiguration();
			
			
//			CyclonConfiguration cc = (CyclonConfiguration) Configuration.init(args, CyclonConfiguration.class);
//			cc.getHomepage();
//			cc.getIp();
//			cc.getDaemonAddress();
//			cc.getPort();

//			DaemonConfiguration dc = (DaemonConfiguration) Configuration.init(args, DaemonConfiguration.class);
//			dc.getDaemonRetryPeriod();
//			dc.getDaemonRetryCount();

			
			PlanetLabConfiguration pc = (PlanetLabConfiguration) 
					Configuration.init(args, PlanetLabConfiguration.class);
			
			
			assertTrue(true);
		} catch (ConfigurationException e) {
			e.printStackTrace();
			assertTrue(false);
		} 
	}
}
