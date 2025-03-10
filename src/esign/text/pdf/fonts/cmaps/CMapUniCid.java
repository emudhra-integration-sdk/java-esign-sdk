package esign.text.pdf.fonts.cmaps;

import esign.text.Utilities;
import esign.text.pdf.IntHashtable;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;

public class CMapUniCid
        extends AbstractCMap {

    private IntHashtable map = new IntHashtable(65537);

    void addChar(PdfString mark, PdfObject code) {
        int codepoint;
        if (!(code instanceof PdfNumber)) {
            return;
        }
        String s = decodeStringToUnicode(mark);
        if (Utilities.isSurrogatePair(s, 0)) {
            codepoint = Utilities.convertToUtf32(s, 0);
        } else {
            codepoint = s.charAt(0);
        }
        this.map.put(codepoint, ((PdfNumber) code).intValue());
    }

    public int lookup(int character) {
        return this.map.get(character);
    }

    public CMapToUnicode exportToUnicode() {
        CMapToUnicode uni = new CMapToUnicode();
        int[] keys = this.map.toOrderedKeys();
        for (int key : keys) {
            uni.addChar(this.map.get(key), Utilities.convertFromUtf32(key));
        }
        return uni;
    }
}
