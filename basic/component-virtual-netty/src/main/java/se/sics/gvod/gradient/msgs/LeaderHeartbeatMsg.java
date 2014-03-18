/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.gvod.gradient.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.common.msgs.RelayMsgNetty;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.msgs.ScheduleRetryTimeout;
import se.sics.gvod.net.util.UserTypesEncoderFactory;
import se.sics.gvod.timer.TimeoutId;


/**
 *
 * @author jdowling
 */
public class LeaderHeartbeatMsg {

    public static class Request extends RelayMsgNetty.Request {

        private static final long serialVersionUID = 1456977888143L;
        private final boolean outgoing;

        public Request(VodAddress source, VodAddress destination,
                int clientId, int remoteId,
                boolean outgoing) {
            super(source, destination, clientId, remoteId);
            this.outgoing = outgoing;
        }

        public boolean isOutgoing() {
            return outgoing;
        }
        
        @Override
        public int getSize() {
            return super.getSize() 
                    + 1 /* outgoing */
                    ;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeBoolean(buffer, outgoing);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.GRADIENT_HEARTBEAT_REQUEST;
        }

        @Override
        public RewriteableMsg copy() {
            LeaderHeartbeatMsg.Request copy = new LeaderHeartbeatMsg.Request(vodSrc, 
                    vodDest, clientId, remoteId, outgoing);
            copy.setTimeoutId(timeoutId);
            return copy;
        }
    }

    public static class Response extends RelayMsgNetty.Response {

        private static final long serialVersionUID = -44444444410L;
        private final boolean hasConnection;
        private final long utility;


        public Response(VodAddress source, VodAddress destination, int clientId,
                int remoteId, VodAddress nextDest, 
                TimeoutId timeoutId, boolean hasConnection, long utility,
                RelayMsgNetty.Status status) {
            super(source, destination, clientId, remoteId, nextDest, timeoutId, status);
            this.hasConnection = hasConnection;
            this.utility = utility;
        }

        public long getUtility() {
            return utility;
        }

        public boolean isHasConnection() {
            return hasConnection;
        }

        @Override
        public int getSize() {
            return super.getSize() 
                    + 1  // hasConnection
                    + 8 // utility
                    ;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeBoolean(buffer, hasConnection);
            buffer.writeLong(utility);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.GRADIENT_HEARTBEAT_RESPONSE;
        }

        @Override
        public RewriteableMsg copy() {
            return new LeaderHeartbeatMsg.Response(vodSrc, vodDest, clientId, 
                    remoteId, nextDest, timeoutId, hasConnection, utility, getStatus());
        }
        
    }

    public static final class RequestTimeout extends RelayMsgNetty.RequestTimeout {

        private final Request requestMsg;

        public RequestTimeout(ScheduleRetryTimeout st, Request requestMsg) {
            super(st, requestMsg);
            this.requestMsg = requestMsg;
        }

        public Request getRequest() {
            return requestMsg;
        }
    }
}
