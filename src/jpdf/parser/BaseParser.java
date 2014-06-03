package jpdf.parser;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import jpdf.objects.PdfArray;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfHexadecimalString;
import jpdf.objects.PdfIndirectObject;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.objects.PdfLiteralString;
import jpdf.objects.PdfStreamObject;

abstract public class BaseParser {
	protected static char[] delimiters = {'(', ')', '<', '>', '[', ']', '{', '}', '/', '%'};
	
	private static final boolean DEBUG = false;
	
	static {
		Arrays.sort(delimiters);
	}
	
	protected Map<PdfNumber,Map<PdfNumber,PdfIndirectObject>> indirectObjects;
	
	protected Set<PdfStreamObject> streamObjects = new HashSet<>();
	
	/**
	 * Buffer holding temporary strings
	 */
	
	protected StringBuffer buffer;
	
	protected Stack<PdfObject> objects = new Stack<>();
	
	protected PdfObject object = null;

	private BufferedStream stream;
	
	private void debug(String s) {
		if(DEBUG)
			System.out.println(s);
	}
	
	public BaseParser(BufferedStream stream) {
		this.stream = stream;
		indirectObjects = new HashMap<>();
		buffer = new StringBuffer();
	}
	
	protected boolean contains(char[] arr, char c) {
		for (int i = 0; i < arr.length; i++) {
		    if (arr[i] == c) {
		        return true;
		    }
		}
		
		return false;
	}
	
	protected boolean isDelimiter(char c) {
		return contains(delimiters, c);
	}

	protected String clearBuffer() {
		String oldBuffer = buffer.toString();
		buffer = new StringBuffer();
		return oldBuffer;
	}
	
