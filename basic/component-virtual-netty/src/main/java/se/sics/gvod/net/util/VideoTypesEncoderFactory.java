/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.sics.gvod.net.util;

import io.netty.buffer.ByteBuf;
import java.util.Set;
import se.sics.gvod.common.msgs.MessageEncodingException;
import se.sics.gvod.video.msgs.EncodedSubPiece;

/**
 *
 * @author jdowling
 */
public class VideoTypesEncoderFactory {
    
    public static void writeEncodedSubPiece(ByteBuf buffer, EncodedSubPiece esp) throws MessageEncodingException {
        if (esp == null) {
            throw new IllegalArgumentException("null EncodedSubPiece not allowed");
        }
        buffer.writeInt(esp.getGlobalId());
        buffer.writeInt(esp.getEncodedIndex());
        UserTypesEncoderFactory.writeArrayBytes(buffer, esp.getData());
        buffer.writeInt(esp.getParentId());
    }

    public static void writeEncodedSubPieceSet(ByteBuf buffer, Set<EncodedSubPiece> pieces) throws MessageEncodingException {
        if (pieces == null || pieces.isEmpty()) {
            throw new IllegalArgumentException("Empty EncodedSubPiece set not allowed");
        } else {
            UserTypesEncoderFactory.writeUnsignedintAsTwoBytes(buffer, pieces.size());
            for (EncodedSubPiece p : pieces) {
                buffer.writeInt(p.getGlobalId());
                buffer.writeInt(p.getEncodedIndex());
                UserTypesEncoderFactory.writeArrayBytes(buffer, p.getData());
                buffer.writeInt(p.getParentId());
            }
        }
    }
    
    
}
