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
