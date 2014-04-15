/* 
 * This file is part of the CaracalDB distributed storage system.
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
package se.sics.kompics.network.netty.serialization;

import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public interface Serializer {
    /*
     * Unique ID to differentiate the serializer from others over the network.
     * 
     * Identifiers 1-16 are reserved for internal use.
     * 
     * Make sure to configure serializers to use enough bytes for your ID to be uniqe!
     */
    public int identifier();
    
    /**
     * Serialize o into buf.
     * 
     * @param o
     * @param buf 
     */
    public void toBinary(Object o, ByteBuf buf);
    
    /**
     * Deserialize from buf.
     * Optionally use hint to decide what to deserialize.
     * 
     * @param buf
     * @param hint
     * @return 
     */
    public Object fromBinary(ByteBuf buf, Optional<Class> hint);
}
