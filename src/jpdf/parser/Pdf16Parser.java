package jpdf.parser;

import java.io.EOFException;
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
import jpdf.objects.PdfStreamObject;

public class Pdf16Parser extends RandomAccessParser implements Parser {
	public Pdf16Parser(RandomAccessFile file) {
		super(file);
	}
	
	public void parse() throws ParserException {
		super.parse();
	}
	
	/**
	 * Parses the cross reference table. From PDF Version 1.5 onwards there also can be cross reference streams
	 * 
	 * @return						Byte Offset of next Cross Reference Table
	 * @throws ParserException		
	 * @throws EOFException
	 */
	
	protected int parseCrossReferenceTable() throws ParserException, EOFException {
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
	
	private void parseCrossReferenceStream() throws EOFException, ParserException {
		// parse until 's' from startxref
		while(buffer.charAt(0) != 's') {
			PdfIndirectObject io = parseIndirectObject();
			PdfObject obj = io.getObj();
			if(obj instanceof PdfStreamObject) {
				PdfStreamObject stream = (PdfStreamObject) obj;
				crossReferenceDictionary = stream.getDictionary();
				// W always 3 integers
				// Defining the field's sizes for each entry
				PdfArray arr = (PdfArray) crossReferenceDictionary.get("W");
				// System.out.println(arr);
				byte[] data = stream.getData();
				ByteBuffer bb = ByteBuffer.wrap(data);
				System.out.println(data.length);
				
				while(bb.hasRemaining()) {
					int type = (bb.get() & 0xFF);
					System.out.print(type);
					if(type < 0 || type > 2)
						continue;
					int value = ((bb.get() & 0xFF) << 16) | ((bb.get() & 0xFF) << 8) | ((bb.get() & 0xFF));
					System.out.print(" " + value);
					int generation = (bb.get() & 0xFF);
					System.out.println(" " + generation);
				}
				/*
				int offset = 0;
				
				int lengths[] = new int[arr.size()];
				for(int i=0; i<arr.size();i++) {
					lengths[i] = Integer.parseInt(((PdfNumber) arr.get(0)).toString() );
				}
				
				int[] fields = new int[lengths.length];
				*/
				
				/*
				while(offset < data.length) {
					// Loop through fields
					
					for(int f=0; f<lengths.length; f++) {
						fields[f] = 0;
						for(int o=0;o<lengths[f];o++) {
							fields[f] <<= 8;
							fields[f] |= (int) data[offset++] & 0xff;
						}
					}
					
					System.out.println(fields[0] + " | " + fields[1] + " | " + fields[2]);
					
					/*
					switch(data[0]) {
						case 0:
							System.out.println();
							break;
						case 1:
							System.out.println("Ok");
							break;
						case 2 :
							System.out.println("Ok");
							break;
						default :
							System.err.println("err");
						break;
					}
					
					/*
					 
					
					System.out.println(data[offset++]);
					 
				}
			*/
				/*
				
				try {
					String res = new String(data, 0, data.length, "UTF-8");
					System.out.println(res);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				*/
				/*
				ContentParser parser = new ContentParser(new BufferedStream(new ByteArrayInputStream(data)));
				parser.parse();
				*/
			} else {
				throw new ParserException("Expecting Stream object in cross reference section");
			}
		}
	}
}
