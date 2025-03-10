package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.ExceptionConverter;
import esign.text.Utilities;
import esign.text.io.RandomAccessSourceFactory;
import esign.text.pdf.fonts.cmaps.AbstractCMap;
import esign.text.pdf.fonts.cmaps.CMapParserEx;
import esign.text.pdf.fonts.cmaps.CMapToUnicode;
import esign.text.pdf.fonts.cmaps.CidLocation;
import esign.text.pdf.fonts.cmaps.CidLocationFromByte;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DocumentFont
        extends BaseFont {

    private HashMap<Integer, int[]> metrics = (HashMap) new HashMap<Integer, int[]>();
    private String fontName;
    private PRIndirectReference refFont;
    private PdfDictionary font;
    private IntHashtable uni2byte = new IntHashtable();
    private IntHashtable byte2uni = new IntHashtable();
    private IntHashtable diffmap;
    private float ascender = 800.0F;
    private float capHeight = 700.0F;
    private float descender = -200.0F;
    private float italicAngle = 0.0F;
    private float fontWeight = 0.0F;
    private float llx = -50.0F;
    private float lly = -200.0F;
    private float urx = 100.0F;
    private float ury = 900.0F;
    protected boolean isType0 = false;
    protected int defaultWidth = 1000;

    private IntHashtable hMetrics;

    protected String cjkEncoding;
    protected String uniMap;
    private BaseFont cjkMirror;
    private static final int[] stdEnc = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 33, 34, 35, 36, 37, 38, 8217, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 8216, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 161, 162, 163, 8260, 165, 402, 167, 164, 39, 8220, 171, 8249, 8250, 64257, 64258, 0, 8211, 8224, 8225, 183, 0, 182, 8226, 8218, 8222, 8221, 187, 8230, 8240, 0, 191, 0, 96, 180, 710, 732, 175, 728, 729, 168, 0, 730, 184, 0, 733, 731, 711, 8212, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 198, 0, 170, 0, 0, 0, 0, 321, 216, 338, 186, 0, 0, 0, 0, 0, 230, 0, 0, 0, 305, 0, 0, 322, 248, 339, 223, 0, 0, 0, 0};

    DocumentFont(PdfDictionary font) {
        this.refFont = null;
        this.font = font;
        init();
    }

    DocumentFont(PRIndirectReference refFont) {
        this.refFont = refFont;
        this.font = (PdfDictionary) PdfReader.getPdfObject(refFont);
        init();
    }

    DocumentFont(PRIndirectReference refFont, PdfDictionary drEncoding) {
        this.refFont = refFont;
        this.font = (PdfDictionary) PdfReader.getPdfObject(refFont);
        if (this.font.get(PdfName.ENCODING) == null && drEncoding != null) {
            for (PdfName key : drEncoding.getKeys()) {
                this.font.put(PdfName.ENCODING, drEncoding.get(key));
            }
        }
        init();
    }

    public PdfDictionary getFontDictionary() {
        return this.font;
    }

    private void init() {
        this.encoding = "";
        this.fontSpecific = false;
        this.fontType = 4;
        PdfName baseFont = this.font.getAsName(PdfName.BASEFONT);
        this.fontName = (baseFont != null) ? PdfName.decodeName(baseFont.toString()) : "Unspecified Font Name";
        PdfName subType = this.font.getAsName(PdfName.SUBTYPE);
        if (PdfName.TYPE1.equals(subType) || PdfName.TRUETYPE.equals(subType)) {
            doType1TT();
        } else if (PdfName.TYPE3.equals(subType)) {

            fillEncoding((PdfName) null);
            fillDiffMap(this.font.getAsDict(PdfName.ENCODING), (CMapToUnicode) null);
            fillWidths();
        } else {

            PdfName encodingName = this.font.getAsName(PdfName.ENCODING);
            if (encodingName != null) {
                String enc = PdfName.decodeName(encodingName.toString());
                String ffontname = CJKFont.GetCompatibleFont(enc);
                if (ffontname != null) {
                    try {
                        this.cjkMirror = BaseFont.createFont(ffontname, enc, false);
                    } catch (Exception e) {
                        throw new ExceptionConverter(e);
                    }
                    this.cjkEncoding = enc;
                    this.uniMap = ((CJKFont) this.cjkMirror).getUniMap();
                }
                if (PdfName.TYPE0.equals(subType)) {
                    this.isType0 = true;
                    if (!enc.equals("Identity-H") && this.cjkMirror != null) {
                        PdfArray df = (PdfArray) PdfReader.getPdfObjectRelease(this.font.get(PdfName.DESCENDANTFONTS));
                        PdfDictionary cidft = (PdfDictionary) PdfReader.getPdfObjectRelease(df.getPdfObject(0));
                        PdfNumber dwo = (PdfNumber) PdfReader.getPdfObjectRelease(cidft.get(PdfName.DW));
                        if (dwo != null) {
                            this.defaultWidth = dwo.intValue();
                        }
                        this.hMetrics = readWidths((PdfArray) PdfReader.getPdfObjectRelease(cidft.get(PdfName.W)));

                        PdfDictionary fontDesc = (PdfDictionary) PdfReader.getPdfObjectRelease(cidft.get(PdfName.FONTDESCRIPTOR));
                        fillFontDesc(fontDesc);
                    } else {
                        processType0(this.font);
                    }
                }
            }
        }
    }

    private void processType0(PdfDictionary font) {
        try {
            PdfObject toUniObject = PdfReader.getPdfObjectRelease(font.get(PdfName.TOUNICODE));
            PdfArray df = (PdfArray) PdfReader.getPdfObjectRelease(font.get(PdfName.DESCENDANTFONTS));
            PdfDictionary cidft = (PdfDictionary) PdfReader.getPdfObjectRelease(df.getPdfObject(0));
            PdfNumber dwo = (PdfNumber) PdfReader.getPdfObjectRelease(cidft.get(PdfName.DW));
            int dw = 1000;
            if (dwo != null) {
                dw = dwo.intValue();
            }
            IntHashtable widths = readWidths((PdfArray) PdfReader.getPdfObjectRelease(cidft.get(PdfName.W)));
            PdfDictionary fontDesc = (PdfDictionary) PdfReader.getPdfObjectRelease(cidft.get(PdfName.FONTDESCRIPTOR));
            fillFontDesc(fontDesc);
            if (toUniObject instanceof PRStream) {
                fillMetrics(PdfReader.getStreamBytes((PRStream) toUniObject), widths, dw);
            } else if ((new PdfName("Identity-H")).equals(toUniObject)) {
                fillMetricsIdentity(widths, dw);
            }
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    private IntHashtable readWidths(PdfArray ws) {
        IntHashtable hh = new IntHashtable();
        if (ws == null) {
            return hh;
        }
        for (int k = 0; k < ws.size(); k++) {
            int c1 = ((PdfNumber) PdfReader.getPdfObjectRelease(ws.getPdfObject(k))).intValue();
            PdfObject obj = PdfReader.getPdfObjectRelease(ws.getPdfObject(++k));
            if (obj.isArray()) {
                PdfArray a2 = (PdfArray) obj;
                for (int j = 0; j < a2.size(); j++) {
                    int c2 = ((PdfNumber) PdfReader.getPdfObjectRelease(a2.getPdfObject(j))).intValue();
                    hh.put(c1++, c2);
                }
            } else {

                int c2 = ((PdfNumber) obj).intValue();
                int w = ((PdfNumber) PdfReader.getPdfObjectRelease(ws.getPdfObject(++k))).intValue();
                for (; c1 <= c2; c1++) {
                    hh.put(c1, w);
                }
            }
        }
        return hh;
    }

    private String decodeString(PdfString ps) {
        if (ps.isHexWriting()) {
            return PdfEncodings.convertToString(ps.getBytes(), "UnicodeBigUnmarked");
        }
        return ps.toUnicodeString();
    }

    private void fillMetricsIdentity(IntHashtable widths, int dw) {
        for (int i = 0; i < 65536; i++) {
            int w = dw;
            if (widths.containsKey(i)) {
                w = widths.get(i);
            }
            this.metrics.put(Integer.valueOf(i), new int[]{i, w});
        }
    }

    private void fillMetrics(byte[] touni, IntHashtable widths, int dw) {
        try {
            PdfContentParser ps = new PdfContentParser(new PRTokeniser(new RandomAccessFileOrArray((new RandomAccessSourceFactory()).createSource(touni))));
            PdfObject ob = null;
            boolean notFound = true;
            int nestLevel = 0;
            int maxExc = 50;
            label69:
            while (notFound || nestLevel > 0) {
                try {
                    ob = ps.readPRObject();
                } catch (Exception ex) {
                    if (--maxExc < 0) {
                        break;
                    }
                    continue;
                }
                if (ob == null) {
                    break;
                }
                if (ob.type() == 200) {
                    if (ob.toString().equals("begin")) {
                        notFound = false;
                        nestLevel++;
                        continue;
                    }
                    if (ob.toString().equals("end")) {
                        nestLevel--;
                        continue;
                    }
                    if (ob.toString().equals("beginbfchar")) {
                        while (true) {
                            PdfObject nx = ps.readPRObject();
                            if (nx.toString().equals("endbfchar")) {
                                continue label69;
                            }
                            String cid = decodeString((PdfString) nx);
                            String uni = decodeString((PdfString) ps.readPRObject());
                            if (uni.length() == 1) {
                                int cidc = cid.charAt(0);
                                int unic = uni.charAt(uni.length() - 1);
                                int w = dw;
                                if (widths.containsKey(cidc)) {
                                    w = widths.get(cidc);
                                }
                                this.metrics.put(Integer.valueOf(unic), new int[]{cidc, w});
                            }
                        }
                    }
                    if (ob.toString().equals("beginbfrange")) {
                        while (true) {
                            PdfObject nx = ps.readPRObject();
                            if (nx.toString().equals("endbfrange")) {
                                continue label69;
                            }
                            String cid1 = decodeString((PdfString) nx);
                            String cid2 = decodeString((PdfString) ps.readPRObject());
                            int cid1c = cid1.charAt(0);
                            int cid2c = cid2.charAt(0);
                            PdfObject ob2 = ps.readPRObject();
                            if (ob2.isString()) {
                                String uni = decodeString((PdfString) ob2);
                                if (uni.length() == 1) {
                                    int unic = uni.charAt(uni.length() - 1);
                                    for (; cid1c <= cid2c; cid1c++, unic++) {
                                        int w = dw;
                                        if (widths.containsKey(cid1c)) {
                                            w = widths.get(cid1c);
                                        }
                                        this.metrics.put(Integer.valueOf(unic), new int[]{cid1c, w});
                                    }
                                }
                                continue;
                            }
                            PdfArray a = (PdfArray) ob2;
                            for (int j = 0; j < a.size(); j++, cid1c++) {
                                String uni = decodeString(a.getAsString(j));
                                if (uni.length() == 1) {
                                    int unic = uni.charAt(uni.length() - 1);
                                    int w = dw;
                                    if (widths.containsKey(cid1c)) {
                                        w = widths.get(cid1c);
                                    }
                                    this.metrics.put(Integer.valueOf(unic), new int[]{cid1c, w});
                                }

                            }

                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    private void doType1TT() {
        CMapToUnicode toUnicode = null;
        PdfObject enc = PdfReader.getPdfObject(this.font.get(PdfName.ENCODING));
        if (enc == null) {
            PdfName baseFont = this.font.getAsName(PdfName.BASEFONT);
            if (BuiltinFonts14.containsKey(this.fontName) && (PdfName.SYMBOL
                    .equals(baseFont) || PdfName.ZAPFDINGBATS.equals(baseFont))) {
                fillEncoding(baseFont);
            } else {
                fillEncoding((PdfName) null);
            }
            try {
                toUnicode = processToUnicode();
                if (toUnicode != null) {
                    Map<Integer, Integer> rm = toUnicode.createReverseMapping();
                    for (Map.Entry<Integer, Integer> kv : rm.entrySet()) {
                        this.uni2byte.put(((Integer) kv.getKey()).intValue(), ((Integer) kv.getValue()).intValue());
                        this.byte2uni.put(((Integer) kv.getValue()).intValue(), ((Integer) kv.getKey()).intValue());
                    }

                }
            } catch (Exception ex) {
                throw new ExceptionConverter(ex);
            }

        } else if (enc.isName()) {
            fillEncoding((PdfName) enc);
        } else if (enc.isDictionary()) {
            PdfDictionary encDic = (PdfDictionary) enc;
            enc = PdfReader.getPdfObject(encDic.get(PdfName.BASEENCODING));
            if (enc == null) {
                fillEncoding((PdfName) null);
            } else {
                fillEncoding((PdfName) enc);
            }
            fillDiffMap(encDic, toUnicode);
        }

        if (BuiltinFonts14.containsKey(this.fontName)) {
            BaseFont bf;
            try {
                bf = BaseFont.createFont(this.fontName, "Cp1252", false);
            } catch (Exception exception) {
                throw new ExceptionConverter(exception);
            }
            int[] e = this.uni2byte.toOrderedKeys();
            int k;
            for (k = 0; k < e.length; k++) {
                int n = this.uni2byte.get(e[k]);
                this.widths[n] = bf.getRawWidth(n, GlyphList.unicodeToName(e[k]));
            }
            if (this.diffmap != null) {
                e = this.diffmap.toOrderedKeys();
                for (k = 0; k < e.length; k++) {
                    int n = this.diffmap.get(e[k]);
                    this.widths[n] = bf.getRawWidth(n, GlyphList.unicodeToName(e[k]));
                }
                this.diffmap = null;
            }
            this.ascender = bf.getFontDescriptor(1, 1000.0F);
            this.capHeight = bf.getFontDescriptor(2, 1000.0F);
            this.descender = bf.getFontDescriptor(3, 1000.0F);
            this.italicAngle = bf.getFontDescriptor(4, 1000.0F);
            this.fontWeight = bf.getFontDescriptor(23, 1000.0F);
            this.llx = bf.getFontDescriptor(5, 1000.0F);
            this.lly = bf.getFontDescriptor(6, 1000.0F);
            this.urx = bf.getFontDescriptor(7, 1000.0F);
            this.ury = bf.getFontDescriptor(8, 1000.0F);
        }
        fillWidths();
        fillFontDesc(this.font.getAsDict(PdfName.FONTDESCRIPTOR));
    }

    private void fillWidths() {
        PdfArray newWidths = this.font.getAsArray(PdfName.WIDTHS);
        PdfNumber first = this.font.getAsNumber(PdfName.FIRSTCHAR);
        PdfNumber last = this.font.getAsNumber(PdfName.LASTCHAR);
        if (first != null && last != null && newWidths != null) {
            int f = first.intValue();
            int nSize = f + newWidths.size();
            if (this.widths.length < nSize) {
                int[] tmp = new int[nSize];
                System.arraycopy(this.widths, 0, tmp, 0, f);
                this.widths = tmp;
            }
            for (int k = 0; k < newWidths.size(); k++) {
                this.widths[f + k] = newWidths.getAsNumber(k).intValue();
            }
        }
    }

    private void fillDiffMap(PdfDictionary encDic, CMapToUnicode toUnicode) {
        PdfArray diffs = encDic.getAsArray(PdfName.DIFFERENCES);
        if (diffs != null) {
            this.diffmap = new IntHashtable();
            int currentNumber = 0;
            for (int k = 0; k < diffs.size(); k++) {
                PdfObject obj = diffs.getPdfObject(k);
                if (obj.isNumber()) {
                    currentNumber = ((PdfNumber) obj).intValue();
                } else {
                    int[] c = GlyphList.nameToUnicode(PdfName.decodeName(((PdfName) obj).toString()));
                    if (c != null && c.length > 0) {
                        this.uni2byte.put(c[0], currentNumber);
                        this.byte2uni.put(currentNumber, c[0]);
                        this.diffmap.put(c[0], currentNumber);
                    } else {

                        if (toUnicode == null) {
                            toUnicode = processToUnicode();
                            if (toUnicode == null) {
                                toUnicode = new CMapToUnicode();
                            }
                        }
                        String unicode = toUnicode.lookup(new byte[]{(byte) currentNumber}, 0, 1);
                        if (unicode != null && unicode.length() == 1) {
                            this.uni2byte.put(unicode.charAt(0), currentNumber);
                            this.byte2uni.put(currentNumber, unicode.charAt(0));
                            this.diffmap.put(unicode.charAt(0), currentNumber);
                        }
                    }
                    currentNumber++;
                }
            }
        }
    }

    private CMapToUnicode processToUnicode() {
        CMapToUnicode cmapRet = null;
        PdfObject toUni = PdfReader.getPdfObjectRelease(this.font.get(PdfName.TOUNICODE));
        if (toUni instanceof PRStream) {
            try {
                byte[] touni = PdfReader.getStreamBytes((PRStream) toUni);
                CidLocationFromByte lb = new CidLocationFromByte(touni);
                cmapRet = new CMapToUnicode();
                CMapParserEx.parseCid("", (AbstractCMap) cmapRet, (CidLocation) lb);
            } catch (Exception e) {
                cmapRet = null;
            }
        }
        return cmapRet;
    }

    private void fillFontDesc(PdfDictionary fontDesc) {
        if (fontDesc == null) {
            return;
        }
        PdfNumber v = fontDesc.getAsNumber(PdfName.ASCENT);
        if (v != null) {
            this.ascender = v.floatValue();
        }
        v = fontDesc.getAsNumber(PdfName.CAPHEIGHT);
        if (v != null) {
            this.capHeight = v.floatValue();
        }
        v = fontDesc.getAsNumber(PdfName.DESCENT);
        if (v != null) {
            this.descender = v.floatValue();
        }
        v = fontDesc.getAsNumber(PdfName.ITALICANGLE);
        if (v != null) {
            this.italicAngle = v.floatValue();
        }
        v = fontDesc.getAsNumber(PdfName.FONTWEIGHT);
        if (v != null) {
            this.fontWeight = v.floatValue();
        }
        PdfArray bbox = fontDesc.getAsArray(PdfName.FONTBBOX);
        if (bbox != null) {
            this.llx = bbox.getAsNumber(0).floatValue();
            this.lly = bbox.getAsNumber(1).floatValue();
            this.urx = bbox.getAsNumber(2).floatValue();
            this.ury = bbox.getAsNumber(3).floatValue();
            if (this.llx > this.urx) {
                float t = this.llx;
                this.llx = this.urx;
                this.urx = t;
            }
            if (this.lly > this.ury) {
                float t = this.lly;
                this.lly = this.ury;
                this.ury = t;
            }
        }
        float maxAscent = Math.max(this.ury, this.ascender);
        float minDescent = Math.min(this.lly, this.descender);
        this.ascender = maxAscent * 1000.0F / (maxAscent - minDescent);
        this.descender = minDescent * 1000.0F / (maxAscent - minDescent);
    }

    private void fillEncoding(PdfName encoding) {
        if (encoding == null && isSymbolic()) {
            for (int k = 0; k < 256; k++) {
                this.uni2byte.put(k, k);
                this.byte2uni.put(k, k);
            }
        } else if (PdfName.MAC_ROMAN_ENCODING.equals(encoding) || PdfName.WIN_ANSI_ENCODING.equals(encoding) || PdfName.SYMBOL
                .equals(encoding) || PdfName.ZAPFDINGBATS.equals(encoding)) {
            byte[] b = new byte[256];
            for (int k = 0; k < 256; k++) {
                b[k] = (byte) k;
            }
            String enc = "Cp1252";
            if (PdfName.MAC_ROMAN_ENCODING.equals(encoding)) {
                enc = "MacRoman";
            } else if (PdfName.SYMBOL.equals(encoding)) {
                enc = "Symbol";
            } else if (PdfName.ZAPFDINGBATS.equals(encoding)) {
                enc = "ZapfDingbats";
            }
            String cv = PdfEncodings.convertToString(b, enc);
            char[] arr = cv.toCharArray();
            for (int i = 0; i < 256; i++) {
                this.uni2byte.put(arr[i], i);
                this.byte2uni.put(i, arr[i]);
            }
            this.encoding = enc;
        } else {

            for (int k = 0; k < 256; k++) {
                this.uni2byte.put(stdEnc[k], k);
                this.byte2uni.put(k, stdEnc[k]);
            }
        }
    }

    public String[][] getFamilyFontName() {
        return getFullFontName();
    }

    public float getFontDescriptor(int key, float fontSize) {
        if (this.cjkMirror != null) {
            return this.cjkMirror.getFontDescriptor(key, fontSize);
        }
        switch (key) {
            case 1:
            case 9:
                return this.ascender * fontSize / 1000.0F;
            case 2:
                return this.capHeight * fontSize / 1000.0F;
            case 3:
            case 10:
                return this.descender * fontSize / 1000.0F;
            case 4:
                return this.italicAngle;
            case 5:
                return this.llx * fontSize / 1000.0F;
            case 6:
                return this.lly * fontSize / 1000.0F;
            case 7:
                return this.urx * fontSize / 1000.0F;
            case 8:
                return this.ury * fontSize / 1000.0F;
            case 11:
                return 0.0F;
            case 12:
                return (this.urx - this.llx) * fontSize / 1000.0F;
            case 23:
                return this.fontWeight * fontSize / 1000.0F;
        }
        return 0.0F;
    }

    public String[][] getFullFontName() {
        return new String[][]{{"", "", "", this.fontName}};
    }

    public String[][] getAllNameEntries() {
        return new String[][]{{"4", "", "", "", this.fontName}};
    }

    public int getKerning(int char1, int char2) {
        return 0;
    }

    public String getPostscriptFontName() {
        return this.fontName;
    }

    int getRawWidth(int c, String name) {
        return 0;
    }

    public boolean hasKernPairs() {
        return false;
    }

    void writeFont(PdfWriter writer, PdfIndirectReference ref, Object[] params) throws DocumentException, IOException {
    }

    public PdfStream getFullFontStream() {
        return null;
    }

    public int getWidth(int char1) {
        if (this.isType0) {
            if (this.hMetrics != null && this.cjkMirror != null && !this.cjkMirror.isVertical()) {
                int c = this.cjkMirror.getCidCode(char1);
                int v = this.hMetrics.get(c);
                if (v > 0) {
                    return v;
                }
                return this.defaultWidth;
            }
            int[] ws = this.metrics.get(Integer.valueOf(char1));
            if (ws != null) {
                return ws[1];
            }
            return 0;
        }

        if (this.cjkMirror != null) {
            return this.cjkMirror.getWidth(char1);
        }
        return super.getWidth(char1);
    }

    public int getWidth(String text) {
        if (this.isType0) {
            int total = 0;
            if (this.hMetrics != null && this.cjkMirror != null && !this.cjkMirror.isVertical()) {
                if (((CJKFont) this.cjkMirror).isIdentity()) {
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
            } else {
                char[] chars = text.toCharArray();
                int len = chars.length;
                for (int k = 0; k < len; k++) {
                    int[] ws = this.metrics.get(Integer.valueOf(chars[k]));
                    if (ws != null) {
                        total += ws[1];
                    }
                }
            }
            return total;
        }
        if (this.cjkMirror != null) {
            return this.cjkMirror.getWidth(text);
        }
        return super.getWidth(text);
    }

    public byte[] convertToBytes(String text) {
        if (this.cjkMirror != null) {
            return this.cjkMirror.convertToBytes(text);
        }
        if (this.isType0) {
            char[] chars = text.toCharArray();
            int len = chars.length;
            byte[] arrayOfByte1 = new byte[len * 2];
            int bptr = 0;
            for (int i = 0; i < len; i++) {
                int[] ws = this.metrics.get(Integer.valueOf(chars[i]));
                if (ws != null) {
                    int g = ws[0];
                    arrayOfByte1[bptr++] = (byte) (g / 256);
                    arrayOfByte1[bptr++] = (byte) g;
                }
            }
            if (bptr == arrayOfByte1.length) {
                return arrayOfByte1;
            }
            byte[] nb = new byte[bptr];
            System.arraycopy(arrayOfByte1, 0, nb, 0, bptr);
            return nb;
        }

        char[] cc = text.toCharArray();
        byte[] b = new byte[cc.length];
        int ptr = 0;
        for (int k = 0; k < cc.length; k++) {
            if (this.uni2byte.containsKey(cc[k])) {
                b[ptr++] = (byte) this.uni2byte.get(cc[k]);
            }
        }
        if (ptr == b.length) {
            return b;
        }
        byte[] b2 = new byte[ptr];
        System.arraycopy(b, 0, b2, 0, ptr);
        return b2;
    }

    byte[] convertToBytes(int char1) {
        if (this.cjkMirror != null) {
            return this.cjkMirror.convertToBytes(char1);
        }
        if (this.isType0) {
            int[] ws = this.metrics.get(Integer.valueOf(char1));
            if (ws != null) {
                int g = ws[0];
                return new byte[]{(byte) (g / 256), (byte) g};
            }

            return new byte[0];
        }

        if (this.uni2byte.containsKey(char1)) {
            return new byte[]{(byte) this.uni2byte.get(char1)};
        }
        return new byte[0];
    }

    PdfIndirectReference getIndirectReference() {
        if (this.refFont == null) {
            throw new IllegalArgumentException("Font reuse not allowed with direct font objects.");
        }
        return this.refFont;
    }

    public boolean charExists(int c) {
        if (this.cjkMirror != null) {
            return this.cjkMirror.charExists(c);
        }
        if (this.isType0) {
            return this.metrics.containsKey(Integer.valueOf(c));
        }

        return super.charExists(c);
    }

    public double[] getFontMatrix() {
        if (this.font.getAsArray(PdfName.FONTMATRIX) != null) {
            return this.font.getAsArray(PdfName.FONTMATRIX).asDoubleArray();
        }
        return DEFAULT_FONT_MATRIX;
    }

    public void setPostscriptFontName(String name) {
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

    public boolean isVertical() {
        if (this.cjkMirror != null) {
            return this.cjkMirror.isVertical();
        }
        return super.isVertical();
    }

    IntHashtable getUni2Byte() {
        return this.uni2byte;
    }

    IntHashtable getByte2Uni() {
        return this.byte2uni;
    }

    IntHashtable getDiffmap() {
        return this.diffmap;
    }

    boolean isSymbolic() {
        PdfDictionary fontDescriptor = this.font.getAsDict(PdfName.FONTDESCRIPTOR);
        if (fontDescriptor == null) {
            return false;
        }
        PdfNumber flags = fontDescriptor.getAsNumber(PdfName.FLAGS);
        if (flags == null) {
            return false;
        }
        return ((flags.intValue() & 0x4) != 0);
    }
}
