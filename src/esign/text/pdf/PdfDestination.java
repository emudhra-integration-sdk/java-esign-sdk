package esign.text.pdf;

import java.util.StringTokenizer;

public class PdfDestination
        extends PdfArray {

    public static final int XYZ = 0;
    public static final int FIT = 1;
    public static final int FITH = 2;
    public static final int FITV = 3;
    public static final int FITR = 4;
    public static final int FITB = 5;
    public static final int FITBH = 6;
    public static final int FITBV = 7;
    private boolean status = false;

    public PdfDestination(PdfDestination d) {
        super(d);
        this.status = d.status;
    }

    public PdfDestination(int type) {
        if (type == 5) {
            add(PdfName.FITB);
        } else {

            add(PdfName.FIT);
        }
    }

    public PdfDestination(int type, float parameter) {
        super(new PdfNumber(parameter));
        switch (type) {
            default:
                addFirst(PdfName.FITH);
                return;
            case 3:
                addFirst(PdfName.FITV);
                return;
            case 6:
                addFirst(PdfName.FITBH);
                return;
            case 7:
                break;
        }
        addFirst(PdfName.FITBV);
    }

    public PdfDestination(int type, float left, float top, float zoom) {
        super(PdfName.XYZ);
        if (left < 0.0F) {
            add(PdfNull.PDFNULL);
        } else {
            add(new PdfNumber(left));
        }
        if (top < 0.0F) {
            add(PdfNull.PDFNULL);
        } else {
            add(new PdfNumber(top));
        }
        add(new PdfNumber(zoom));
    }

    public PdfDestination(int type, float left, float bottom, float right, float top) {
        super(PdfName.FITR);
        add(new PdfNumber(left));
        add(new PdfNumber(bottom));
        add(new PdfNumber(right));
        add(new PdfNumber(top));
    }

    public PdfDestination(String dest) {
        StringTokenizer tokens = new StringTokenizer(dest);
        if (tokens.hasMoreTokens()) {
            add(new PdfName(tokens.nextToken()));
        }
        while (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if ("null".equals(token)) {
                add(new PdfNull());
                continue;
            }
            try {
                add(new PdfNumber(token));
            } catch (RuntimeException e) {
                add(new PdfNull());
            }
        }
    }

    public boolean hasPage() {
        return this.status;
    }

    public boolean addPage(PdfIndirectReference page) {
        if (!this.status) {
            addFirst(page);
            this.status = true;
            return true;
        }
        return false;
    }
}
