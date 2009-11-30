package se.sics.kompics.network.model.king;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Random;

import org.junit.Ignore;

@Ignore
public class KingTest {

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		Random r = new Random(1);

		int[][] k = KingMatrix.KING;
		int[][] kk = new int[1740][1740];

		int zeros = 0, nondiag = 0;
		double m = 0, m0 = 0, m1 = 0, m2 = 0;
		int c = 0, c0 = 0, c1 = 0, c2 = 0;

		for (int i = 0; i < k.length; i++) {
			for (int j = 0; j < k[i].length; j++) {
				if (k[i][j] == 0) {
					zeros++;
					if (i != j) {
						nondiag++;

						int rr = 80 + r.nextInt(21);

						k[i][j] = rr;

						m2 = (m2 * c2 + k[i][j]) / (c2 + 1);
						c2++;

						kk[i][j] = rr;
					} else {
						kk[i][j] = 0;
					}
				} else {
					kk[i][j] = k[i][j];

					m = (m * c + k[i][j]) / (c + 1);
					c++;
				}

				m0 = (m0 * c0 + k[i][j]) / (c0 + 1);
				c0++;

				if (i != j) {
					m1 = (m1 * c1 + k[i][j]) / (c1 + 1);
					c1++;

				}
			}
		}

		System.err.println("Z=" + zeros + " non-diag=" + nondiag);
		System.err.println("M=" + m + " M0=" + m0 + " M1=" + m1 + " M2=" + m2);
		System.err.println("C2=" + c2);

		int nd = 0;
		for (int i = 0; i < kk.length; i++) {
			for (int j = 0; j < kk.length; j++) {
				if (kk[i][j] == 0 && i != j) {
					nd++;
				}
			}
		}

		System.err.println("ND=" + nd);

//		saveKing(kk);
	}

	// private static void saveKing(int[][] m) throws FileNotFoundException,
	// IOException {
	// ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
	// "KingMatrix.data"));
	// oos.writeObject(m);
	// oos.flush();
	// oos.close();
	// }

	private static final class KingMatrix {
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
}
