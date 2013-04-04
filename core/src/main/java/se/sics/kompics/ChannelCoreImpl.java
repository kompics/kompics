/**
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

// TODO: Auto-generated Javadoc
/**
 * The
 * <code>ChannelCoreImpl</code> class.
 *
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @author Lars Kroll <lkroll@sics.se>
 * @version $Id: ChannelCore.java 4053 2012-06-01 12:17:19Z lars $
 */
public class ChannelCoreImpl<P extends PortType> implements ChannelCore<P> {

    /*
     * (non-Javadoc)
     * 
     * @see se.sics.kompics.Channel#hold()
     */
    @Override
    public void hold() {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see se.sics.kompics.Channel#plug()
     */
    @Override
    public void plug() {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see se.sics.kompics.Channel#resume()
     */
    @Override
    public void resume() {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see se.sics.kompics.Channel#unplug()
     */
    @Override
    public void unplug() {
        // TODO Auto-generated method stub
    }

    /*
     * (non-Javadoc)
     * 
     * @see se.sics.kompics.Channel#getPortType()
     */
    @Override
    public P getPortType() {
        return portType;
    }

    /* === PRIVATE === */
    private boolean destroyed;
    private Positive<P> positivePort;
    private Negative<P> negativePort;
    private P portType;

    public ChannelCoreImpl(Positive<P> positivePort, Negative<P> negativePort, P portType) {
        this.positivePort = positivePort;
        this.negativePort = negativePort;
        this.portType = portType;
        this.destroyed = false;
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public void destroy() {
        destroyed = true;
    }

    @Override
    public Positive<P> getPositivePort() {
        return positivePort;
    }

    @Override
    public Negative<P> getNegativePort() {
        return negativePort;
    }

    @Override
    public void forwardToPositive(Event event, int wid) {
        if (!destroyed) {
            positivePort.doTrigger(event, wid, this);
        }
    }

    @Override
    public void forwardToNegative(Event event, int wid) {
        if (!destroyed) {
            negativePort.doTrigger(event, wid, this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public String toString() {
        return "Channel<" + portType.getClass().getCanonicalName() + ">";
    }
}
