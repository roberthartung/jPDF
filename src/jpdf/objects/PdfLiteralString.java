package jpdf.objects;

public class PdfLiteralString implements PdfObject {
	private String s;

	public PdfLiteralString(String s) {
		this.s = s;
	}
	
	public String toString() {
		return s;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		
		if(obj instanceof String && ((String) obj).equals(s)) {
			return true;
		}
		
		if (getClass() != obj.getClass())
			return false;
		PdfLiteralString other = (PdfLiteralString) obj;
		if (s == null) {
			if (other.s != null)
				return false;
		} else if (!s.equals(other.s))
			return false;
		return true;
	}
}
