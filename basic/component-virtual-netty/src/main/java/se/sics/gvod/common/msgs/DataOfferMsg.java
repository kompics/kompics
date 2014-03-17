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
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.common.UtilityVod;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesEncoderFactory;

public class DataOfferMsg extends DirectMsgNetty.Oneway {

    private final UtilityVod utility;
    private final byte[] availableChunks;
    private final byte[][] availablePieces;

    public DataOfferMsg(VodAddress source, VodAddress destination,
            UtilityVod utility, byte[] availableChunks, byte[][] availablePieces) {
        super(source, destination);
        this.utility = new UtilityVod(utility.getChunk(), utility.getPiece(), utility.getOffset());
        this.availableChunks = availableChunks;
        this.availablePieces = availablePieces;
    }

    public DataOfferMsg(VodAddress source, VodAddress destination,
            UtilityVod utility, byte[] availableChunks) {
        super(source, destination);
        this.utility = new UtilityVod(utility.getChunk(), utility.getPiece(), utility.getOffset());
        this.availableChunks = availableChunks;
        this.availablePieces = new byte[0][0];
    }

    public UtilityVod getUtility() {
        return utility;
    }

    public byte[] getAvailableChunks() {
        return availableChunks;
    }

    public byte[][] getAvailablePieces() {
        return availablePieces;
    }

    @Override
    public int getSize() {
        int sz  = getHeaderSize()
                + UserTypesEncoderFactory.UTILITY_LEN
                + UserTypesEncoderFactory.getArraySize(availableChunks)
                + UserTypesEncoderFactory.getArrayArraySize(availablePieces)
                ;
        return sz;
    }

    @Override
    public byte getOpcode() {
        return VodMsgFrameDecoder.DATAOFFER;
    }

    @Override
    public ByteBuf toByteArray() throws MessageEncodingException {
        ByteBuf buf = createChannelBufferWithHeader();
        UserTypesEncoderFactory.writeUtility(buf, utility);
        UserTypesEncoderFactory.writeArrayBytes(buf, availableChunks);
        UserTypesEncoderFactory.writeArrayArrayBytes(buf, availablePieces);
        return buf;
    }

    @Override
    public RewriteableMsg copy() {
         DataOfferMsg copy = new DataOfferMsg(vodSrc, vodDest, utility, availableChunks, 
                 availablePieces);
         copy.setTimeoutId(timeoutId);
         return copy;
    }
}
