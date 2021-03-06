package jpdf.parser;

import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpdf.decode.Predictor;
import jpdf.objects.PdfArray;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfHexadecimalString;
import jpdf.objects.PdfIndirectObject;
import jpdf.objects.PdfKeyword;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.objects.PdfLiteralString;
import jpdf.objects.PdfStreamObject;
import jpdf.references.PdfByteOffsetReference;
import jpdf.references.PdfReference;
import jpdf.util.BufferedStream;

abstract public class BaseParser {
	protected static char[] delimiters = {'(', ')', '<', '>', '[', ']', '{', '}', '/', '%'};
	
	protected static boolean DEBUG = false;
	
	static {
		Arrays.sort(delimiters);
	}
	
	protected Map<PdfNumber,Map<PdfNumber,PdfIndirectObject>> indirectObjects;
	
	// protected Set<PdfStreamObject> streamObjects = new HashSet<>();
	
	/**
	 * Buffer holding temporary strings
	 */
	
	protected StringBuffer buffer;
	
	// protected Stack<PdfObject> objects = new Stack<>();
	
	// protected PdfObject object = null;
	
	protected List<PdfDictionary> crossReferenceDictionaries = new ArrayList<>(); 
	
	protected PdfDictionary rootDictionary = null;
	
	protected HashMap<Integer,HashMap<Integer, PdfReference>> crossReferences;

	// private BufferedStream stream;
	
	// private RandomAccessFile file;
	
	protected void debug(String s) {
		if(DEBUG)
			System.out.println(s);
	}
	
	/**
	 * Read byte
	 * @return byte or -1
	 * @throws IOException 
	 */
	
	abstract protected int read() throws IOException;
	
	abstract protected int read(byte[] bytes, int offset, int i) throws IOException;
	
	abstract protected String readLine() throws IOException;
	
