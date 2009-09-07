/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics.wan.main;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import se.sics.kompics.wan.daemon.maven.MavenExecException;
import se.sics.kompics.wan.daemon.maven.MavenWrapper;

/**
 *
 * @author jdowling
 */
public class MavenEmbedderT {

    public MavenEmbedderT() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    // @Test
    // public void hello() {}

    @Test
    public void mavenEmbedder() {
        try {
            MavenWrapper mw = new MavenWrapper("/home/jdowling/.kompics/se/sics/kompics/kompics-manual/0.4.0/pom.xml");

            mw.execute("assembly:assembly",null);
            try {
                Thread.currentThread().sleep(5 * 1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MavenEmbedderT.class.getName()).log(Level.SEVERE, null, ex);
            }

            mw.execute("exec:exec",null);

        } catch (MavenExecException ex) {
            Logger.getLogger(MavenEmbedderT.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

}