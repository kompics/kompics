package se.sics.kompics.p2p.network.topology;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;

/**
 * The <code>KingMatrixMain</code> class
 * 
 * @author Cosmin Arad
 * @version $Id: KingMatrixMain.java 149 2008-06-06 08:55:36Z Cosmin $
 */
public final class KingMatrixMain {

	private static final int SIZE = 1740;

	public static void main(String[] args) throws IOException {
		// working fast (250 milliseconds to read the King matrix in)
		saveKingAsObjectStream();

		// working but slow (54 seconds to read the King matrix in)
		// saveKingAsDataStream();

		// not working due to Java static initializer limit
		// saveKingAsStaticArrayInClass();

		// not working due to Java static initializer limit
		// saveKingAsStaticArraysInClass();
	}

	public static void saveKingAsObjectStream() throws FileNotFoundException,
			IOException {

		int[][] king = new int[SIZE][SIZE];

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream("src/main/resources/se/sics/kompics/p2p"
						+ "/network/topology/matrix")));

		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(
				"src/main/resources/se/sics/kompics/p2p/network/topology"
						+ "/KingMatrix.data"));

		long t = System.currentTimeMillis();

		for (int i = 0; i < SIZE; i++) {
			String line = br.readLine();
			String rtts[] = line.split(" ");

			for (int j = 0; j < SIZE; j++) {
				king[i][j] = Integer.parseInt(rtts[j]) / 2000;
			}
		}

		System.out.println("Read from text takes "
				+ (System.currentTimeMillis() - t));

		t = System.currentTimeMillis();

		oos.writeObject(king);
		oos.flush();
		oos.close();

		System.out.println("Write to binary takes "
				+ (System.currentTimeMillis() - t));

		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(
				"src/main/resources/se/sics/kompics/p2p/network"
						+ "/topology/KingMatrix.data"));

		int[][] kingIn = null;

		t = System.currentTimeMillis();

		try {
			kingIn = (int[][]) ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		System.out.println("Read from binary takes "
				+ (System.currentTimeMillis() - t));

		ois.close();

		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				if (kingIn[i][j] != kingIn[j][i]) {
					System.err.println("Asymmetric");
					System.exit(1);
				}
			}
		}

		System.err.println("Symmetric");
	}

	public static void saveKingAsDataStream() throws FileNotFoundException,
			IOException {

		long[][] king = new long[SIZE][SIZE];

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream("src/main/resources/se/sics/kompics/p2p"
						+ "/network/topology/matrix")));

		DataOutputStream dos = new DataOutputStream(new FileOutputStream(
				"src/main/resources/se/sics/kompics/p2p/network/topology"
						+ "/KingMatrix.data"));

		long t = System.currentTimeMillis();

		for (int i = 0; i < SIZE; i++) {
			String line = br.readLine();
			String rtts[] = line.split(" ");

			for (int j = 0; j < SIZE; j++) {
				king[i][j] = Long.parseLong(rtts[j]) / 2000;
			}
		}

		System.out.println("Read from text takes "
				+ (System.currentTimeMillis() - t));

		int k = 0;
		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				dos.writeInt((int) king[i][j]);
				k++;
			}
			System.out.println("Done row " + i);
		}
		dos.flush();
		dos.close();

		System.err.println("Wrote " + k + " values");

		DataInputStream dis = new DataInputStream(new FileInputStream(
				"src/main/resources/se/sics/kompics/p2p/network"
						+ "/topology/KingMatrix.data"));

		long[][] kingIn = new long[SIZE][SIZE];

		t = System.currentTimeMillis();

		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				kingIn[i][j] = dis.readInt();
			}
		}

		System.out.println("Read from binary takes "
				+ (System.currentTimeMillis() - t));

		dis.close();
	}

	public static void saveKingAsStaticArraysInClass() throws IOException {

		long[][] king = new long[SIZE][SIZE];

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream("src/main/resources/se/sics/kompics/p2p"
						+ "/network/topology/matrix")));

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(
						"src/main/resources/se/sics/kompics/p2p/network/topology"
								+ "/KingMatrix.data")));

		long t = System.currentTimeMillis();

		for (int i = 0; i < SIZE; i++) {
			String line = br.readLine();
			String rtts[] = line.split(" ");

			for (int j = 0; j < SIZE; j++) {
				king[i][j] = Long.parseLong(rtts[j]) / 2000;
			}
		}

		System.out.println("Read from text takes "
				+ (System.currentTimeMillis() - t));

		bw.write("package se.sics.kompics.p2p.network.topology;\n"
				+ "public class KingMatrix {\n");
		for (int i = 0; i < SIZE; i++) {
			bw.write(" private static final int king" + i + "[] = { ");
			for (int j = 0; j < SIZE - 1; j++) {
				bw.write(king[i][j] + ", ");
			}
			bw.write(king[i][SIZE - 1] + " };\n");
		}
		bw.write(" public static final int king[][] = new int[" + SIZE + "]["
				+ SIZE + "];\n");
		bw.write(" static {\n");
		for (int i = 0; i < SIZE; i++) {
			bw.write(" king[" + i + "] = king" + i + ";\n");
		}
		bw.write(" }\n");

		bw.write("}\n");
		bw.flush();
		bw.close();
	}

	public static void saveKingAsStaticArrayInClass() throws IOException {

		long[][] king = new long[SIZE][SIZE];

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream("src/main/resources/se/sics/kompics/p2p"
						+ "/network/topology/matrix")));

		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(
						"src/main/resources/se/sics/kompics/p2p/network/topology"
								+ "/KingMatrix.data")));

		long t = System.currentTimeMillis();

		for (int i = 0; i < SIZE; i++) {
			String line = br.readLine();
			String rtts[] = line.split(" ");

			for (int j = 0; j < SIZE; j++) {
				king[i][j] = Long.parseLong(rtts[j]) / 2000;
			}
		}

		System.out.println("Read from text takes "
				+ (System.currentTimeMillis() - t));

		bw.write("package se.sics.kompics.p2p.network.topology;\n"
				+ "public class KingMatrix {\n"
				+ " public static final int king[][] = {\n");

		for (int i = 0; i < SIZE - 1; i++) {
			bw.write("{ ");
			for (int j = 0; j < SIZE - 1; j++) {
				bw.write(king[i][j] + ", ");
			}
			bw.write(king[i][SIZE - 1] + " }, \n");
		}
		bw.write("{ ");
		for (int j = 0; j < SIZE - 1; j++) {
			bw.write(king[SIZE - 1][j] + ", ");
		}
		bw.write(king[SIZE - 1][SIZE - 1] + " } ");
		bw.write("};\n}");
		bw.flush();
		bw.close();
	}
}
