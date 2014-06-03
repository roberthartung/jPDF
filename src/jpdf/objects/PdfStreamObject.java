package jpdf.objects;

public class PdfStreamObject implements PdfObject {
	private PdfDictionary dict;
	
	private byte[] data;
	
	public PdfStreamObject(PdfDictionary dict, byte[] data) {
		this.dict = dict;
		this.data = data;
	}
	
	public PdfDictionary getDictionary() {
		return dict;
	}
	
	public byte[] getData() {
		return data;
	}
}
