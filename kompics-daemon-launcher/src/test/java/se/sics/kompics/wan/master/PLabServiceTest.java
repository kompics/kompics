package se.sics.kompics.wan.master;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.springframework.context.ApplicationContext;

import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.PlanetLabConfiguration;
import se.sics.kompics.wan.plab.PLabHost;
import se.sics.kompics.wan.plab.PLabService;
import se.sics.kompics.wan.plab.PLabSite;
import se.sics.kompics.wan.plab.PLabStore;


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
			
		}
	}
	
	@org.junit.Test 
	public void testPLabService()
	{
//		HibernateUtil.setup("create table EVENTS ( uid int, name VARCHAR, start_Date date);");
		
		if (MOCKED) {
			final PLabService pLabService = jmockCtx.mock(PLabService.class);
			final Set<PLabHost> hosts = new HashSet<PLabHost>();
			PLabHost host = new PLabHost("lucan.sics.se", 3);		
			hosts.add(host);
			PLabSite site = new PLabSite();
			Set<PLabSite> sites = new HashSet<PLabSite>();
			sites.add(site);
			
			final PLabStore store = new PLabStore();
			store.setHosts(hosts);
			store.setSites(sites);
			store.setSlice("sics_grid4all");
	
			jmockCtx.checking(new Expectations() {{
			    oneOf (pLabService).load("sics_grid4all");
			}});
			jmockCtx.checking(new Expectations() {{
			    oneOf (pLabService).save(store);
			}});
		}
		else {
			PLabService service = (PLabService) springCtx.getBean("PLabService");
			if (service == null)
			{
				System.out.println("Service object was null");
			}

			PLabStore store = new PLabStore("sics_grid4all", "kosta");
			PLabHost host = new PLabHost("lqist.com");
			Set<PLabHost> hosts =  new HashSet<PLabHost>();
			hosts.add(host);
			PLabSite site = new PLabSite();
			Set<PLabSite> sites =  new HashSet<PLabSite>();
			sites.add(site);
			store.setHosts(hosts);
			store.setSites(sites);
			service.save(store);

			store = service.load("sics_grid4all");
			Set<PLabHost> returnedHosts = store.getHosts();
			System.out.println("returned num hosts: " + returnedHosts.size());
			assert(store.getHosts().size() == 1);
		}
	}
}
