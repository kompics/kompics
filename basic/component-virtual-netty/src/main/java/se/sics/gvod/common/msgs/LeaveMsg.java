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
package se.sics.gvod.common.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;

/**
 * 
 * @author gautier
 */
public class LeaveMsg extends DirectMsgNetty.Oneway {

    public LeaveMsg(VodAddress source, VodAddress destination) {
        super(source, destination);
    }

    @Override
    public int getSize() {
        return super.getHeaderSize()
                ;
    }

    @Override
    public byte getOpcode() {
        return VodMsgFrameDecoder.LEAVE;
    }

    @Override
    public ByteBuf toByteArray() throws MessageEncodingException {
        ByteBuf buf = createChannelBufferWithHeader();
        return buf;
    }

    @Override
    public RewriteableMsg copy() {
        return new LeaveMsg(vodSrc, vodDest);
    }
}
