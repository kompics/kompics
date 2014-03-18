package se.sics.gvod.gradient.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.VodDescriptor;
import java.util.List;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.RelayMsgNettyFactory;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesDecoderFactory;

public class GradientSetsExchangeMsgFactory {

    public static class Request extends RelayMsgNettyFactory.Request {

        Request() {
        }

        public static GradientSetsExchangeMsg.Request fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (GradientSetsExchangeMsg.Request) 
                    new GradientSetsExchangeMsgFactory.Request().decode(buffer);
        }

        @Override
        protected RewriteableMsg process(ByteBuf buffer) throws MessageDecodingException {
            return new GradientSetsExchangeMsg.Request(gvodSrc, gvodDest, clientId, remoteId, timeoutId);
        }
    }

    public static class Response extends RelayMsgNettyFactory.Response {

        private Response() {
        }

        public static GradientSetsExchangeMsg.Response fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (GradientSetsExchangeMsg.Response)
                    new GradientSetsExchangeMsgFactory.Response().decode(buffer);
        }

        @Override
        protected RewriteableMsg process(ByteBuf buffer) throws MessageDecodingException {
            List<VodDescriptor> similarSet = UserTypesDecoderFactory.readListVodNodeDescriptors(buffer);
            return new GradientSetsExchangeMsg.Response(gvodSrc, gvodDest, nextDest, timeoutId, 
                    similarSet);
        }

    }
};
