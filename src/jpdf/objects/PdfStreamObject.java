package jpdf.objects;

public class PdfStreamObject implements PdfObject {
	PdfDictionary dict;
	
	public PdfStreamObject(PdfDictionary dict) {
		this.dict = dict;
	}
}
