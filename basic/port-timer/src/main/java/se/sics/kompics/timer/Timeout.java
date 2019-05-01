/*
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
package se.sics.kompics.timer;

import java.util.UUID;

import se.sics.kompics.Response;

/**
 * The <code>Timeout</code> class.
 * 
 * @author Cosmin Arad {@literal <cosmin@sics.se>}
 * @author Jim Dowling {@literal <jdowling@sics.se>}
 * @version $Id$
 */
@SuppressWarnings("deprecation")
public abstract class Timeout extends Response implements Cloneable {

    private UUID timeoutId;

    /**
     * Instantiates a new timeout.
     * 
     * @param request
     *            the request
     */
    protected Timeout(ScheduleTimeout request) {
        super(request);
        timeoutId = UUID.randomUUID();
    }

    /**
     * Instantiates a new timeout.
     * 
     * @param request
     *            the request
     */
    protected Timeout(SchedulePeriodicTimeout request) {
        super(request);
        timeoutId = UUID.randomUUID();
    }

    /**
     * Gets the timeout id.
     * 
     * @return the timeout id
     */
    public final UUID getTimeoutId() {
        return timeoutId;
    }

    /*
     * (non-Javadoc)
     * 
     * @see se.sics.kompics.Response#clone()
     */
    @Override
    public final Object clone() throws CloneNotSupportedException {
        Timeout timeout = (Timeout) super.clone();
        timeout.timeoutId = timeoutId;
        return timeout;
    }
}
