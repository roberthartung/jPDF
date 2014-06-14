package jpdf.references;

public class PdfByteOffsetReference implements PdfReference {

	private int byteOffset;

	public PdfByteOffsetReference(int byteOffset) {
		this.byteOffset = byteOffset;
	}

	public int getByteOffset() {
		return byteOffset;
	}
}
