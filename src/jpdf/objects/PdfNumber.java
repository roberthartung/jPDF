package jpdf.objects;

public class PdfNumber extends Number implements PdfObject {
	private String n;

	private int intValue;
	
	private double doubleValue;
	
	private float floatValue;
	
	private long longValue;
	
	public PdfNumber(String n) {
		this.n = n;
		try {
			intValue = Integer.parseInt(n);
			doubleValue = (double) intValue;
		} catch(NumberFormatException e) {
			doubleValue = Double.parseDouble(n);
			intValue = (int) doubleValue;
		}
	}
	
	public String toString() {
		return n;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((n == null) ? 0 : n.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PdfNumber other = (PdfNumber) obj;
		if (n == null) {
			if (other.n != null)
				return false;
		} else if (!n.equals(other.n))
			return false;
		return true;
	}

	@Override
	public long longValue() {
		return longValue;
	}

	@Override
	public float floatValue() {
		return floatValue;
	}

	@Override
	public double doubleValue() {
		return doubleValue;
	}

	@Override
	public int intValue() {
		return intValue;
	}
}
