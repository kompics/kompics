/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.kompics;

/**
 *
 * @author lkroll
 */
public class UniDirectionalChannel<PT extends PortType> extends SimpleChannel<PT> {

    public static enum Direction {

        TO_POSITIVE,
        TO_NEGATIVE;
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
            UniDirectionalChannel<P> c = new UniDirectionalChannel(positivePort, negativePort, direction);
            positivePort.addChannel(c);
            negativePort.addChannel(c);
            return c;
        }

        @Override
        public <P extends PortType> Channel<P> connect(PortCore<P> positivePort, PortCore<P> negativePort, ChannelSelector selector) {
            UniDirectionalChannel<P> c = new UniDirectionalChannel(positivePort, negativePort, direction);
             Class<? extends KompicsEvent> eventType = selector.getEventType();
            if (selector.isPositive()) {
                if (!c.portType.hasPositive(eventType)) {
                    throw new RuntimeException("Port type " + c.portType
                            + " has no positive " + eventType);
                }
                if (this.direction != Direction.TO_NEGATIVE) {
                    throw new RuntimeException("Selectors have to be added to the sending side of a one-way channel!");
                }
                positivePort.addChannel(c, selector);
                negativePort.addChannel(c);
            } else {
                if (!c.portType.hasNegative(eventType)) {
                    throw new RuntimeException("Port type " + c.portType
                            + " has no negative " + eventType);
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
