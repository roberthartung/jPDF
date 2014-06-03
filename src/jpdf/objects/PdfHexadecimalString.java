package jpdf.objects;

public class PdfHexadecimalString implements PdfObject {
	private String s;

	public PdfHexadecimalString(String s) {
		this.s = s;
	}
	
	public String toString() {
		return s;
	}
}
