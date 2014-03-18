package se.sics.gvod.common.msgs;

import io.netty.buffer.ByteBuf;
import java.util.List;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.common.UtilityVod;
import se.sics.gvod.net.util.UserTypesDecoderFactory;

public class ReferencesMsgFactory {

    public static class Request extends DirectMsgNettyFactory.Request {

        private Request() {
        }

        public static ReferencesMsg.Request fromBuffer(ByteBuf buffer)
                 throws MessageDecodingException {
            return (ReferencesMsg.Request) new ReferencesMsgFactory.Request().decode(buffer);
        }

        @Override
        protected ReferencesMsg.Request process(ByteBuf buffer) throws MessageDecodingException {
            int ref = UserTypesDecoderFactory.readUnsignedIntAsTwoBytes(buffer);
            UtilityVod utility = (UtilityVod) UserTypesDecoderFactory.readUtility(buffer);
            List<VodAddress> children = UserTypesDecoderFactory.readListVodAddresses(buffer);
            ReferencesMsg.Request msg = new ReferencesMsg.Request(vodSrc, vodDest, 
                    ref, utility, children);
            return msg;
        }
    }

    public static class Response extends DirectMsgNettyFactory.Response {

        private Response() {
        }

        public static ReferencesMsg.Response fromBuffer(ByteBuf buffer)
                 throws MessageDecodingException {
            return (ReferencesMsg.Response) 
                    new ReferencesMsgFactory.Response().decode(buffer);
        }

        @Override
        protected ReferencesMsg.Response process(ByteBuf buffer) throws MessageDecodingException {
            int ref = UserTypesDecoderFactory.readUnsignedIntAsTwoBytes(buffer);
            UtilityVod utility = (UtilityVod) UserTypesDecoderFactory.readUtility(buffer);
            List<VodAddress> children = UserTypesDecoderFactory.readListVodAddresses(buffer);
            return new ReferencesMsg.Response(vodSrc, vodDest,
                    timeoutId, ref, utility, children);
        }
    }
};
