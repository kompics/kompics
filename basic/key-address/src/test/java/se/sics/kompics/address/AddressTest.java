/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.address;

import java.net.InetAddress;
import java.net.UnknownHostException;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 */
public class AddressTest extends TestCase {
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(AddressTest.class);
    }
    
    @Test
    public void testStringify() {
        InetAddress ia;
        try {
            ia = InetAddress.getByName("127.0.0.1");
            Address a1 = new Address(ia, 1234, new byte[] {0x01, 0x02, 0x04, 0x08});
            Address a2 = new Address(ia, 1234, new byte[] {-0x01, -0x02, -0x04, -0x08});
            
            
            assertEquals("127.0.0.1:1234/01 02 04 08", a1.toString());
            assertEquals("127.0.0.1:1234/FF FE FC F8", a2.toString());
            

            
        } catch (UnknownHostException ex) {
            fail(ex.getMessage());
        }
        
    }
    
    @Test
    public void testCompare() {
        InetAddress ia, ia2, ia3;
        try {
            ia = InetAddress.getByName("127.0.0.1");
            ia2 = InetAddress.getByName("127.0.0.1");
            ia3 = InetAddress.getByName("127.0.0.2");
            Address a1 = new Address(ia, 1234, new byte[] {0x01, 0x02, 0x04, 0x08});
            Address a2 = new Address(ia2, 1234, new byte[] {0x01, 0x02, 0x04, 0x08});
            Address a3 = new Address(ia3, 4321, new byte[] {-0x01, -0x02, -0x04, -0x08});
            Address a4 = new Address(ia, 4312, new byte[] {-0x01, -0x02, -0x04, -0x08});
            Address a5 = new Address(ia, 1234, new byte[] {-0x01, -0x02, -0x04, -0x08});
            
            
            assertEquals(a1, a2);
            assertEquals(a2, a1);
            assertThat(a1, not(a3));
            assertThat(a1, not(a4));
            assertThat(a1, not(a5));
            
            assertEquals(0, a1.compareTo(a2));
            assertEquals(0, a2.compareTo(a1));
            assertTrue(a1.compareTo(a3) < 0);
            assertTrue(a3.compareTo(a1) > 0);
            assertTrue(a1.compareTo(a4) < 0);
            assertTrue(a4.compareTo(a1) > 0);
            assertTrue(a1.compareTo(a5) < 0);
            assertTrue(a5.compareTo(a1) > 0);
            
        } catch (UnknownHostException ex) {
            fail(ex.getMessage());
        }
    }
    
    @Test
    public void testStoreAndParse() {
        byte[] id1 = new byte[] {0x01, 0x02, 0x04, 0x08};
        byte[] id2 = new byte[] {-0x01, -0x02, -0x04, -0x08};
        
        String store1 = IdUtils.storeFormat(id1);
        String store2 = IdUtils.storeFormat(id2);
        
        assertEquals("0x01020408", store1);
        assertEquals("0xFFFEFCF8", store2);
        
        byte[] storedId1 = IdUtils.parseStoreFormat(store1);
        byte[] storedId2 = IdUtils.parseStoreFormat(store2);
        
        assertEquals("01 02 04 08", IdUtils.printFormat(storedId1));
        assertEquals("FF FE FC F8", IdUtils.printFormat(storedId2));
        
        assertArrayEquals(id1, storedId1);
        assertArrayEquals(id2, storedId2);
    }
}
