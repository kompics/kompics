/**
 * This file is part of the Kompics P2P Framework.
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
package se.sics.kompics.web.jetty;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 * The <code>JettyHandler</code> class.
 * 
 * @author Cosmin Arad <cosmin@sics.se>
 * @version $Id: JettyHandler.java 750 2009-04-02 09:55:01Z Cosmin $
 */
final class JettyHandler extends AbstractHandler {

	private final JettyWebServer webComponent;

	private static final int FAVICON_LENGTH = 4286;

	private final byte[] favicon;

	public JettyHandler(JettyWebServer webComponent) throws IOException {
		super();
		
		this.webComponent = webComponent;
		InputStream iconStrem = JettyWebServer.class
				.getResourceAsStream("favicon.ico");

		favicon = new byte[FAVICON_LENGTH];
		int ret = iconStrem.read(favicon);
		if (ret == -1)
			throw new RuntimeException("Cannot read icon file");
	}

	public void handle(String target, HttpServletRequest request,
			HttpServletResponse response, int dispatch) throws IOException,
			ServletException {

		Request base_request = (request instanceof Request) ? (Request) request
				: HttpConnection.getCurrentConnection().getRequest();
		base_request.setHandled(true);

		if (target.equals("/favicon.ico")) {
			response.setContentType("image/x-icon");
			response.setContentLength(FAVICON_LENGTH);
			response.setStatus(HttpServletResponse.SC_OK);
			response.getOutputStream().write(favicon);
		} else {
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(
					"<head><link rel=\"icon\" href=\"/favicon."
							+ "ico\" type=\"image/x-icon\" /></head>");

			webComponent.handleRequest(target, base_request, response);
		}
	}
}
