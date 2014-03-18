package se.sics.gvod.interas.msgs;

import io.netty.buffer.ByteBuf;
import java.util.List;
import se.sics.gvod.common.VodDescriptor;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.common.msgs.RelayMsgNetty;
import se.sics.gvod.common.msgs.RelayMsgNetty.Status;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.msgs.RewriteableRetryTimeout;
import se.sics.gvod.net.msgs.ScheduleRetryTimeout;
import se.sics.gvod.net.util.UserTypesEncoderFactory;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.TimeoutId;

public class InterAsGossipMsg {

    public static class Request extends RelayMsgNetty.Request {

        public Request(VodAddress source, VodAddress destination) {
            this(source, destination, null);
        }

        public Request(VodAddress source, VodAddress destination, TimeoutId timeoutId) {
            super(source, destination, source.getId(), destination.getId(), timeoutId);
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.INTER_AS_GOSSIP_REQUEST;
        }

        @Override
        public RewriteableMsg copy() {
            return new InterAsGossipMsg.Request(vodSrc,
                    vodDest, timeoutId);
        }
    }

    public static class Response extends RelayMsgNetty.Response {

        private final List<VodDescriptor> interAsNeighbours;

        public Response(VodAddress source, Request request,
                List<VodDescriptor> interAsNeighbours) {
            super(source, request, Status.OK);
            this.interAsNeighbours = interAsNeighbours;
        }

        public Response(VodAddress source, VodAddress destination,
                VodAddress nextDest, TimeoutId timeoutId,
                List<VodDescriptor> similarPeers) {
            super(source, destination,
                    nextDest.getId(), source.getId(),
                    nextDest, timeoutId, Status.OK);
            this.interAsNeighbours = similarPeers;
        }

        @Override
        public int getSize() {
            return super.getSize()
                    + UserTypesEncoderFactory.getListVodNodeDescriptorSize(interAsNeighbours);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeListVodNodeDescriptors(buffer, interAsNeighbours);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.INTER_AS_GOSSIP_RESPONSE;
        }

        public List<VodDescriptor> getInterAsNeighbours() {
            return interAsNeighbours;
        }

        @Override
        public RewriteableMsg copy() {
            return new InterAsGossipMsg.Response(vodSrc, vodDest,
                    nextDest, timeoutId, interAsNeighbours);
        }
    }

    public static class RequestRetryTimeout extends RewriteableRetryTimeout {

        private final se.sics.gvod.interas.msgs.InterAsGossipMsg.Request requestMsg;

        public RequestRetryTimeout(ScheduleRetryTimeout st, se.sics.gvod.interas.msgs.InterAsGossipMsg.Request requestMsg) {
            super(st, requestMsg, requestMsg.getVodSource().getOverlayId());
            this.requestMsg = requestMsg;
        }

        public se.sics.gvod.interas.msgs.InterAsGossipMsg.Request getRequestMsg() {
            return requestMsg;
        }
    }

    public static class RequestTimeout extends Timeout {

        private final se.sics.gvod.interas.msgs.InterAsGossipMsg.Request requestMsg;

        public RequestTimeout(ScheduleTimeout st, se.sics.gvod.interas.msgs.InterAsGossipMsg.Request requestMsg) {
            super(st);
            this.requestMsg = requestMsg;
        }

        public se.sics.gvod.interas.msgs.InterAsGossipMsg.Request getRequestMsg() {
            return requestMsg;
        }
    }
}
