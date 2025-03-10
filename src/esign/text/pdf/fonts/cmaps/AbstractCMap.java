package esign.text.pdf.fonts.cmaps;

import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfEncodings;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;

public abstract class AbstractCMap {

    private String cmapName;
    private String registry;
    private String ordering;
    private int supplement;

    public String getName() {
        return this.cmapName;
    }

    void setName(String cmapName) {
        this.cmapName = cmapName;
    }

    public String getOrdering() {
        return this.ordering;
    }

    void setOrdering(String ordering) {
        this.ordering = ordering;
    }

    public String getRegistry() {
        return this.registry;
    }

    void setRegistry(String registry) {
        this.registry = registry;
    }

    public int getSupplement() {
        return this.supplement;
    }

    void setSupplement(int supplement) {
        this.supplement = supplement;
    }

    abstract void addChar(PdfString paramPdfString, PdfObject paramPdfObject);

    void addRange(PdfString from, PdfString to, PdfObject code) {
        byte[] a1 = decodeStringToByte(from);
        byte[] a2 = decodeStringToByte(to);
        if (a1.length != a2.length || a1.length == 0) {
            throw new IllegalArgumentException("Invalid map.");
        }
        byte[] sout = null;
        if (code instanceof PdfString) {
            sout = decodeStringToByte((PdfString) code);
        }
        int start = byteArrayToInt(a1);
        int end = byteArrayToInt(a2);
        for (int k = start; k <= end; k++) {
            intToByteArray(k, a1);
            PdfString s = new PdfString(a1);
            s.setHexWriting(true);
            if (code instanceof PdfArray) {
                addChar(s, ((PdfArray) code).getPdfObject(k - start));
            } else if (code instanceof PdfNumber) {
                int nn = ((PdfNumber) code).intValue() + k - start;
                addChar(s, (PdfObject) new PdfNumber(nn));
            } else if (code instanceof PdfString) {
                PdfString s1 = new PdfString(sout);
                s1.setHexWriting(true);
                sout[sout.length - 1] = (byte) (sout[sout.length - 1] + 1);
                addChar(s, (PdfObject) s1);
            }
        }
    }

    private static void intToByteArray(int v, byte[] b) {
        for (int k = b.length - 1; k >= 0; k--) {
            b[k] = (byte) v;
            v >>>= 8;
        }
    }

    private static int byteArrayToInt(byte[] b) {
        int v = 0;
        for (int k = 0; k < b.length; k++) {
            v <<= 8;
            v |= b[k] & 0xFF;
        }
        return v;
    }

    public static byte[] decodeStringToByte(PdfString s) {
        byte[] b = s.getBytes();
        byte[] br = new byte[b.length];
        System.arraycopy(b, 0, br, 0, b.length);
        return br;
    }

    public String decodeStringToUnicode(PdfString ps) {
        if (ps.isHexWriting()) {
            return PdfEncodings.convertToString(ps.getBytes(), "UnicodeBigUnmarked");
        }
        return ps.toUnicodeString();
    }
}
