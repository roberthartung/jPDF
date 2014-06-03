package jpdf.objects;

public class PdfNumber implements PdfObject {
	private String n;

	public PdfNumber(String n) {
		this.n = n;
	}
	
	public String toString() {
		return n;
	}
}
