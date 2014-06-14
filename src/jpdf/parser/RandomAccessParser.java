package jpdf.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
//import java.util.Map;


import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.crypto.NullCipher;
import javax.print.attribute.standard.Compression;

import jpdf.Document;
import jpdf.decode.Predictor;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfIndirectObject;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.objects.PdfObjectStream;
import jpdf.objects.PdfStreamObject;
import jpdf.references.PdfByteOffsetReference;
import jpdf.references.PdfCompressedReference;
import jpdf.references.PdfReference;
import jpdf.util.BufferedStream;

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
	
	abstract protected int parseCrossReferenceTable() throws ParserException, EOFException, IOException;
	
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
	 * @throws IOException 
	 */
	
	@Override
	public void parse() throws ParserException, IOException {
		try {
			// Offset always points to next character offset (in bytes) to read.
			offset = file.length()-1;
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
				if(crossReferenceDictionaries.size() == 1 && !crossReferenceDictionaries.get(0).containsKey("Prev"))
					break;
			}
			
			// TODO: Find Last Cross-Reference Table and parse it
			// hint: Use all previous dictionaries, find Max(Prev)
			
			if(crossReferenceDictionaries.get(0).containsKey("Prev")) {
				offset = ((PdfNumber) crossReferenceDictionaries.get(0).get("Prev")).intValue();
				file.seek(offset);
				clearBuffer();
				nextChar();
				parseCrossReferenceTable();
			}
			
			/*
			offset = Integer.parseInt(last.toString());
			file.seek(offset);
			clearBuffer();
			nextChar();
			objectsMap.putAll(parseCrossReferenceTable());
			TrailerResult result = null;
			result = parseTrailer();
			*/
			for(PdfDictionary dict : crossReferenceDictionaries) {
				if(dict.containsKey("Root")) {
					PdfIndirectReference ref = (PdfIndirectReference) dict.get("Root");
					// System.out.println(crossReferenceDictionary);
					// Turn ref to object
					PdfIndirectObject root = getIndirectObject(ref);
					// Get dict from object
					rootDictionary = (PdfDictionary) root.getObj();
					break;
				}
			}
			
			/*
			 // Get reference from crossRefDirectory for the Root Entry
		
			 */
			/*
			if(crossReferenceDictionary != null) {
				// System.out.println(crossReferenceDictionary);
				super.crossReferenceDictionary = crossReferenceDictionary;
			} else {
				System.err.println("Root not found.");
			}
			*/
			
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
	
	public PdfIndirectObject getIndirectObject(PdfIndirectReference ref) throws ParserException, IOException {
		return getIndirectObject(Integer.parseInt(ref.getId().toString()), Integer.parseInt(ref.getGeneration().toString()));
	}
	
	public PdfIndirectObject getIndirectObject(Integer id, Integer generation) throws ParserException, IOException {
		int offset;
		try {
			offset = objectsMap.get(id).get(generation);
		} catch(NullPointerException e) {
			try {
				PdfReference ref = crossReferences.get(id).get(generation);
				if(ref instanceof PdfByteOffsetReference) {
					offset = ((PdfByteOffsetReference) ref).getByteOffset();
				} else if(ref instanceof PdfCompressedReference) {
					/*PdfCompressedReference compressedReference = (PdfCompressedReference) ref;
					return getIndirectObject(compressedReference.getContainingObjectId(), 0);
					*/
					throw new ParserException("Needed?");
				} else {
					throw new ParserException("No reference found for " + id);
				}
			} catch(NullPointerException ee) {
				return null;
			}
		}
		
		file.seek(offset);
		clearBuffer();
		nextChar();
		return parseIndirectObject();
	}
	
	public PdfObject getObject(PdfIndirectReference ref) throws ParserException, IOException {
		return getObject(Integer.parseInt(ref.getId().toString()), Integer.parseInt(ref.getGeneration().toString()));
	}
	
	public PdfObject getObject(Integer id, Integer generation) throws ParserException, IOException {
		int offset;
		
		try {
			PdfReference ref = crossReferences.get(id).get(generation);
			if(ref instanceof PdfByteOffsetReference) {
				offset = ((PdfByteOffsetReference) ref).getByteOffset();
				//System.out.println("ByteOffset: " + offset);
			} else if(ref instanceof PdfCompressedReference) {
				PdfCompressedReference compressedReference = (PdfCompressedReference) ref;
				PdfIndirectObject obj = getIndirectObject(compressedReference.getContainingObjectId(), 0);
				PdfObjectStream objectStream = (PdfObjectStream) obj.getObj();
				return objectStream.getObjects().get(id);
			} else {
				throw new ParserException("No reference found for " + id);
			}
		} catch(NullPointerException ee) {
			return null;
		}
		
		try {
			offset = objectsMap.get(id).get(generation);
		} catch(NullPointerException e) {
			
		}
		
		file.seek(offset);
		clearBuffer();
		nextChar();
		PdfIndirectObject io = parseIndirectObject();
		return io;
	}
	
	/**
	 * Overrides for BaseParser functions
	 */

	@Override
	protected int read() throws IOException {
		offset++;
		return file.read();
	}

	@Override
	protected int read(byte[] bytes, int offset, int i) throws IOException {
		int num = file.read(bytes, offset, i);
		this.offset += num;
		return num;
	}

	@Override
	protected String readLine() throws IOException {
		String line = file.readLine();
		this.offset = file.getFilePointer();
		return line;
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
	
	protected PdfIndirectObject parseIndirectObjectAt(long byteOffset) throws IOException, ParserException {
		file.seek(byteOffset);
		return parseIndirectObject();
	}
	
	public PdfDictionary getRootDictionary() throws ParserException, IOException {
		return rootDictionary;
	}
	
	protected PdfIndirectObject parseIndirectObject() throws ParserException, IOException {
		skipComments();
		PdfNumber num1 = readNumber();
		PdfNumber num2 = readNumber();
		
		debug("indirectObject: " + num1 + "." + num2);
		
		boolean hasKeywords = false;
		
		if(buffer.toString().equals("o")) {
			debug("'obj' keyword found");
			readWord("bj");
			clearBuffer();
			hasKeywords = true;
			nextChar(true);
		}
		
		PdfObject obj = parseObject();
		
		if(buffer.toString().equals("s")) {
			debug("stream object at " + file.getFilePointer());
			_readLine();
			
			if(!(obj instanceof PdfDictionary)) {
				throw new ParserException("Expecting the object before a stream to be a dictionary.");
			}
			
			clearBuffer();
			PdfDictionary dict = (PdfDictionary) obj;
			PdfObject lengthObject = dict.get("Length");
			if(lengthObject instanceof PdfIndirectReference) {
				long savedOffset = file.getFilePointer();
				lengthObject = ((PdfIndirectObject) getObject((PdfIndirectReference) lengthObject)).getObj(); // Resolve indirect reference
				clearBuffer();
				file.seek(savedOffset);
			}
			
			int length;
			if(lengthObject instanceof PdfNumber) {
				length = ((PdfNumber) lengthObject).intValue();
			}  else {
				throw new ParserException("Unable to read Length from Stream");
			}
			
			// int length = Integer.parseInt(dict.get("Length").toString());
			byte[] data = readNBytes(length);
			byte[] result = null;
			if(dict.containsKey("Filter")) {
				debug("FilterType: " + dict.get("Filter"));
				
				if(dict.get("Filter").equals("FlateDecode")) {
					Inflater inflater = new Inflater();
					inflater.setInput(data);
					
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					try {
						byte[] tmp = new byte[1024];
						while(!inflater.finished()) {
							int resultLength = inflater.inflate(tmp);
							buffer.write(tmp, 0, resultLength);
						}
						inflater.end();
						result = buffer.toByteArray();
						ByteBuffer outBytes = ByteBuffer.wrap(result);
						
						if(dict.containsKey("DecodeParms")) {
							PdfDictionary params = (PdfDictionary) dict.get("DecodeParms");
							if(params.containsKey("Predictor")) {
								Predictor predictor = Predictor.getPredictor(params);
					            if (predictor != null) {
					                result = predictor.unpredict(outBytes).array();
					            }
							}
							
						}
					} catch (DataFormatException e) {
						e.printStackTrace();
						throw new ParserException("Unable to decode data for " + dict + " " + e.getMessage());
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if(dict.get("Filter").equals("DCTDecode")) {
					
				} else {
					throw new ParserException("Unsupported filter. " + dict);
				}
				
				PdfStreamObject stream = new PdfStreamObject(dict, result);
				// streamObjects.add(stream);
				obj = stream;
			} else {
				// System.out.println(dict);
			}
			
			if(dict.containsKey("Type") && dict.get("Type").equals("ObjStm")) {
				ObjectStreamParser objectStreamParser = new ObjectStreamParser(new BufferedStream(new ByteArrayInputStream(result)), dict);
				try {
					objectStreamParser.parse();
				} catch(EOFException e) {
					
				} catch(IOException e) {
					throw new ParserException("Unable to parse Content Stream");
				}
				
				obj = objectStreamParser.getObjectStream();
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
}
