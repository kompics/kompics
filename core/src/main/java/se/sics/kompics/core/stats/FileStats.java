package se.sics.kompics.core.stats;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileStats {

	static final int BUFFER_SIZE = 4096;

	static final int MAX_COUNT = BUFFER_SIZE / 8;
	
	static FileChannel out; 
	
	ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		
	static {
		try {
			out = new FileOutputStream("tau.stats").getChannel();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private int count = 0;

	public int getCount() {
		return count;
	}

	public void push(double x) {
		count++;
		
		buffer.putDouble(x);

		if (count == MAX_COUNT) {
			count = 0;
			// dump to file
			buffer.flip();
			try {
				out.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			buffer.clear();
		}
	}
}
