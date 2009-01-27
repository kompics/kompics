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
 * @version $Id: ChannelCore.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public class ChannelCore<P extends PortType> implements Channel<P> {

	/* (non-Javadoc)
	 * @see se.sics.kompics.Channel#hold()
	 */
	public void hold() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.Channel#plug()
	 */
	public void plug() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.Channel#resume()
	 */
	public void resume() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.Channel#unplug()
	 */
	public void unplug() {
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see se.sics.kompics.Channel#getPortType()
	 */
	public P getPortType() {
		return portType;
	}

	/* === PRIVATE === */

	private PortCore<P> positivePort, negativePort;

	private P portType;

	ChannelCore(PortCore<P> positivePort, PortCore<P> negativePort, P portType) {
		this.positivePort = positivePort;
		this.negativePort = negativePort;
		this.portType = portType;
	}

	PortCore<P> getPositivePort() {
		return positivePort;
	}

	PortCore<P> getNegativePort() {
		return negativePort;
	}

	void forwardToPositive(Event event, int wid) {
		event.forwardedBy(this);
		positivePort.doTrigger(event, wid);
	}

	void forwardToNegative(Event event, int wid) {
		event.forwardedBy(this);
		negativePort.doTrigger(event, wid);
	}
}
