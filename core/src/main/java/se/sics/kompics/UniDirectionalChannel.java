/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public class UniDirectionalChannel<PT extends PortType> extends SimpleChannel<PT> {

    public static enum Direction {

        TO_POSITIVE, TO_NEGATIVE;
    }

    private final Direction direction;

    private UniDirectionalChannel(PortCore<PT> positivePort, PortCore<PT> negativePort, Direction direction) {
        super(positivePort, negativePort);
        this.direction = direction;
    }

    @Override
    public void forwardToPositive(KompicsEvent event, int wid) {
        if (!destroyed && (direction == Direction.TO_POSITIVE)) {
            positivePort.doTrigger(event, wid, this);
        }
    }

    @Override
    public void forwardToNegative(KompicsEvent event, int wid) {
        if (!destroyed && (direction == Direction.TO_NEGATIVE)) {
            negativePort.doTrigger(event, wid, this);
        }
    }

    public static class Factory implements ChannelFactory {

        private final Direction direction;

        public Factory(Direction direction) {
            this.direction = direction;
        }

        @Override
        public <P extends PortType> Channel<P> connect(PortCore<P> positivePort, PortCore<P> negativePort) {
            UniDirectionalChannel<P> c = new UniDirectionalChannel<P>(positivePort, negativePort, direction);
            positivePort.addChannel(c);
            negativePort.addChannel(c);
            return c;
        }

        @Override
        public <P extends PortType, E extends KompicsEvent, F> Channel<P> connect(PortCore<P> positivePort,
                PortCore<P> negativePort, ChannelSelector<E, F> selector) {
            UniDirectionalChannel<P> c = new UniDirectionalChannel<P>(positivePort, negativePort, direction);
            Class<E> eventType = selector.getEventType();
            if (selector.isPositive()) {
                if (!c.portType.hasPositive(eventType)) {
                    throw new RuntimeException("Port type " + c.portType + " has no positive " + eventType);
                }
                if (this.direction != Direction.TO_NEGATIVE) {
                    throw new RuntimeException("Selectors have to be added to the sending side of a one-way channel!");
                }
                positivePort.addChannel(c, selector);
                negativePort.addChannel(c);
            } else {
                if (!c.portType.hasNegative(eventType)) {
                    throw new RuntimeException("Port type " + c.portType + " has no negative " + eventType);
                }
                if (this.direction != Direction.TO_POSITIVE) {
                    throw new RuntimeException("Selectors have to be added to the sending side of a one-way channel!");
                }
                positivePort.addChannel(c);
                negativePort.addChannel(c, selector);
            }
            return c;
        }

    }

}
