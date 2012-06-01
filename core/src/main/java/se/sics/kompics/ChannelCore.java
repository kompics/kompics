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
 * The <code>ChannelCore</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id$
 */
public class ChannelCore<P extends PortType> implements Channel<P> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.Channel#hold()
	 */
	public void hold() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.Channel#plug()
	 */
	public void plug() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.Channel#resume()
	 */
	public void resume() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.Channel#unplug()
	 */
	public void unplug() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see se.sics.kompics.Channel#getPortType()
	 */
	public P getPortType() {
		return portType;
	}

	/* === PRIVATE === */

	private boolean destroyed;

	private Positive<P> positivePort;
	private Negative<P> negativePort;

	private P portType;

	public ChannelCore(Positive<P> positivePort, Negative<P> negativePort, P portType) {
		this.positivePort = positivePort;
		this.negativePort = negativePort;
		this.portType = portType;
		this.destroyed = false;
	}

	boolean isDestroyed() {
		return destroyed;
	}

	void destroy() {
		destroyed = true;
	}

	public Positive<P> getPositivePort() {
		return positivePort;
	}

	public Negative<P> getNegativePort() {
		return negativePort;
	}

	public void forwardToPositive(Event event, int wid) {
		if (!destroyed)
			positivePort.doTrigger(event, wid, this);
	}

	public void forwardToNegative(Event event, int wid) {
		if (!destroyed)
			negativePort.doTrigger(event, wid, this);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public String toString() {
		return "Channel<"+portType.getClass().getCanonicalName()+">";
	}
}
