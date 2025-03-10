package esign.text.pdf;

import esign.text.DocumentException;
import esign.text.Utilities;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.fonts.otf.GlyphSubstitutionTableReader;
import esign.text.pdf.fonts.otf.Language;
import esign.text.pdf.languages.ArabicLigaturizer;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class TrueTypeFontUnicode
        extends TrueTypeFont
        implements Comparator<int[]> {

    private static final List<Language> SUPPORTED_LANGUAGES_FOR_OTF = Arrays.asList(new Language[]{Language.BENGALI});

    private Map<String, Glyph> glyphSubstitutionMap;

    private Language supportedLanguage;

    TrueTypeFontUnicode(String ttFile, String enc, boolean emb, byte[] ttfAfm, boolean forceRead) throws DocumentException, IOException {
        String nameBase = getBaseName(ttFile);
        String ttcName = getTTCName(nameBase);
        if (nameBase.length() < ttFile.length()) {
            this.style = ttFile.substring(nameBase.length());
        }
        this.encoding = enc;
        this.embedded = emb;
        this.fileName = ttcName;
        this.ttcIndex = "";
        if (ttcName.length() < nameBase.length()) {
            this.ttcIndex = nameBase.substring(ttcName.length() + 1);
        }
        this.fontType = 3;
        if ((this.fileName.toLowerCase().endsWith(".ttf") || this.fileName.toLowerCase().endsWith(".otf") || this.fileName.toLowerCase().endsWith(".ttc")) && (enc.equals("Identity-H") || enc.equals("Identity-V")) && emb) {
            process(ttfAfm, forceRead);
            if (this.os_2.fsType == 2) {
                throw new DocumentException(MessageLocalization.getComposedMessage("1.cannot.be.embedded.due.to.licensing.restrictions", new Object[]{this.fileName + this.style}));
            }
            if ((this.cmap31 == null && !this.fontSpecific) || (this.cmap10 == null && this.fontSpecific)) {
                this.directTextToByte = true;
            }
            if (this.fontSpecific) {
                this.fontSpecific = false;
                String tempEncoding = this.encoding;
                this.encoding = "";
                createEncoding();
                this.encoding = tempEncoding;
                this.fontSpecific = true;
            }
        } else {

            throw new DocumentException(MessageLocalization.getComposedMessage("1.2.is.not.a.ttf.font.file", new Object[]{this.fileName, this.style}));
        }
        this.vertical = enc.endsWith("V");
    }

    void process(byte[] ttfAfm, boolean preload) throws DocumentException, IOException {
        super.process(ttfAfm, preload);
    }

    public int getWidth(int char1) {
        if (this.vertical) {
            return 1000;
        }
        if (this.fontSpecific) {
            if ((char1 & 0xFF00) == 0 || (char1 & 0xFF00) == 61440) {
                return getRawWidth(char1 & 0xFF, (String) null);
            }
            return 0;
        }

        return getRawWidth(char1, this.encoding);
    }

    public int getWidth(String text) {
        if (this.vertical) {
            return text.length() * 1000;
        }
        int total = 0;
        if (this.fontSpecific) {
            char[] cc = text.toCharArray();
            int len = cc.length;
            for (int k = 0; k < len; k++) {
                char c = cc[k];
                if ((c & 0xFF00) == 0 || (c & 0xFF00) == 61440) {
                    total += getRawWidth(c & 0xFF, (String) null);
                }
            }
        } else {
            int len = text.length();
            for (int k = 0; k < len; k++) {
                if (Utilities.isSurrogatePair(text, k)) {
                    total += getRawWidth(Utilities.convertToUtf32(text, k), this.encoding);
                    k++;
                } else {

                    total += getRawWidth(text.charAt(k), this.encoding);
                }
            }
        }
        return total;
    }

    public PdfStream getToUnicode(Object[] metrics) {
        if (metrics.length == 0) {
            return null;
        }
        StringBuffer buf = new StringBuffer("/CIDInit /ProcSet findresource begin\n12 dict begin\nbegincmap\n/CIDSystemInfo\n<< /Registry (TTX+0)\n/Ordering (T42UV)\n/Supplement 0\n>> def\n/CMapName /TTX+0 def\n/CMapType 2 def\n1 begincodespacerange\n<0000><FFFF>\nendcodespacerange\n");

        int size = 0;
        for (int k = 0; k < metrics.length; k++) {
            if (size == 0) {
                if (k != 0) {
                    buf.append("endbfrange\n");
                }
                size = Math.min(100, metrics.length - k);
                buf.append(size).append(" beginbfrange\n");
            }
            size--;
            int[] metric = (int[]) metrics[k];
            String fromTo = toHex(metric[0]);
            buf.append(fromTo).append(fromTo).append(toHex(metric[2])).append('\n');
        }
        buf.append("endbfrange\nendcmap\nCMapName currentdict /CMap defineresource pop\nend end\n");

        String s = buf.toString();
        PdfStream stream = new PdfStream(PdfEncodings.convertToBytes(s, (String) null));
        stream.flateCompress(this.compressionLevel);
        return stream;
    }

    private static String toHex4(int n) {
        String s = "0000" + Integer.toHexString(n);
        return s.substring(s.length() - 4);
    }

    static String toHex(int n) {
        if (n < 65536) {
            return "<" + toHex4(n) + ">";
        }
        n -= 65536;
        int high = n / 1024 + 55296;
        int low = n % 1024 + 56320;
        return "[<" + toHex4(high) + toHex4(low) + ">]";
    }

    public PdfDictionary getCIDFontType2(PdfIndirectReference fontDescriptor, String subsetPrefix, Object[] metrics) {
        PdfDictionary dic = new PdfDictionary(PdfName.FONT);

        if (this.cff) {
            dic.put(PdfName.SUBTYPE, PdfName.CIDFONTTYPE0);
            dic.put(PdfName.BASEFONT, new PdfName(subsetPrefix + this.fontName + "-" + this.encoding));
        } else {

            dic.put(PdfName.SUBTYPE, PdfName.CIDFONTTYPE2);
            dic.put(PdfName.BASEFONT, new PdfName(subsetPrefix + this.fontName));
        }
        dic.put(PdfName.FONTDESCRIPTOR, fontDescriptor);
        if (!this.cff) {
            dic.put(PdfName.CIDTOGIDMAP, PdfName.IDENTITY);
        }
        PdfDictionary cdic = new PdfDictionary();
        cdic.put(PdfName.REGISTRY, new PdfString("Adobe"));
        cdic.put(PdfName.ORDERING, new PdfString("Identity"));
        cdic.put(PdfName.SUPPLEMENT, new PdfNumber(0));
        dic.put(PdfName.CIDSYSTEMINFO, cdic);
        if (!this.vertical) {
            dic.put(PdfName.DW, new PdfNumber(1000));
            StringBuffer buf = new StringBuffer("[");
            int lastNumber = -10;
            boolean firstTime = true;
            for (int k = 0; k < metrics.length; k++) {
                int[] metric = (int[]) metrics[k];
                if (metric[1] != 1000) {

                    int m = metric[0];
                    if (m == lastNumber + 1) {
                        buf.append(' ').append(metric[1]);
                    } else {

                        if (!firstTime) {
                            buf.append(']');
                        }
                        firstTime = false;
                        buf.append(m).append('[').append(metric[1]);
                    }
                    lastNumber = m;
                }
            }
            if (buf.length() > 1) {
                buf.append("]]");
                dic.put(PdfName.W, new PdfLiteral(buf.toString()));
            }
        }
        return dic;
    }

    public PdfDictionary getFontBaseType(PdfIndirectReference descendant, String subsetPrefix, PdfIndirectReference toUnicode) {
        PdfDictionary dic = new PdfDictionary(PdfName.FONT);

        dic.put(PdfName.SUBTYPE, PdfName.TYPE0);

        if (this.cff) {
            dic.put(PdfName.BASEFONT, new PdfName(subsetPrefix + this.fontName + "-" + this.encoding));
        } else {

            dic.put(PdfName.BASEFONT, new PdfName(subsetPrefix + this.fontName));
        }
        dic.put(PdfName.ENCODING, new PdfName(this.encoding));
        dic.put(PdfName.DESCENDANTFONTS, new PdfArray(descendant));
        if (toUnicode != null) {
            dic.put(PdfName.TOUNICODE, toUnicode);
        }
        return dic;
    }

    public int GetCharFromGlyphId(int gid) {
        if (this.glyphIdToChar == null) {
            int[] g2 = new int[this.maxGlyphId];
            HashMap<Integer, int[]> map = null;
            if (this.cmapExt != null) {
                map = this.cmapExt;
            } else if (this.cmap31 != null) {
                map = this.cmap31;
            }
            if (map != null) {
                for (Map.Entry<Integer, int[]> entry : map.entrySet()) {
                    g2[((int[]) entry.getValue())[0]] = ((Integer) entry.getKey()).intValue();
                }
            }
            this.glyphIdToChar = g2;
        }
        return this.glyphIdToChar[gid];
    }

    public int compare(int[] o1, int[] o2) {
        int m1 = o1[0];
        int m2 = o2[0];
        if (m1 < m2) {
            return -1;
        }
        if (m1 == m2) {
            return 0;
        }
        return 1;
    }

    private static final byte[] rotbits = new byte[]{Byte.MIN_VALUE, 64, 32, 16, 8, 4, 2, 1};

    void writeFont(PdfWriter writer, PdfIndirectReference ref, Object[] params) throws DocumentException, IOException {
        writer.getTtfUnicodeWriter().writeFont(this, ref, params, rotbits);
    }

    public PdfStream getFullFontStream() throws IOException, DocumentException {
        if (this.cff) {
            return new BaseFont.StreamFont(readCffFont(), "CIDFontType0C", this.compressionLevel);
        }
        return super.getFullFontStream();
    }

    public byte[] convertToBytes(String text) {
        return null;
    }

    byte[] convertToBytes(int char1) {
        return null;
    }

    public int[] getMetricsTT(int c) {
        if (this.cmapExt != null) {
            return this.cmapExt.get(Integer.valueOf(c));
        }
        HashMap<Integer, int[]> map = null;
        if (this.fontSpecific) {
            map = this.cmap10;
        } else {
            map = this.cmap31;
        }
        if (map == null) {
            return null;
        }
        if (this.fontSpecific) {
            if ((c & 0xFFFFFF00) == 0 || (c & 0xFFFFFF00) == 61440) {
                return map.get(Integer.valueOf(c & 0xFF));
            }
            return null;
        }

        int[] result = map.get(Integer.valueOf(c));
        if (result == null) {
            Character ch = ArabicLigaturizer.getReverseMapping((char) c);
            if (ch != null) {
                result = map.get(Integer.valueOf(ch.charValue()));
            }
        }
        return result;
    }

    public boolean charExists(int c) {
        return (getMetricsTT(c) != null);
    }

    public boolean setCharAdvance(int c, int advance) {
        int[] m = getMetricsTT(c);
        if (m == null) {
            return false;
        }
        m[1] = advance;
        return true;
    }

    public int[] getCharBBox(int c) {
        if (this.bboxes == null) {
            return null;
        }
        int[] m = getMetricsTT(c);
        if (m == null) {
            return null;
        }
        return this.bboxes[m[0]];
    }

    protected Map<String, Glyph> getGlyphSubstitutionMap() {
        return this.glyphSubstitutionMap;
    }

    Language getSupportedLanguage() {
        return this.supportedLanguage;
    }

    private void readGsubTable() throws IOException {
        if (this.tables.get("GSUB") != null) {

            Map<Integer, Character> glyphToCharacterMap = new HashMap<Integer, Character>(this.cmap31.size());

            for (Integer charCode : this.cmap31.keySet()) {
                char c = (char) charCode.intValue();
                int glyphCode = ((int[]) this.cmap31.get(charCode))[0];
                glyphToCharacterMap.put(Integer.valueOf(glyphCode), Character.valueOf(c));
            }

            GlyphSubstitutionTableReader gsubReader = new GlyphSubstitutionTableReader(this.rf, ((int[]) this.tables.get("GSUB"))[0], glyphToCharacterMap, this.glyphWidthsByIndex);

            try {
                gsubReader.read();
                this.supportedLanguage = gsubReader.getSupportedLanguage();

                if (SUPPORTED_LANGUAGES_FOR_OTF.contains(this.supportedLanguage)) {
                    this.glyphSubstitutionMap = gsubReader.getGlyphSubstitutionMap();

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
