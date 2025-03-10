package esign.text.pdf;

import esign.text.ExceptionConverter;
import esign.text.error_messages.MessageLocalization;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class ICC_Profile {

    protected byte[] data;
    protected int numComponents;
    private static HashMap<String, Integer> cstags = new HashMap<String, Integer>();

    public static ICC_Profile getInstance(byte[] data, int numComponents) {
        if (data.length < 128 || data[36] != 97 || data[37] != 99 || data[38] != 115 || data[39] != 112) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("invalid.icc.profile", new Object[0]));
        }
        try {
            ICC_Profile icc = new ICC_Profile();
            icc.data = data;

            Integer cs = cstags.get(new String(data, 16, 4, "US-ASCII"));
            int nc = (cs == null) ? 0 : cs.intValue();
            icc.numComponents = nc;

            if (nc != numComponents) {
                throw new IllegalArgumentException("ICC profile contains " + nc + " component(s), the image data contains " + numComponents + " component(s)");
            }
            return icc;
        } catch (UnsupportedEncodingException e) {
            throw new ExceptionConverter(e);
        }
    }

    public static ICC_Profile getInstance(byte[] data) {
        try {
            Integer cs = cstags.get(new String(data, 16, 4, "US-ASCII"));
            int numComponents = (cs == null) ? 0 : cs.intValue();
            return getInstance(data, numComponents);
        } catch (UnsupportedEncodingException e) {
            throw new ExceptionConverter(e);
        }
    }

    public static ICC_Profile getInstance(InputStream file) {
        try {
            byte[] head = new byte[128];
            int remain = head.length;
            int ptr = 0;
            while (remain > 0) {
                int n = file.read(head, ptr, remain);
                if (n < 0) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("invalid.icc.profile", new Object[0]));
                }
                remain -= n;
                ptr += n;
            }
            if (head[36] != 97 || head[37] != 99 || head[38] != 115 || head[39] != 112) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("invalid.icc.profile", new Object[0]));
            }
            remain = (head[0] & 0xFF) << 24 | (head[1] & 0xFF) << 16 | (head[2] & 0xFF) << 8 | head[3] & 0xFF;

            byte[] icc = new byte[remain];
            System.arraycopy(head, 0, icc, 0, head.length);
            remain -= head.length;
            ptr = head.length;
            while (remain > 0) {
                int n = file.read(icc, ptr, remain);
                if (n < 0) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("invalid.icc.profile", new Object[0]));
                }
                remain -= n;
                ptr += n;
            }
            return getInstance(icc);
        } catch (Exception ex) {
            throw new ExceptionConverter(ex);
        }
    }

    public static ICC_Profile GetInstance(String fname) {
        FileInputStream fs = null;
        try {
            fs = new FileInputStream(fname);
            ICC_Profile icc = getInstance(fs);
            return icc;
        } catch (Exception ex) {
            throw new ExceptionConverter(ex);
        } finally {

            try {
                fs.close();
            } catch (Exception exception) {
            }
        }
    }

    public byte[] getData() {
        return this.data;
    }

    public int getNumComponents() {
        return this.numComponents;
    }

    static {
        cstags.put("XYZ ", Integer.valueOf(3));
        cstags.put("Lab ", Integer.valueOf(3));
        cstags.put("Luv ", Integer.valueOf(3));
        cstags.put("YCbr", Integer.valueOf(3));
        cstags.put("Yxy ", Integer.valueOf(3));
        cstags.put("RGB ", Integer.valueOf(3));
        cstags.put("GRAY", Integer.valueOf(1));
        cstags.put("HSV ", Integer.valueOf(3));
        cstags.put("HLS ", Integer.valueOf(3));
        cstags.put("CMYK", Integer.valueOf(4));
        cstags.put("CMY ", Integer.valueOf(3));
        cstags.put("2CLR", Integer.valueOf(2));
        cstags.put("3CLR", Integer.valueOf(3));
        cstags.put("4CLR", Integer.valueOf(4));
        cstags.put("5CLR", Integer.valueOf(5));
        cstags.put("6CLR", Integer.valueOf(6));
        cstags.put("7CLR", Integer.valueOf(7));
        cstags.put("8CLR", Integer.valueOf(8));
        cstags.put("9CLR", Integer.valueOf(9));
        cstags.put("ACLR", Integer.valueOf(10));
        cstags.put("BCLR", Integer.valueOf(11));
        cstags.put("CCLR", Integer.valueOf(12));
        cstags.put("DCLR", Integer.valueOf(13));
        cstags.put("ECLR", Integer.valueOf(14));
        cstags.put("FCLR", Integer.valueOf(15));
    }
}
