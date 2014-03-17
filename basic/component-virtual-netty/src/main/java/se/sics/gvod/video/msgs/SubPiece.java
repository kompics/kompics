package se.sics.gvod.video.msgs;

import java.util.Arrays;

/**
 *
 * @author Niklas Wahl&#233;n <nwahlen@kth.se>
 */
public class SubPiece {

    // 1316 size taken from Gossip++
    public static final int SUBPIECE_DATA_SIZE = 1316;
    private int id;
    private byte[] data;
    private Piece parent;

    /**
     *
     * @param id The SubPiece's id (position) in the parent Piece
     * @param data
     * @param parent The Piece which this SubPiece belongs to.
     */
    public SubPiece(int id, byte[] data, Piece parent) {
        if (data.length != SUBPIECE_DATA_SIZE) {
            throw new IllegalArgumentException("For input: "
                    + "id " + id
                    + ", data.length " + data.length
                    + ", parent id " + parent.getId()
                    + ": data has to contain 1316 bytes.");
        }
        this.id = id;
        this.data = data;
        this.parent = parent;
    }

    public byte[] getData() {
        return data;
    }

    public int getId() {
        return id;
    }

    public Piece getParent() {
        return parent;
    }

    @Override
    /*
     * Don't compare parents since it will create a loop (parent pieces compare
     * their sub pieces)
     */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SubPiece other = (SubPiece) obj;
        if (this.id != other.id) {
            return false;
        }
        if (!Arrays.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.id;
        return hash;
    }
}
