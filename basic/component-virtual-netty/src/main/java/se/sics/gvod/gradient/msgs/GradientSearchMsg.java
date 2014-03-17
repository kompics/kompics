/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.gvod.gradient.msgs;

import io.netty.buffer.ByteBuf;
import java.util.List;
import se.sics.gvod.common.Utility;
import se.sics.gvod.common.VodDescriptor;
import se.sics.gvod.config.VodConfig;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.common.msgs.RelayMsgNetty;
import se.sics.gvod.common.msgs.RelayMsgNetty.Status;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesEncoderFactory;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.TimeoutId;

/**
 *
 */
public class GradientSearchMsg {

    public static class Request extends RelayMsgNetty.Request {

        private final int ttl;
        private final Utility targetUtility;
        private final VodAddress origSrc;

        public Request(VodAddress source, VodAddress destination, VodAddress originalSrc,
                TimeoutId timeoutId, Utility targetUtility, int ttl) {
            // do not retry this msg, set params to 0.
            super(source, destination, source.getId(), destination.getId(), timeoutId);
            this.targetUtility = targetUtility;
            this.ttl = ttl;
            this.origSrc = originalSrc;
        }

        public VodAddress getOrigSrc() {
            return origSrc;
        }

        @Override
        public int getSize() {
            return super.getSize() 
                    + /*ttl size*/ 1 
                    + 24 /* guess at utility size */
                    + UserTypesEncoderFactory.VOD_ADDRESS_LEN_NO_PARENTS
                    + VodConfig.PM_NUM_PARENTS * UserTypesEncoderFactory.ADDRESS_LEN
                    ;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeUnsignedintAsOneByte(buffer, ttl);
            UserTypesEncoderFactory.writeUtility(buffer, targetUtility);
            UserTypesEncoderFactory.writeVodAddress(buffer, origSrc);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.TARGET_UTILITY_PROBE_REQUEST;
        }

        /**
         * @return the ttl
         */
        public int getTtl() {
            return ttl;
        }

        /**
         * @return the targetChunk
         */
        public Utility getTargetUtility() {
            return targetUtility;
        }

        @Override
        public RewriteableMsg copy() {
            GradientSearchMsg.Request copy = 
                    new GradientSearchMsg.Request(vodSrc, vodDest, origSrc, timeoutId, 
                    targetUtility.clone(), ttl);
            copy.setTimeoutId(timeoutId);
            return copy;
        }
    }

    public static class Response extends RelayMsgNetty.Response {

        private final List<VodDescriptor> similarPeers;

        public Response(VodAddress source, VodAddress destination, 
                int clientId, int remoteId,
                VodAddress nextDest, TimeoutId timeoutId,
                List<VodDescriptor> similarPeers) {
            super(source, destination, clientId, remoteId, nextDest, timeoutId, Status.OK);
            this.similarPeers = similarPeers;
        }
        
        public Response(VodAddress source, Request request,
                List<VodDescriptor> similarPeers) {
            super(source, request, Status.OK);
            this.similarPeers = similarPeers;
        }

        @Override
        public int getSize() {
            return super.getSize()
                    + UserTypesEncoderFactory.getListVodNodeDescriptorSize(getSimilarPeers());
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeListVodNodeDescriptors(buffer, getSimilarPeers());
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.TARGET_UTILITY_PROBE_RESPONSE;
        }

        /**
         * @return the similarPeers
         */
        public List<VodDescriptor> getSimilarPeers() {
            return similarPeers;
        }

        @Override
        public RewriteableMsg copy() {
            return new GradientSearchMsg.Response(vodSrc, vodDest, clientId,
                    remoteId, nextDest,  timeoutId, similarPeers);
        }
    }

    public static class RequestRetryTimeout extends Timeout {

        public RequestRetryTimeout(ScheduleTimeout st) {
            super(st);
        }
    }
}
