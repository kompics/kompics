/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network.grizzly.kryo;

import com.esotericsoftware.kryo.Kryo;
//import com.esotericsoftware.kryo.io.Input;
//import com.esotericsoftware.kryo.io.Output;
import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.JUnit4TestAdapter;
import junit.framework.TestCase;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.grizzly.kryo.KryoMessage;
import se.sics.kompics.network.grizzly.test.TestMessage;

/**
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 */
public class KryoTest {// extends TestCase {

//    public static junit.framework.Test suite() {
//        return new JUnit4TestAdapter(KryoTest.class);
//    }

//    @Test
//    public void testMessageSerialisation() {
//        try {
//            Address src, dst;
//            src = new Address(InetAddress.getByName("127.0.0.1"), 22333, (byte) 0);
//            dst = new Address(InetAddress.getByName("127.0.0.1"), 22334, (byte) 0);
//            TestMessage msg = new TestMessage(src, dst, "World".getBytes());
//
//            Kryo kryo = new KryoReflectionFactorySupport();
//            KryoMessage.register(TestMessage.class);
//            KryoMessage.registerMessages(kryo, true);
//
//            for (int i = 0; i < 1000000; i++) {
//
//                ByteArrayOutputStream bos = new ByteArrayOutputStream();
//                Output output = new Output(bos);
//
//                kryo.writeClassAndObject(output, msg);
//
//                output.flush();
//                output.close();
//
//                byte[] bytes = bos.toByteArray();
//
//                ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
//                Input input = new Input(bis);
//
//                TestMessage msg2 = (TestMessage) kryo.readClassAndObject(input);
//
//                assertTrue(msg.getSource().equals(msg2.getSource()));
//                assertTrue(msg.getDestination().equals(msg2.getDestination()));
//                assertArrayEquals(msg.getPayload(), msg2.getPayload());
//                assertEquals(msg.getSeq(), msg2.getSeq());
//            }
//
//        } catch (Exception ex) {
//            fail(ex.getMessage());
//        }
//    }
}
