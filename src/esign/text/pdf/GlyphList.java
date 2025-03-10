package esign.text.pdf;

import esign.text.io.StreamUtil;
import esign.text.pdf.fonts.FontsResourceAnchor;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.StringTokenizer;

public class GlyphList {

    private static HashMap<Integer, String> unicode2names = new HashMap<Integer, String>();
    private static HashMap<String, int[]> names2unicode = (HashMap) new HashMap<String, int[]>();

    static {
        InputStream is = null;
        try {
            is = StreamUtil.getResourceStream("esign/text/pdf/fonts/glyphlist.txt", (new FontsResourceAnchor()).getClass().getClassLoader());
            if (is == null) {
                String msg = "glyphlist.txt not found as resource. (It must exist as resource in the package esign.text.pdf.fonts)";
                throw new Exception(msg);
            }
            byte[] buf = new byte[1024];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            while (true) {
                int size = is.read(buf);
                if (size < 0) {
                    break;
                }
                out.write(buf, 0, size);
            }
            is.close();
            is = null;
            String s = PdfEncodings.convertToString(out.toByteArray(), null);
            StringTokenizer tk = new StringTokenizer(s, "\r\n");
            while (tk.hasMoreTokens()) {
                String line = tk.nextToken();
                if (line.startsWith("#")) {
                    continue;
                }
                StringTokenizer t2 = new StringTokenizer(line, " ;\r\n\t\f");
                String name = null;
                String hex = null;
                if (!t2.hasMoreTokens()) {
                    continue;
                }
                name = t2.nextToken();
                if (!t2.hasMoreTokens()) {
                    continue;
                }
                hex = t2.nextToken();
                Integer num = Integer.valueOf(hex, 16);
                unicode2names.put(num, name);
                names2unicode.put(name, new int[]{num.intValue()});
            }

        } catch (Exception e) {
            System.err.println("glyphlist.txt loading error: " + e.getMessage());
        } finally {

            if (is != null) {
                try {
                    is.close();
                } catch (Exception exception) {
                }
            }
        }
    }

    public static int[] nameToUnicode(String name) {
        int[] v = names2unicode.get(name);
        if (v == null && name.length() == 7 && name.toLowerCase().startsWith("uni")) {
            try {
                return new int[]{Integer.parseInt(name.substring(3), 16)};
            } catch (Exception exception) {
            }
        }

        return v;
    }

    public static String unicodeToName(int num) {
        return unicode2names.get(Integer.valueOf(num));
    }
}
