/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics.master.swing.model;

import junit.framework.TestCase;
import se.sics.kompics.wan.config.MasterConfiguration;

/**
 *
 * @author jdowling
 */
public class UserModelTest extends TestCase {
    
    public UserModelTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of load method, of class UserModel.
     */
    public void testLoad_String() throws Exception {
        System.out.println("load");
        String file = MasterConfiguration.USER_FILE;
        UserModel instance = new UserModel();
        UserEntry entry = new UserEntry();
        entry.setSshLoginName("csl");
        entry.setSshPassword("blah");
        entry.setSshKeyFilename("/home/jdowling/.ssh/id_rsa");
        entry.setSshKeyFilePassword("blahblah");
        entry.setSlice("sics_blah");

        instance.setUserEntry(entry);

        instance.save(file);

        instance.load(file);
        UserEntry entry2 = instance.getUserEntry();
        assertTrue(entry.getSshLoginName().compareTo(entry2.getSshLoginName()) == 0);
        assertTrue(entry.getSshPassword().compareTo(entry2.getSshPassword()) == 0);
        assertTrue(entry.getSshKeyFilename().compareTo(entry2.getSshKeyFilename()) == 0);
        assertTrue(entry.getSshKeyFilePassword().compareTo(entry2.getSshKeyFilePassword()) == 0);
        assertTrue(entry.getSlice().compareTo(entry2.getSlice()) == 0);
    }


}
