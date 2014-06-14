package jpdf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import jpdf.parser.Parser;
import jpdf.parser.ParserException;
import jpdf.parser.Pdf14Parser;
import jpdf.parser.Pdf16Parser;

/**
 * The AbstractParser will initialize the file and initialize the correct parser for the correct version
 * 
 * @author Robert Hartung
 */

public class AbstractParser {
	/**
	 * Random access file to access random parts of the file
	 */
	
	private RandomAccessFile file;
	
	/**
	 * Constructor
	 * 
	 * @param path						File Path
	 * @throws FileNotFoundException	Thrown if file is not available
	 */
	
	public AbstractParser(String path) throws FileNotFoundException {
		file = new RandomAccessFile(new File(path), "r");
	}
	
	/**
	 * Parses the document
	 * 
	 * @return					Document instance
	 * @throws ParserException	Parser Exception if any error occurs
	 */
	
	public Document parse() throws ParserException {
		String version;
		Document doc = null;
		try {
			version = file.readLine();
			Parser p = null;
			
			switch(version) {
				case "%PDF-1.4" :
					p = new Pdf14Parser(file);
				break;
				case "%PDF-1.3" :
					p = new Pdf14Parser(file);
				break;
				case "%PDF-1.6" :
					p = new Pdf16Parser(file);
				break;
				default :
					throw new ParserException("Unknown PDF Version: " + version);
			}
			
			doc = new Document(p);
			p.parse();
		} catch (IOException e) {
			throw new ParserException("IOException: " + e.getMessage());
		}
		
		return doc;
	}
}
