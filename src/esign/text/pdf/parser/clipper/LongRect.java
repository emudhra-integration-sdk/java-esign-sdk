package esign.text.pdf.parser.clipper;

public class LongRect {

    public long left;
    public long top;
    public long right;
    public long bottom;

    public LongRect() {
    }

    public LongRect(long l, long t, long r, long b) {
        this.left = l;
        this.top = t;
        this.right = r;
        this.bottom = b;
    }

    public LongRect(LongRect ir) {
        this.left = ir.left;
        this.top = ir.top;
        this.right = ir.right;
        this.bottom = ir.bottom;
    }
}
