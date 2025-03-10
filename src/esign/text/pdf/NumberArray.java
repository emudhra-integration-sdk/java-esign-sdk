package esign.text.pdf;

import java.util.List;

public class NumberArray
        extends PdfArray {

    public NumberArray(float... numbers) {
        for (float f : numbers) {
            add(new PdfNumber(f));
        }
    }

    public NumberArray(List<PdfNumber> numbers) {
        for (PdfNumber n : numbers) {
            add(n);
        }
    }
}
