package se.sics.kompics.kdld.main;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import se.sics.kompics.launch.Scenario;
import se.sics.kompics.launch.Topology;

/**
 * Unit test for simple App.
 */
public class RootTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public RootTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( RootTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testRoot()
    {
		Topology topology1 = new Topology() {
			{
				node(1, "127.0.0.1", 22031);
				node(2, "127.0.0.1", 22032);
				defaultLinks(1000,0);
			}
		};

		Scenario scenario1 = new Scenario(ApplicationGroup.class) {
			{
				command(1, "S1000:H:S10000:X"); // 
			}
		};

		scenario1.executeOn(topology1);
        assertTrue( true );
    }
}
