package jpdf.parser;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import jpdf.Document;
import jpdf.objects.PdfArray;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfIndirectObject;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.objects.PdfObjectStream;
import jpdf.objects.PdfStreamObject;
import jpdf.references.PdfByteOffsetReference;
import jpdf.references.PdfCompressedReference;
import jpdf.util.BufferedStream;

public class Pdf16Parser extends RandomAccessParser implements Parser {
	public Pdf16Parser(RandomAccessFile file) {
		super(file);
	}
	
	public void parse() throws ParserException, IOException {
		super.parse();
		
		/*
		// Get reference from crossRefDirectory for the Root Entry
		PdfIndirectReference ref = (PdfIndirectReference) crossReferenceDictionary.get("Root");
		// Turn ref to object
		PdfIndirectObject root = getIndirectObject(ref);
		// Get dict from object
		PdfDictionary rootDict = (PdfDictionary) root.getObj();
		PdfDictionary pages = (PdfDictionary) getObject((PdfIndirectReference) rootDict.get("Pages"));
		System.out.println("Pages: " + pages.get("Count"));
		*/
	}
	
	/**
	 * Parses the cross reference table. From PDF Version 1.5 onwards there also can be cross reference streams
	 * 
	 * @return						Byte Offset of next Cross Reference Table
	 * @throws ParserException		
	 * @throws IOException 
	 */
	
	protected int parseCrossReferenceTable() throws ParserException, IOException {
		// Backwards compatibility to versions < 1.5
		if(buffer.toString().equals("x")) {
			readWord("ref");
			clearBuffer();
			nextChar(true);
			parseCrossReferenceSections();
		} else {
			parseCrossReferenceStream();
		}
		
		parseTrailer();
		
		return 0;
	}
	
	private void parseCrossReferenceStream() throws ParserException, IOException {
		// parse until 's' from startxref
		while(buffer.charAt(0) != 's') {
			PdfIndirectObject io = parseIndirectObject();
			PdfObject obj = io.getObj();
			if(obj instanceof PdfStreamObject) {
				PdfStreamObject stream = (PdfStreamObject) obj;
				PdfDictionary dict = stream.getDictionary();
				crossReferenceDictionaries.add(dict);
				// TODO check for algorithm / compression type
				// W always 3 integers for PDF 1.6
				// Defining the field's sizes for each entry
				//PdfArray arr = (PdfArray) crossReferenceDictionary.get("W");
				byte[] data = stream.getData();
				ByteBuffer bb = ByteBuffer.wrap(data);
				
				int firstObjectNumber;
				if(dict.containsKey("Index")) {
					PdfArray index = (PdfArray) dict.get("Index");
					firstObjectNumber = ((PdfNumber)index.get(0)).intValue();
				} else {
					int length = stream.getDataLength();
					int size = ((PdfNumber)dict.get("Size")).intValue();
					// Todo Use sum of W array entries here!
					firstObjectNumber = size - length/5;
				}
				
				int num = firstObjectNumber;
				while(bb.hasRemaining()) {
					int type = (bb.get() & 0xFF);
					//System.out.print(num);
					//System.out.print(" " + type);
					if(type < 0 || type > 2)
						// TODO dont skip - use as ref to null object
						continue;
					
					int field1 = ((bb.get() & 0xFF) << 16) | ((bb.get() & 0xFF) << 8) | ((bb.get() & 0xFF));
					int field2 = (bb.get() & 0xFF);
					
					//System.out.print(" " + field1);
					//System.out.println(" " + field2);
					
					switch(type) {
					case 0 : // Free object
							// objectId = 
							// object number of next free object
							// generation number to use
						break;
					case 1 : // entry in use, not compressed
						// crossReferences.put(num);
						addCrossReference(num, field2, new PdfByteOffsetReference(field1));
							// byte offset from beginning of the file
							// generation number
						break;
					case 2 : // compressed object
						addCrossReference(num, 0, new PdfCompressedReference(field1, field2));
							// num = object number of the object stream this object is stored in 
							// field2 = offset 
						break;
					}
					num++;
				}
			} else {
				throw new ParserException("Expecting Stream object in cross reference section");
			}
		}
	}
}
