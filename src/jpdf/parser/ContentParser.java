package jpdf.parser;

import java.io.EOFException;
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

	public boolean parse() throws ParserException {
		try {
			nextChar(true);
			
			// DEBUG = true;
			
			// Check for Artifact
			/*
			if(buffer.toString().equals("/")) {
				System.out.println(parseObject());
				if(buffer.toString().equals("<")) {
					PdfObject obj = parseObject();
				}
			}
			*/
			
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
					debug("Token: " + token + " " + buffer);
					PdfObject[] objects = clear();
					switch(((PdfKeyword) token).getKeyword()) {
						case "BDC" :
							while(buffer.charAt(0) == '/') {
								parseObject(true);
								parseObject(true);
							}
						break;
						case "m" :
						case "re" :
							debug("Transition to PATH_OBJECT");
							state = STATES.PATH_OBJECT;
							break;
						case "q" :
							// Save graphic state on stack
							break;
						case "BT" :
							debug("Transition to TEXT_OBJECT");
								state = STATES.TEXT_OBJECT;
							break;
						// case "EMC" :
							// return;
					}
				} else {
					switch((String) token) {
							default :
								add(parseObject());
								break;
								// throw new ParserException("Unimplemented token: '"+token+"'");
					}
				}
					
				break;
			case CLIPPING_PATH_OBJECT:
				token = readOneOf(
						"S", "s", "f", "F", "f*", "B", "B*", "b", "b*", "n" // State transitions
					);
					
					if(token instanceof PdfKeyword) {
						PdfObject[] objects = clear();
						debug("Token: " + token + " " + buffer);
						
						switch(((PdfKeyword) token).getKeyword()) {
							default:
								debug("Transition to PAGE_DESCRIPTION_LEVEL");
								state = STATES.PAGE_DESCRIPTION_LEVEL;
								break;
						}
						/*
						 W* n
0 792.03 612 -792 re
W n
q 1 0 0 1 72 696.48 cm 0 0 m
0 1.02 l
f
Q
72 696.48 468 1.02 re
						 */
					} else {
						switch((String) token) {
								default :
									add(parseObject());
									break;
									// throw new ParserException("Unimplemented token: '"+token+"'");
						}
					}
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
				token = readOneOf(
					"m", "l", "c", "v", "y", "h", "re", // Path construction
					"S", "s", "f", "F", "f*", "B", "B*", "b", "b*", "n", // State transitions to PAGE_LEVEL
					"W", "W*" // State transitions CLIPPING
				);
				
				if(token instanceof PdfKeyword) {
					PdfObject[] objects = clear();
					
					switch(((PdfKeyword) token).getKeyword()) {
						case "S":
						case "s" :
						case "f" :
						case "F" :
						case "f+" :
						case "B":
						case "B*" :
						case "b" :
						case "b*" :
						case "n" :
							state = STATES.PAGE_DESCRIPTION_LEVEL;
						break;
						case "W" :
						case "W*" :
							debug("Transition to CLIPPING_PATH_OBJECT");
							state = STATES.CLIPPING_PATH_OBJECT;
							break;
					}
				} else {
					switch((String) token) {
							default :
								add(parseObject());
								break;
					}
				}
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
					PdfObject[] objects = clear();
					switch(((PdfKeyword) token).getKeyword()) {
						case "ET" :
							state = STATES.PAGE_DESCRIPTION_LEVEL;
							debug("Transition to PAGE_DESCRIPTION_LEVEL");
						break;
						case "Tj" :
						case "TJ" :
							//System.out.println("Token: " + token);
							for(PdfObject obj : objects) {
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
							default :
								//System.err.println("Unhandled token: " + token);
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
			
			
		} catch(EOFException e) {
			return true;
		}
	}

	private Object readOneOf(String ...tokens) throws IndexOutOfBoundsException, ParserException, EOFException {
		ArrayList<String> possible = new ArrayList<>();
		ArrayList<String> matching = new ArrayList<>();
		
		//System.out.print("Tokens:");
		for(String s : tokens) {
			possible.add(s);
			//System.out.print(" " +s);
		}
		//System.out.println();
		
		//System.out.println("Buffer: " + buffer);
		
		int index = 0;
		while(possible.size() > 0) {
			// System.out.println(buffer);
			Iterator<String> it = possible.iterator();
			while(it.hasNext()) {
				String s = it.next();
				if(s.length() == index) {
					//System.out.println("Matching: " + s);
					matching.add(s);
					it.remove();
				} else if(s.charAt(index) != buffer.charAt(index)) {
					it.remove();
					//System.out.println("Remove: " + s);
				}
			}
			
			if(possible.size() > 0) {
				nextChar();
				index++;
			}
		}
	
		try
		{
			PdfKeyword k = new PdfKeyword(matching.get(0));
			clearBuffer();
			nextChar(true);
			return k;
		} catch(IndexOutOfBoundsException e) {
			
		}
		
		return buffer.toString();
	}
}
