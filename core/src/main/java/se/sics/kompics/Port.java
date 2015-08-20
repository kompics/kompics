/**
 * This file is part of the Kompics component model runtime.
 * 
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS)
 * Copyright (C) 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or
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
package se.sics.kompics;

// TODO: Auto-generated Javadoc
/**
 * The <code>Port</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public interface Port<P extends PortType> {

	/**
	 * Gets the port type.
	 * 
	 * @return the port type
	 */
	public P getPortType();
	
	/**
	 * trigger event on this port
	 * 
	 * @param event to be triggered
	 * @param wid ?
	 * @param channel that triggered the event
	 */
	public void doTrigger(KompicsEvent event, int wid, ChannelCore<?> channel);
	
	/**
	 * trigger event on this port
	 * 
	 * @param event to be triggered
	 * @param wid ?
	 * @param component that triggered the event
	 */
	public void doTrigger(KompicsEvent event, int wid, ComponentCore component);
	
	/**
	 * 
	 * @return the component the port is part of
	 */
	public ComponentCore getOwner();
	
	/**
	 * 
	 * @return complement port this one is connected to (if any)
	 */
	public PortCore<P> getPair();
	
	/**
	 * 
	 * @param port complement port
	 */
	public void setPair(PortCore<P> port);
	
	public <E extends KompicsEvent> void doSubscribe(Handler<E> handler);
        public void doSubscribe(MatchedHandler handler);
	
	public void addChannel(ChannelCore<P> channel);
	
	public void addChannel(ChannelCore<P> channel, ChannelFilter<?, ?> filter);
	
	public void removeChannelTo(PortCore<P> remotePort);
	
	public void enqueue(KompicsEvent event);
}
