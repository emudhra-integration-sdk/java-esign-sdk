package esign.text.pdf.fonts.cmaps;

import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PRTokeniser;
import esign.text.pdf.PdfContentParser;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;
import java.io.IOException;
import java.util.ArrayList;

public class CMapParserEx {

    private static final PdfName CMAPNAME = new PdfName("CMapName");
    private static final String DEF = "def";
    private static final String ENDCIDRANGE = "endcidrange";
    private static final String ENDCIDCHAR = "endcidchar";
    private static final String ENDBFRANGE = "endbfrange";
    private static final String ENDBFCHAR = "endbfchar";
    private static final String USECMAP = "usecmap";
    private static final int MAXLEVEL = 10;

    public static void parseCid(String cmapName, AbstractCMap cmap, CidLocation location) throws IOException {
        parseCid(cmapName, cmap, location, 0);
    }

    private static void parseCid(String cmapName, AbstractCMap cmap, CidLocation location, int level) throws IOException {
        if (level >= 10) {
            return;
        }
        PRTokeniser inp = location.getLocation(cmapName);
        try {
            ArrayList<PdfObject> list = new ArrayList<PdfObject>();
            PdfContentParser cp = new PdfContentParser(inp);
            int maxExc = 50;
            while (true) {
                try {
                    cp.parse(list);
                } catch (Exception ex) {
                    if (--maxExc < 0) {
                        break;
                    }
                    continue;
                }
                if (list.isEmpty()) {
                    break;
                }
                String last = ((PdfObject) list.get(list.size() - 1)).toString();
                if (level == 0 && list.size() == 3 && last.equals("def")) {
                    PdfObject key = list.get(0);
                    if (PdfName.REGISTRY.equals(key)) {
                        cmap.setRegistry(((PdfObject) list.get(1)).toString());
                        continue;
                    }
                    if (PdfName.ORDERING.equals(key)) {
                        cmap.setOrdering(((PdfObject) list.get(1)).toString());
                        continue;
                    }
                    if (CMAPNAME.equals(key)) {
                        cmap.setName(((PdfObject) list.get(1)).toString());
                        continue;
                    }
                    if (PdfName.SUPPLEMENT.equals(key)) {
                        try {
                            cmap.setSupplement(((PdfNumber) list.get(1)).intValue());
                        } catch (Exception exception) {
                        }
                    }
                    continue;
                }
                if ((last.equals("endcidchar") || last.equals("endbfchar")) && list.size() >= 3) {
                    int lmax = list.size() - 2;
                    for (int k = 0; k < lmax; k += 2) {
                        if (list.get(k) instanceof PdfString) {
                            cmap.addChar((PdfString) list.get(k), list.get(k + 1));
                        }
                    }
                    continue;
                }
                if ((last.equals("endcidrange") || last.equals("endbfrange")) && list.size() >= 4) {
                    int lmax = list.size() - 3;
                    for (int k = 0; k < lmax; k += 3) {
                        if (list.get(k) instanceof PdfString && list.get(k + 1) instanceof PdfString) {
                            cmap.addRange((PdfString) list.get(k), (PdfString) list.get(k + 1), list.get(k + 2));
                        }
                    }
                    continue;
                }
                if (last.equals("usecmap") && list.size() == 2 && list.get(0) instanceof PdfName) {
                    parseCid(PdfName.decodeName(((PdfObject) list.get(0)).toString()), cmap, location, level + 1);
                }
            }
        } finally {

            inp.close();
        }
    }

    private static void encodeSequence(int size, byte[] seqs, char cid, ArrayList<char[]> planes) {
        size--;
        int nextPlane = 0;
        for (int idx = 0; idx < size; idx++) {
            char[] arrayOfChar = planes.get(nextPlane);
            int i = seqs[idx] & 0xFF;
            char c1 = arrayOfChar[i];
            if (c1 != '\000' && (c1 & 0x8000) == 0) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("inconsistent.mapping", new Object[0]));
            }
            if (c1 == '\000') {
                planes.add(new char[256]);
                c1 = (char) (planes.size() - 1 | 0x8000);
                arrayOfChar[i] = c1;
            }
            nextPlane = c1 & 0x7FFF;
        }
        char[] plane = planes.get(nextPlane);
        int one = seqs[size] & 0xFF;
        char c = plane[one];
        if ((c & 0x8000) != 0) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("inconsistent.mapping", new Object[0]));
        }
        plane[one] = cid;
    }
}
