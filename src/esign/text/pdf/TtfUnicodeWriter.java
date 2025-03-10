package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.log.LoggerFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class TtfUnicodeWriter {

    protected PdfWriter writer = null;

    public TtfUnicodeWriter(PdfWriter writer) {
        this.writer = writer;
    }

    public void writeFont(TrueTypeFontUnicode font, PdfIndirectReference ref, Object[] params, byte[] rotbits) throws DocumentException, IOException {
        HashMap<Integer, int[]> longTag = (HashMap<Integer, int[]>) params[0];
        font.addRangeUni(longTag, true, font.subset);
        int[][] metrics = (int[][]) longTag.values().toArray((Object[]) new int[0][]);
        Arrays.sort(metrics, font);
        PdfIndirectReference ind_font;
        if (font.cff) {
            byte[] b = font.readCffFont();
            if (font.subset || font.subsetRanges != null) {
                CFFFontSubset cff = new CFFFontSubset(new RandomAccessFileOrArray(b), longTag);
                try {
                    b = cff.Process(cff.getNames()[0]);
                } catch (Exception e) {
                    LoggerFactory.getLogger(TtfUnicodeWriter.class).error("Issue in CFF font subsetting.Subsetting was disabled", e);

                    font.setSubset(false);
                    font.addRangeUni(longTag, true, font.subset);
                    metrics = (int[][]) longTag.values().toArray((Object[]) new int[0][]);
                    Arrays.sort(metrics, font);
                }
            }
            PdfObject pobj = new BaseFont.StreamFont(b, "CIDFontType0C", font.compressionLevel);
            PdfIndirectObject obj = this.writer.addToBody(pobj);
            ind_font = obj.getIndirectReference();
        } else {
            byte[] b;
            if (font.subset || font.directoryOffset != 0) {
                synchronized (font.rf) {
                    TrueTypeFontSubSet sb = new TrueTypeFontSubSet(font.fileName, new RandomAccessFileOrArray(font.rf), new HashSet<Integer>(longTag.keySet()), font.directoryOffset, true, false);
                    b = sb.process();
                }
            } else {

                b = font.getFullFont();
            }
            int[] lengths = {b.length};
            PdfObject pobj = new BaseFont.StreamFont(b, lengths, font.compressionLevel);
            PdfIndirectObject obj = this.writer.addToBody(pobj);
            ind_font = obj.getIndirectReference();
        }
        String str = "";
        if (font.subset) {
            str = TrueTypeFontUnicode.createSubsetPrefix();
        }
        PdfDictionary dic = font.getFontDescriptor(ind_font, str, (PdfIndirectReference) null);
        PdfIndirectObject pdfIndirectObject = this.writer.addToBody(dic);
        ind_font = pdfIndirectObject.getIndirectReference();

        PdfDictionary pdfDictionary1 = font.getCIDFontType2(ind_font, str, (Object[]) metrics);
        pdfIndirectObject = this.writer.addToBody(pdfDictionary1);
        ind_font = pdfIndirectObject.getIndirectReference();

        pdfDictionary1 = font.getToUnicode((Object[]) metrics);
        PdfIndirectReference toUnicodeRef = null;

        if (pdfDictionary1 != null) {
            pdfIndirectObject = this.writer.addToBody(pdfDictionary1);
            toUnicodeRef = pdfIndirectObject.getIndirectReference();
        }

        pdfDictionary1 = font.getFontBaseType(ind_font, str, toUnicodeRef);
        this.writer.addToBody(pdfDictionary1, ref);
    }
}
