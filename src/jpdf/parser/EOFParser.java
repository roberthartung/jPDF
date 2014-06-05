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
 * Parses the PDF file from its end.
 * 
 * @author Robert Hartung
 */

public class EOFParser extends BaseParser implements Parser {
	private Map<Integer,Map<Integer, Integer>> objectsMap;
	
	protected RandomAccessFile file;
	
	private long offset;
	
	public EOFParser(RandomAccessFile file) {
		super();
		this.file = file;
		objectsMap = new HashMap<>();
	}
	
	private void _readUntil(char c) throws IOException {
		_readUntil(c, false);
	}
	
	private Character _prevChar() throws IOException {
		int b = file.read();
		if(b == -1)
			throw new EOFException();
		
		offset--;
		file.seek(offset);
		
		return (char) b;
	}
	
	private Character prevChar() throws IOException {
		return prevChar(false);
	}
	
	private Character prevChar(boolean ignoreWhitespaceCharacters) throws IOException {
		Character tmp;
		do
		{
			tmp = _prevChar();
		} while((ignoreWhitespaceCharacters && (tmp == '\r' || tmp == '\n' || tmp == ' ')));
		buffer.insert(0, tmp);
		return tmp;
	}
	
	private String _readUntil(char c, boolean ignoreWhitespace) throws IOException {
		Character chr;
		do {
			chr = prevChar(ignoreWhitespace);
		} while(chr != c);
		
		return clearBuffer();
	}
	
	private String previousLine() throws IOException {
		Character chr;
		while((chr = _prevChar()) != null && chr != '\n' && chr != '\r') {
			buffer.insert(0, chr);
		}
		
		String b = clearBuffer();
		prevChar(true);
		return b;
	}
	
	@Override
	public Document parse() throws ParserException {
		try {
			Document doc = new Document(this);
			// Offset always points to next character offset (in bytes) to read.
			this.offset = file.length()-1;
			file.seek(offset);
			_readUntil('%', true);
			_prevChar(); // '%' of '%% EOF'
			prevChar(true); // Read next none space character.
			int lastXref = Integer.parseInt(previousLine());
			LinkedList<PdfDictionary> dicts = new LinkedList<>();
			PdfObject last = null;
			PdfObject root = null;
			while(lastXref != 0) {
				offset = lastXref;
				file.seek(offset);
				clearBuffer();
				nextChar();
				
				objectsMap.putAll(parseCrossReferenceTable());
				TrailerResult result = parseTrailer();
				PdfDictionary dict = result.getDictionary();
				if(dict.containsKey("Prev") && last == null) {
					last = dict.get("Prev");
				}
				
				if(dict.containsKey("Root")) {
					if(root != null) {
						throw new ParserException("More than one Root entry in Trailer found.");
					}
					root = dict.get("Root");
				}
				
				lastXref = result.getNext();
			}
			
			if(last != null) {
				offset = Integer.parseInt(last.toString());
				file.seek(offset);
				clearBuffer();
				nextChar();
				objectsMap.putAll(parseCrossReferenceTable());
				TrailerResult result = null;
				result = parseTrailer();
			}
			
			if(root != null && root instanceof PdfIndirectReference) {
				PdfIndirectObject o = getIndirectObject((PdfIndirectReference) root);
				System.out.println(o.getObj());
			} else {
				System.err.println("Root not found.");
			}
			
			return doc;
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
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
}
