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
package se.sics.gvod.gradient.msgs;

import io.netty.buffer.ByteBuf;
import java.util.List;
import se.sics.gvod.common.VodDescriptor;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.common.msgs.RelayMsgNetty;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesEncoderFactory;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.TimeoutId;

/**
 * 
 * @author gautier
 */
public class SetsExchangeMsg {

    public static class Request extends RelayMsgNetty.Request {


        public Request(VodAddress self, VodAddress destination, int clientId, int remoteId) {
            super(self, destination, clientId, remoteId);
        }
        
        public Request(VodAddress self, VodAddress destination, int clientId, int remoteId, TimeoutId timeoutId) {
            super(self, destination, clientId, remoteId, timeoutId);
        }
        private Request(VodAddress self, VodAddress destination, int clientId, int remoteId,
                VodAddress nextDest, TimeoutId timeoutId) {
            super(self, destination, clientId, remoteId, nextDest, timeoutId);
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.SETS_EXCHANGE_REQUEST;
        }

        @Override
        public RewriteableMsg copy() {
            SetsExchangeMsg.Request copy = new SetsExchangeMsg.Request(vodSrc, vodDest,
                    clientId, remoteId, nextDest, timeoutId);
//            copy.setTimeoutId(timeoutId);
            return copy;
        }

    }

    public static class Response extends RelayMsgNetty.Response {

        private final List<VodDescriptor> utilitySet;
        private final List<VodDescriptor> upperSet;

        public Response(VodAddress source, VodAddress dest,
                int clientId, int remoteId,
                VodAddress nextDest, TimeoutId timeoutId,
                List<VodDescriptor> utilitySet, List<VodDescriptor> upperSet) {
            super(source, dest, clientId, remoteId, nextDest, timeoutId, RelayMsgNetty.Status.OK);
            this.utilitySet = utilitySet;
            this.upperSet = upperSet;
        }
        
        public Response(VodAddress source,  SetsExchangeMsg.Request request,
                List<VodDescriptor> utilitySet, List<VodDescriptor> upperSet) {
            super(source, request, RelayMsgNetty.Status.OK);
            this.utilitySet = utilitySet;
            this.upperSet = upperSet;
        }

        public List<VodDescriptor> getUpperSet() {
            return upperSet;
        }

        public List<VodDescriptor> getUtilitySet() {
            return utilitySet;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.SETS_EXCHANGE_RESPONSE;
        }

        @Override
        public int getSize() {
            return super.getSize()
                   + UserTypesEncoderFactory.getListVodNodeDescriptorSize(utilitySet)
                   + UserTypesEncoderFactory.getListVodNodeDescriptorSize(upperSet)
                    ;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeListVodNodeDescriptors(buf, utilitySet);
            UserTypesEncoderFactory.writeListVodNodeDescriptors(buf, upperSet);
            return buf;
        }

        @Override
        public RewriteableMsg copy() {
            return new SetsExchangeMsg.Response(vodSrc, vodDest, clientId, remoteId,
                    nextDest,  timeoutId, utilitySet, upperSet);
        }
    }

    public static class RequestTimeout extends Timeout {

        private final VodAddress peer;

        public RequestTimeout(ScheduleTimeout request, VodAddress peer) {
            super(request);
            this.peer = peer;
        }

        /**
         * @return the peer who did not reply within the shuffle timeout period.
         */
        public VodAddress getPeer() {
            return peer;
        }
    }
}
