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
public interface ChannelFactory {
    public <P extends PortType> Channel<P> connect(PortCore<P> positivePort, PortCore<P> negativePort);
    public <P extends PortType> Channel<P> connect(PortCore<P> positivePort, PortCore<P> negativePort, ChannelSelector selector);
}
