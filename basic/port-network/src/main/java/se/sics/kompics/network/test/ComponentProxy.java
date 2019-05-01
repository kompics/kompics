/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.sics.kompics.network.test;

import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.ControlPort;
import se.sics.kompics.KompicsEvent;
import se.sics.kompics.Init;
import se.sics.kompics.Negative;
import se.sics.kompics.Port;
import se.sics.kompics.PortType;
import se.sics.kompics.Positive;

/**
 *
 * @author Lars Kroll {@literal <lkroll@kth.se>}
 */
public interface ComponentProxy {

    public <P extends PortType> void trigger(KompicsEvent e, Port<P> p);

    public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent);

    public <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent);

    public void destroy(Component component);

    public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative);

    public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive);

    public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive);

    public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative);

    public Negative<ControlPort> getControlPort();
}
