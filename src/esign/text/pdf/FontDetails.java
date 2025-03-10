package esign.text.pdf;

import esign.text.ExceptionConverter;
import esign.text.Utilities;
import esign.text.pdf.fonts.otf.Language;
import esign.text.pdf.languages.BanglaGlyphRepositioner;
import esign.text.pdf.languages.GlyphRepositioner;
import esign.text.pdf.languages.IndicCompositeCharacterComparator;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

class FontDetails {

    PdfIndirectReference indirectReference;
    PdfName fontName;
    BaseFont baseFont;
    TrueTypeFontUnicode ttu;
    CJKFont cjkFont;
    byte[] shortTag;
    HashMap<Integer, int[]> longTag;
    IntHashtable cjkTag;
    int fontType;
    boolean symbolic;
    protected boolean subset = true;

    FontDetails(PdfName fontName, PdfIndirectReference indirectReference, BaseFont baseFont) {
        this.fontName = fontName;
        this.indirectReference = indirectReference;
        this.baseFont = baseFont;
        this.fontType = baseFont.getFontType();
        switch (this.fontType) {
            case 0:
            case 1:
                this.shortTag = new byte[256];
                break;
            case 2:
                this.cjkTag = new IntHashtable();
                this.cjkFont = (CJKFont) baseFont;
                break;
            case 3:
                this.longTag = (HashMap) new HashMap<Integer, int[]>();
                this.ttu = (TrueTypeFontUnicode) baseFont;
                this.symbolic = baseFont.isFontSpecific();
                break;
        }
    }

    PdfIndirectReference getIndirectReference() {
        return this.indirectReference;
    }

    PdfName getFontName() {
        return this.fontName;
    }

    BaseFont getBaseFont() {
        return this.baseFont;
    }

