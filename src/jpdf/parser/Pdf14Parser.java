package jpdf.parser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jpdf.Document;
import jpdf.objects.PdfArray;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfIndirectObject;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.objects.PdfLiteralString;
import jpdf.objects.PdfStreamObject;

public class Pdf14Parser extends BaseParser implements Parser {
	public Pdf14Parser(BufferedStream stream) {
		super(stream);
	}
	
	/**
	 * Parser the stream and return the document
	 * 
	 * @return Document the parsed document or null
	 */

	public Document parse() throws ParserException {
		parseIndirectObjects();
		parseCrossReferenceTable();
		PdfDictionary dict = parseTrailer();
		while(!buffer.toString().equals("x")) {
			parseIndirectObject();
		}
		parseCrossReferenceTable();
		try {
			parseTrailer();
		} catch(IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		for(Entry<PdfNumber, Map<PdfNumber, PdfIndirectObject>> entry : indirectObjects.entrySet()) {
			for(PdfIndirectObject io : entry.getValue().values()) {
				PdfObject obj = io.getObj();
				if(obj instanceof PdfDictionary) {
					replaceIndirectReference((PdfDictionary) obj);
				}
			}
		}
		
		for(PdfStreamObject so : streamObjects) {
			replaceIndirectReference(so.getDictionary());
		}
		
		replaceIndirectReference(dict);
		
		PdfDictionary root = (PdfDictionary) dict.get("Root");
		System.out.println(root);
		PdfDictionary pages = (PdfDictionary) root.get("Pages");
		System.out.println(pages);
		// System.out.println(pages.get("Pages")); = null
		System.out.println(pages.get("Count"));
		System.out.println(pages.get("Type"));
		PdfArray kids = (PdfArray) pages.get("Kids");
		System.out.println(kids);
		for(PdfObject kid : kids) {
			if(kid instanceof PdfDictionary) {
				PdfDictionary kidDict = (PdfDictionary) kid;
				System.out.println(kidDict.get("Count"));
				System.out.println(kidDict.get("Type"));
				System.out.println(kidDict.get("Kids"));
			}
		}
		return new Document();
	}
	
	private void replaceIndirectReference(PdfDictionary dict) {
		for(Entry<String, PdfObject> entry : dict.entrySet()) {
			PdfObject obj = entry.getValue();
			if(obj instanceof PdfIndirectReference) {
				PdfIndirectReference ref = (PdfIndirectReference) obj;
				//System.out.println("Indirect Reference ... " + ref.getId() + " " + ref.getGeneration());
				Map<PdfNumber, PdfIndirectObject> generations = indirectObjects.get(ref.getId());
				if(generations == null) {
					System.err.println("Indirect Reference " + ref + " could not be resolved.");
				} else {
					dict.put(entry.getKey(), generations.get(ref.getGeneration()).getObj());
				}
			} else if(obj instanceof PdfDictionary) {
				replaceIndirectReference((PdfDictionary) obj);
			} else if(obj instanceof PdfArray) {
				replaceIndirectReference((PdfArray) obj);
			}
		}
	}
	
	private void replaceIndirectReference(PdfArray arr) {
		for(int i=0; i<arr.size();i++) {
			PdfObject obj = arr.get(i);
			if(obj instanceof PdfIndirectReference) {
				PdfIndirectReference ref = (PdfIndirectReference) obj;
				Map<PdfNumber, PdfIndirectObject> generations = indirectObjects.get(ref.getId());
				if(generations == null) {
					System.err.println("Indirect Reference " + ref + " could not be resolved.");
				} else {
					arr.set(i, generations.get(ref.getGeneration()).getObj());
				}
			} else if(obj instanceof PdfDictionary) {
				replaceIndirectReference((PdfDictionary) obj);
			} else if(obj instanceof PdfArray) {
				replaceIndirectReference((PdfArray) obj);
			}
		}
	}
	
	private PdfDictionary parseTrailer() throws ParserException {
		if(buffer.toString().equals("t")) {
			readWord("railer");
			clearBuffer();
			nextChar(true);
			PdfDictionary dict = (PdfDictionary) parseObject();
			// s in buffer
			readWord("tartxref");
			clearBuffer();
			nextChar(true);
			readLine();
			clearBuffer();
			readWord("%%EOF");
			clearBuffer();
			nextChar(true);
			return dict;
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
		if(buffer.toString().equals("x")) {
			readWord("ref");
			clearBuffer();
			nextChar(true);
			PdfNumber first = readNumber();
			PdfNumber count = readNumber();
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