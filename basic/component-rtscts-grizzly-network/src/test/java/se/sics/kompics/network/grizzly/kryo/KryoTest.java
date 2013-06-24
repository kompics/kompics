/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics.network.grizzly.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableSortedSet;
import de.javakaffee.kryoserializers.KryoReflectionFactorySupport;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author Lars Kroll <lkr@lars-kroll.com>
 */
//@RunWith(JUnit4.class)
public class KryoTest {
    
    
    // Don't run, doesn't work either...Kryo sux -.- especially the old one
    //@Test
    public void testImmutableCollections() {
        String[] things = new String[] {"A", "B", "C", "D"};
        List<String> list = new ArrayList<String>();
        Set<String> set = new HashSet<String>();
        SortedSet<String> sortedSet = new TreeSet<String>();
        Map<String, String> map = new HashMap<String, String>();
        SortedMap<String, String> sortedMap = new TreeMap<String, String>();
        for (String s : things) {
            list.add(s);
            set.add(s);
            sortedSet.add(s);
            map.put(s, s);
            sortedMap.put(s, s);
        }
        ImmutableList<String> iList = ImmutableList.copyOf(list);
        System.out.println(iList.getClass());
        ImmutableSet<String> iSet = ImmutableSet.copyOf(set);
        ImmutableSortedSet<String> iSortedSet = ImmutableSortedSet.copyOf(sortedSet);
        ImmutableMap<String, String> iMap = ImmutableMap.copyOf(map);
        ImmutableSortedMap<String, String> iSortedMap = ImmutableSortedMap.copyOfSorted(sortedMap);
        
        Kryo kryo = new KryoReflectionFactorySupport() {
            
            private final ImmutableCollectionsSerializer imCSerializer = new ImmutableCollectionsSerializer(this);
            
            @Override
            public Serializer newSerializer(final Class clazz) {
                if (ImmutableCollection.class.isAssignableFrom(clazz)) {
                    return imCSerializer;
                }
                if (ImmutableMap.class.isAssignableFrom(clazz)) {
                    return imCSerializer;
                }
                return super.newSerializer( clazz );
            }
        };
        KryoMessage.registerMessages(kryo, true);
        
        // List
        ByteBuffer buf = serialise(kryo, iList);
        ImmutableList<String> iListRes = (ImmutableList<String>) deserialise(kryo, buf);
        assertCollection(iList, iListRes);
        
        // Set
        buf = serialise(kryo, iSet);
        ImmutableSet<String> iSetRes = (ImmutableSet<String>) deserialise(kryo, buf);
        assertCollection(iSet, iSetRes);
        
        // SortedSet
        buf = serialise(kryo, iSortedSet);
        ImmutableSortedSet<String> iSortedSetRes = (ImmutableSortedSet<String>) deserialise(kryo, buf);
        assertCollection(iSortedSet, iSortedSetRes);
        
        // Map
        buf = serialise(kryo, iMap);
        ImmutableMap<String, String> iMapRes = (ImmutableMap<String, String>) deserialise(kryo, buf);
        assertMap(iMap, iMapRes);
        
        // SortedMap
        buf = serialise(kryo, iSortedMap);
        ImmutableSortedMap<String, String> iSortedMapRes = (ImmutableSortedMap<String, String>) deserialise(kryo, buf);
        assertMap(iSortedMap, iSortedMapRes);        
    }
    
    private void assertCollection(Collection<String> orig, ImmutableCollection<String> col) {
        Iterator<String> origIT = orig.iterator();
        Iterator<String> colIT = col.iterator();
        while(origIT.hasNext()) {
            String oS = origIT.next();
            assertTrue(colIT.hasNext());
            String cS = colIT.next();
            assertEquals(oS, cS);
        }
    }
    
    private void assertMap(Map<String, String> orig, ImmutableMap<String, String> col) {
        Iterator<Entry<String, String>> origIT = orig.entrySet().iterator();
        Iterator<Entry<String, String>> colIT = col.entrySet().iterator();
        while(origIT.hasNext()) {
            Entry<String, String> oE = origIT.next();
            String oK = oE.getKey();
            String oV = oE.getValue();
            assertTrue(colIT.hasNext());
            Entry<String, String> cE = colIT.next();
            String cK = cE.getKey();
            String cV = cE.getValue();
            assertEquals(oK, cK);
            assertEquals(oV, cV);
        }
    }
    
    private ByteBuffer serialise(Kryo kryo, Object o) {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        kryo.writeClassAndObject(buf, o);
        buf.flip();
        return buf;
    }
    private Object deserialise(Kryo kryo, ByteBuffer buf) {
        Object o = kryo.readClassAndObject(buf);
        buf.clear();
        return o;
    }

    // COMMENTED OUT BECAUSE IT'S FOR KRYO2 WHICH I COULDN'T GET TO WORK -.-
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
