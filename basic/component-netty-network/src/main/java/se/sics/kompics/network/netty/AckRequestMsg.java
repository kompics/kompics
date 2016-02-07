/*
 * This file is part of the Kompics component model runtime.
 *
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) 
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
package se.sics.kompics.network.netty;

import java.util.UUID;
import se.sics.kompics.network.Address;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Msg;
import se.sics.kompics.network.Transport;

/**
 *
 * @author lkroll
 */
public class AckRequestMsg implements Msg {

    public final Msg content;
    public final UUID id;

    public AckRequestMsg(Msg msg, UUID id) {
        this.content = msg;
        this.id = id;
    }

    public NotifyAck reply() {
        return new NotifyAck(this.getDestination(), this.getSource(), this.getProtocol(), id);
    }

    @Override
    public Header getHeader() {
        return content.getHeader();
    }

    @Override
    public Address getSource() {
        return content.getHeader().getSource();
    }

    @Override
    public Address getDestination() {
        return content.getHeader().getDestination();
    }

    @Override
    public Transport getProtocol() {
        return content.getHeader().getProtocol();
    }

}
