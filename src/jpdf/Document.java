package jpdf;

import java.io.File;
import java.io.FileNotFoundException;

import jpdf.parser.AbstractParser;
import jpdf.parser.Parser;
import jpdf.parser.ParserException;

public class Document {
	private Parser parser;

	public Document(Parser p) {
		this.parser = p;
	}
	
	public static Document fromFile(String file) throws FileNotFoundException, ParserException {
		AbstractParser p = new AbstractParser(new File(file));
		return p.parse();
	}
	
	public int getPageCount() {
		return 0;
	}
}