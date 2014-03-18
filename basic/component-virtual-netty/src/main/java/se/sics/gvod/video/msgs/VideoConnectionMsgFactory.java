package se.sics.gvod.video.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.net.util.UserTypesDecoderFactory;

public class VideoConnectionMsgFactory {

    public static class Request extends DirectMsgNettyFactory.Request {

        Request() {
        }

        public static VideoConnectionMsg.Request fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (VideoConnectionMsg.Request) new VideoConnectionMsgFactory.Request().decode(buffer);
        }

        @Override
        protected VideoConnectionMsg.Request process(ByteBuf buffer) throws MessageDecodingException {
            boolean randomRequest = UserTypesDecoderFactory.readBoolean(buffer);
            return new VideoConnectionMsg.Request(vodSrc, vodDest, randomRequest);
        }
    }

    public static class Response extends DirectMsgNettyFactory.Response {

        private Response() {
        }

        public static VideoConnectionMsg.Response fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (VideoConnectionMsg.Response) new VideoConnectionMsgFactory.Response().decode(buffer);
        }

        @Override
        protected VideoConnectionMsg.Response process(ByteBuf buffer) throws MessageDecodingException {
            boolean randomRequest = UserTypesDecoderFactory.readBoolean(buffer);
            boolean acceptConnection = UserTypesDecoderFactory.readBoolean(buffer);
            return new VideoConnectionMsg.Response(vodSrc, vodDest, timeoutId, randomRequest,
                    acceptConnection);
        }
    }

    public static class Disconnect extends DirectMsgNettyFactory.Oneway {

        private Disconnect() {
        }

        public static VideoConnectionMsg.Disconnect fromBuffer(ByteBuf buffer) throws MessageDecodingException {
            return (VideoConnectionMsg.Disconnect) 
                    new VideoConnectionMsgFactory.Disconnect().decode(buffer);
        }

        @Override
        protected VideoConnectionMsg.Disconnect process(ByteBuf buffer) throws MessageDecodingException {
            boolean randomConnection = UserTypesDecoderFactory.readBoolean(buffer);
            return new VideoConnectionMsg.Disconnect(vodSrc, vodDest, randomConnection);
        }
    }
};
