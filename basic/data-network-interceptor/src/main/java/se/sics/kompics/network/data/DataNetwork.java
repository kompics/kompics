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
package se.sics.kompics.network.data;

import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ComponentProxy;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class DataNetwork extends ComponentDefinition {

    final Negative<Network> net = provides(Network.class);

    public DataNetwork(Init init) {
        Component dataInterceptorC = create(DataStreamInterceptor.class, Init.NONE);
        Component networkC = init.hook.setupNetwork(proxy);
        init.hook.connectTimer(proxy, dataInterceptorC);
        Positive<Network> nettyPort = networkC.getPositive(Network.class);
        Negative<Network> interceptorPortReq = dataInterceptorC.getNegative(Network.class);
        Positive<Network> interceptorPortProv = dataInterceptorC.getPositive(Network.class);
        connect(nettyPort, interceptorPortReq, Channel.TWO_WAY);
        connect(interceptorPortProv, net, new DataSelector(), Channel.ONE_WAY_POS);
        connect(interceptorPortProv, net, new DataNotifySelector(), Channel.ONE_WAY_POS);
        connect(nettyPort, net, new NotDataSelector(), Channel.ONE_WAY_POS);
        connect(nettyPort, net, new NotDataNotifySelector(), Channel.ONE_WAY_POS);
        connect(nettyPort, net, Channel.ONE_WAY_NEG);
    }

    public static class Init extends se.sics.kompics.Init<DataNetwork> {
        public final NetHook hook;

        public Init(NetHook hook) {
            this.hook = hook;
        }
    }

    public static interface NetHook {

        public Component setupNetwork(ComponentProxy proxy);

        public void connectTimer(ComponentProxy proxy, Component c);
    }
}
