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

import java.util.Collection;
import java.util.Map;

/**
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public abstract class Unsafe {

    public static Map<Class<? extends PortType>, JavaPort<? extends PortType>> getPositivePorts(Component component) {
        return ((JavaComponent) component).getPositivePorts();
    }

    public static Map<Class<? extends PortType>, JavaPort<? extends PortType>> getNegativePorts(Component component) {
        return ((JavaComponent) component).getNegativePorts();
    }

    public static Collection<Class<? extends KompicsEvent>> getPositiveEvents(PortType portType) {
        return portType.getPositiveEvents();
    }

    public static Collection<Class<? extends KompicsEvent>> getNegativeEvents(PortType portType) {
        return portType.getNegativeEvents();
    }

    public static void setOrigin(Direct.Request<? extends Direct.Response> request, Port<?> origin) {
        request.origin = origin;
    }

    public static Port<?> getOrigin(Direct.Request<? extends Direct.Response> request) {
        return request.getOrigin();
    }

    public static <P extends PortType> JavaPort<P> createJavaPort(boolean positive, P portType, ComponentCore owner) {
        return new JavaPort<P>(positive, portType, owner);
    }
}
