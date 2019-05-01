/*
 * This file is part of the Kompics component model runtime.
 * <p>
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package se.sics.kompics.util;

import com.google.common.io.BaseEncoding;

/**
 * @author Alex Ormenisan {@literal <aaor@kth.se>}
 */
public class ByteIdentifier implements Identifier {

    public final byte id;

    public ByteIdentifier(byte id) {
        this.id = id;
    }

    @Override
    public int partition(int nrPartitions) {
        return hashCode() % nrPartitions;
    }

    @Override
    public int compareTo(Identifier o) {
        ByteIdentifier that = (ByteIdentifier) o;
        if (this.id == that.id) {
            return 0;
        }
        return this.id < that.id ? -1 : 1;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ByteIdentifier other = (ByteIdentifier) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return BaseEncoding.base16().encode(new byte[] { id });
    }
}
