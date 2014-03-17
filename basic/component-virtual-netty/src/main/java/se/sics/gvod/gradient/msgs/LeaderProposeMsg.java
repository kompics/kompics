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
public class LeaderProposeMsg {

    public static class Request extends RelayMsgNetty.Request {

        private static final long serialVersionUID = 11555558888143L;
        private final long utility;

        public Request(VodAddress source, VodAddress destination,
                int clientId, int remoteId, long utility) {
            super(source, destination, clientId, remoteId);
            this.utility = utility;
        }
        
        public long getUtility() {
            return this.utility;
        }
        @Override
        public int getSize() {
            return super.getSize() 
                    + 8 /* utility*/
                    ;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            buffer.writeLong(utility);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.LEADER_SELECTION_REQUEST;
        }

        @Override
        public RewriteableMsg copy() {
            LeaderProposeMsg.Request copy = new LeaderProposeMsg.Request(vodSrc, 
                    vodDest, clientId, remoteId, utility);
            copy.setTimeoutId(timeoutId);
            return copy;
        }
    }

    public static class Response extends RelayMsgNetty.Response {

        private static final long serialVersionUID = -44996996410L;
        private final boolean isLeader;


        public Response(VodAddress source, VodAddress destination,
                int clientId, int remoteId, VodAddress nextDest, 
                TimeoutId timeoutId, boolean isLeader, RelayMsgNetty.Status status) {
            super(source, destination, clientId, remoteId, nextDest, timeoutId, status);
            this.isLeader = isLeader;
        }
        
        public boolean isLeader(){
            return this.isLeader;
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
            UserTypesEncoderFactory.writeBoolean(buffer, isLeader);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.LEADER_SELECTION_RESPONSE;
        }

        @Override
        public RewriteableMsg copy() {
            return new LeaderProposeMsg.Response(vodSrc, vodDest, clientId, 
                    remoteId, nextDest, timeoutId, isLeader, getStatus());
        }
    }

    // TODO: Is this necessary?
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
