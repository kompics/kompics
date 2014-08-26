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
 * The <code>Filter</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public abstract class Filter<E extends KompicsEvent> {

	Port <? extends PortType> port;
	
	Filter<?> next;
	
	/**
	 * Instantiates a new filter.
	 */
	public Filter() {
		port = null;
		next = null;
	}
	
	/**
	 * On.
	 * 
	 * @param port
	 *            the port
	 * 
	 * @return the filter< e>
	 */
	public Filter<E> on(Port<? extends PortType> port) {
		this.port = port;
		return this;
	}

	/**
	 * Or.
	 * 
	 * @param filter
	 *            the filter
	 * 
	 * @return the filter<?>
	 */
	public Filter<?> or(Filter<?> filter) {
		this.next = filter;
		return filter;
	}

	/**
	 * Filter.
	 * 
	 * @param event
	 *            the event
	 * 
	 * @return true, if successful
	 */
	protected abstract boolean filter(E event);
}
