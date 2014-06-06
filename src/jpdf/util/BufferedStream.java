package jpdf.util;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class BufferedStream {
	InputStream is;
	
	int nextByte = -1;
	
	public BufferedStream(InputStream is) {
		this.is = is;
	}
	
	public String readLine() throws IOException {
		StringBuilder sb = new StringBuilder();
		
		int b;
		while((b = read()) != -1) {
			char c = (char) ((byte) b);
			if(c == '\r') {
				// a \n might follow
				nextByte = is.read();
				if((char) nextByte == '\n') {
					nextByte = -1;
				}
				break;
			}
			if(c == '\n') {
				break;
			}
			sb.append(c);
		}
		
		return sb.toString();
	}

	public int read() throws IOException {
		if(nextByte != -1) {
			int tmp = nextByte;
			nextByte = -1;
			return tmp;
		}
		int b = is.read();
		if(b == -1) {
			throw new EOFException("-1 found");
		}
		return b;
	}

	public int read(byte[] bytes, int offset, int i) throws IOException {
		if(nextByte != -1) {
			bytes[offset] = (byte) nextByte;
			nextByte = -1;
			return 1;
		}
		
		return is.read(bytes, offset, i);
	}
}