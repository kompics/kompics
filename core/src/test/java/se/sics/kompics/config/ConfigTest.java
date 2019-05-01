/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.config;

import com.google.common.base.Optional;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.junit.matchers.JUnitMatchers.hasItems;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
@RunWith(JUnit4.class)
public class ConfigTest {

    @Test
    public void basicTest() {
        Config conf = TypesafeConfig.load();

        // Primitives types
        Optional<Long> lval = conf.readValue("config.testl");
        assertTrue(lval.isPresent());
        try {
            Long lll = lval.get();
            assertEquals(Long.valueOf(5), lll);
        } catch (ClassCastException ex) { // Typesafe might read this as an integer instead
            @SuppressWarnings("rawtypes")
            Optional oval = (Optional) lval;
            @SuppressWarnings("unchecked")
            Optional<Integer> ival = oval;
            assertEquals(Integer.valueOf(5), ival.get());
        }
        lval = conf.readValue("config.testl", Long.class);
        assertTrue(lval.isPresent());
        assertEquals(Long.valueOf(5), lval.get());
        long ll = conf.getValue("config.testl", Long.class);
        assertEquals(5l, ll);
        ll = conf.getValueOrDefault("config.testl", 5l);
        assertEquals(5l, ll);
        // Simple types
        Optional<String> val = conf.readValue("config.test");
        assertTrue(val.isPresent());
        assertEquals("testValue", val.get());
        val = conf.readValue("config.test", String.class);
        assertTrue(val.isPresent());
        assertEquals("testValue", val.get());
        String s = conf.getValue("config.test", String.class);
        assertEquals("testValue", s);
        s = conf.getValueOrDefault("config.test", "wrongValue");
        assertEquals("testValue", s);
        // Lists
        List<String> l = conf.getValues("config.testList");
        assertNotNull(l);
        assertFalse(l.isEmpty());
        assertThat(l, hasItems("v1", "v2", "v3"));
        l = conf.getValues("config.testList", String.class);
        assertNotNull(l);
        assertFalse(l.isEmpty());
        assertThat(l, hasItems("v1", "v2", "v3"));
    }

    @Test
    public void wrongTypesTest() {
        Config conf = TypesafeConfig.load();

        // Simple types
        Optional<Long> val = conf.readValue("config.test");
        assertTrue(val.isPresent());
        try {
            long l = val.get();
            fail("Value  " + l + " is not actually a long");
        } catch (Exception ex) {
            System.out.println("Thrown exception was: \n" + ex);
        }
        val = conf.readValue("config.test", Long.class);
        assertFalse(val.isPresent()); // since it can't be cast it should be absent
        try {
            Long s = conf.getValue("config.test", Long.class);
            fail("Value " + s + " is not actually a long");
        } catch (Exception ex) {
            System.out.println("Thrown exception was: \n" + ex);
        }
        Long s = conf.getValueOrDefault("config.test", 1l);
        assertEquals(1l, s.longValue());

    }

    @Test
    public void modifyTests() {
        Config conf = TypesafeConfig.load();
        long ll = conf.getValue("config.testl", Long.class);
        assertEquals(5l, ll);

        Config.Builder builder = conf.modify(UUID.randomUUID());
        builder.setValue("config.testl", 10l);
        ConfigUpdate up = builder.finalise();
        Config.Impl configB = (Config.Impl) conf.copy(false);
        configB.apply(up, ValueMerger.NONE);
        ll = conf.getValue("config.testl", Long.class);
        assertEquals(5l, ll);
        long lll = configB.getValue("config.testl", Long.class);
        assertEquals(10l, lll);
    }
}
