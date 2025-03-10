package esign.text.pdf.fonts.cmaps;

public class CMapSequence {

    public byte[] seq;
    public int off;
    public int len;

    public CMapSequence() {
    }

    public CMapSequence(byte[] seq, int off, int len) {
        this.seq = seq;
        this.off = off;
        this.len = len;
    }
}
