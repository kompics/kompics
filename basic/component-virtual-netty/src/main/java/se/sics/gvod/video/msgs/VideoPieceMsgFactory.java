package se.sics.gvod.video.msgs;

import io.netty.buffer.ByteBuf;
import java.util.Set;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.common.msgs.DirectMsgNettyFactory;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.net.util.UserTypesDecoderFactory;
import se.sics.gvod.net.util.VideoTypesDecoderFactory;

public class VideoPieceMsgFactory {

    public static class Advertisement extends DirectMsgNettyFactory.Oneway {

        Advertisement() {
        }

        public static VideoPieceMsg.Advertisement fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (VideoPieceMsg.Advertisement) 
                    new VideoPieceMsgFactory.Advertisement().decode(buffer);
        }

        @Override
        protected VideoPieceMsg.Advertisement process(ByteBuf buffer) throws MessageDecodingException {
            Set<Integer> piecesIds = UserTypesDecoderFactory.readIntegerSet(buffer);
            return new VideoPieceMsg.Advertisement(vodSrc, vodDest, piecesIds);
        }
    }

    public static class Request extends DirectMsgNettyFactory.Request {

        private Request() {
        }

        public static VideoPieceMsg.Request fromBuffer(ByteBuf buffer)
                throws MessageDecodingException {
            return (VideoPieceMsg.Request) new VideoPieceMsgFactory.Request().decode(buffer);
        }

        @Override
        protected VideoPieceMsg.Request process(ByteBuf buffer) throws MessageDecodingException {
            Set<Integer> piecesIds = UserTypesDecoderFactory.readIntegerSet(buffer);
            return new VideoPieceMsg.Request(vodSrc, vodDest, piecesIds);
        }
    }

    public static class Response extends DirectMsgNettyFactory.Response {

        private Response() {
        }

        public static VideoPieceMsg.Response fromBuffer(ByteBuf buffer) throws MessageDecodingException {
            return (VideoPieceMsg.Response) new VideoPieceMsgFactory.Response().decode(buffer);
        }

        @Override
        protected VideoPieceMsg.Response process(ByteBuf buffer) throws MessageDecodingException {
            EncodedSubPiece esp = VideoTypesDecoderFactory.readEncodedSubPiece(buffer);
            return new VideoPieceMsg.Response(vodSrc, vodDest, timeoutId, esp);
        }
    }
};
