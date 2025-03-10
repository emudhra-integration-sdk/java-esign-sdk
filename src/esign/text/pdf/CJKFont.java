package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.ExceptionConverter;
import esign.text.Utilities;
import esign.text.error_messages.MessageLocalization;
import esign.text.io.StreamUtil;
import esign.text.pdf.fonts.cmaps.CMapCache;
import esign.text.pdf.fonts.cmaps.CMapCidByte;
import esign.text.pdf.fonts.cmaps.CMapCidUni;
import esign.text.pdf.fonts.cmaps.CMapUniCid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

class CJKFont
        extends BaseFont {

    static final String CJK_ENCODING = "UnicodeBigUnmarked";
    private static final int FIRST = 0;
    private static final int BRACKET = 1;
    private static final int SERIAL = 2;
    private static final int V1Y = 880;
    static Properties cjkFonts = new Properties();
    static Properties cjkEncodings = new Properties();
    private static final HashMap<String, HashMap<String, Object>> allFonts = new HashMap<String, HashMap<String, Object>>();

    private static boolean propertiesLoaded = false;

    public static final String RESOURCE_PATH_CMAP = "com/itextpdf/text/pdf/fonts/cmaps/";
    private static final HashMap<String, Set<String>> registryNames = new HashMap<String, Set<String>>();

    private CMapCidByte cidByte;

    private CMapUniCid uniCid;

    private CMapCidUni cidUni;
    private String uniMap;
    private String fontName;
    private String style = "";

    private String CMap;

    private boolean cidDirect = false;

    private IntHashtable vMetrics;

    private IntHashtable hMetrics;
    private HashMap<String, Object> fontDesc;

    private static void loadProperties() {
        if (propertiesLoaded) {
            return;
        }
        synchronized (allFonts) {
            if (propertiesLoaded) {
                return;
            }
            try {
                loadRegistry();
                for (String font : registryNames.get("fonts")) {
                    allFonts.put(font, readFontProperties(font));
                }
            } catch (Exception exception) {
            }

            propertiesLoaded = true;
        }
    }

    private static void loadRegistry() throws IOException {
        InputStream is = StreamUtil.getResourceStream("com/itextpdf/text/pdf/fonts/cmaps/cjk_registry.properties");
        Properties p = new Properties();
        p.load(is);
        is.close();
        for (Object key : p.keySet()) {
            String value = p.getProperty((String) key);
            String[] sp = value.split(" ");
            Set<String> hs = new HashSet<String>();
            for (String s : sp) {
                if (s.length() > 0) {
                    hs.add(s);
                }
            }
            registryNames.put((String) key, hs);
        }
    }

    CJKFont(String fontName, String enc, boolean emb) throws DocumentException {
        loadProperties();
        this.fontType = 2;
        String nameBase = getBaseName(fontName);
        if (!isCJKFont(nameBase, enc)) {
            throw new DocumentException(MessageLocalization.getComposedMessage("font.1.with.2.encoding.is.not.a.cjk.font", new Object[]{fontName, enc}));
        }
        if (nameBase.length() < fontName.length()) {
            this.style = fontName.substring(nameBase.length());
            fontName = nameBase;
        }
        this.fontName = fontName;
        this.encoding = "UnicodeBigUnmarked";
        this.vertical = enc.endsWith("V");
        this.CMap = enc;
        if (enc.equals("Identity-H") || enc.equals("Identity-V")) {
            this.cidDirect = true;
        }
        loadCMaps();
    }

    String getUniMap() {
        return this.uniMap;
    }

    private void loadCMaps() throws DocumentException {
        try {
            this.fontDesc = allFonts.get(this.fontName);
            this.hMetrics = (IntHashtable) this.fontDesc.get("W");
            this.vMetrics = (IntHashtable) this.fontDesc.get("W2");
            String registry = (String) this.fontDesc.get("Registry");
            this.uniMap = "";
            for (String name : registryNames.get(registry + "_Uni")) {
                this.uniMap = name;
                if (name.endsWith("V") && this.vertical) {
                    break;
                }
                if (!name.endsWith("V") && !this.vertical) {
                    break;
                }
            }
            if (this.cidDirect) {
                this.cidUni = CMapCache.getCachedCMapCidUni(this.uniMap);
            } else {

                this.uniCid = CMapCache.getCachedCMapUniCid(this.uniMap);
                this.cidByte = CMapCache.getCachedCMapCidByte(this.CMap);
            }

        } catch (Exception ex) {
            throw new DocumentException(ex);
        }
    }

    public static String GetCompatibleFont(String enc) {
        loadProperties();
        String registry = null;
        for (Map.Entry<String, Set<String>> e : registryNames.entrySet()) {
            if (((Set) e.getValue()).contains(enc)) {
                registry = e.getKey();
                for (Map.Entry<String, HashMap<String, Object>> e1 : allFonts.entrySet()) {
                    if (registry.equals(((HashMap) e1.getValue()).get("Registry"))) {
                        return e1.getKey();
                    }
                }
            }
        }
        return null;
    }

    public static boolean isCJKFont(String fontName, String enc) {
        loadProperties();
        if (!registryNames.containsKey("fonts")) {
            return false;
        }
        if (!((Set) registryNames.get("fonts")).contains(fontName)) {
            return false;
        }
        if (enc.equals("Identity-H") || enc.equals("Identity-V")) {
            return true;
        }
        String registry = (String) ((HashMap) allFonts.get(fontName)).get("Registry");
        Set<String> encodings = registryNames.get(registry);
        return (encodings != null && encodings.contains(enc));
    }

    public int getWidth(int char1) {
        int v, c = char1;
        if (!this.cidDirect) {
            c = this.uniCid.lookup(char1);
        }
        if (this.vertical) {
            v = this.vMetrics.get(c);
        } else {
            v = this.hMetrics.get(c);
        }
        if (v > 0) {
            return v;
        }
        return 1000;
    }

    public int getWidth(String text) {
        int total = 0;
        if (this.cidDirect) {
            for (int k = 0; k < text.length(); k++) {
                total += getWidth(text.charAt(k));
            }
        } else {

            for (int k = 0; k < text.length(); k++) {
                int val;
                if (Utilities.isSurrogatePair(text, k)) {
                    val = Utilities.convertToUtf32(text, k);
                    k++;
                } else {

                    val = text.charAt(k);
                }
                total += getWidth(val);
            }
        }
        return total;
    }

    int getRawWidth(int c, String name) {
        return 0;
    }

    public int getKerning(int char1, int char2) {
        return 0;
    }

    private PdfDictionary getFontDescriptor() {
        PdfDictionary dic = new PdfDictionary(PdfName.FONTDESCRIPTOR);
        dic.put(PdfName.ASCENT, new PdfLiteral((String) this.fontDesc.get("Ascent")));
        dic.put(PdfName.CAPHEIGHT, new PdfLiteral((String) this.fontDesc.get("CapHeight")));
        dic.put(PdfName.DESCENT, new PdfLiteral((String) this.fontDesc.get("Descent")));
        dic.put(PdfName.FLAGS, new PdfLiteral((String) this.fontDesc.get("Flags")));
        dic.put(PdfName.FONTBBOX, new PdfLiteral((String) this.fontDesc.get("FontBBox")));
        dic.put(PdfName.FONTNAME, new PdfName(this.fontName + this.style));
        dic.put(PdfName.ITALICANGLE, new PdfLiteral((String) this.fontDesc.get("ItalicAngle")));
        dic.put(PdfName.STEMV, new PdfLiteral((String) this.fontDesc.get("StemV")));
        PdfDictionary pdic = new PdfDictionary();
        pdic.put(PdfName.PANOSE, new PdfString((String) this.fontDesc.get("Panose"), null));
        dic.put(PdfName.STYLE, pdic);
        return dic;
    }

    private PdfDictionary getCIDFont(PdfIndirectReference fontDescriptor, IntHashtable cjkTag) {
        PdfDictionary dic = new PdfDictionary(PdfName.FONT);
        dic.put(PdfName.SUBTYPE, PdfName.CIDFONTTYPE0);
        dic.put(PdfName.BASEFONT, new PdfName(this.fontName + this.style));
        dic.put(PdfName.FONTDESCRIPTOR, fontDescriptor);
        int[] keys = cjkTag.toOrderedKeys();
        String w = convertToHCIDMetrics(keys, this.hMetrics);
        if (w != null) {
            dic.put(PdfName.W, new PdfLiteral(w));
        }
        if (this.vertical) {
            w = convertToVCIDMetrics(keys, this.vMetrics, this.hMetrics);
            if (w != null) {
                dic.put(PdfName.W2, new PdfLiteral(w));
            }
        } else {
            dic.put(PdfName.DW, new PdfNumber(1000));
        }
        PdfDictionary cdic = new PdfDictionary();
        if (this.cidDirect) {
            cdic.put(PdfName.REGISTRY, new PdfString(this.cidUni.getRegistry(), null));
            cdic.put(PdfName.ORDERING, new PdfString(this.cidUni.getOrdering(), null));
            cdic.put(PdfName.SUPPLEMENT, new PdfNumber(this.cidUni.getSupplement()));
        } else {

            cdic.put(PdfName.REGISTRY, new PdfString(this.cidByte.getRegistry(), null));
            cdic.put(PdfName.ORDERING, new PdfString(this.cidByte.getOrdering(), null));
            cdic.put(PdfName.SUPPLEMENT, new PdfNumber(this.cidByte.getSupplement()));
        }
        dic.put(PdfName.CIDSYSTEMINFO, cdic);
        return dic;
    }

    private PdfDictionary getFontBaseType(PdfIndirectReference CIDFont) {
        PdfDictionary dic = new PdfDictionary(PdfName.FONT);
        dic.put(PdfName.SUBTYPE, PdfName.TYPE0);
        String name = this.fontName;
        if (this.style.length() > 0) {
            name = name + "-" + this.style.substring(1);
        }
        name = name + "-" + this.CMap;
        dic.put(PdfName.BASEFONT, new PdfName(name));
        dic.put(PdfName.ENCODING, new PdfName(this.CMap));
        dic.put(PdfName.DESCENDANTFONTS, new PdfArray(CIDFont));
        return dic;
    }

    void writeFont(PdfWriter writer, PdfIndirectReference ref, Object[] params) throws DocumentException, IOException {
        IntHashtable cjkTag = (IntHashtable) params[0];
        PdfIndirectReference ind_font = null;
        PdfObject pobj = null;
        PdfIndirectObject obj = null;
        pobj = getFontDescriptor();
        if (pobj != null) {
            obj = writer.addToBody(pobj);
            ind_font = obj.getIndirectReference();
        }
        pobj = getCIDFont(ind_font, cjkTag);
        if (pobj != null) {
            obj = writer.addToBody(pobj);
            ind_font = obj.getIndirectReference();
        }
        pobj = getFontBaseType(ind_font);
        writer.addToBody(pobj, ref);
    }

    public PdfStream getFullFontStream() {
        return null;
    }

    private float getDescNumber(String name) {
        return Integer.parseInt((String) this.fontDesc.get(name));
    }

    private float getBBox(int idx) {
        String s = (String) this.fontDesc.get("FontBBox");
        StringTokenizer tk = new StringTokenizer(s, " []\r\n\t\f");
        String ret = tk.nextToken();
        for (int k = 0; k < idx; k++) {
            ret = tk.nextToken();
        }
        return Integer.parseInt(ret);
    }

    public float getFontDescriptor(int key, float fontSize) {
        switch (key) {
            case 1:
            case 9:
                return getDescNumber("Ascent") * fontSize / 1000.0F;
            case 2:
                return getDescNumber("CapHeight") * fontSize / 1000.0F;
            case 3:
            case 10:
                return getDescNumber("Descent") * fontSize / 1000.0F;
            case 4:
                return getDescNumber("ItalicAngle");
            case 5:
                return fontSize * getBBox(0) / 1000.0F;
            case 6:
                return fontSize * getBBox(1) / 1000.0F;
            case 7:
                return fontSize * getBBox(2) / 1000.0F;
            case 8:
                return fontSize * getBBox(3) / 1000.0F;
            case 11:
                return 0.0F;
            case 12:
                return fontSize * (getBBox(2) - getBBox(0)) / 1000.0F;
        }
        return 0.0F;
    }

    public String getPostscriptFontName() {
        return this.fontName;
    }

    public String[][] getFullFontName() {
        return new String[][]{{"", "", "", this.fontName}};
    }

    public String[][] getAllNameEntries() {
        return new String[][]{{"4", "", "", "", this.fontName}};
    }

    public String[][] getFamilyFontName() {
        return getFullFontName();
    }

    static IntHashtable createMetric(String s) {
        IntHashtable h = new IntHashtable();
        StringTokenizer tk = new StringTokenizer(s);
        while (tk.hasMoreTokens()) {
            int n1 = Integer.parseInt(tk.nextToken());
            h.put(n1, Integer.parseInt(tk.nextToken()));
        }
        return h;
    }

    static String convertToHCIDMetrics(int[] keys, IntHashtable h) {
        if (keys.length == 0) {
            return null;
        }
        int lastCid = 0;
        int lastValue = 0;
        int start;
        for (start = 0; start < keys.length; start++) {
            lastCid = keys[start];
            lastValue = h.get(lastCid);
            if (lastValue != 0) {
                start++;
                break;
            }
        }
        if (lastValue == 0) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        buf.append(lastCid);
        int state = 0;
        for (int k = start; k < keys.length; k++) {
            int cid = keys[k];
            int value = h.get(cid);
            if (value != 0) {

                switch (state) {
                    case 0:
                        if (cid == lastCid + 1 && value == lastValue) {
                            state = 2;
                            break;
                        }
                        if (cid == lastCid + 1) {
                            state = 1;
                            buf.append('[').append(lastValue);
                            break;
                        }
                        buf.append('[').append(lastValue).append(']').append(cid);
                        break;

                    case 1:
                        if (cid == lastCid + 1 && value == lastValue) {
                            state = 2;
                            buf.append(']').append(lastCid);
                            break;
                        }
                        if (cid == lastCid + 1) {
                            buf.append(' ').append(lastValue);
                            break;
                        }
                        state = 0;
                        buf.append(' ').append(lastValue).append(']').append(cid);
                        break;

                    case 2:
                        if (cid != lastCid + 1 || value != lastValue) {
                            buf.append(' ').append(lastCid).append(' ').append(lastValue).append(' ').append(cid);
                            state = 0;
                        }
                        break;
                }

                lastValue = value;
                lastCid = cid;
            }
        }
        switch (state) {
            case 0:
                buf.append('[').append(lastValue).append("]]");
                break;

            case 1:
                buf.append(' ').append(lastValue).append("]]");
                break;

            case 2:
                buf.append(' ').append(lastCid).append(' ').append(lastValue).append(']');
                break;
        }

        return buf.toString();
    }

    static String convertToVCIDMetrics(int[] keys, IntHashtable v, IntHashtable h) {
        if (keys.length == 0) {
            return null;
        }
        int lastCid = 0;
        int lastValue = 0;
        int lastHValue = 0;
        int start;
        for (start = 0; start < keys.length; start++) {
            lastCid = keys[start];
            lastValue = v.get(lastCid);
            if (lastValue != 0) {
                start++;

                break;
            }
            lastHValue = h.get(lastCid);
        }
        if (lastValue == 0) {
            return null;
        }
        if (lastHValue == 0) {
            lastHValue = 1000;
        }
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        buf.append(lastCid);
        int state = 0;
        for (int k = start; k < keys.length; k++) {
            int cid = keys[k];
            int value = v.get(cid);
            if (value != 0) {

                int hValue = h.get(lastCid);
                if (hValue == 0) {
                    hValue = 1000;
                }
                switch (state) {
                    case 0:
                        if (cid == lastCid + 1 && value == lastValue && hValue == lastHValue) {
                            state = 2;
                            break;
                        }
                        buf.append(' ').append(lastCid).append(' ').append(-lastValue).append(' ').append(lastHValue / 2).append(' ').append(880).append(' ').append(cid);
                        break;

                    case 2:
                        if (cid != lastCid + 1 || value != lastValue || hValue != lastHValue) {
                            buf.append(' ').append(lastCid).append(' ').append(-lastValue).append(' ').append(lastHValue / 2).append(' ').append(880).append(' ').append(cid);
                            state = 0;
                        }
                        break;
                }

                lastValue = value;
                lastCid = cid;
                lastHValue = hValue;
            }
        }
        buf.append(' ').append(lastCid).append(' ').append(-lastValue).append(' ').append(lastHValue / 2).append(' ').append(880).append(" ]");
        return buf.toString();
    }

    private static HashMap<String, Object> readFontProperties(String name) throws IOException {
        name = name + ".properties";
        InputStream is = StreamUtil.getResourceStream("com/itextpdf/text/pdf/fonts/cmaps/" + name);
        Properties p = new Properties();
        p.load(is);
        is.close();
        IntHashtable W = createMetric(p.getProperty("W"));
        p.remove("W");
        IntHashtable W2 = createMetric(p.getProperty("W2"));
        p.remove("W2");
        HashMap<String, Object> map = new HashMap<String, Object>();
        for (Enumeration<Object> e = p.keys(); e.hasMoreElements();) {
            Object obj = e.nextElement();
            map.put((String) obj, p.getProperty((String) obj));
        }
        map.put("W", W);
        map.put("W2", W2);
        return map;
    }

    public int getUnicodeEquivalent(int c) {
        if (this.cidDirect) {
            if (c == 32767) {
                return 10;
            }
            return this.cidUni.lookup(c);
        }
        return c;
    }

    public int getCidCode(int c) {
        if (this.cidDirect) {
            return c;
        }
        return this.uniCid.lookup(c);
    }

    public boolean hasKernPairs() {
        return false;
    }

    public boolean charExists(int c) {
        if (this.cidDirect) {
            return true;
        }
        return ((this.cidByte.lookup(this.uniCid.lookup(c))).length > 0);
    }

    public boolean setCharAdvance(int c, int advance) {
        return false;
    }

    public void setPostscriptFontName(String name) {
        this.fontName = name;
    }

    public boolean setKerning(int char1, int char2, int kern) {
        return false;
    }

    public int[] getCharBBox(int c) {
        return null;
    }

    protected int[] getRawCharBBox(int c, String name) {
        return null;
    }

    public byte[] convertToBytes(String text) {
        if (this.cidDirect) {
            return super.convertToBytes(text);
        }
        try {
            if (text.length() == 1) {
                return convertToBytes(text.charAt(0));
            }
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            for (int k = 0; k < text.length(); k++) {
                int val;
                if (Utilities.isSurrogatePair(text, k)) {
                    val = Utilities.convertToUtf32(text, k);
                    k++;
                } else {

                    val = text.charAt(k);
                }
                bout.write(convertToBytes(val));
            }
            return bout.toByteArray();
        } catch (Exception ex) {
            throw new ExceptionConverter(ex);
        }
    }

    byte[] convertToBytes(int char1) {
        if (this.cidDirect) {
            return super.convertToBytes(char1);
        }
        return this.cidByte.lookup(this.uniCid.lookup(char1));
    }

    public boolean isIdentity() {
        return this.cidDirect;
    }
}
