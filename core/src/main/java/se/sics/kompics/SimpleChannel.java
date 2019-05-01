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
