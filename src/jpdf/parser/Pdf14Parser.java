package jpdf.parser;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import jpdf.Document;
import jpdf.objects.PdfArray;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfIndirectObject;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.objects.PdfStreamObject;

public class Pdf14Parser extends RandomAccessParser implements Parser {
	public Pdf14Parser(RandomAccessFile file) {
		super(file);
	}
	
	/**
	 * Parser the stream and return the document
	 * 
	 * @return Document the parsed document or null
	 * @throws IOException 
	 */
	
	public void parse() throws ParserException, IOException {
		super.parse();
		
		/*
		PdfDictionary dict = null;
		try {
			parseIndirectObjects();
			
			parseCrossReferenceTable();
			dict = parseTrailer();
			while(!buffer.toString().equals("x")) {
				parseIndirectObject();
			}
			parseCrossReferenceTable();
			try {
				parseTrailer();
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
			}
		} catch (EOFException e) {
			System.out.println("EOF of PDF reached.");
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
		parsePageTree((PdfDictionary) root.get("Pages"));
		
		return new Document();
		*/
	}
	
	/*
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
	*/
	
	/**
	 * Parse Cross Reference Table for PDF Version <= 1.4
	 * The is the xref keyword, Followed by a number of sections and the trailer
	 */
	
	protected int parseCrossReferenceTable() throws ParserException, EOFException {
		if(buffer.charAt(0) == 'x') {
			readWord("ref");
			clearBuffer();
			nextChar(true);
			parseCrossReferenceSections();
			
			return parseTrailer();
		} else {
			throw new ParserException("Expecting keyword 'xref'. Found: '"+buffer+"'");
		}
	}
}