jPDF
====

jPDF is a JAVA based PDF parser, reader and text extractor.

jPDF parses the whole PDF using a JAVA based Class that returns a document with all needed references.

Usage
=====

public class MyApp {

	public static void main(String[] args) throws FileNotFoundException, ParserException {
		Document doc = Document.fromFile("some.pdf");
		// Further processing
	}
}
