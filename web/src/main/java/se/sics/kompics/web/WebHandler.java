package se.sics.kompics.web;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

/**
 * The <code>WebHandler</code> class
 * 
 * @author Cosmin Arad
 * @version $Id$
 */
final class WebHandler extends AbstractHandler {

	private final WebServer webComponent;

	private static final int FAVICON_LENGTH = 4286;

	private final byte[] favicon;

	public WebHandler(WebServer webComponent) throws IOException {
		super();
		this.webComponent = webComponent;
		InputStream iconStrem = WebServer.class
				.getResourceAsStream("favicon.ico");

		favicon = new byte[FAVICON_LENGTH];
		iconStrem.read(favicon);
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
