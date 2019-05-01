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

import java.lang.ref.WeakReference;

/**
 * 
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 *
 */
public final class RequestPathElement implements Comparable<RequestPathElement> {

    private final WeakReference<ChannelCore<?>> channel;

    private final WeakReference<ComponentCore> component;

    private final boolean isChannel;

    public RequestPathElement(ChannelCore<?> channel) {
        super();
        this.channel = new WeakReference<ChannelCore<?>>(channel);
        this.component = null;
        this.isChannel = true;
    }

    public RequestPathElement(ComponentCore component) {
        super();
        this.channel = null;
        this.component = new WeakReference<ComponentCore>(component);
        this.isChannel = false;
    }

    public ChannelCore<?> getChannel() {
        return channel.get();
    }

    public ComponentCore getComponent() {
        return component.get();
    }

    public boolean isChannel() {
        return isChannel;
    }

    @Override
    public String toString() {
        if (isChannel) {
            return "Channel: " + channel.get();
        }
        ComponentCore c = component.get();
        return "Component: " + (c == null ? null : c.getComponent());
    }

    @Override
    public int compareTo(RequestPathElement o) {
        if (isChannel != o.isChannel) {
            return isChannel ? -1 : 1;
        }
        if (isChannel) {
            ChannelCore<?> thisChannel = channel.get();
            ChannelCore<?> oChannel = o.channel.get();
            if (thisChannel == null) {
                return (oChannel == null) ? 0 : -1;
            }
            if (oChannel == null) {
                return 1;
            }
            if (thisChannel.equals(oChannel)) {
                return 0;
            } else {
                return isChannel ? -1 : 1;
            }
        } else {
            ComponentCore thisComp = component.get();
            ComponentCore oComp = o.component.get();
            if (thisComp == null) {
                return (oComp == null) ? 0 : -1;
            }
            if (oComp == null) {
                return 1;
            }
            if (thisComp.equals(oComp)) {
                return 0;
            } else {
                return isChannel ? -1 : 1;
            }
        }
    }
}
