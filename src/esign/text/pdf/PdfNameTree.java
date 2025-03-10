package esign.text.pdf;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class PdfNameTree {

    private static final int leafSize = 64;

    public static PdfDictionary writeTree(HashMap<String, ? extends PdfObject> items, PdfWriter writer) throws IOException {
        if (items.isEmpty()) {
            return null;
        }
        String[] names = new String[items.size()];
        names = (String[]) items.keySet().toArray((Object[]) names);
        Arrays.sort((Object[]) names);
        if (names.length <= 64) {
            PdfDictionary dic = new PdfDictionary();
            PdfArray ar = new PdfArray();
            for (int i = 0; i < names.length; i++) {
                ar.add(new PdfString(names[i], null));
                ar.add(items.get(names[i]));
            }
            dic.put(PdfName.NAMES, ar);
            return dic;
        }
        int skip = 64;
        PdfIndirectReference[] kids = new PdfIndirectReference[(names.length + 64 - 1) / 64];
        for (int k = 0; k < kids.length; k++) {
            int offset = k * 64;
            int end = Math.min(offset + 64, names.length);
            PdfDictionary dic = new PdfDictionary();
            PdfArray arr = new PdfArray();
            arr.add(new PdfString(names[offset], null));
            arr.add(new PdfString(names[end - 1], null));
            dic.put(PdfName.LIMITS, arr);
            arr = new PdfArray();
            for (; offset < end; offset++) {
                arr.add(new PdfString(names[offset], null));
                arr.add(items.get(names[offset]));
            }
            dic.put(PdfName.NAMES, arr);
            kids[k] = writer.addToBody(dic).getIndirectReference();
        }
        int top = kids.length;
        while (true) {
            if (top <= 64) {
                PdfArray arr = new PdfArray();
                for (int j = 0; j < top; j++) {
                    arr.add(kids[j]);
                }
                PdfDictionary dic = new PdfDictionary();
                dic.put(PdfName.KIDS, arr);
                return dic;
            }
            skip *= 64;
            int tt = (names.length + skip - 1) / skip;
            for (int i = 0; i < tt; i++) {
                int offset = i * 64;
                int end = Math.min(offset + 64, top);
                PdfDictionary dic = new PdfDictionary();
                PdfArray arr = new PdfArray();
                arr.add(new PdfString(names[i * skip], null));
                arr.add(new PdfString(names[Math.min((i + 1) * skip, names.length) - 1], null));
                dic.put(PdfName.LIMITS, arr);
                arr = new PdfArray();
                for (; offset < end; offset++) {
                    arr.add(kids[offset]);
                }
                dic.put(PdfName.KIDS, arr);
                kids[i] = writer.addToBody(dic).getIndirectReference();
            }
            top = tt;
        }
    }

    private static PdfString iterateItems(PdfDictionary dic, HashMap<String, PdfObject> items, PdfString leftOverString) {
        PdfArray nn = (PdfArray) PdfReader.getPdfObjectRelease(dic.get(PdfName.NAMES));
        if (nn != null) {
            for (int k = 0; k < nn.size(); k++) {
                PdfString s;
                if (leftOverString == null) {
                    s = (PdfString) PdfReader.getPdfObjectRelease(nn.getPdfObject(k++));
                } else {

                    s = leftOverString;
                    leftOverString = null;
                }
                if (k < nn.size()) {
                    items.put(PdfEncodings.convertToString(s.getBytes(), null), nn.getPdfObject(k));
                } else {
                    return s;
                }
            }
        } else if ((nn = (PdfArray) PdfReader.getPdfObjectRelease(dic.get(PdfName.KIDS))) != null) {
            for (int k = 0; k < nn.size(); k++) {
                PdfDictionary kid = (PdfDictionary) PdfReader.getPdfObjectRelease(nn.getPdfObject(k));
                leftOverString = iterateItems(kid, items, leftOverString);
            }
        }
        return null;
    }

    public static HashMap<String, PdfObject> readTree(PdfDictionary dic) {
        HashMap<String, PdfObject> items = new HashMap<String, PdfObject>();
        if (dic != null) {
            iterateItems(dic, items, null);
        }
        return items;
    }
}
