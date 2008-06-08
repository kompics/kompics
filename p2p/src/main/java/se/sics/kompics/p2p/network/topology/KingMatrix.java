package se.sics.kompics.p2p.network.topology;

import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * The <code>KingMatrix</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: KingMatrix.java 149 2008-06-06 08:55:36Z Cosmin $
 */
public final class KingMatrix {

	public static final int KING[][];

	static {
		int king[][];
		try {
			ObjectInputStream ois = new ObjectInputStream(KingMatrix.class
					.getResourceAsStream("KingMatrix.data"));
			king = (int[][]) ois.readObject();
		} catch (IOException e) {
			king = null;
		} catch (ClassNotFoundException e) {
			king = null;
		}

		KING = king;
	}
}
