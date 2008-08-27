package se.sics.kompics.web.events;

import se.sics.kompics.api.Event;
import se.sics.kompics.api.annotation.EventType;

/**
 * The <code>WebResponse</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
@EventType
public final class WebResponse implements Event {

	private final String html;

	private final WebRequest requestEvent;

	private final int partIndex;

	private final int partsTotal;

	public WebResponse(String html, WebRequest requestEvent, int index,
			int total) {
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
