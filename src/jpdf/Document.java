package jpdf;

import java.io.File;
import java.io.FileNotFoundException;

import jpdf.parser.AbstractParser;
import jpdf.parser.ParserException;

public class Document {
	public static Document fromFile(String file) throws FileNotFoundException {
		AbstractParser p = new AbstractParser(new File(file));
		try {
			return p.parse();
		} catch(ParserException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
}