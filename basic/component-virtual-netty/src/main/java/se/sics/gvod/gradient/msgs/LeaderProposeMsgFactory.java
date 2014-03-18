package se.sics.gvod.gradient.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.VodDescriptor;
import java.util.List;
import se.sics.gvod.common.Utility;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.RelayMsgNettyFactory;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesDecoderFactory;

public class LeaderProposeMsgFactory {

    public static class Request extends RelayMsgNettyFactory.Request {

        Request() {
        }

        public static GradientSearchMsg.Request fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (GradientSearchMsg.Request) 
                    new LeaderProposeMsgFactory.Request().decode(buffer);
        }

        @Override
        protected RewriteableMsg process(ByteBuf buffer) throws MessageDecodingException {
            int ttl = UserTypesDecoderFactory.readUnsignedIntAsOneByte(buffer);
            Utility targetUtility = UserTypesDecoderFactory.readUtility(buffer);
            VodAddress origSrc = UserTypesDecoderFactory.readVodAddress(buffer);
            return new GradientSearchMsg.Request(gvodSrc, gvodDest, origSrc,
                    timeoutId, targetUtility, ttl);
        }
    }

    public static class Response extends RelayMsgNettyFactory.Response {

        private Response() {
        }

        public static GradientSearchMsg.Response fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (GradientSearchMsg.Response)
                    new LeaderProposeMsgFactory.Response().decode(buffer);
        }

        @Override
        protected RewriteableMsg process(ByteBuf buffer) throws MessageDecodingException {
            List<VodDescriptor> similarSet = UserTypesDecoderFactory.readListVodNodeDescriptors(buffer);
            return new GradientSearchMsg.Response(gvodSrc, gvodDest, clientId, remoteId, nextDest, timeoutId, 
                    similarSet);
        }

    }
};
