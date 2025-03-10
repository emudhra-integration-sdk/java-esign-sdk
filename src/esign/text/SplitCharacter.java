package esign.text;

import esign.text.pdf.PdfChunk;

public interface SplitCharacter {

    boolean isSplitCharacter(int paramInt1, int paramInt2, int paramInt3, char[] paramArrayOfchar, PdfChunk[] paramArrayOfPdfChunk);
}
