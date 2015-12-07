/**
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Kompics is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics;

import java.util.ArrayDeque;

// TODO: Auto-generated Javadoc
/**
 * The <code>Response</code> class.
 *
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @version $Id$
 * @deprecated Use {@link Direct.Response} instead if possible.
 */
@Deprecated
public abstract class Response implements KompicsEvent {

    private ArrayDeque<RequestPathElement> requestPath;

    /**
     * Instantiates a new response.
     *
     * @param request the request
     */
    protected Response(Request request) {
        requestPath = request.requestPath;
    }

    public RequestPathElement getTopPathElement() {
        return requestPath.poll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        Response response = (Response) super.clone();
        response.requestPath = requestPath.clone();
        return response;
    }
}
