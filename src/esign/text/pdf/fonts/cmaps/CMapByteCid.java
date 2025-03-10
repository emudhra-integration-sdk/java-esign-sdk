package esign.text.pdf.fonts.cmaps;

import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;
import java.util.ArrayList;

public class CMapByteCid
        extends AbstractCMap {

    private ArrayList<char[]> planes = new ArrayList<char[]>();

    public CMapByteCid() {
        this.planes.add(new char[256]);
    }

    void addChar(PdfString mark, PdfObject code) {
        if (!(code instanceof PdfNumber)) {
            return;
        }
        encodeSequence(decodeStringToByte(mark), (char) ((PdfNumber) code).intValue());
    }

    private void encodeSequence(byte[] seqs, char cid) {
        int size = seqs.length - 1;
        int nextPlane = 0;
        for (int idx = 0; idx < size; idx++) {
            char[] arrayOfChar = this.planes.get(nextPlane);
            int i = seqs[idx] & 0xFF;
            char c1 = arrayOfChar[i];
            if (c1 != '\000' && (c1 & 0x8000) == 0) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("inconsistent.mapping", new Object[0]));
            }
            if (c1 == '\000') {
                this.planes.add(new char[256]);
                c1 = (char) (this.planes.size() - 1 | 0x8000);
                arrayOfChar[i] = c1;
            }
            nextPlane = c1 & 0x7FFF;
        }
        char[] plane = this.planes.get(nextPlane);
        int one = seqs[size] & 0xFF;
        char c = plane[one];
        if ((c & 0x8000) != 0) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("inconsistent.mapping", new Object[0]));
        }
        plane[one] = cid;
    }

    public int decodeSingle(CMapSequence seq) {
        int end = seq.off + seq.len;
        int currentPlane = 0;
        while (seq.off < end) {
            int one = seq.seq[seq.off++] & 0xFF;
            seq.len--;
            char[] plane = this.planes.get(currentPlane);
            int cid = plane[one];
            if ((cid & 0x8000) == 0) {
                return cid;
            }

            currentPlane = cid & 0x7FFF;
        }
        return -1;
    }

    public String decodeSequence(CMapSequence seq) {
        StringBuilder sb = new StringBuilder();
        int cid = 0;
        while ((cid = decodeSingle(seq)) >= 0) {
            sb.append((char) cid);
        }
        return sb.toString();
    }
}


/* Location:              D:\test\tp\itextpdf-5.5.10.jar!\com\itextpdf\text\pdf\fonts\cmaps\CMapByteCid.class
 * Java compiler version: 5 (49.0)
 * JD-Core Version:       1.1.3
 */
