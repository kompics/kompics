/**
 * This file is part of the Kompics component model runtime.
 * <p>
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics;

/**
 *
 * @author lkroll
 */
public class BiDirectionalChannel<PT extends PortType> extends SimpleChannel<PT> {

    private BiDirectionalChannel(PortCore<PT> positivePort, PortCore<PT> negativePort) {
        super(positivePort, negativePort);
    }

    @Override
    public void forwardToPositive(KompicsEvent event, int wid) {
        if (!destroyed) {
            positivePort.doTrigger(event, wid, this);
        }
    }

    @Override
    public void forwardToNegative(KompicsEvent event, int wid) {
        if (!destroyed) {
            negativePort.doTrigger(event, wid, this);
        }
    }

    public static class Factory implements ChannelFactory {

        @Override
        public <P extends PortType> Channel<P> connect(PortCore<P> positivePort, PortCore<P> negativePort) {
            BiDirectionalChannel<P> c = new BiDirectionalChannel(positivePort, negativePort);
            positivePort.addChannel(c);
            negativePort.addChannel(c);
            return c;
        }

        @Override
        public <P extends PortType> Channel<P> connect(PortCore<P> positivePort, PortCore<P> negativePort, ChannelSelector selector) {
            BiDirectionalChannel<P> c = new BiDirectionalChannel(positivePort, negativePort);
            Class<? extends KompicsEvent> eventType = selector.getEventType();
            if (selector.isPositive()) {
                if (!c.portType.hasPositive(eventType)) {
                    throw new RuntimeException("Port type " + c.portType
                            + " has no positive " + eventType);
                }
                positivePort.addChannel(c, selector);
                negativePort.addChannel(c);
            } else {
                if (!c.portType.hasNegative(eventType)) {
                    throw new RuntimeException("Port type " + c.portType
                            + " has no negative " + eventType);
                }
                positivePort.addChannel(c);
                negativePort.addChannel(c, selector);
            }

            return c;
        }

    }

}
