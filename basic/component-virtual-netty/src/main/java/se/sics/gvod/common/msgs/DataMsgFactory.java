package se.sics.gvod.common.msgs;

import io.netty.buffer.ByteBuf;
import se.sics.gvod.common.UtilityVod;
import se.sics.gvod.net.msgs.DirectMsg;
import se.sics.gvod.net.util.UserTypesDecoderFactory;
import se.sics.gvod.timer.TimeoutId;
import se.sics.gvod.timer.UUID;

public class DataMsgFactory {

    public static class Request extends DirectMsgNettyFactory.Request {

        private Request() {
        }

        public static DataMsg.Request fromBuffer(ByteBuf buffer)
                
                throws MessageDecodingException {
            return (DataMsg.Request)
                    new DataMsgFactory.Request().decode(buffer);
        }

        @Override
        protected DataMsg.Request process(ByteBuf buffer) throws MessageDecodingException {
            TimeoutId ackId = new UUID(buffer.readInt());
            int piece = buffer.readInt();
            int subpiece = buffer.readInt();
            long delay = buffer.readLong();
            return new DataMsg.Request(vodSrc, vodDest, ackId, piece,
                    subpiece,  delay);
        }
    }

    public static class Response extends DirectMsgNettyFactory.Response {

        private Response() {
        }


        public static DataMsg.Response fromBuffer(ByteBuf buffer)
                
                throws MessageDecodingException {
            return (DataMsg.Response)
                    new DataMsgFactory.Response().decode(buffer);
        }

        @Override
        protected DataMsg.Response process(ByteBuf buffer) throws MessageDecodingException {
//            TimeoutId ack = UserTypesDecoderFactory.readTimeoutId(buffer);
            TimeoutId ack = new UUID(buffer.readInt());
            byte[] sp = UserTypesDecoderFactory.readArrayBytes(buffer);
            int nb = buffer.readInt();
            int p = buffer.readInt();
            int cwSz = buffer.readInt();
            long t = buffer.readLong();
            return new DataMsg.Response(vodSrc, vodDest, timeoutId, ack, sp,
                    nb, p, cwSz, t);
        }
    }

    public static class PieceNotAvailable extends DirectMsgNettyFactory.Oneway {

        private PieceNotAvailable() {
        }

        public static DataMsg.PieceNotAvailable fromBuffer(ByteBuf buffer)
                
                throws MessageDecodingException {
            return (DataMsg.PieceNotAvailable) 
                    new DataMsgFactory.PieceNotAvailable().decode(buffer);
        }

        @Override
        protected DataMsg.PieceNotAvailable process(ByteBuf buffer) throws MessageDecodingException {
            byte[] availableChunks = UserTypesDecoderFactory.readArrayBytes(buffer);
            UtilityVod utility = (UtilityVod) UserTypesDecoderFactory.readUtility(buffer);
            int piece = buffer.readInt();
            byte[][] availablePieces = UserTypesDecoderFactory.readArrayArrayBytes(buffer);
            return new DataMsg.PieceNotAvailable(vodSrc, vodDest, 
                    availableChunks, utility, piece, availablePieces);
        }
    }

    public static class Saturated extends DirectMsgNettyFactory.Oneway {

        private Saturated() {
        }

        public static DataMsg.Saturated fromBuffer(ByteBuf buffer)
                
                throws MessageDecodingException {
            return (DataMsg.Saturated)
                    new DataMsgFactory.Saturated().decode(buffer);
        }

        @Override
        protected DataMsg.Saturated process(ByteBuf buffer) throws MessageDecodingException {
            int subpiece = buffer.readInt();
            int comWindowSize = buffer.readInt();
            return new DataMsg.Saturated(vodSrc, vodDest,
                    subpiece, comWindowSize);
        }
    }

    public static class HashRequest extends DirectMsgNettyFactory.Request {

        private HashRequest() {
        }

        public static DataMsg.HashRequest fromBuffer(ByteBuf buffer)
                
                throws MessageDecodingException {
            return (DataMsg.HashRequest)
                    new DataMsgFactory.HashRequest().decode(buffer);
        }

        @Override
        protected DataMsg.HashRequest process(ByteBuf buffer) throws MessageDecodingException {
            int chunk = UserTypesDecoderFactory.readUnsignedIntAsTwoBytes(buffer);
            int part = UserTypesDecoderFactory.readUnsignedIntAsOneByte(buffer);
            return new DataMsg.HashRequest(vodSrc, vodDest,  chunk, part);
        }
    }

    public static class HashResponse extends DirectMsgNettyFactory.Response {

        private HashResponse() {
        }

        public static DataMsg.HashResponse fromBuffer(ByteBuf buffer)
                
                throws MessageDecodingException {
            return (DataMsg.HashResponse)
                    new DataMsgFactory.HashResponse().decode(buffer);
        }

        @Override
        protected DataMsg.HashResponse process(ByteBuf buffer) throws MessageDecodingException {
            int chunk = UserTypesDecoderFactory.readUnsignedIntAsTwoBytes(buffer);
            byte[] hashes = UserTypesDecoderFactory.readArrayBytes(buffer);
            int part = UserTypesDecoderFactory.readUnsignedIntAsOneByte(buffer);
            int numParts = UserTypesDecoderFactory.readUnsignedIntAsOneByte(buffer);
            return new DataMsg.HashResponse(vodSrc, vodDest, timeoutId,
                    chunk, hashes, part, numParts);
        }
    }

    public static class Ack extends DirectMsgNettyFactory.Response {

        private Ack() {
        }

        public static DataMsg.Ack fromBuffer(ByteBuf buffer)
                
                throws MessageDecodingException {
            return (DataMsg.Ack) new DataMsgFactory.Ack().decode(buffer);
        }

        @Override
        protected DataMsg.Ack process(ByteBuf buffer) throws MessageDecodingException {
            long delay = buffer.readLong();
            return new DataMsg.Ack(vodSrc, vodDest, timeoutId, delay);
        }
    }
};
