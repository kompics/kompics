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

import java.util.UUID;

/**
 *
 * @author lkroll
 */
public interface ComponentProxy {

    public <P extends PortType> void trigger(KompicsEvent e, Port<P> p);
    
    public <P extends PortType> void answer(Direct.Request event);

    public <P extends PortType> void answer(Direct.Request req, Direct.Response resp);

    public <T extends ComponentDefinition> Component create(Class<T> definition, Init<T> initEvent);

    public <T extends ComponentDefinition> Component create(Class<T> definition, Init.None initEvent);

    public void destroy(Component component);

    @Deprecated
    public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative);

    @Deprecated
    public <P extends PortType> Channel<P> connect(Negative<P> negative, Positive<P> positive);

    @Deprecated
    public <P extends PortType> void disconnect(Negative<P> negative, Positive<P> positive);

    @Deprecated
    public <P extends PortType> void disconnect(Positive<P> positive, Negative<P> negative);

    @Deprecated
    public <P extends PortType> Channel<P> connect(Positive<P> positive,
            Negative<P> negative, ChannelSelector<?, ?> filter);

    @Deprecated
    public <P extends PortType> Channel<P> connect(Negative<P> negative,
            Positive<P> positive, ChannelSelector<?, ?> filter);

    public Negative<ControlPort> getControlPort();

    public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative, ChannelFactory factory);

    public <P extends PortType> Channel<P> connect(Positive<P> positive, Negative<P> negative, ChannelSelector<?, ?> selector, ChannelFactory factory);

    public <P extends PortType> void disconnect(Channel<P> c);

    public <E extends KompicsEvent, P extends PortType> void subscribe(Handler<E> handler, Port<P> port);

    public void subscribe(MatchedHandler handler, Port port);

    public void unsubscribe(MatchedHandler handler, Port port);

    public <E extends KompicsEvent, P extends PortType> void unsubscribe(Handler<E> handler, Port<P> port);

    public UUID id();

    public <P extends PortType> Positive<P> getPositive(Class<P> portType);

    public <P extends PortType> Negative<P> getNegative(Class<P> portType);
    
    public <P extends PortType> Positive<P> requires(Class<P> portType);
    
    public <P extends PortType> Negative<P> provides(Class<P> portType);
}
