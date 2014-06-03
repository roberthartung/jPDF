package jpdf.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import jpdf.objects.PdfArray;
import jpdf.objects.PdfKeyword;
import jpdf.objects.PdfLiteralString;
import jpdf.objects.PdfObject;

public class ContentParser extends BaseParser {
	private enum STATES {PAGE_DESCRIPTION_LEVEL, TEXT_OBJECT, SHADING_OBJECT, EXTERNAL_OBJECT, INLINE_IMAGE_OBJECT, CLIPPING_PATH_OBJECT, PATH_OBJECT};
	
	private STATES state = STATES.PAGE_DESCRIPTION_LEVEL;
	
	private LinkedList<PdfObject> parameterBuffer = new LinkedList<>();
	
	public ContentParser(BufferedStream stream) {
		super(stream);
	}
	
	private void add(PdfObject o) {
		parameterBuffer.add(o);
	}
	
	private PdfObject[] clear() {
		PdfObject[] objects = new PdfObject[parameterBuffer.size()];
		parameterBuffer.toArray(objects);
		parameterBuffer.clear();
		return objects;
	}

	public void parse() throws ParserException {
		nextChar(true);
		
		// Check for Artifact
		if(buffer.toString().equals("/")) {
			System.out.println(parseObject());
			if(buffer.toString().equals("<")) {
				System.out.println(parseObject());
			}
		}
		
		while(true) {
			Object token;
			switch(state) {
			case PAGE_DESCRIPTION_LEVEL :

				token = readOneOf(
							"MP", "DP", "BMC", "BDC", "EMC", // Marked-content
							"q", "Q", "cm", // Special graphics state
							"w", "J", "j", "M", "d", "ri", "i", "gs", // General graphics state
							"CS", "cs", "SC", "SCN", "sc", "scn", "G", "g", "RG", "rg", "K", "k", // Color
							"Tc", "Tw", "Tz", "TL", "Tf", "Tr", "Ts",//Text state
							"BT", "sh", "Do", "BI", "m", "re" // State transitions
						);
				
				if(token instanceof PdfKeyword) {
					switch(((PdfKeyword) token).getKeyword()) {
						case "BDC" :
							while(buffer.charAt(0) == '/') {
								System.out.println(parseObject(true));
								System.out.println(parseObject(true));
							}
						break;
						case "BT" :
								state = STATES.TEXT_OBJECT;
							break;
							default :
								throw new ParserException("Unimplemented token: '"+token+"'");
					}
				} else {
					switch((String) token) {
							default :
								throw new ParserException("Unimplemented token: '"+token+"'");
					}
				}
					
				break;
			case CLIPPING_PATH_OBJECT:
				if(true)
					throw new ParserException("CLIPPING_PATH_OBJECT: unsupported");
				break;
			case EXTERNAL_OBJECT:
				if(true)
					throw new ParserException("EXTERNAL_OBJECT: unsupported");
				break;
			case INLINE_IMAGE_OBJECT:
				if(true)
					throw new ParserException("INLINE_IMAGE_OBJECT: unsupported");
				break;
			case PATH_OBJECT:
				if(true)
					throw new ParserException("PATH_OBJECT: unsupported");
				break;
			case SHADING_OBJECT:
				if(true)
					throw new ParserException("SHADING_OBJECT: unsupported");
				break;
			case TEXT_OBJECT:
				token = readOneOf(
						"w", "J", "j", "M", "d", "ri", "i", "gs", // General graphics state
						"CS", "cs", "SC", "SCN", "sc", "scn", "G", "g", "RG", "rg", "K", "k", // Color
						"Tc", "Tw", "Tz", "TL", "Tf", "Tr", "Ts", // Text state
						"Tj", "TJ", "'", "\"", // Text showing
						"Td", "TD", "Tm", "T*", // Text-positioning
						"MP", "DP", "BMC", "BDC", "EMC", // Marked-content
						"ET" // State transitions
				);
				
				if(token instanceof PdfKeyword) {
					switch(((PdfKeyword) token).getKeyword()) {
						case "ET" :
							state = STATES.PAGE_DESCRIPTION_LEVEL;
						break;
						case "Tj" :
						case "TJ" :
							for(PdfObject obj : clear()) {
								if(obj instanceof PdfLiteralString) {
									PdfLiteralString str = (PdfLiteralString) obj;
									System.out.print(str);
								} else if(obj instanceof PdfArray) {
									for(PdfObject inner : (PdfArray) obj) {
										if(inner instanceof PdfLiteralString) {
											PdfLiteralString str = (PdfLiteralString) inner;
											System.out.print(str);
										}
									}
								}
							}
							break;
						case "BT" :
								state = STATES.TEXT_OBJECT;
							break;
							default :
								PdfObject[] o11 = clear();
								break;
					}
				} else {
					switch((String) token) {
						case "/" :
							add(parseObject());
							break;
						case "[" :
							add(parseObject());
						break;
						default :
							add(parseObject());
						break;
					}
				}
				break;
			}
		}
	}

	private Object readOneOf(String ...tokens) throws IndexOutOfBoundsException {
		ArrayList<String> possible = new ArrayList<>();
		for(String s : tokens) {
			if(s.charAt(0) == buffer.charAt(0)) {
				possible.add(s);
			}
		}
		
		int index = 1;
		try {
			while(possible.size() > 1 || possible.get(0).length() > index) {
				nextChar();
				String b = buffer.toString();
				Iterator<String> it = possible.iterator();
				while(it.hasNext()) {
					String s = it.next();
					if(s.charAt(index) != b.charAt(index)) {
						it.remove();
					}
				}
				index++;
			}
			clearBuffer();
			nextChar(true);
			
			return new PdfKeyword(possible.get(0));
		} catch(IndexOutOfBoundsException e) {
			
		}
		
		return buffer.toString();
	}
}
