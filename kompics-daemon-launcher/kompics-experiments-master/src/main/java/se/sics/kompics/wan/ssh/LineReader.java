package se.sics.kompics.wan.ssh;

import java.io.IOException;
import java.io.InputStream;

public class LineReader {
	InputStream inputStream;

	StringBuffer buf;

	public LineReader(InputStream inputStream) {
		this.inputStream = inputStream;
		this.buf = new StringBuffer();
	}

	public String readLine() throws IOException {

		// System.out.println(b)
		int available = inputStream.available();
		if (available > 0) {
			byte[] byteBuffer = new byte[1];
			while (inputStream.read(byteBuffer, 0, 1) > 0) {
				String str = new String(byteBuffer);
				if (str.equals("\n") || str.equals("\r")) {
					if (buf.length() > 0) {
						String ret = buf.toString();
						buf = new StringBuffer();
						return ret;
					} else {
						continue;
					}
				}
				buf.append(str);
			}
		}
		return null;

	}

	public String readRest() throws IOException {
		int available = inputStream.available();
		if (available > 0) {
			byte[] byteBuffer = new byte[available];
			inputStream.read(byteBuffer, 0, available);
			buf.append(new String(byteBuffer));
			return buf.toString();
		}
		return "";
	}

}