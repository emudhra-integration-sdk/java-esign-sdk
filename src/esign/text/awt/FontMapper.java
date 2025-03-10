package esign.text.awt;

import esign.text.pdf.BaseFont;
import java.awt.Font;

public interface FontMapper {

    BaseFont awtToPdf(Font paramFont);

    Font pdfToAwt(BaseFont paramBaseFont, int paramInt);
}
