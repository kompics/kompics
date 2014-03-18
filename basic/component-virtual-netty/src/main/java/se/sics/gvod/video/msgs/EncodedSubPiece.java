package se.sics.gvod.video.msgs;

import java.io.Serializable;
import java.util.Arrays;

/**
 * The data representation used in Three-phase gossip. 
 *
 * @author Niklas Wahl&#233;n <nwahlen@kth.se>
 */
public class EncodedSubPiece implements Serializable {

    private final int globalId;
    private final int encodedIndex;
    private final byte[] data;
    private final int parentId;

    public EncodedSubPiece(int globalId, int encodedIndex, byte[] data, int parentId) {
        this.globalId = globalId;
        this.encodedIndex = encodedIndex;
        this.data = data;
        this.parentId = parentId;
    }
    
    public int getGlobalId() {
        return globalId;
    }

    public int getEncodedIndex() {
        return encodedIndex;
    }

    public byte[] getData() {
        return data;
    }

    public int getParentId() {
        return parentId;
    }

    public static int getSize() {
        return 1 + 1 + SubPiece.SUBPIECE_DATA_SIZE + 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EncodedSubPiece other = (EncodedSubPiece) obj;
        if (this.globalId != other.globalId) {
            return false;
        }
        if (this.encodedIndex != other.encodedIndex) {
            return false;
        }
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        if (this.parentId != other.parentId) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + this.globalId;
        return hash;
    }
}
