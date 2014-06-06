package jpdf.parser;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.LinkedList;
//import java.util.Map;


import java.util.Map;
import java.util.Map.Entry;

import jpdf.Document;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfIndirectObject;
import jpdf.objects.PdfObject;

/**
 * Parses the PDF file from its end. Extends the base parser for functionality to read from the end
 * 
 * @author Robert Hartung
 */

abstract public class RandomAccessParser extends BaseParser implements Parser {
	private Map<Integer,Map<Integer, Integer>> objectsMap;
	
	protected RandomAccessFile file;
	
	private long offset = 0;
	
	// abstract protected TrailerResult parseTrailer() throws ParserException, EOFException;
	
	abstract protected int parseCrossReferenceTable() throws ParserException, EOFException;
	
	/**
	 * @param file
	 */
	
	public RandomAccessParser(RandomAccessFile file) {
		super();
		
		this.file = file;
		objectsMap = new HashMap<>();
	}
	
	/**
	 * Parses the Document. But does not parse the whole document but parses
	 */
	
	@Override
	public void parse() throws ParserException {
		try {
			// Offset always points to next character offset (in bytes) to read.
			this.offset = file.length()-1;
			file.seek(offset);
			// Read until second '%' of '%% EOF'
			prevUntil('%', true);
			// first '%' of '%% EOF'
			_prevChar();
			// Read next none space character.
			prevChar(true);
			int lastXref = Integer.parseInt(previousLine());
			// Parse all cross reference tables until we reach the last one
			while(lastXref != 0) {
				offset = lastXref;
				file.seek(offset);
				clearBuffer();
				nextChar();
				lastXref = parseCrossReferenceTable();
			}
			
			// TODO: Find Last Cross-Reference Table and parse it
			// hint: Use all previous dictionaries, find Max(Prev)
			/*
			offset = Integer.parseInt(last.toString());
			file.seek(offset);
			clearBuffer();
			nextChar();
			objectsMap.putAll(parseCrossReferenceTable());
			TrailerResult result = null;
			result = parseTrailer();
			*/
			
			if(crossReferenceDictionary != null) {
				System.out.println(crossReferenceDictionary);
			} else {
				System.err.println("Root not found.");
			}
			
			//return doc;
		} catch (IOException ex) {
			ex.printStackTrace();
			//return null;
		}
	}
	
	/**
	 * Functions to parse from the end
	 * 
	 * @param c				Character
	 * @throws IOException	Thrown if there is an exception
	 */
	
	/*
	private void _readUntil(char c) throws IOException {
		_readUntil(c, false);
	}
	*/
	
	/**
	 * Reads the previous char by decrementing the offset and reading the next character
	 * 
	 * @return				Previous character
	 * @throws IOException	If there are no more characters to read 
	 */
	
	private Character _prevChar() throws IOException {
		int b = file.read();
		if(b == -1)
			throw new EOFException();
		
		offset--;
		file.seek(offset);
		
		return (char) b;
	}
	
	/*
	private Character prevChar() throws IOException {
		return prevChar(false);
	}
	*/
	
	/**
	 * Returns the previous char but ignores whitespaces optionally and writes the character into the buffer
	 * 
	 * @param ignoreWhitespaceCharacters	Should whitespaces be ignored?
	 * @return								Previous character
	 * @throws IOException					If no more characters are available.
	 */
	
	private Character prevChar(boolean ignoreWhitespaceCharacters) throws IOException {
		Character tmp;
		do
		{
			tmp = _prevChar();
		} while((ignoreWhitespaceCharacters && (tmp == '\r' || tmp == '\n' || tmp == ' ')));
		buffer.insert(0, tmp);
		return tmp;
	}
	
	/**
	 * Reads the previous char until the specified character appears. Optionally ignores whitespaces 
	 * 
	 * @param c					The character to read to
	 * @param ignoreWhitespace	Ignore whitespaces
	 * @return					Returns the string until the stop-chracter
	 * @throws IOException		If there are no more characters.
	 */
	
	private String prevUntil(char c, boolean ignoreWhitespace) throws IOException {
		Character chr;
		do {
			chr = prevChar(ignoreWhitespace);
		} while(chr != c);
		
		return clearBuffer();
	}
	
	/**
	 * Reads the previous line
	 * 
	 * @return
	 * @throws IOException
	 */
	
	private String previousLine() throws IOException {
		Character chr;
		while((chr = _prevChar()) != null && chr != '\n' && chr != '\r') {
			buffer.insert(0, chr);
		}
		
		String b = clearBuffer();
		prevChar(true);
		return b;
	}
	
	public PdfIndirectObject getIndirectObject(PdfIndirectReference ref) throws NumberFormatException, ParserException {
		return getIndirectObject(Integer.parseInt(ref.getId().toString()), Integer.parseInt(ref.getGeneration().toString()));
	}
	
	public PdfIndirectObject getIndirectObject(Integer id, Integer generation) throws ParserException {
		try {
			int offset = objectsMap.get(id).get(generation);
			file.seek(offset);
			clearBuffer();
			nextChar();
			return parseIndirectObject();
		} catch(NullPointerException e) {
			
		} catch(IOException e) {
			
		}
		
		return null;
	}
	
	/**
	 * Overrides for BaseParser functions
	 */

	@Override
	protected int read() throws IOException {
		return file.read();
	}

	@Override
	protected int read(byte[] bytes, int offset, int i) throws IOException {
		return file.read(bytes, offset, i);
	}

	@Override
	protected String readLine() throws IOException {
		return file.readLine();
	}
	
	/*
	protected static class TrailerResult {
		private PdfDictionary dict;
		private int next;

		TrailerResult(PdfDictionary dict, int next) {
			this.dict = dict;
			this.next = next;
		}
		
		public PdfDictionary getDictionary() {
			return dict;
		}
		
		public int getNext() {
			return next;
		}
	}
	*/
}
