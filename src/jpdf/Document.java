package jpdf;

import java.io.File;
import java.io.FileNotFoundException;

import jpdf.parser.Parser;
import jpdf.parser.ParserException;

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
	 */
	
	public int getPageCount() {
		return 0;
	}
}