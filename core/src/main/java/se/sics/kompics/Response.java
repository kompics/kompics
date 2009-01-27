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

import java.util.ArrayDeque;

// TODO: Auto-generated Javadoc
/**
 * The <code>Response</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @author Jim Dowling <jdowling@sics.se>
 * @version $Id: Response.java 268 2008-09-28 19:18:04Z Cosmin $
 */
public abstract class Response extends Event {

	private ArrayDeque<Channel<?>> channelStack;
	
	/**
	 * Instantiates a new response.
	 * 
	 * @param request
	 *            the request
	 */
	protected Response(Request request) {
		channelStack = request.channelStack;
	}

	@Override
	void forwardedBy(Channel<?> channel) {
		channelStack.pop();
	}

	@Override
	Channel<?> getTopChannel() {
		return channelStack.peek();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		Response response = (Response) super.clone();
		response.channelStack = channelStack.clone();
		return response;
	}
}