	public BaseParser() {
		indirectObjects = new HashMap<>();
		buffer = new StringBuffer();
		crossReferences = new HashMap<>();
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
	
	protected Character _nextChar() throws EOFException {
		try 
		{
			int b = read();
			if(b == -1)
				throw new EOFException("EOF Reached in BaseParser._nextChar()");
			
			return (char) b;
		} catch(EOFException e) {
			throw e;
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Reads a number
	 * 
	 * @return the number
	 * @throws EOFException 
	 */
	
	protected PdfNumber readNumber() throws EOFException {
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
	 * @return PdfString
	 * @throws EOFException 
	 */
	
	protected PdfLiteralString readName() throws EOFException {
		Character tmp;
		while(((tmp = _nextChar()) != null) && tmp >= 0x21 && tmp <= 0x7e && !isDelimiter(tmp)) {
			buffer.append(tmp);
		}
		String s = clearBuffer();
		if(tmp == ' '  || tmp == '\n' || tmp == '\r') { 
			nextChar(true);
		} else {
			buffer.append(tmp);
		}
		return new PdfLiteralString(s);
	}
	
	protected String readUntil(char c) throws EOFException {
		Character tmp;
		while(((tmp = _nextChar()) != null) && tmp != c) {
			buffer.append(tmp);
		}
		String b = clearBuffer();
		return b;
	}
	
	protected Character nextChar(boolean skipSpaceCharacters) throws EOFException {
		Character tmp;
		do
		{
			tmp = _nextChar();
		} while((skipSpaceCharacters && (tmp == '\r' || tmp == '\n' || tmp == ' ')));
		buffer.append(tmp);
		return tmp;
	}
	
	protected Character nextChar() throws EOFException {
		return nextChar(false);
	}

	protected void skipComments() throws EOFException {
		while(buffer.toString().equals("%")) {
			//debug("skip comment");
			parseComment();
		}
	}
	
	protected byte[] readNBytes(int n) throws ParserException {
		int offset = 0;
		byte[] bytes = new byte[n];
		try {
			while(offset < n) {
				offset += read(bytes, offset, n-offset);
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
				offset += read(bytes, offset, l-offset);
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
	
	protected String readLiteralString() throws EOFException {
		StringBuilder sb = new StringBuilder();
		// HashMap<Character,Integer> charCount = new HashMap<>();
		int parenthesesCount = 0;
		
		Character tmp;
		boolean escape = buffer.charAt(0) == '\\';
		if(!escape) {
			sb.append(buffer);
		}
		while(true) {
			tmp = nextChar(false);
			
			if(escape) {
				escape = false;
				
				// ignore backslash if not one of the following characters
				if(tmp != '\n' && tmp != '\r' && tmp != '\t' && tmp != '\b' && tmp != '\f' && tmp != '(' && tmp != ')' && tmp != '\\') {
					// continue;
				}
			} else {
				if(tmp == '\\') {
					escape = true;
					continue;
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
			sb.append(tmp);
		}
		
		return sb.toString();
	}
	
	protected PdfObject parseObject() throws ParserException, EOFException {
		return parseObject(false);
	}
	
	protected PdfObject parseObject(boolean strict) throws ParserException, EOFException {
		//debug("[parseObject] buffer:" + buffer);
		
		switch(buffer.toString()) {
			// Dictionary or String
			case "<" :
				// Check second <
				clearBuffer();
				Character next = nextChar(); // second <;
				if(next == '<') {
					debug("[parseObject] Dictionary started");
					PdfDictionary dic = new PdfDictionary();
					clearBuffer();
					nextChar(true);
					// LinkedList<PdfObject> objectBuffer = new LinkedList<>();
					PdfLiteralString name = null;
					PdfObject value = null;
					PdfNumber generation = null;
					while(buffer.charAt(0) != '>') {
						PdfObject obj = parseObject(true);
						debug("dict.obj=" + obj + "'"+buffer+"'");
						if(obj instanceof PdfLiteralString) {
							if(value != null) {
								//debug(name + " = " + value);
								dic.put(name.toString(), value);
								value = null;
								name = (PdfLiteralString) obj;
							} else if(name != null) {
								// Two "Names"
								// debug(name + " = null");
								dic.put(name.toString(), obj);
								name = null;
							} else {
								name = (PdfLiteralString) obj;
							}
						} else if(obj instanceof PdfKeyword) {
							//debug(name + " = R(" + value + " " + generation + ")");
							dic.put(name.toString(), new PdfIndirectReference((PdfNumber) value, generation));
							name = null;
							value = null;
							generation = null;
						} else if(value == null) {
							value = obj;
						} else if(obj instanceof PdfNumber) {
							generation = (PdfNumber) obj;
						} else {
							throw new ParserException("Found two objects after dictionary name. Expected number but found " + obj);
						}
					}
					
					if(name != null) {
						if(value != null) {
							if(generation != null) {
								throw new ParserException("Unimplemented");
							} else {
								debug(name + " = " + value);
								dic.put(name.toString(), value);
							}
						} else {
							dic.put(name.toString(), null);
						}
					}
					
					/*
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
					*/
					readWord(">");
					clearBuffer();
					nextChar(true);
					debug("[DICT] Done.");
					return dic;
				} else {
					String s = readUntil('>');
					clearBuffer();
					nextChar(true); // >
					return new PdfHexadecimalString(s);
				}
			case "(" :
				clearBuffer(); // remove (
				nextChar(true);
				// TODO enhance white space handling
				if(buffer.charAt(0) == ')') {
					clearBuffer(); // remove (
					nextChar(true);
					return new PdfLiteralString(" ");
				}
				String s = readLiteralString();
				clearBuffer();
				nextChar(true);
				return new PdfLiteralString(s);
			case "[" :
				clearBuffer();
				nextChar(true);
				PdfArray array = new PdfArray();
				while(!buffer.toString().equals("]")) {
					PdfObject obj = parseObject();
					if(obj instanceof PdfKeyword) {
						int size = array.size();
						PdfNumber generation = (PdfNumber) array.remove(size-1);
						PdfNumber id = (PdfNumber) array.remove(size-2);
						array.add(new PdfIndirectReference(id, generation));
					} else {
						array.add(obj);
					}
				}
				
				if(!buffer.toString().equals("]")) {
					throw new ParserException("Expected ']' at end of array. Found '"+buffer+"'");
				}
				clearBuffer();
				nextChar(true);
				return array;
			case "/" :
				clearBuffer();
				nextChar(true);
				return readName();
			case "R" :
				clearBuffer();
				nextChar(true);
				return new PdfKeyword("R");
			default :
				char firstChar = buffer.charAt(0);
				//debug("firstChar:" + firstChar);
				if ( (firstChar >= '0' && firstChar <= '9') || firstChar == '-' || firstChar == '.') {
					return readNumber();
				} else if(firstChar == 't') {
					readWord("rue");
					clearBuffer();
					nextChar(true);
					return new PdfLiteralString("true");
				} else if(firstChar == 'f') {
					readWord("alse");
					clearBuffer();
					nextChar(true);
					return new PdfLiteralString("false");
				} else if(firstChar == 'n') {
					readWord("ull");
					clearBuffer();
					nextChar(true);
					return new PdfLiteralString("null");
				} else if(firstChar == 'g') {
					clearBuffer();
					nextChar(true);
					if(buffer.charAt(0) == 's') {
						clearBuffer();
						nextChar(true);
						return new PdfKeyword("gs");
					}
					
					return new PdfKeyword("g");
				}
				
				throw new ParserException("Unknown next character: '" + firstChar + "'");
		}
	}

	protected String parseComment() throws EOFException {
		_readLine();
		String comment = buffer.toString();
		clearBuffer();
		nextChar();
		return comment;
	}

	protected void _readLine() {
		try {
			buffer.append(readLine());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void readLine(String s) throws ParserException {
		_readLine();
		if(!s.equals(buffer.toString())) {
			throw new ParserException("Expected line '"+s+"' instead of '"+buffer+"'");
		}
	}
	
	/*
	protected void parsePageTree(PdfDictionary pages) throws ParserException {
		if(!pages.containsKey("Kids")) {
			parsePageObject(pages);
			return;
		}
		PdfArray kids = (PdfArray) pages.get("Kids");
		for(PdfObject kid : kids) {
			if(kid instanceof PdfDictionary) {
				PdfDictionary kidDict = (PdfDictionary) kid;
				parsePageTree(kidDict);
			}
		}
	}
	
	protected void parsePageObject(PdfDictionary pages) throws ParserException {
		pageCount++;
		if(pageCount == 594 || true) {
			PdfObject contents = pages.get("Contents");
			if(contents instanceof PdfStreamObject) {
				PdfStreamObject stream = (PdfStreamObject) contents; 
				byte[] data = stream.getData();
				try {
					String res = new String(data, 0, data.length, "UTF-8");
					// System.out.println(res);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				ContentParser parser = new ContentParser(new BufferedStream(new ByteArrayInputStream(data)));
				parser.parse();
				System.out.println("\n");
			} else {
				throw new ParserException("Contents is not a stream.");
			}
		}
	}
	*/
	
	/**
	 * Parses the cross reference sections after the xref keyword
	 * 
	 * @throws EOFException 			If there are no more characters
	 */
	
	protected void parseCrossReferenceSections() throws EOFException {
		while(buffer.charAt(0) != 't') {
			int first = Integer.parseInt(readNumber().toString());
			int i = Integer.parseInt(readNumber().toString());
			
			while(i > 0) {
				int offset = Integer.parseInt(readNumber().toString());
				int generation = Integer.parseInt(readNumber().toString());
				_readLine();
				
				// Only add to map if entry is used.
				if(buffer.charAt(0) == 'n') {
					addCrossReference(first, generation, new PdfByteOffsetReference(offset));
					/*if(!crossReferences.containsKey(first)) {
						crossReferences.put(first, new HashMap<Integer,Integer>());
					}
					
					crossReferences.get(first).put(generation, offset);
					*/
				}
				
				clearBuffer();
				i--;
				first++;
			}
			nextChar(true);
		}
	}
	
	/**
	 * Parses the trailer
	 * For PDF Version < 1.5 we support the "trailer" keyword. For others only startxref will be parsed.
	 * 
	 * @return					Integer of next 
	 * @throws ParserException
	 * @throws EOFException
	 */
	
	protected int parseTrailer() throws ParserException, EOFException {
		// <= PDF 1.5 (trailer keyword)
		if(buffer.charAt(0) == 't') {
			// t in buffer
			readWord("railer");
			clearBuffer();
			nextChar(true);
			crossReferenceDictionaries.add((PdfDictionary) parseObject());
		}
		// s in buffer
		readWord("tartxref");
		clearBuffer();
		nextChar(true);
		_readLine();
		int next = Integer.parseInt(buffer.toString());
		clearBuffer();
		readWord("%%EOF");
		clearBuffer();
		try {
			nextChar(true);
		} catch(EOFException e) {
			
		}
		
		return next;
	}
	
	protected void addCrossReference(int objectId, int generation, PdfReference ref) {
		HashMap<Integer,PdfReference> obj = crossReferences.get(objectId);
		if(obj == null) {
			obj = new HashMap<Integer, PdfReference>();
			crossReferences.put(objectId, obj);
		}
		
		obj.put(generation, ref);
	}
	
	/**
	 * @throws ParserException 
	 * @throws EOFException 
	 * @since PDFv1.5
	 */
	
	/*
	protected void parseObjectStream() throws EOFException, ParserException {
		PdfIndirectObject obj = parseIndirectObject();
		// N = number of compressed objects
		// Type = ObjStm
		// First = Byte Offset in decoded data of first object
		// Extends reference to another object stream
		// byte[] data = obj.getObj();
	}
	*/
}
