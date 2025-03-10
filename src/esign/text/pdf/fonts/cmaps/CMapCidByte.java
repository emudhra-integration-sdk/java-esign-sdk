package esign.text.pdf.fonts.cmaps;

import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;
import java.util.HashMap;

public class CMapCidByte
        extends AbstractCMap {

    private HashMap<Integer, byte[]> map =  new HashMap<Integer, byte[]>();
    private final byte[] EMPTY = new byte[0];

    void addChar(PdfString mark, PdfObject code) {
        if (!(code instanceof PdfNumber)) {
            return;
        }
        byte[] ser = decodeStringToByte(mark);
        this.map.put(Integer.valueOf(((PdfNumber) code).intValue()), ser);
    }

    public byte[] lookup(int cid) {
        byte[] ser = this.map.get(Integer.valueOf(cid));
        if (ser == null) {
            return this.EMPTY;
        }
        return ser;
    }
}

