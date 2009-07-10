package se.sics.kompics.wan.master;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.springframework.context.ApplicationContext;

import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.plab.PLabHost;
import se.sics.kompics.wan.plab.PLabService;


/**
 * The <code>PLabServiceTest</code> class.
 * 
 * @author Jim Dowling <jdowling@sics.se>
 * @author Cosmin Arad <cosmin@sics.se>
 */
public class PLabServiceTest {

	private final static boolean MOCKED = false;
	
	private static ApplicationContext springCtx; 
	Mockery jmockCtx = new Mockery();


	@SuppressWarnings("unchecked")
	public PLabServiceTest() {

		if (MOCKED == false) {
			try {
			Configuration.init(new String[]{}, PlanetLabConfiguration.class);
		} catch (ConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		springCtx = PlanetLabConfiguration.getCtx();
			
			
//			springCtx = new ClassPathXmlApplicationContext(PlanetLabConfiguration.PLANETLAB_APP_CONTEXT);
		}

	}
	
	@org.junit.Test 
	public void testPLabService()
	{
		if (MOCKED) {
			final PLabService pLabService = jmockCtx.mock(PLabService.class);
			final List<PLabHost> listHosts = new ArrayList<PLabHost>();
			PLabHost host = new PLabHost("lucan.sics.se", 3);		
			listHosts.add(host);
	
			jmockCtx.checking(new Expectations() {{
			    oneOf (pLabService).getHostsFromDB();
			}});
			jmockCtx.checking(new Expectations() {{
			    oneOf (pLabService).storeHostsToDB(listHosts);
			}});
		}
		else {
			PLabService service = (PLabService) springCtx.getBean("PLabService");
			List<PLabHost> listHosts = service.getHostsFromDB();
			assert(listHosts.size() == 0);
			service.storeHostsToDB(listHosts);
		}
	}
}
