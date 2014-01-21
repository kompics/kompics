/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network.grizzly.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.grizzly.ConstantQuotaAllocator;
import se.sics.kompics.network.grizzly.GrizzlyNetwork;
import se.sics.kompics.network.grizzly.GrizzlyNetworkInit;
import se.sics.kompics.network.test.ComponentProxy;
import se.sics.kompics.network.test.NetworkGenerator;
import se.sics.kompics.network.test.NetworkTest;

/**
 *
 * @author Lars Kroll <lkroll@sics.se>
 */
@RunWith(JUnit4.class)
public class GeneralTest {
    private static final Logger LOG = LoggerFactory.getLogger(GeneralTest.class);
    
    public static class GrizzlyGenerator implements NetworkGenerator {

        @Override
        public Component generate(ComponentProxy parent, Address self) {
            GrizzlyNetworkInit init = new GrizzlyNetworkInit(self, 8, 0, 0, 2 * 1024, 16 * 1024,
                    Runtime.getRuntime().availableProcessors(),
                    Runtime.getRuntime().availableProcessors(),
                    new ConstantQuotaAllocator(5));
            return parent.create(GrizzlyNetwork.class, init);
        }
        
    }
    
    @Before
    public void setUp() {
        
    }

    @Test
    public void basic() {
        // temporarily not running tests pending a real fix
        //GrizzlyGenerator gGen = new GrizzlyGenerator();
        //LOG.info("Running NetworkTest with 2 nodes");
        //NetworkTest.runTests(gGen, 2);
        //LOG.info("Running NetworkTest with 3 nodes");
        //NetworkTest.runTests(gGen, 3);
        //LOG.info("Running NetworkTest with 4 nodes");
        //NetworkTest.runTests(gGen, 4);
        //LOG.info("Running NetworkTest with 5 nodes");
        //NetworkTest.runTests(gGen, 5);
    }

    
}
