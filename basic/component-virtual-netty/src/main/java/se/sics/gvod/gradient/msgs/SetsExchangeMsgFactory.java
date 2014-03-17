package se.sics.gvod.gradient.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.VodDescriptor;
import java.util.List;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.RelayMsgNettyFactory;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesDecoderFactory;

public class SetsExchangeMsgFactory {

    public static class Request extends RelayMsgNettyFactory.Request {

        Request() {
        }

        public static SetsExchangeMsg.Request fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (SetsExchangeMsg.Request) 
                    new SetsExchangeMsgFactory.Request().decode(buffer);
        }

        @Override
        protected RewriteableMsg process(ByteBuf buffer) throws MessageDecodingException {
            return new SetsExchangeMsg.Request(gvodSrc, gvodDest, clientId, remoteId, timeoutId);
        }
    }

    public static class Response extends RelayMsgNettyFactory.Response {

        private Response() {
        }

        public static SetsExchangeMsg.Response fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (SetsExchangeMsg.Response)
                    new SetsExchangeMsgFactory.Response().decode(buffer);
        }

        @Override
        protected RewriteableMsg process(ByteBuf buffer) throws MessageDecodingException {
            List<VodDescriptor> utilitySet = UserTypesDecoderFactory.readListVodNodeDescriptors(buffer);
            List<VodDescriptor> upperSet = UserTypesDecoderFactory.readListVodNodeDescriptors(buffer);
            return new SetsExchangeMsg.Response(gvodSrc, gvodDest, clientId, remoteId, nextDest, timeoutId, 
                    utilitySet, upperSet);
        }

    }
};
