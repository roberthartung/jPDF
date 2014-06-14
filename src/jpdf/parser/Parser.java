package jpdf.parser;

import java.io.IOException;

import jpdf.Document;
import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfObject;

public interface Parser {
	void parse() throws ParserException, IOException;
	public PdfDictionary getRootDictionary() throws ParserException, IOException;
	public PdfObject getObject(PdfIndirectReference ref) throws ParserException, IOException;
}
