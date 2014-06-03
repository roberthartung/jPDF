package jpdf.parser;

import jpdf.Document;


public interface Parser {
	Document parse() throws ParserException;
}
