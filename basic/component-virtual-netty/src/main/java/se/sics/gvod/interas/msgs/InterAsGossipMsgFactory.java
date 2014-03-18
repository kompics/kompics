package se.sics.gvod.interas.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.VodDescriptor;
import java.util.List;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.RelayMsgNettyFactory;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesDecoderFactory;

public class InterAsGossipMsgFactory {

    public static class Request extends RelayMsgNettyFactory.Request {

        Request() {
        }

        public static InterAsGossipMsg.Request fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (InterAsGossipMsg.Request) 
                    new InterAsGossipMsgFactory.Request().decode(buffer);
        }

        @Override
        protected RewriteableMsg process(ByteBuf buffer) throws MessageDecodingException {
            return new InterAsGossipMsg.Request(gvodSrc, gvodDest, timeoutId);
        }
    }

    public static class Response extends RelayMsgNettyFactory.Response {

        private Response() {
        }

        public static InterAsGossipMsg.Response fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (InterAsGossipMsg.Response)
                    new InterAsGossipMsgFactory.Response().decode(buffer);
        }

        @Override
        protected RewriteableMsg process(ByteBuf buffer) throws MessageDecodingException {
            List<VodDescriptor> asNeighbours = UserTypesDecoderFactory.readListVodNodeDescriptors(buffer);
            return new InterAsGossipMsg.Response(gvodSrc, gvodDest, nextDest, timeoutId, 
                    asNeighbours);
        }

    }
};
