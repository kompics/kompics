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
package se.sics.kompics.network.test;

import java.io.Serializable;
import se.sics.kompics.network.Header;
import se.sics.kompics.network.Transport;

/**
 *
 * @author lkroll
 */
public class TestHeader implements Header<TestAddress>, Serializable {

    public final TestAddress src;
    public final TestAddress dst;
    public final Transport proto;

    public TestHeader(TestAddress src, TestAddress dst, Transport proto) {
        this.src = src;
        this.dst = dst;
        this.proto = proto;
    }

    @Override
    public Transport getProtocol() {
        return this.proto;
    }

    @Override
    public TestAddress getSource() {
        return this.src;
    }

    @Override
    public TestAddress getDestination() {
        return this.dst;
    }

}
