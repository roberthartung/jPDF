package jpdf.parser;

import jpdf.objects.PdfNumber;
import jpdf.objects.PdfObject;

public class PdfIndirectReference implements PdfObject {
	private PdfNumber id;
	private PdfNumber generation;

	public PdfIndirectReference(PdfNumber id, PdfNumber generation) {
		this.setId(id);
		this.setGeneration(generation);
	}

	public PdfNumber getId() {
		return id;
	}

	private void setId(PdfNumber id) {
		this.id = id;
	}

	public PdfNumber getGeneration() {
		return generation;
	}

	private void setGeneration(PdfNumber generation) {
		this.generation = generation;
	}
}
