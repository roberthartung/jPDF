package jpdf.objects;

import java.util.Map;

public class PdfObjectStream implements PdfObject {

	private Map<Integer, PdfObject> objects;

	public PdfObjectStream(Map<Integer, PdfObject> objects) {
		this.objects = objects;
	}
	
	public Map<Integer, PdfObject> getObjects() {
		return objects;
	}
}
