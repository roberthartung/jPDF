package jpdf.parser;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jpdf.objects.PdfDictionary;
import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;
import jpdf.objects.PdfObjectStream;
import jpdf.util.BufferedStream;

public class ObjectStreamParser extends BaseParser {
	
	private BufferedStream stream;
	
	private PdfDictionary dict;
	
	private Map<Integer,PdfObject> objects;
	
	public ObjectStreamParser(BufferedStream bufferedStream, PdfDictionary dict) {
		super();
		this.stream = bufferedStream;
		this.dict = dict;
		objects = new LinkedHashMap<Integer,PdfObject>();
	}

	@Override
	protected int read() throws IOException {
		return stream.read();
	}

	@Override
	protected int read(byte[] bytes, int offset, int i) throws IOException {
		return stream.read(bytes, offset, i);
	}

	@Override
	protected String readLine() throws IOException {
		return stream.readLine();
	}

	public void parse() throws ParserException, IOException {
		nextChar(true);
		int n = ((PdfNumber) dict.get("N")).intValue();
		List<Integer> idList = new LinkedList<>();
		for(int i=0;i<n;i++) {
			PdfNumber id = readNumber();
			PdfNumber offset = readNumber();
			idList.add(id.intValue());
		}
		for(int i=0;i<n;i++) {
			objects.put(idList.get(i), parseObject());
		}
	}
	
	public PdfObjectStream getObjectStream() {
		return new PdfObjectStream(objects);
	}
}
