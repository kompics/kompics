package se.sics.gvod.video.msgs;

import io.netty.buffer.ByteBuf;
import java.util.*;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.common.msgs.DirectMsgNetty;
import se.sics.gvod.net.VodAddress;
import se.sics.gvod.net.VodMsgFrameDecoder;
import se.sics.gvod.net.msgs.RewriteableMsg;
import se.sics.gvod.net.util.UserTypesEncoderFactory;
import se.sics.gvod.net.util.VideoTypesEncoderFactory;
import se.sics.gvod.timer.ScheduleTimeout;
import se.sics.gvod.timer.Timeout;
import se.sics.gvod.timer.TimeoutId;

/**
 * Message types used in Three-phase Gossip.
 *
 * @author Niklas Wahl&#233;n <nwahlen@kth.se>
 */
public class VideoPieceMsg {

    /**
     * Contains advertisements of EncodedSubPiece IDs that a peer is ready to
     * disseminate into the system. These IDs are unique among all
     * EncodedSubPieces in the system.
     *
     * @see EncodedSubPiece
     */
    public static final class Advertisement extends DirectMsgNetty.Oneway {

        private final Set<Integer> piecesIds;

        public Advertisement(VodAddress gvodSrc, VodAddress gvodDest, Set<Integer> piecesIds) {
            super(gvodSrc, gvodDest);
            checkSetValidity(piecesIds);
            this.piecesIds = Collections.unmodifiableSet(piecesIds);
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 2 // length of Set<Integer> saves as to bytes
                    + 4 * piecesIds.size();
        }

        @Override
        public RewriteableMsg copy() {
            Advertisement copy = new Advertisement(vodSrc, vodDest, new HashSet<Integer>(piecesIds));
            return copy;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeIntegerSet(buffer, piecesIds);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.VIDEO_PIECES_ADVERTISEMENT;
        }

        public Set<Integer> getAdvertisedPiecesIds() {
            return piecesIds;
        }
    }

    public static final class Request extends DirectMsgNetty.Request {

        private final Set<Integer> piecesIds;

        public Request(VodAddress gvodSrc, VodAddress gvodDest, Set<Integer> piecesIds) {
            super(gvodSrc, gvodDest);
            checkSetValidity(piecesIds);
            this.piecesIds = Collections.unmodifiableSet(piecesIds);
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + 2 // length of Set<Integer> saves as to bytes
                    + 4 * piecesIds.size();
        }

        @Override
        public RewriteableMsg copy() {
            Request r = new Request(vodSrc, vodDest, new HashSet<Integer>(piecesIds));
            r.setTimeoutId(timeoutId);
            return r;
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            UserTypesEncoderFactory.writeIntegerSet(buffer, piecesIds);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.VIDEO_PIECES_REQUEST;
        }

        public Set<Integer> getPiecesIds() {
            return piecesIds;
        }
    }

    public static final class RequestTimeout extends Timeout {

        private final Request requestMsg;
        private final long delay;

        public RequestTimeout(ScheduleTimeout st, Request requestMsg) {
            super(st);
            if (requestMsg == null) {
                throw new IllegalArgumentException("Request msg cannot be null");
            }
            this.requestMsg = requestMsg;
            delay = st.getDelay();
        }

        public Request getRequestMsg() {
            return requestMsg;
        }
        
        public long getDelay() {
            return delay;
        }
    }

    public static final class Response extends DirectMsgNetty.Response {

        private final EncodedSubPiece esp;

        public Response(VodAddress gvodSrc, VodAddress gvodDest, TimeoutId timeoutId, EncodedSubPiece esp) {
            super(gvodSrc, gvodDest, timeoutId);
            if(esp == null) {
                throw new IllegalArgumentException("Message content cannot be null");
            }
            this.esp = esp;
        }

        @Override
        public int getSize() {
            return super.getHeaderSize()
                    + esp.getSize();
        }

        @Override
        public RewriteableMsg copy() {
            EncodedSubPiece espCopy = new EncodedSubPiece(esp.getGlobalId(), esp.getEncodedIndex(), Arrays.copyOf(esp.getData(), esp.getData().length), esp.getParentId());
            return new Response(vodSrc, vodDest, timeoutId, espCopy);
        }

        @Override
        public ByteBuf toByteArray() throws MessageEncodingException {
            ByteBuf buffer = createChannelBufferWithHeader();
            VideoTypesEncoderFactory.writeEncodedSubPiece(buffer, esp);
            return buffer;
        }

        @Override
        public byte getOpcode() {
            return VodMsgFrameDecoder.VIDEO_PIECES_RESPONSE;
        }

        public EncodedSubPiece getEncodedSubPiece() {
            return esp;
        }
    }

    private static void checkSetValidity(Set set) {
        if (set.isEmpty()) {
            throw new IllegalArgumentException("Message has to contain data");
        }
        for (Object pieceId : set) {
            if (pieceId == null) {
                throw new IllegalArgumentException("Message content cannot be null");
            }
        }
    }
}
