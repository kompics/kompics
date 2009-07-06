package se.sics.kompics.wan.master;

import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.master.plab.PLabHost;
import se.sics.kompics.wan.master.plab.PLabService;


/**
 * The <code>PLabServiceTest</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class PLabServiceTest {

	private static ApplicationContext ctx; 
	

	@SuppressWarnings("unchecked")
	public PLabServiceTest() {
//		try {
//			Configuration.init(new String[]{}, PlanetLabConfiguration.class);
//		} catch (ConfigurationException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}

//		ctx = PlanetLabConfiguration.getCtx();
		ctx = new ClassPathXmlApplicationContext(PlanetLabConfiguration.PLANETLAB_APP_CONTEXT);

	}
	
	@org.junit.Test 
	public void testPLabService()
	{
		PLabService service = (PLabService) ctx.getBean("PLabService");
		
		List<PLabHost> listHosts = service.getHostsFromDB();
		assert(listHosts.size() == 0);
		
		PLabHost host = new PLabHost("lucan.sics.se");
		
		listHosts.add(host);
		
		service.storeHostsToDB(listHosts);
		
	}
}
