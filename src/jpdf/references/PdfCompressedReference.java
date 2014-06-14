package jpdf.references;

public class PdfCompressedReference implements PdfReference {
	private int containingObjectId;
	
	private int offset;

	public PdfCompressedReference(int containingObjectId, int offset) {
		this.containingObjectId = containingObjectId;
		this.offset = offset;
	}
	
	public int getContainingObjectId() {
		return containingObjectId;
	}
	
	public int getOffset() {
		return offset;
	}
}
