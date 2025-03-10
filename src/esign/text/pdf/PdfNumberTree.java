package esign.text.pdf;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class PdfNumberTree {

    private static final int leafSize = 64;

    public static <O extends PdfObject> PdfDictionary writeTree(HashMap<Integer, O> items, PdfWriter writer) throws IOException {
        if (items.isEmpty()) {
            return null;
        }
        Integer[] numbers = new Integer[items.size()];
        numbers = (Integer[]) items.keySet().toArray((Object[]) numbers);
        Arrays.sort((Object[]) numbers);
        if (numbers.length <= 64) {
            PdfDictionary dic = new PdfDictionary();
            PdfArray ar = new PdfArray();
            for (int i = 0; i < numbers.length; i++) {
                ar.add(new PdfNumber(numbers[i].intValue()));
                ar.add((PdfObject) items.get(numbers[i]));
            }
            dic.put(PdfName.NUMS, ar);
            return dic;
        }
        int skip = 64;
        PdfIndirectReference[] kids = new PdfIndirectReference[(numbers.length + 64 - 1) / 64];
        for (int k = 0; k < kids.length; k++) {
            int offset = k * 64;
            int end = Math.min(offset + 64, numbers.length);
            PdfDictionary dic = new PdfDictionary();
            PdfArray arr = new PdfArray();
            arr.add(new PdfNumber(numbers[offset].intValue()));
            arr.add(new PdfNumber(numbers[end - 1].intValue()));
            dic.put(PdfName.LIMITS, arr);
            arr = new PdfArray();
            for (; offset < end; offset++) {
                arr.add(new PdfNumber(numbers[offset].intValue()));
                arr.add((PdfObject) items.get(numbers[offset]));
            }
            dic.put(PdfName.NUMS, arr);
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
            int tt = (numbers.length + skip - 1) / skip;
            for (int i = 0; i < tt; i++) {
                int offset = i * 64;
                int end = Math.min(offset + 64, top);
                PdfDictionary dic = new PdfDictionary();
                PdfArray arr = new PdfArray();
                arr.add(new PdfNumber(numbers[i * skip].intValue()));
                arr.add(new PdfNumber(numbers[Math.min((i + 1) * skip, numbers.length) - 1].intValue()));
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

    private static void iterateItems(PdfDictionary dic, HashMap<Integer, PdfObject> items) {
        PdfArray nn = (PdfArray) PdfReader.getPdfObjectRelease(dic.get(PdfName.NUMS));
        if (nn != null) {
            for (int k = 0; k < nn.size(); k++) {
                PdfNumber s = (PdfNumber) PdfReader.getPdfObjectRelease(nn.getPdfObject(k++));
                items.put(Integer.valueOf(s.intValue()), nn.getPdfObject(k));
            }

        } else if ((nn = (PdfArray) PdfReader.getPdfObjectRelease(dic.get(PdfName.KIDS))) != null) {
            for (int k = 0; k < nn.size(); k++) {
                PdfDictionary kid = (PdfDictionary) PdfReader.getPdfObjectRelease(nn.getPdfObject(k));
                iterateItems(kid, items);
            }
        }
    }

    public static HashMap<Integer, PdfObject> readTree(PdfDictionary dic) {
        HashMap<Integer, PdfObject> items = new HashMap<Integer, PdfObject>();
        if (dic != null) {
            iterateItems(dic, items);
        }
        return items;
    }
}
