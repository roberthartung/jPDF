package jpdf.parser;

import java.io.BufferedReader;
import java.io.FileInputStream;

import jpdf.Document;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.objects.PdfLiteralString;

public class Pdf14Parser extends BaseParser implements Parser {
	/*
	public Pdf14Parser(BufferedReader bufferedReader, FileInputStream fileInputStream) {
		super(bufferedReader, fileInputStream);
	}
	*/
	public Pdf14Parser(BufferedStream stream) {
		super(stream);
	}

	/**
	 16141 0 obj
<</MarkInfo<</Marked true>>/Metadata 15147 0 R/Names 16144 0 R/OpenAction[16145 0 R/XYZ null null null]/Outlines 17308 0 R/PageLabels 15016 0 R/PageMode/UseOutlines/Pages 15018 0 R/StructTreeRoot 15149 0 R/Threads 16142 0 R/Type/Catalog>>
endobj
	 * @return 
	 */
	
	public Document parse() throws ParserException {
		parseIndirectObjects();
		parseCrossReferenceTable();
		parseTrailer();
		// Read until "x"(ref)
		while(!buffer.toString().equals("x")) {
			System.out.println("obj:" + parseIndirectObject());
		}
		parseCrossReferenceTable();
		try {
			parseTrailer();
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
		}
		return new Document();
	}
	
	private void parseTrailer() throws ParserException {
		if(buffer.toString().equals("t")) {
			readWord("railer");
			clearBuffer();
			nextChar(true);
			System.out.println("Trailer...");
			PdfDictionary dict = (PdfDictionary) parseObject();
			// s in buffer
			readWord("tartxref");
			clearBuffer();
			nextChar(true);
			readLine();
			clearBuffer();
			//nextChar(true);
			//System.out.println(buffer);
			readWord("%%EOF");
			clearBuffer();
			nextChar(true);
		} else {
			throw new ParserException("Expecting 'trailer' keyword. found: '"+buffer+"'");
		}
	}

	private void parseIndirectObjects() throws ParserException {
		// read next char into buffer
		nextChar(true);
		Character tmp;
		//while(((tmp = nextChar(true)) != null) && ((tmp >= '0' && tmp <= '9') || tmp == '%'))
		while((tmp = buffer.charAt(0)) != null && ((tmp >= '0' && tmp <= '9') || tmp == '%'))
		{
			parseIndirectObject();
		}
	}
	
	private void parseCrossReferenceTable() throws ParserException {
		System.out.println("Buffer: " + buffer);
		if(buffer.toString().equals("x")) {
			readWord("ref");
			clearBuffer();
			nextChar(true);
			PdfNumber first = readNumber();
			PdfNumber count = readNumber();
			System.out.println(first + " " + count);
			
			int i = Integer.parseInt(count.toString());
			
			while(i > 0) {
				readLine();
				//System.out.println("buffer:" + buffer);
				clearBuffer();
				i--;
			}
			nextChar(true);
		} else {
			throw new ParserException("Expecting keyword 'xref'. Found: '"+buffer+"'");
		}
	}
}