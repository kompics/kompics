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

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import org.jscience.mathematics.number.Rational;
import se.sics.kompics.config.Config;
import se.sics.kompics.network.data.policies.ProtocolRatioPolicy;
import se.sics.kompics.network.data.policies.ProtocolSelectionPolicy;
import se.sics.kompics.network.data.policies.RandomSelection;
import se.sics.kompics.network.data.policies.StaticRatio;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
@SuppressWarnings("rawtypes") // TODO fix this at some point maybe
class ConnectionFactory {

    // private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final ClassLoader classLoader = getClass().getClassLoader(); // TODO write some better class loader
                                                                         // selection logic

    private final Class<?> ratioPolicy; // should be Class<ProtocolSelectionPolicy> but Java's generics are stupid
    private final Class<?> selectionPolicy; // should be Class<ProtocolRatioPolicy> but Java's generics are stupid
    private final Config config;

    ConnectionFactory(Config conf, Optional<String> ratioPolicyS, Optional<String> selectionPolicyS) {
        config = conf;
        if (ratioPolicyS.isPresent()) {
            try {
                ratioPolicy = classLoader.loadClass(ratioPolicyS.get());

                DataStreamInterceptor.EXT_LOG.info("Using RatioPolicy: {}", ratioPolicy.getName());
            } catch (Throwable ex) {
                DataStreamInterceptor.EXT_LOG.error("Could not find ratio policy.", ex);
                throw new RuntimeException(ex);
            }
        } else {
            DataStreamInterceptor.EXT_LOG.info("Using default ratio policy: static 50/50");
            ratioPolicy = StaticRatio.FiftyFifty.class;
        }
        if (selectionPolicyS.isPresent()) {
            try {
                selectionPolicy = classLoader.loadClass(selectionPolicyS.get());
                DataStreamInterceptor.EXT_LOG.info("Using SelectionPolicy: {}", selectionPolicy.getName());
            } catch (Throwable ex) {
                DataStreamInterceptor.EXT_LOG.error("Could not find selection policy.", ex);
                throw new RuntimeException(ex);
            }
        } else {
            DataStreamInterceptor.EXT_LOG.info("Using default selection policy: random");
            selectionPolicy = RandomSelection.class;
        }
    }

    @SuppressWarnings("unchecked")
    ConnectionTracker newConnection(InetSocketAddress target) {
        try {
            Constructor<?> ratioConstructor = ratioPolicy.getConstructor(Config.class);
            ProtocolSelectionPolicy<?> psp = (ProtocolSelectionPolicy<?>) selectionPolicy.newInstance();
            ProtocolRatioPolicy prp = (ProtocolRatioPolicy) ratioConstructor.newInstance(config);
            return new ConnectionTracker(target, psp, prp);
        } catch (Throwable ex) {
            DataStreamInterceptor.EXT_LOG.error("Could not instantiate policy. Error was: \n {}", ex);
            throw new RuntimeException(ex);
        }
    }

    ConnectionTracker deserialiseConnection(ByteBuf buf) {
        try {
            Constructor<?> ratioConstructor = ratioPolicy.getConstructor(Config.class);
            ProtocolSelectionPolicy<?> psp = (ProtocolSelectionPolicy<?>) selectionPolicy.newInstance();
            ProtocolRatioPolicy prp = (ProtocolRatioPolicy) ratioConstructor.newInstance(config);
            return ConnectionTracker.fromBinary(buf, psp, prp);
        } catch (Throwable ex) {
            DataStreamInterceptor.EXT_LOG.error("Could not instantiate policy. Error was: \n {}", ex);
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    ConnectionTracker newConnection(InetSocketAddress target, Rational initialRatio) {
        try {
            Constructor<?> ratioConstructor = ratioPolicy.getConstructor(Config.class);
            ProtocolSelectionPolicy<?> psp = (ProtocolSelectionPolicy<?>) selectionPolicy.newInstance();
            ProtocolRatioPolicy prp = (ProtocolRatioPolicy) ratioConstructor.newInstance(config);
            return new ConnectionTracker(target, initialRatio, psp, prp);
        } catch (Throwable ex) {
            DataStreamInterceptor.EXT_LOG.error("Could not instantiate policy. Error was: \n {}", ex);
            throw new RuntimeException(ex);
        }
    }

    ConnectionTracker findConnection(InetSocketAddress target) {
        return newConnection(target); // TODO replace with gossipped data if available
    }
}
