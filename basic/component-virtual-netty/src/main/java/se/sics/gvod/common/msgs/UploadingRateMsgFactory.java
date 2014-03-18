package se.sics.gvod.common.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.util.UserTypesDecoderFactory;

public class UploadingRateMsgFactory {

    public static class Request extends DirectMsgNettyFactory.Request {

        private Request() {
        }

        public static UploadingRateMsg.Request fromBuffer(ByteBuf buffer)
                
                throws MessageDecodingException {
            return (UploadingRateMsg.Request)
                    new UploadingRateMsgFactory.Request().decode(buffer);
        }

        @Override
        protected UploadingRateMsg.Request process(ByteBuf buffer) throws MessageDecodingException {
            VodAddress target = UserTypesDecoderFactory.readVodAddress(buffer);
            return new UploadingRateMsg.Request(vodSrc, vodDest, target);
        }
    }

    public static class Response extends DirectMsgNettyFactory.Response {

        private Response() {
        }

        public static UploadingRateMsg.Response fromBuffer(ByteBuf buffer)
                
                throws MessageDecodingException {
            return (UploadingRateMsg.Response)
                    new UploadingRateMsgFactory.Response().decode(buffer);
        }

        @Override
        protected UploadingRateMsg.Response process(ByteBuf buffer) throws MessageDecodingException {
            VodAddress target = UserTypesDecoderFactory.readVodAddress(buffer);
            int rate = buffer.readInt();
            return  new UploadingRateMsg.Response(vodSrc, vodDest, timeoutId,  target, rate);
        }
    }
};
