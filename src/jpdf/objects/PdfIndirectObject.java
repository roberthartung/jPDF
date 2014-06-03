package jpdf.objects;

public class PdfIndirectObject implements PdfObject {
	private PdfNumber num1;
	private PdfNumber num2;
	private PdfObject obj;

	public PdfIndirectObject(PdfNumber num1, PdfNumber num2, PdfObject obj) {
		this.num1 = num1;
		this.num2 = num2;
		this.obj = obj;
	}

	public String toString() {
		return "'" + num1 + "' '" + num2 + "'";
	}

	public PdfObject getObj() {
		return obj;
	}
}
