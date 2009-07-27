package se.sics.kompics.wan;

import org.apache.log4j.Logger;


public class TestSocketServer {

	static Logger logger = Logger.getLogger(TestSocketServer.class);
	
	public TestSocketServer() {
		
	}
	
	@org.junit.Test
	public void testLogging()
	{
		logger.info("Testing socket logger");
	}
}
