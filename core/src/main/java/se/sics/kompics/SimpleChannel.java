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
public abstract class SimpleChannel<PT extends PortType> implements ChannelCore<PT> {
    /* === PRIVATE === */
    protected volatile boolean destroyed = false;
    protected final PortCore<PT> positivePort;
    protected final PortCore<PT> negativePort;
    protected final PT portType;
    
    public SimpleChannel(PortCore<PT> positivePort, PortCore<PT> negativePort) {
        this.positivePort = positivePort;
        this.negativePort = negativePort;
        this.portType = positivePort.portType;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    private void destroy() {
        destroyed = true;
    }

    @Override
    public boolean hasPositivePort(Port<PT> port) {
        return positivePort == port;
    }

    @Override
    public boolean hasNegativePort(Port<PT> port) {
        return negativePort == port;
    }
    
    @Override
    public void disconnect() {
        this.destroy();
        positivePort.removeChannel(this);
        negativePort.removeChannel(this);
    }

    @Override
    public PT getPortType() {
        return portType;
    }
}
