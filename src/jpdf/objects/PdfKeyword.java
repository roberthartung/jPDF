package jpdf.objects;

public class PdfKeyword implements PdfObject {
	private String keyword;
	
	public PdfKeyword(String keyword) {
		this.keyword = keyword;
	}
	
	public String toString() {
		return getKeyword();
	}
	
	public String getKeyword() {
		return keyword;
	}
}
