/* 
 * This file is part of the CaracalDB distributed storage system.
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
package se.sics.kompics.network.netty;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Transport;
import se.sics.kompics.network.test.ComponentProxy;
import se.sics.kompics.network.test.NetworkGenerator;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
@RunWith(JUnit4.class)
public class NetworkTest {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkTest.class);

    @Test
    public void streamTest() {
        final Transport[] protos = new Transport[]{Transport.TCP, Transport.UDT};

        NetworkGenerator netGen = new NetworkGenerator() {

            @Override
            public Component generate(ComponentProxy parent, Address self) {
                NettyInit init = new NettyInit(self, 0, ImmutableSet.copyOf(protos));
                return parent.create(NettyNetwork.class, init);
            }

        };
        //Transport[] protos = new Transport[]{Transport.TCP};

        LOG.info("********* 2 Node Network Test ***********");
        se.sics.kompics.network.test.NetworkTest.runTests(netGen, 2, protos);
        LOG.info("********* 5 Node Network Test ***********");
        se.sics.kompics.network.test.NetworkTest.runTests(netGen, 5, protos);

    }

    @Test
    public void datagramTest() {

        final Transport[] protos = new Transport[]{Transport.UDP};

        NetworkGenerator netGen = new NetworkGenerator() {

            @Override
            public Component generate(ComponentProxy parent, Address self) {
                NettyInit init = new NettyInit(self, 0, ImmutableSet.copyOf(protos));
                return parent.create(NettyNetwork.class, init);
            }

        };

        LOG.info("********* 2 Node Datagram Network Test ***********");
        se.sics.kompics.network.test.NetworkTest.runAtLeastTests(netGen, 2, protos);
        LOG.info("********* 5 Node Datagram Network Test ***********");
        se.sics.kompics.network.test.NetworkTest.runAtLeastTests(netGen, 5, protos);
    }
}
