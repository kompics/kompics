package se.sics.gvod.video.msgs;

import java.util.Arrays;

/**
 *
 * @author Niklas Wahl&#233;n <nwahlen@kth.se>
 */
public class Piece {

    private final int id;
    private SubPiece[] subPieces;
    public static final int SUBPIECES = 100;
    public static final int PIECE_DATA_SIZE = SUBPIECES * SubPiece.SUBPIECE_DATA_SIZE;
    public static final byte[] PADDING_CODE = {(byte) 0xff, 0, (byte) 0xff, 0};

    public Piece(int id) {
        this.id = id;
    }

    public Piece(int id, SubPiece[] subPieces) {
        this.id = id;
        this.subPieces = subPieces;
    }

    public Piece(int id, byte[] data) {
        if (data.length != PIECE_DATA_SIZE) {
            throw new IllegalArgumentException("Incorrect data size: " + data.length + ". Should be " + PIECE_DATA_SIZE + ".");
        }
        this.id = id;
        subPieces = new SubPiece[SUBPIECES];
        for (int i = 0, n = 0; i < data.length; i += SubPiece.SUBPIECE_DATA_SIZE, n++) {
            subPieces[n] = new SubPiece(n, Arrays.copyOfRange(data, i, i + SubPiece.SUBPIECE_DATA_SIZE), this);
        }
    }

    public void setSubPieces(SubPiece[] subPieces) {
        for (int i = 0; i < subPieces.length; i++) {
            if (subPieces[i] == null) {
                throw new IllegalArgumentException("null SubPieces not allowed.");
            }
            if (subPieces[i].getId() != i) {
                throw new IllegalArgumentException("SubPiece index and id are inconsistent.");
            }
            if (!subPieces[i].getParent().equals(this)) {
                throw new IllegalArgumentException("SubPiece does not belong to this Piece.");
            }
        }
        this.subPieces = subPieces;
    }

    public void setSubPiece(int index, SubPiece subPiece) {
        if (subPiece.getId() != index) {
            throw new IllegalArgumentException("SubPiece index and id are inconsistent.");
        }
        subPieces[index] = subPiece;
    }

    public int getId() {
        return id;
    }

    public SubPiece[] getSubPieces() {
        return subPieces;
    }
    
    public SubPiece getSubPiece(int i) {
        return subPieces[i];
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Piece other = (Piece) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!Arrays.deepEquals(this.subPieces, other.subPieces)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + this.id;
        return hash;
    }
}
