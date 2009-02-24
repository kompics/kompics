package ${package}.main;

import se.sics.kompics.Component;
import se.sics.kompics.Kompics;
import ${package}.main.Root;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
    	Kompics.createAndStart(Root.class);
    	
        assertTrue( true );
    }
}