	protected Character _nextChar() {
		try 
		{
			return Character.toChars(stream.read())[0];
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * Reads a number
	 * 
	 * @return the number
	 */
	
	protected PdfNumber readNumber() {
		Character tmp;
		while(((tmp = _nextChar()) != null) && ((tmp >= '0' && tmp <= '9') || tmp == '.')) {
			buffer.append(tmp);
		}
		String b = clearBuffer();
		if(tmp == ' ' || tmp == '\r' || tmp == '\n') {
			tmp = nextChar(true);
		} else {
			buffer.append(tmp);
		}
		return new PdfNumber(b);
	}
	
	/**
	 * Reads a name
	 * 
	 * @param strict Dont allow special characters like [
	 * 
	 * @return PdfString
	 */
	
	protected PdfLiteralString readName(boolean strict) {
		Character tmp;
		while(((tmp = _nextChar()) != null) && tmp >= 0x21 && tmp <= 0x7e && !isDelimiter(tmp)) {
			buffer.append(tmp);
		}
		String s = clearBuffer();
		if(tmp == ' ') {
			nextChar(true);
		} else {
			buffer.append(tmp);
		}
		return new PdfLiteralString(s);
	}
	
	protected String readUntil(char c) {
		Character tmp;
		while(((tmp = _nextChar()) != null) && tmp != c) {
			buffer.append(tmp);
		}
		String b = clearBuffer();
		return b;
	}
	
	protected Character nextChar(boolean skipNewline) {
		Character tmp;
		do
		{
			tmp = _nextChar();
		} while(tmp == ' ' || (skipNewline && (tmp == '\r' || tmp == '\n')));
		buffer.append(tmp);
		return tmp;
	}
	
	protected Character nextChar() {
		return nextChar(false);
	}
	
	protected PdfIndirectObject parseIndirectObject() throws ParserException {
		skipComments();
		PdfNumber num1 = readNumber();
		PdfNumber num2 = readNumber();
		
		debug("indirectObject: " + num1 + "." + num2);
		
		boolean hasKeywords = false;
		
		debug("Buffer: " + buffer.toString());
		
		if(buffer.toString().equals("o")) {
			debug("'obj' keyword found");
			readWord("bj");
			clearBuffer();
			hasKeywords = true;
			nextChar(true);
		}
		
		PdfObject obj = parseObject();
		
		if(buffer.toString().equals("s")) {
			//readWord("tream");
			readLine();
			
			if(!(obj instanceof PdfDictionary)) {
				throw new ParserException("Expecting the object before a stream to be a dictionary.");
			}
			
			clearBuffer();
			
			PdfDictionary dict = (PdfDictionary) obj;
			
			obj = new PdfStreamObject(dict);
			streamObjects.add((PdfStreamObject) obj);
			
			int length = Integer.parseInt(dict.get("Length").toString());
			byte[] data = readNBytes(length);
			if(dict.containsKey("Filter")) {
				if(dict.containsKey("FlateDecode")) {
					// TODO better decoding
					Inflater decompressor = new Inflater();
					byte[] result = new byte[length];
					decompressor.setInput(data, 0, length);
					try {
						int resultLength = decompressor.inflate(result);
						decompressor.end();
						String res = new String(result, 0, resultLength, "UTF-8");
						//System.out.println(decompressor.needsInput());
					} catch (DataFormatException e) {
						throw new ParserException("Unable to decode data for " + dict);
					} catch (UnsupportedEncodingException e) {
						throw new ParserException("Unable to decode data for " + dict);
					}
					
				} else if(dict.containsKey("DCTDecode")) {
					
				} else {
					throw new ParserException("Unsupported filter. " + dict);
				}
			} else {
				// System.out.println(dict);
			}
			
			nextChar(true);
			readLine("endstream");
			clearBuffer();
			nextChar(true);
		}
		
		if(hasKeywords) {
			readWord("ndobj");
			clearBuffer();
			nextChar(true);
		}
		
		PdfIndirectObject io = new PdfIndirectObject(num1, num2, obj);
		
		if(!indirectObjects.containsKey(num1)) {
			indirectObjects.put(num1, new HashMap<PdfNumber,PdfIndirectObject>());
		}
		
		indirectObjects.get(num1).put(num2, io);
		
		return io;
	}

	private void skipComments() {
		while(buffer.toString().equals("%")) {
			debug("skip comment");
			parseComment();
		}
	}
	
	protected byte[] readNBytes(int n) throws ParserException {
		int offset = 0;
		byte[] bytes = new byte[n];
		try {
			while(offset < n) {
				offset += stream.read(bytes, offset, n-offset);
			}
		} catch (IOException ex) {
			throw new ParserException(ex.getMessage());
		}
		
		return bytes;
	}
	
	protected void readWord(String word) throws ParserException {
		int l = word.length();
		int offset = 0;
		byte[] bytes = new byte[l];
		try {
			while(offset < l) {
				//offset += bufferedReader.read(chars, offset, l-offset);
				offset += stream.read(bytes, offset, l-offset);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		char[] chars = new char[l];
		int i = 0;
		for(byte b : bytes) {
			chars[i] = (char) b;
			i++;
		}
		String s = String.valueOf(chars);
		
		if(!s.equals(word)) {
			throw new ParserException("Expected word '"+word+"' found '"+s+"'");
		}
	}
	
	protected String readLiteralString() {
		StringBuilder sb = new StringBuilder();
		// HashMap<Character,Integer> charCount = new HashMap<>();
		int parenthesesCount = 0;
		
		Character tmp;
		boolean escape = false;
		while(true) {
			tmp = nextChar();
			
			if(escape) {
				escape = false;
				debug("escaped: " + tmp);
				
				// ignore backslash if not one of the following characters
				if(tmp != '\n' && tmp != '\r' && tmp != '\t' && tmp != '\b' && tmp != '\f' && tmp != '(' && tmp != ')' && tmp != '\\') {
					// continue;
				}
			} else {
				if(tmp == '\\') {
					escape = true;
				} else if(tmp == '(') {
					parenthesesCount++;
				} else if(tmp == ')') {
					if(parenthesesCount == 0) {
						break;
					} else {
						parenthesesCount--;
					}
				}
			}
			
			buffer.append(tmp);
		}
		
		return sb.toString();
	}
	
	protected PdfObject parseObject() throws ParserException {
		return parseObject(false);
	}
	
	protected PdfObject parseObject(boolean strict) throws ParserException {
		debug("[parseObject] buffer:" + buffer);
		
		switch(buffer.toString()) {
		// Dictionary
			case "<" :
				// Check second <
				clearBuffer();
				Character next = nextChar(); // second <;
				if(next == '<') {
					debug("[parseObject] Dictionary");
					PdfDictionary dic = new PdfDictionary();
					clearBuffer();
					nextChar(true);
					while(buffer.toString().equals("/")) {
						// Remove '/'
						clearBuffer();
						nextChar(); // p17: "no white space may intervene between the SOLIDUS and the encoded name"
						PdfObject name = parseObject(true);
						if(!(name instanceof PdfLiteralString)) {
							throw new ParserException("Expecting the dictionary name to be a string.");
						}
						LinkedList<PdfObject> objects = new LinkedList<>();
						
						debug("[DICT] Name: " + name);
						//objects.add(parseObject(true));
						int size = 0;
						while(!buffer.toString().equals("/") && !buffer.toString().equals(">")) {
							PdfObject obj = parseObject(true);
							debug("[DICT] obj("+size+"): " + obj);
							objects.add(obj);
							size++;
						}
						
						if(size == 0) {
							dic.put(name.toString(), null);
						} else if(size == 1) {
							dic.put(name.toString(), objects.get(0));
						} else if(size == 3) {
							if(objects.get(2) instanceof PdfLiteralString) {
								//System.out.println(indirectObjects.get(objects.get(0)));
								dic.put(name.toString(), new PdfIndirectReference((PdfNumber) objects.get(0), (PdfNumber) objects.get(1)));
							} else {
								throw new ParserException("Expecting object 3 to be a literal string.");
							}
						} else {
							throw new ParserException("Unknown number of objects after name token in dictionary: '"+size+"'");
						}
					}
					readWord(">");
					clearBuffer();
					nextChar(true);
					debug("[DICT] Done. Next Buffer: '"+ buffer + "'");
					return dic;
				} else {
					String s = readUntil('>');
					clearBuffer();
					nextChar(true); // >
					return new PdfHexadecimalString(s);
				}
			case "(" :
				//throw new ParserException("unimplemented");
				clearBuffer();
				nextChar(true);
				String s = readLiteralString();
				clearBuffer();
				nextChar(true);
				return new PdfLiteralString(s);
				
				//break;
			case "[" :
				clearBuffer();
				nextChar(true);
				debug("array");
				PdfArray array = new PdfArray();
				
				while(!buffer.toString().equals("]")) {
					array.add(parseObject());
				}
				
				if(!buffer.toString().equals("]")) {
					throw new ParserException("Expected ']' at end of array. Found '"+buffer+"'");
				}
				clearBuffer();
				nextChar(true);
				return array;
			default :
				char firstChar = buffer.charAt(0);
				debug("firstChar:" + firstChar);
				// Number
				if ( (firstChar >= '0' && firstChar <= '9') || firstChar == '-' || firstChar == '.') {
					return readNumber();
				} else {
					return readName(strict);
				}
		}
		
		//return null;
	}

	protected String parseComment() {
		readLine();
		String comment = buffer.toString();
		clearBuffer();
		nextChar();
		return comment;
	}

	protected void readLine() {
		try {
			buffer.append(stream.readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void readLine(String s) throws ParserException {
		readLine();
		if(!s.equals(buffer.toString())) {
			throw new ParserException("Expected line '"+s+"' instead of '"+buffer+"'");
		}
	}
}
