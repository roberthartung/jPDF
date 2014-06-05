package jpdf.parser;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Map.Entry;

import jpdf.Document;
import jpdf.objects.PdfArray;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfIndirectObject;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.objects.PdfStreamObject;

public class Pdf14Parser extends EOFParser implements Parser {
	// RandomAccessFile file;
	public Pdf14Parser(RandomAccessFile file) {
		super(file);
		// this.file = file;
	}
	
	/**
	 * Parser the stream and return the document
	 * 
	 * @return Document the parsed document or null
	 */

	public Document parse() throws ParserException {
		return super.parse();
		
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

	private void parseIndirectObjects() throws ParserException, EOFException {
		// read next char into buffer
		nextChar(true);
		Character tmp;
		//while(((tmp = nextChar(true)) != null) && ((tmp >= '0' && tmp <= '9') || tmp == '%'))
		while((tmp = buffer.charAt(0)) != null && ((tmp >= '0' && tmp <= '9') || tmp == '%'))
		{
			parseIndirectObject();
		}
	}
/*
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
	*/
}