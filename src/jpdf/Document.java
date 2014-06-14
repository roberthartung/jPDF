package jpdf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jpdf.objects.PdfArray;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfIndirectObject;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.objects.PdfStreamObject;
import jpdf.parser.ContentParser;
import jpdf.parser.Parser;
import jpdf.parser.ParserException;
import jpdf.parser.PdfIndirectReference;
import jpdf.util.BufferedStream;

/**
 * PDF Document representation
 * 
 * @author Robert Hartung
 *
 */

public class Document {
	
	/**
	 * The parser of this document
	 */
	
	private Parser parser;

	public Document(Parser p) {
		this.parser = p;
	}
	
	/**
	 * Access Point 
	 * 
	 * @param path						File path
	 * @return							Document instance
	 * @throws FileNotFoundException 	If file is not available
	 * @throws ParserException			If the is any parsing error
	 */
	
	public static Document fromFile(String path) throws FileNotFoundException, ParserException {
		AbstractParser p = new AbstractParser(path);
		return p.parse();
	}
	
	/**
	 * Number of pages
	 * 
	 * @return Number of pages
	 * @throws IOException 
	 * @throws ParserException 
	 */
	
	public int getPageCount() throws ParserException, IOException {
		PdfDictionary rootDict = parser.getRootDictionary();
		PdfObject obj = parser.getObject((PdfIndirectReference) rootDict.get("Pages"));
		PdfDictionary pagesDict;
		if(obj instanceof PdfIndirectObject) {
			pagesDict = (PdfDictionary) ((PdfIndirectObject) obj).getObj();
		} else {
			pagesDict = (PdfDictionary) obj;
		}
	
		PdfNumber count = (PdfNumber) pagesDict.get("Count");
		return count.intValue();
	}
	
	private Map<Integer,PdfObject> pages = new HashMap<>();
	
	private PdfDictionary getPagesDictionary() throws ParserException, IOException {
		PdfDictionary rootDict = parser.getRootDictionary();
		PdfObject obj = parser.getObject((PdfIndirectReference) rootDict.get("Pages"));
		PdfDictionary pagesDict;
		if(obj instanceof PdfIndirectObject) {
			pagesDict = (PdfDictionary) ((PdfIndirectObject) obj).getObj();
		} else {
			pagesDict = (PdfDictionary) obj;
		}
		return pagesDict;
	}
	
	private PdfDictionary getPage(int page) throws ParserException, IOException {
		PdfDictionary pagesDict = getPagesDictionary();
		return (PdfDictionary) getPage(pagesDict, page, 1);	
	}
	
	private PdfObject getPage(PdfDictionary dict, int page, int offset) throws ParserException, IOException {
		switch(dict.get("Type").toString()) {
		case "Pages" :
			PdfArray kids = (PdfArray) dict.get("Kids");
			for(PdfObject kid : kids) {
				if(kid instanceof PdfIndirectReference) {
					PdfObject kidObject = parser.getObject((PdfIndirectReference) kid);
					if(kidObject instanceof PdfIndirectObject) {
						PdfDictionary kidDict = (PdfDictionary) ((PdfIndirectObject) kidObject).getObj();
						
						switch(kidDict.get("Type").toString()) {
						case "Page" :
							if(offset == page) {
								return kidDict;
							}
							
								offset++;
							break;
						case "Pages": 
							int count = ((PdfNumber) kidDict.get("Count")).intValue();
							if(offset <= page && page <= offset+count) {
								return getPage(kidDict, page, offset);
							}
							
							offset += count;
							break;
						}
					}
				} else {
					throw new ParserException("Expecting PdfIndicrectReference");
				}
			}
			break;
		}
		
		return null;
	}
	
	public String getPageText(int page) throws ParserException, IOException {
		PdfDictionary pageDict = getPage(page);
		// B		= ArticleBeads
		// CropBox 	= ...
		// Contents = IndirectReference
		// Rotate	= 0
		// Parent	= ...
		// Resource = ...
		// MediaBox = ...
		PdfDictionary resourcesDict = (PdfDictionary) ((PdfIndirectObject) parser.getObject((PdfIndirectReference) pageDict.get("Resources"))).getObj();
		PdfDictionary fontsDict = (PdfDictionary) resourcesDict.get("Font");
		for(String fontName : fontsDict.keySet()) {
			PdfIndirectReference fontRef = (PdfIndirectReference) fontsDict.get(fontName);
			PdfDictionary fontDictionary = (PdfDictionary) ((PdfIndirectObject) parser.getObject(fontRef)).getObj();
			//System.out.println(fontName + " " + fontDictionary);
			PdfArray widths = (PdfArray) fontDictionary.get("Widths");
			PdfDictionary fontDescriptor = (PdfDictionary) ( (PdfIndirectObject) parser.getObject( (PdfIndirectReference) fontDictionary.get("FontDescriptor") ) ).getObj();
			//System.out.println(fontDescriptor);
			//System.out.println(fontDescriptor.get("Ascent") + " " + fontDescriptor.get("Descent"));
			//System.out.println(fontDictionary.get("BaseFont"));
			// System.out.println(fontDictionary.get("Flags"));
			double sum = 0;
			for(PdfObject width : widths) {
				if(width instanceof PdfNumber) {
					PdfNumber w = (PdfNumber) width;
					sum += w.doubleValue();
				}
			}
			double avg = sum / widths.size();
			// System.out.println(avg);
			// System.out.println(fontDictionary.get("Subtype") + " " + fontDictionary.get("Encoding"));
		}
		// System.out.println(((PdfIndirectObject) parser.getObject((PdfIndirectReference) ((PdfArray) pageDict.get("B")).get(0))).getObj());
		PdfIndirectReference pageRef = (PdfIndirectReference) pageDict.get("Contents");
		PdfIndirectObject pageObj = (PdfIndirectObject) parser.getObject(pageRef);
		byte[] data = ((PdfStreamObject) pageObj.getObj()).getData();
		//String res = new String(data);
		//System.out.println(res);
		ContentParser parser = new ContentParser(new BufferedStream(new ByteArrayInputStream(data)));
		parser.parse();
		System.out.println("\n");
		return "";
	}
}