    Object[] convertToBytesGid(String gids) {
        if (this.fontType != 3) {
            throw new IllegalArgumentException("GID require TT Unicode");
        }
        try {
            StringBuilder sb = new StringBuilder();
            int totalWidth = 0;
            for (char gid : gids.toCharArray()) {
                int width = this.ttu.getGlyphWidth(gid);
                totalWidth += width;
                int vchar = this.ttu.GetCharFromGlyphId(gid);
                if (vchar != 0) {
                    sb.append(Utilities.convertFromUtf32(vchar));
                }
                Integer gl = Integer.valueOf(gid);
                if (!this.longTag.containsKey(gl)) {
                    this.longTag.put(gl, new int[]{gid, width, vchar});
                }
            }
            return new Object[]{gids.getBytes("UnicodeBigUnmarked"), sb.toString(), Integer.valueOf(totalWidth)};
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    byte[] convertToBytes(String text) {
        int len, k;
        byte[] b = null;
        switch (this.fontType) {
            case 5:
                return this.baseFont.convertToBytes(text);
            case 0:
            case 1:
                b = this.baseFont.convertToBytes(text);
                len = b.length;
                for (k = 0; k < len; k++) {
                    this.shortTag[b[k] & 0xFF] = 1;
                }
                break;
            case 2:
                len = text.length();
                if (this.cjkFont.isIdentity()) {
                    for (k = 0; k < len; k++) {
                        this.cjkTag.put(text.charAt(k), 0);
                    }
                } else {

                    for (k = 0; k < len; k++) {
                        int val;
                        if (Utilities.isSurrogatePair(text, k)) {
                            val = Utilities.convertToUtf32(text, k);
                            k++;
                        } else {

                            val = text.charAt(k);
                        }
                        this.cjkTag.put(this.cjkFont.getCidCode(val), 0);
                    }
                }
                b = this.cjkFont.convertToBytes(text);
                break;

            case 4:
                b = this.baseFont.convertToBytes(text);
                break;

            case 3:
                try {
                    len = text.length();
                    int[] metrics = null;
                    char[] glyph = new char[len];
                    int i = 0;
                    if (this.symbolic) {
                        b = PdfEncodings.convertToBytes(text, "symboltt");
                        len = b.length;
                        for (int j = 0; j < len; j++) {
                            metrics = this.ttu.getMetricsTT(b[j] & 0xFF);
                            if (metrics != null) {
                                this.longTag.put(Integer.valueOf(metrics[0]), new int[]{metrics[0], metrics[1], this.ttu.getUnicodeDifferences(b[j] & 0xFF)});
                                glyph[i++] = (char) metrics[0];
                            }
                        }
                    } else {
                        if (canApplyGlyphSubstitution()) {
                            return convertToBytesAfterGlyphSubstitution(text);
                        }
                        for (int j = 0; j < len; j++) {
                            int val;
                            if (Utilities.isSurrogatePair(text, j)) {
                                val = Utilities.convertToUtf32(text, j);
                                j++;
                            } else {

                                val = text.charAt(j);
                            }
                            metrics = this.ttu.getMetricsTT(val);
                            if (metrics != null) {

                                int m0 = metrics[0];
                                Integer gl = Integer.valueOf(m0);
                                if (!this.longTag.containsKey(gl)) {
                                    this.longTag.put(gl, new int[]{m0, metrics[1], val});
                                }
                                glyph[i++] = (char) m0;
                            }
                        }
                    }
                    glyph = Utilities.copyOfRange(glyph, 0, i);
                    b = StringUtils.convertCharsToBytes(glyph);
                } catch (UnsupportedEncodingException e) {
                    throw new ExceptionConverter(e);
                }
                break;
        }

        return b;
    }

    private boolean canApplyGlyphSubstitution() {
        return (this.fontType == 3 && this.ttu.getGlyphSubstitutionMap() != null);
    }

    private byte[] convertToBytesAfterGlyphSubstitution(String text) throws UnsupportedEncodingException {
        if (!canApplyGlyphSubstitution()) {
            throw new IllegalArgumentException("Make sure the font type if TTF Unicode and a valid GlyphSubstitutionTable exists!");
        }

        Map<String, Glyph> glyphSubstitutionMap = this.ttu.getGlyphSubstitutionMap();

        Set<String> compositeCharacters = new TreeSet<String>((Comparator<? super String>) new IndicCompositeCharacterComparator());
        compositeCharacters.addAll(glyphSubstitutionMap.keySet());

        ArrayBasedStringTokenizer tokenizer = new ArrayBasedStringTokenizer(compositeCharacters.<String>toArray(new String[0]));
        String[] tokens = tokenizer.tokenize(text);

        List<Glyph> glyphList = new ArrayList<Glyph>(50);

        for (String token : tokens) {

            Glyph subsGlyph = glyphSubstitutionMap.get(token);

            if (subsGlyph != null) {
                glyphList.add(subsGlyph);
            } else {

                for (char c : token.toCharArray()) {
                    int[] metrics = this.ttu.getMetricsTT(c);
                    int glyphCode = metrics[0];
                    int glyphWidth = metrics[1];
                    glyphList.add(new Glyph(glyphCode, glyphWidth, String.valueOf(c)));
                }
            }
        }

        GlyphRepositioner glyphRepositioner = getGlyphRepositioner();

        if (glyphRepositioner != null) {
            glyphRepositioner.repositionGlyphs(glyphList);
        }

        char[] charEncodedGlyphCodes = new char[glyphList.size()];

        for (int i = 0; i < glyphList.size(); i++) {
            Glyph glyph = glyphList.get(i);
            charEncodedGlyphCodes[i] = (char) glyph.code;
            Integer glyphCode = Integer.valueOf(glyph.code);

            if (!this.longTag.containsKey(glyphCode)) {
                this.longTag.put(glyphCode, new int[]{glyph.code, glyph.width, glyph.chars.charAt(0)});
            }
        }

        return (new String(charEncodedGlyphCodes)).getBytes("UnicodeBigUnmarked");
    }

    private GlyphRepositioner getGlyphRepositioner() {
        Language language = this.ttu.getSupportedLanguage();

        if (language == null) {
            throw new IllegalArgumentException("The supported language field cannot be null in " + this.ttu.getClass().getName());
        }

        switch (language) {
            case BENGALI:
                return (GlyphRepositioner) new BanglaGlyphRepositioner(Collections.unmodifiableMap(this.ttu.cmap31), this.ttu.getGlyphSubstitutionMap());
        }
        return null;
    }

    public void writeFont(PdfWriter writer) {
        try {
            int firstChar;
            int lastChar;
            switch (this.fontType) {
                case 5:
                    this.baseFont.writeFont(writer, this.indirectReference, null);
                    break;

                case 0:
                case 1:
                    for (firstChar = 0; firstChar < 256
                            && this.shortTag[firstChar] == 0; firstChar++);

                    for (lastChar = 255; lastChar >= firstChar
                            && this.shortTag[lastChar] == 0; lastChar--);

                    if (firstChar > 255) {
                        firstChar = 255;
                        lastChar = 255;
                    }
                    this.baseFont.writeFont(writer, this.indirectReference, new Object[]{Integer.valueOf(firstChar), Integer.valueOf(lastChar), this.shortTag, Boolean.valueOf(this.subset)});
                    break;

                case 2:
                    this.baseFont.writeFont(writer, this.indirectReference, new Object[]{this.cjkTag});
                    break;
                case 3:
                    this.baseFont.writeFont(writer, this.indirectReference, new Object[]{this.longTag, Boolean.valueOf(this.subset)});
                    break;
            }

        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public boolean isSubset() {
        return this.subset;
    }

    public void setSubset(boolean subset) {
        this.subset = subset;
    }
}
