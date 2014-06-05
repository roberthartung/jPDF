package jpdf.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

import jpdf.Document;

public class AbstractParser {
	private RandomAccessFile file;
	
	public AbstractParser(File f) throws FileNotFoundException {
		file = new RandomAccessFile(f, "r");
	}
	
	public Document parse() throws ParserException {
		String version;
		try {
			version = file.readLine();
			
			switch(version) {
				case "%PDF-1.4" :
					Parser p = new Pdf14Parser(file);
					return p.parse();
			default :
					System.err.println("Unknown PDF Version: " + version);
					break;
			}
		} catch (IOException e) {
			System.out.println("IOException: " + e.getMessage());
		}
		
		return null;
	}
}
