/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.gvod.net.util;

import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.Set;
import se.sics.gvod.common.msgs.MessageDecodingException;
import se.sics.gvod.video.msgs.EncodedSubPiece;

/**
 *
 * @author jdowling
 */
public class VideoTypesDecoderFactory {


    public static EncodedSubPiece readEncodedSubPiece(ByteBuf buffer) throws MessageDecodingException {
        int globalId = buffer.readInt();
        int encodedIndex = buffer.readInt();
        byte[] pieceData = UserTypesDecoderFactory.readArrayBytes(buffer);
        int parentId = buffer.readInt();
        return new EncodedSubPiece(globalId, encodedIndex, pieceData, parentId);
    }

    public static Set<EncodedSubPiece> readEncodedSubPieceSet(ByteBuf buffer) throws MessageDecodingException {
        int size = UserTypesDecoderFactory.readUnsignedIntAsTwoBytes(buffer);
        Set<EncodedSubPiece> pieces = new HashSet<EncodedSubPiece>(size);
        for (int i = 0; i < size; i++) {
            int globalId = buffer.readInt();
            int encodedIndex = buffer.readInt();
            byte[] pieceData = UserTypesDecoderFactory.readArrayBytes(buffer);
            int parentId = buffer.readInt();
            pieces.add(new EncodedSubPiece(globalId, encodedIndex, pieceData, parentId));
        }
        return pieces;
    }
}
