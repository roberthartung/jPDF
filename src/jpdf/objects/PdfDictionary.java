package jpdf.objects;

import java.util.HashMap;

public class PdfDictionary extends HashMap<String, PdfObject> implements PdfObject {
	public String toString() {
		return "Dictionary: " + this.keySet();
	}
}
