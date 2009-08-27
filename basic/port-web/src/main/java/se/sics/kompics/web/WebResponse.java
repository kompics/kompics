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
package se.sics.kompics.web;

import se.sics.kompics.Response;

/**
 * The <code>WebResponse</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 */
public final class WebResponse extends Response {

	private final String html;

	private final WebRequest requestEvent;

	private final int partIndex;

	private final int partsTotal;

	public WebResponse(String html, WebRequest requestEvent, int index,
			int total) {
		super(requestEvent);
		this.html = html;
		this.requestEvent = requestEvent;
		this.partIndex = index;
		this.partsTotal = total;
	}

	public WebRequest getRequestEvent() {
		return requestEvent;
	}

	public String getHtml() {
		return html;
	}

	public int getPartIndex() {
		return partIndex;
	}

	public int getPartsTotal() {
		return partsTotal;
	}
}
