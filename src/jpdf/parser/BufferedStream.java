package jpdf.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BufferedStream {
	InputStream is;
	
	int nextByte = -1;
	
	BufferedStream(InputStream is) {
		this.is = is;
	}
	
	String readLine() throws IOException {
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
		return is.read();
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