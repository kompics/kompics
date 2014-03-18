package se.sics.gvod.video.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.net.Transport;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesEncoderFactory;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.TimeoutId;

/**
 *
 */
public class VideoConnectionMsg {

    public enum Type {

        STANDARD, RANDOM, PARENT, CHILD;
    }

    public static final class Request extends DirectMsgNetty.Request {

        private boolean randomRequest;

        public Request(VodAddress source, VodAddress destination,
                boolean randomRequest) {
            super(source, destination);
            this.randomRequest = randomRequest;
        }

        @Override
        public int getSize() {
           return super.getHeaderSize()
                    + 1 /*
                     * randomRequest
                     */;
        }

        @Override
        public RewriteableMsg copy() {
            VideoConnectionMsg.Request vcmr = new VideoConnectionMsg.Request(vodSrc, vodDest, randomRequest);
            vcmr.setTimeoutId(timeoutId);
            return vcmr;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeBoolean(buffer, randomRequest);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.VIDEO_CONNECTION_REQUEST;
        }

        public boolean isRandomRequest() {
            return randomRequest;
        }
    }

    public static final class Response extends DirectMsgNetty.Response {

        private boolean randomRequest;
        private boolean acceptConnection;

        public Response(VideoConnectionMsg.Request request, boolean acceptConnection) {
            this(request.getVodDestination(), request.getVodSource(), request.getTimeoutId(), request.isRandomRequest(), acceptConnection);
        }

        public Response(VodAddress source, VodAddress destination,
                TimeoutId timeoutId, boolean randomRequest, boolean acceptConnection) {
            super(source, destination, Transport.UDP, timeoutId);
            this.randomRequest = randomRequest;
            this.acceptConnection = acceptConnection;
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 1 /*
                     * randomRequest
                     */
                    + 1 /*
                     * acceptConnection
                     */;
        }

        @Override
        public RewriteableMsg copy() {
            return new Response(vodSrc, vodDest, timeoutId, randomRequest, acceptConnection);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeBoolean(buffer, randomRequest);
            UserTypesEncoderFactory.writeBoolean(buffer, acceptConnection);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.VIDEO_CONNECTION_RESPONSE;
        }

        public boolean wasRandomRequest() {
            return randomRequest;
        }

        public boolean connectionAccepted() {
            return acceptConnection;
        }
    }

    public static final class RequestTimeout extends Timeout {

        private final Request requestMsg;

        public RequestTimeout(ScheduleTimeout st, Request requestMsg) {
            super(st);
            this.requestMsg = requestMsg;
        }

        public Request getRequestMsg() {
            return requestMsg;
        }
    }

    public static final class Disconnect extends DirectMsgNetty.Oneway {

        private boolean randomConnection;

        public Disconnect(VodAddress source, VodAddress destination, boolean randomConnection) {
            super(source, destination, Transport.UDP);
            this.randomConnection = randomConnection;
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 1;
        }

        @Override
        public RewriteableMsg copy() {
            return new Disconnect(vodSrc, vodDest, randomConnection);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeBoolean(buffer, randomConnection);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.VIDEO_CONNECTION_DISCONNECT;
        }

        public boolean wasRandomConnection() {
            return randomConnection;
        }
    }
}
