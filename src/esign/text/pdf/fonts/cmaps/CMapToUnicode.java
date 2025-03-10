package esign.text.pdf.fonts.cmaps;

import esign.text.ExceptionConverter;
import esign.text.Utilities;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CMapToUnicode
        extends AbstractCMap {

    private Map<Integer, String> singleByteMappings = new HashMap<Integer, String>();
    private Map<Integer, String> doubleByteMappings = new HashMap<Integer, String>();

    public boolean hasOneByteMappings() {
        return !this.singleByteMappings.isEmpty();
    }

    public boolean hasTwoByteMappings() {
        return !this.doubleByteMappings.isEmpty();
    }

    public String lookup(byte[] code, int offset, int length) {
        String result = null;
        Integer key = null;
        if (length == 1) {

            key = Integer.valueOf(code[offset] & 0xFF);
            result = this.singleByteMappings.get(key);
        } else if (length == 2) {
            int intKey = code[offset] & 0xFF;
            intKey <<= 8;
            intKey += code[offset + 1] & 0xFF;
            key = Integer.valueOf(intKey);

            result = this.doubleByteMappings.get(key);
        }

        return result;
    }

    public Map<Integer, Integer> createReverseMapping() throws IOException {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        for (Map.Entry<Integer, String> entry : this.singleByteMappings.entrySet()) {
            result.put(Integer.valueOf(convertToInt(entry.getValue())), entry.getKey());
        }
        for (Map.Entry<Integer, String> entry : this.doubleByteMappings.entrySet()) {
            result.put(Integer.valueOf(convertToInt(entry.getValue())), entry.getKey());
        }
        return result;
    }

    public Map<Integer, Integer> createDirectMapping() throws IOException {
        Map<Integer, Integer> result = new HashMap<Integer, Integer>();
        for (Map.Entry<Integer, String> entry : this.singleByteMappings.entrySet()) {
            result.put(entry.getKey(), Integer.valueOf(convertToInt(entry.getValue())));
        }
        for (Map.Entry<Integer, String> entry : this.doubleByteMappings.entrySet()) {
            result.put(entry.getKey(), Integer.valueOf(convertToInt(entry.getValue())));
        }
        return result;
    }

    private int convertToInt(String s) throws IOException {
        byte[] b = s.getBytes("UTF-16BE");
        int value = 0;
        for (int i = 0; i < b.length - 1; i++) {
            value += b[i] & 0xFF;
            value <<= 8;
        }
        value += b[b.length - 1] & 0xFF;
        return value;
    }

    void addChar(int cid, String uni) {
        this.doubleByteMappings.put(Integer.valueOf(cid), uni);
    }

    void addChar(PdfString mark, PdfObject code) {
        try {
            byte[] src = mark.getBytes();
            String dest = createStringFromBytes(code.getBytes());
            if (src.length == 1) {
                this.singleByteMappings.put(Integer.valueOf(src[0] & 0xFF), dest);
            } else if (src.length == 2) {
                int intSrc = src[0] & 0xFF;
                intSrc <<= 8;
                intSrc |= src[1] & 0xFF;
                this.doubleByteMappings.put(Integer.valueOf(intSrc), dest);
            } else {
                throw new IOException(MessageLocalization.getComposedMessage("mapping.code.should.be.1.or.two.bytes.and.not.1", src.length));
            }

        } catch (Exception ex) {
            throw new ExceptionConverter(ex);
        }
    }

    private String createStringFromBytes(byte[] bytes) throws IOException {
        String retval = null;
        if (bytes.length == 1) {
            retval = new String(bytes);
        } else {
            retval = new String(bytes, "UTF-16BE");
        }
        return retval;
    }

    public static CMapToUnicode getIdentity() {
        CMapToUnicode uni = new CMapToUnicode();
        for (int i = 0; i < 65537; i++) {
            uni.addChar(i, Utilities.convertFromUtf32(i));
        }
        return uni;
    }
}
