/**
 * This file is part of the ID2210 course assignments kit.
 * 
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
package se.sics.kompics.wan.web;

import se.sics.kompics.Request;

/**
 * The <code>WebRequest</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 */
public final class WebRequest extends Request {

	private final long id;
	public final int destination;
	private final String target;
	private final org.mortbay.jetty.Request request;

	public WebRequest(int destination, long id, String target,
			org.mortbay.jetty.Request request) {
		super();
		this.destination = destination;
		this.id = id;
		this.target = target;
		this.request = request;
	}

	public long getId() {
		return id;
	}

	public String getTarget() {
		return target;
	}

	public org.mortbay.jetty.Request getRequest() {
		return request;
	}
	
	public int getDestination() {
		return destination;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + destination;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((target == null) ? 0 : target.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WebRequest other = (WebRequest) obj;
		if (destination != other.destination)
			return false;
		if (id != other.id)
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
			return false;
		return true;
	}
}
