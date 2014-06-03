package jpdf.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import jpdf.Document;

public class AbstractParser {
	// private BufferedReader bufferedReader;
	
	// private FileInputStream fileInputStream;
	
	private BufferedStream stream;
	
	public AbstractParser(File file) throws FileNotFoundException {
		//FileReader fileReader = new FileReader(file);
		stream = new BufferedStream(new FileInputStream(file));
		//bufferedReader = new BufferedReader(fileReader);
	}
	
	public Document parse() throws ParserException {
		String version;
		try {
			//version = bufferedReader.readLine();
			version = stream.readLine();
			
			switch(version) {
				case "%PDF-1.4" :
					Parser p = new Pdf14Parser(stream);
					return p.parse();
			default :
					System.err.println("Unknown PDF Version: " + version);
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
