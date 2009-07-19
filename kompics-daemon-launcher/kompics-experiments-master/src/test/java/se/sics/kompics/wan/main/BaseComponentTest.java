package se.sics.kompics.wan.main;

import java.util.concurrent.Semaphore;

import org.apache.commons.configuration.ConfigurationException;

import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;

public class BaseComponentTest {
		
	protected static Semaphore semaphore = new Semaphore(0);

	protected static final int EVENT_COUNT = 1;

	
		public void setupTest()
		{
			
				try {
					Configuration.init(new String[]{}, PlanetLabConfiguration.class);
				} catch (ConfigurationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			
			
			try {
				semaphore.acquire(EVENT_COUNT);
				System.out.println("Exiting unit test....");
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.err.println(e.getMessage());
			}
			
		}
		
		public void pass() {
			org.junit.Assert.assertTrue(true);
			semaphore.release();
		}
	
		public void fail(boolean release) {
			org.junit.Assert.assertTrue(false);
			if (release == true) {
				semaphore.release();
			}
		}
	
}

