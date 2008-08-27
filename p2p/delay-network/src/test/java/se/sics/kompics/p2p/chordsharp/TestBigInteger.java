package se.sics.kompics.p2p.chordsharp;

import java.math.BigInteger;
import java.nio.charset.Charset;

import org.junit.Ignore;

@Ignore
public class TestBigInteger {

	public static void main(String[] args) {
		Charset charset = Charset.forName("UTF-8");

		BigInteger abc = new BigInteger("se.sics".getBytes(charset));
		System.out.println("Hi " + (abc.bitLength() + 1) / 8 + " = " + abc);
		abc = new BigInteger("123456");
		String str = new String(abc.toByteArray(), charset);
		System.out.println(str);
	}
}
