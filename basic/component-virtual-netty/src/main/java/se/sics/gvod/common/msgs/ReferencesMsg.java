/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.gvod.common.msgs;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.common.UtilityVod;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesEncoderFactory;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.TimeoutId;

/**
 *
 * @author jdowling 
 */
public class ReferencesMsg {

    public static class Request extends DirectMsgNetty.Request {
        private final int ref;
        private final UtilityVod utility;
        private final List<VodAddress> children;

        public Request(VodAddress source, VodAddress destination, 
                int ref, UtilityVod utility, List<VodAddress> children) {
            super(source, destination);
            this.ref = ref;
            this.utility = new UtilityVod(utility.getChunk(), utility.getPiece(), utility.getOffset());
               if (children == null) {
                this.children = new ArrayList<VodAddress>();
            } else {
                this.children = children;
            }
        }

        public int getRef() {
            return ref;
        }

        public UtilityVod getUtility() {
            return utility;
        }

        public List<VodAddress> getChildren() {
            return children;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.REFERENCES_REQUEST;
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 2 /*refs */
                    + UserTypesEncoderFactory.UTILITY_LEN
                    + UserTypesEncoderFactory.getListVodAddressSize(children);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeUnsignedintAsTwoBytes(buf, ref);
            UserTypesEncoderFactory.writeUtility(buf, utility);
            UserTypesEncoderFactory.writeListVodAddresses(buf, children);
            return buf;
        }

        @Override
        public RewriteableMsg copy() {
            Request r = new Request(vodSrc, vodDest, ref, utility, children);
            r.setTimeoutId(timeoutId);
            return r;
        }
    }

    public static class Response extends DirectMsgNetty.Response {

        private final int ref;
        private final UtilityVod utility;
        private final List<VodAddress> children;

        public Response(VodAddress source, VodAddress destination, TimeoutId timeoutId,
                int ref, UtilityVod utility, List<VodAddress> children) {
            super(source, destination, timeoutId);
            this.ref = ref;
            this.utility = new UtilityVod(utility.getChunk(), utility.getPiece(), utility.getOffset());
            if (children == null) {
                this.children = new ArrayList<VodAddress>();
            } else {
                this.children = children;
            }
        }

        public int getRef() {
            return ref;
        }

        public UtilityVod getUtility() {
            return utility;
        }

        public List<VodAddress> getChildren() {
            return children;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.REFERENCES_RESPONSE;
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 2 /*refs */
                    + UserTypesEncoderFactory.UTILITY_LEN
                    + UserTypesEncoderFactory.getListVodAddressSize(children);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buf = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeUnsignedintAsTwoBytes(buf, ref);
            UserTypesEncoderFactory.writeUtility(buf, utility);
            UserTypesEncoderFactory.writeListVodAddresses(buf, children);
            return buf;
        }

        @Override
        public RewriteableMsg copy() {
            return new Response(vodSrc, vodDest, timeoutId, ref, utility, children);
        }
    }

    public static class RequestTimeout extends Timeout {

        private final VodAddress peer;

        public RequestTimeout(ScheduleTimeout timeout, VodAddress peer) {
            super(timeout);
            this.peer = peer;
        }

        public VodAddress getPeer() {
            return peer;
        }
    }
}
