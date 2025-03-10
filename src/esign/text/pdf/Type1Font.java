package esign.text.pdf;

import esign.text.Document;
import esign.text.DocumentException;
import esign.text.error_messages.MessageLocalization;
import esign.text.io.StreamUtil;
import esign.text.pdf.fonts.FontsResourceAnchor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.StringTokenizer;

class Type1Font
        extends BaseFont {

    private static FontsResourceAnchor resourceAnchor;
    protected byte[] pfb;
    private String FontName;
    private String FullName;
    private String FamilyName;
    private String Weight = "";

    private float ItalicAngle = 0.0F;

    private boolean IsFixedPitch = false;

    private String CharacterSet;

    private int llx = -50;

    private int lly = -200;

    private int urx = 1000;

    private int ury = 900;

    private int UnderlinePosition = -100;

    private int UnderlineThickness = 50;

    private String EncodingScheme = "FontSpecific";

    private int CapHeight = 700;

    private int XHeight = 480;

    private int Ascender = 800;

    private int Descender = -200;

    private int StdHW;

    private int StdVW = 80;

    private HashMap<Object, Object[]> CharMetrics = (HashMap) new HashMap<Object, Object>();

    private HashMap<String, Object[]> KernPairs = (HashMap) new HashMap<String, Object>();

    private String fileName;

    private boolean builtinFont = false;

    private static final int[] PFB_TYPES = new int[]{1, 2, 1};

    Type1Font(String afmFile, String enc, boolean emb, byte[] ttfAfm, byte[] pfb, boolean forceRead) throws DocumentException, IOException {
        if (emb && ttfAfm != null && pfb == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("two.byte.arrays.are.needed.if.the.type1.font.is.embedded", new Object[0]));
        }
        if (emb && ttfAfm != null) {
            this.pfb = pfb;
        }
        this.encoding = enc;
        this.embedded = emb;
        this.fileName = afmFile;
        this.fontType = 0;
        RandomAccessFileOrArray rf = null;
        InputStream is = null;
        if (BuiltinFonts14.containsKey(afmFile)) {
            this.embedded = false;
            this.builtinFont = true;
            byte[] buf = new byte[1024];
            try {
                if (resourceAnchor == null) {
                    resourceAnchor = new FontsResourceAnchor();
                }
                is = StreamUtil.getResourceStream("esign/text/pdf/fonts/" + afmFile + ".afm", resourceAnchor.getClass().getClassLoader());
                if (is == null) {
                    String msg = MessageLocalization.getComposedMessage("1.not.found.as.resource", new Object[]{afmFile});
                    System.err.println(msg);
                    throw new DocumentException(msg);
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                while (true) {
                    int size = is.read(buf);
                    if (size < 0) {
                        break;
                    }
                    out.write(buf, 0, size);
                }
                buf = out.toByteArray();
            } finally {

                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception exception) {
                    }
                }
            }

            try {
                rf = new RandomAccessFileOrArray(buf);
                process(rf);
            } finally {

                if (rf != null) {
                    try {
                        rf.close();
                    } catch (Exception exception) {
                    }

                }
            }

        } else if (afmFile.toLowerCase().endsWith(".afm")) {
            try {
                if (ttfAfm == null) {
                    rf = new RandomAccessFileOrArray(afmFile, forceRead, Document.plainRandomAccess);
                } else {
                    rf = new RandomAccessFileOrArray(ttfAfm);
                }
                process(rf);
            } finally {

                if (rf != null) {
                    try {
                        rf.close();
                    } catch (Exception exception) {
                    }

                }
            }

        } else if (afmFile.toLowerCase().endsWith(".pfm")) {
            try {
                ByteArrayOutputStream ba = new ByteArrayOutputStream();
                if (ttfAfm == null) {
                    rf = new RandomAccessFileOrArray(afmFile, forceRead, Document.plainRandomAccess);
                } else {
                    rf = new RandomAccessFileOrArray(ttfAfm);
                }
                Pfm2afm.convert(rf, ba);
                rf.close();
                rf = new RandomAccessFileOrArray(ba.toByteArray());
                process(rf);
            } finally {

                if (rf != null) {
                    try {
                        rf.close();
                    } catch (Exception exception) {
                    }
                }
            }

        } else {

            throw new DocumentException(MessageLocalization.getComposedMessage("1.is.not.an.afm.or.pfm.font.file", new Object[]{afmFile}));
        }
        this.EncodingScheme = this.EncodingScheme.trim();
        if (this.EncodingScheme.equals("AdobeStandardEncoding") || this.EncodingScheme.equals("StandardEncoding")) {
            this.fontSpecific = false;
        }
        if (!this.encoding.startsWith("#")) {
            PdfEncodings.convertToBytes(" ", enc);
        }
        createEncoding();
    }

    int getRawWidth(int c, String name) {
        Object[] metrics;
        if (name == null) {
            metrics = this.CharMetrics.get(Integer.valueOf(c));
        } else {

            if (name.equals(".notdef")) {
                return 0;
            }
            metrics = this.CharMetrics.get(name);
        }
        if (metrics != null) {
            return ((Integer) metrics[1]).intValue();
        }
        return 0;
    }

    public int getKerning(int char1, int char2) {
        String first = GlyphList.unicodeToName(char1);
        if (first == null) {
            return 0;
        }
        String second = GlyphList.unicodeToName(char2);
        if (second == null) {
            return 0;
        }
        Object[] obj = this.KernPairs.get(first);
        if (obj == null) {
            return 0;
        }
        for (int k = 0; k < obj.length; k += 2) {
            if (second.equals(obj[k])) {
                return ((Integer) obj[k + 1]).intValue();
            }
        }
        return 0;
    }

    public void process(RandomAccessFileOrArray rf) throws DocumentException, IOException {
        boolean isMetrics = false;
        String line;
        while ((line = rf.readLine()) != null) {

            StringTokenizer tok = new StringTokenizer(line, " ,\n\r\t\f");
            if (!tok.hasMoreTokens()) {
                continue;
            }
            String ident = tok.nextToken();
            if (ident.equals("FontName")) {
                this.FontName = tok.nextToken("ÿ").substring(1);
                continue;
            }
            if (ident.equals("FullName")) {
                this.FullName = tok.nextToken("ÿ").substring(1);
                continue;
            }
            if (ident.equals("FamilyName")) {
                this.FamilyName = tok.nextToken("ÿ").substring(1);
                continue;
            }
            if (ident.equals("Weight")) {
                this.Weight = tok.nextToken("ÿ").substring(1);
                continue;
            }
            if (ident.equals("ItalicAngle")) {
                this.ItalicAngle = Float.parseFloat(tok.nextToken());
                continue;
            }
            if (ident.equals("IsFixedPitch")) {
                this.IsFixedPitch = tok.nextToken().equals("true");
                continue;
            }
            if (ident.equals("CharacterSet")) {
                this.CharacterSet = tok.nextToken("ÿ").substring(1);
                continue;
            }
            if (ident.equals("FontBBox")) {

                this.llx = (int) Float.parseFloat(tok.nextToken());
                this.lly = (int) Float.parseFloat(tok.nextToken());
                this.urx = (int) Float.parseFloat(tok.nextToken());
                this.ury = (int) Float.parseFloat(tok.nextToken());
                continue;
            }
            if (ident.equals("UnderlinePosition")) {
                this.UnderlinePosition = (int) Float.parseFloat(tok.nextToken());
                continue;
            }
            if (ident.equals("UnderlineThickness")) {
                this.UnderlineThickness = (int) Float.parseFloat(tok.nextToken());
                continue;
            }
            if (ident.equals("EncodingScheme")) {
                this.EncodingScheme = tok.nextToken("ÿ").substring(1);
                continue;
            }
            if (ident.equals("CapHeight")) {
                this.CapHeight = (int) Float.parseFloat(tok.nextToken());
                continue;
            }
            if (ident.equals("XHeight")) {
                this.XHeight = (int) Float.parseFloat(tok.nextToken());
                continue;
            }
            if (ident.equals("Ascender")) {
                this.Ascender = (int) Float.parseFloat(tok.nextToken());
                continue;
            }
            if (ident.equals("Descender")) {
                this.Descender = (int) Float.parseFloat(tok.nextToken());
                continue;
            }
            if (ident.equals("StdHW")) {
                this.StdHW = (int) Float.parseFloat(tok.nextToken());
                continue;
            }
            if (ident.equals("StdVW")) {
                this.StdVW = (int) Float.parseFloat(tok.nextToken());
                continue;
            }
            if (ident.equals("StartCharMetrics")) {

                isMetrics = true;
                break;
            }
        }
        if (!isMetrics) {
            throw new DocumentException(MessageLocalization.getComposedMessage("missing.startcharmetrics.in.1", new Object[]{this.fileName}));
        }
        while ((line = rf.readLine()) != null) {

            StringTokenizer tok = new StringTokenizer(line);
            if (!tok.hasMoreTokens()) {
                continue;
            }
            String ident = tok.nextToken();
            if (ident.equals("EndCharMetrics")) {

                isMetrics = false;
                break;
            }
            Integer C = Integer.valueOf(-1);
            Integer WX = Integer.valueOf(250);
            String N = "";
            int[] B = null;

            tok = new StringTokenizer(line, ";");
            while (tok.hasMoreTokens()) {

                StringTokenizer tokc = new StringTokenizer(tok.nextToken());
                if (!tokc.hasMoreTokens()) {
                    continue;
                }
                ident = tokc.nextToken();
                if (ident.equals("C")) {
                    C = Integer.valueOf(tokc.nextToken());
                    continue;
                }
                if (ident.equals("WX")) {
                    WX = Integer.valueOf((int) Float.parseFloat(tokc.nextToken()));
                    continue;
                }
                if (ident.equals("N")) {
                    N = tokc.nextToken();
                    continue;
                }
                if (ident.equals("B")) {

                    B = new int[]{Integer.parseInt(tokc.nextToken()), Integer.parseInt(tokc.nextToken()), Integer.parseInt(tokc.nextToken()), Integer.parseInt(tokc.nextToken())};
                }
            }
            Object[] metrics = {C, WX, N, B};
            if (C.intValue() >= 0) {
                this.CharMetrics.put(C, metrics);
            }
            this.CharMetrics.put(N, metrics);
        }
        if (isMetrics) {
            throw new DocumentException(MessageLocalization.getComposedMessage("missing.endcharmetrics.in.1", new Object[]{this.fileName}));
        }
        if (!this.CharMetrics.containsKey("nonbreakingspace")) {
            Object[] space = this.CharMetrics.get("space");
            if (space != null) {
                this.CharMetrics.put("nonbreakingspace", space);
            }
        }
        while ((line = rf.readLine()) != null) {

            StringTokenizer tok = new StringTokenizer(line);
            if (!tok.hasMoreTokens()) {
                continue;
            }
            String ident = tok.nextToken();
            if (ident.equals("EndFontMetrics")) {
                return;
            }
            if (ident.equals("StartKernPairs")) {

                isMetrics = true;
                break;
            }
        }
        if (!isMetrics) {
            throw new DocumentException(MessageLocalization.getComposedMessage("missing.endfontmetrics.in.1", new Object[]{this.fileName}));
        }
        while ((line = rf.readLine()) != null) {

            StringTokenizer tok = new StringTokenizer(line);
            if (!tok.hasMoreTokens()) {
                continue;
            }
            String ident = tok.nextToken();
            if (ident.equals("KPX")) {

                String first = tok.nextToken();
                String second = tok.nextToken();
                Integer width = Integer.valueOf((int) Float.parseFloat(tok.nextToken()));
                Object[] relates = this.KernPairs.get(first);
                if (relates == null) {
                    this.KernPairs.put(first, new Object[]{second, width});
                    continue;
                }
                int n = relates.length;
                Object[] relates2 = new Object[n + 2];
                System.arraycopy(relates, 0, relates2, 0, n);
                relates2[n] = second;
                relates2[n + 1] = width;
                this.KernPairs.put(first, relates2);
                continue;
            }
            if (ident.equals("EndKernPairs")) {

                isMetrics = false;
                break;
            }
        }
        if (isMetrics) {
            throw new DocumentException(MessageLocalization.getComposedMessage("missing.endkernpairs.in.1", new Object[]{this.fileName}));
        }
        rf.close();
    }

    public PdfStream getFullFontStream() throws DocumentException {
        if (this.builtinFont || !this.embedded) {
            return null;
        }
        RandomAccessFileOrArray rf = null;
        try {
            String filePfb = this.fileName.substring(0, this.fileName.length() - 3) + "pfb";
            if (this.pfb == null) {
                rf = new RandomAccessFileOrArray(filePfb, true, Document.plainRandomAccess);
            } else {
                rf = new RandomAccessFileOrArray(this.pfb);
            }
            int fileLength = (int) rf.length();
            byte[] st = new byte[fileLength - 18];
            int[] lengths = new int[3];
            int bytePtr = 0;
            for (int k = 0; k < 3; k++) {
                if (rf.read() != 128) {
                    throw new DocumentException(MessageLocalization.getComposedMessage("start.marker.missing.in.1", new Object[]{filePfb}));
                }
                if (rf.read() != PFB_TYPES[k]) {
                    throw new DocumentException(MessageLocalization.getComposedMessage("incorrect.segment.type.in.1", new Object[]{filePfb}));
                }
                int size = rf.read();
                size += rf.read() << 8;
                size += rf.read() << 16;
                size += rf.read() << 24;
                lengths[k] = size;
                while (size != 0) {
                    int got = rf.read(st, bytePtr, size);
                    if (got < 0) {
                        throw new DocumentException(MessageLocalization.getComposedMessage("premature.end.in.1", new Object[]{filePfb}));
                    }
                    bytePtr += got;
                    size -= got;
                }
            }
            return new BaseFont.StreamFont(st, lengths, this.compressionLevel);
        } catch (Exception e) {
            throw new DocumentException(e);
        } finally {

            if (rf != null) {
                try {
                    rf.close();
                } catch (Exception exception) {
                }
            }
        }
    }

    private PdfDictionary getFontDescriptor(PdfIndirectReference fontStream) {
        if (this.builtinFont) {
            return null;
        }
        PdfDictionary dic = new PdfDictionary(PdfName.FONTDESCRIPTOR);
        dic.put(PdfName.ASCENT, new PdfNumber(this.Ascender));
        dic.put(PdfName.CAPHEIGHT, new PdfNumber(this.CapHeight));
        dic.put(PdfName.DESCENT, new PdfNumber(this.Descender));
        dic.put(PdfName.FONTBBOX, new PdfRectangle(this.llx, this.lly, this.urx, this.ury));
        dic.put(PdfName.FONTNAME, new PdfName(this.FontName));
        dic.put(PdfName.ITALICANGLE, new PdfNumber(this.ItalicAngle));
        dic.put(PdfName.STEMV, new PdfNumber(this.StdVW));
        if (fontStream != null) {
            dic.put(PdfName.FONTFILE, fontStream);
        }
        int flags = 0;
        if (this.IsFixedPitch) {
            flags |= 0x1;
        }
        flags |= this.fontSpecific ? 4 : 32;
        if (this.ItalicAngle < 0.0F) {
            flags |= 0x40;
        }
        if (this.FontName.indexOf("Caps") >= 0 || this.FontName.endsWith("SC")) {
            flags |= 0x20000;
        }
        if (this.Weight.equals("Bold")) {
            flags |= 0x40000;
        }
        dic.put(PdfName.FLAGS, new PdfNumber(flags));

        return dic;
    }

    private PdfDictionary getFontBaseType(PdfIndirectReference fontDescriptor, int firstChar, int lastChar, byte[] shortTag) {
        PdfDictionary dic = new PdfDictionary(PdfName.FONT);
        dic.put(PdfName.SUBTYPE, PdfName.TYPE1);
        dic.put(PdfName.BASEFONT, new PdfName(this.FontName));
        boolean stdEncoding = (this.encoding.equals("Cp1252") || this.encoding.equals("MacRoman"));
        if (!this.fontSpecific || this.specialMap != null) {
            for (int k = firstChar; k <= lastChar; k++) {
                if (!this.differences[k].equals(".notdef")) {
                    firstChar = k;
                    break;
                }
            }
            if (stdEncoding) {
                dic.put(PdfName.ENCODING, this.encoding.equals("Cp1252") ? PdfName.WIN_ANSI_ENCODING : PdfName.MAC_ROMAN_ENCODING);
            } else {
                PdfDictionary enc = new PdfDictionary(PdfName.ENCODING);
                PdfArray dif = new PdfArray();
                boolean gap = true;
                for (int i = firstChar; i <= lastChar; i++) {
                    if (shortTag[i] != 0) {
                        if (gap) {
                            dif.add(new PdfNumber(i));
                            gap = false;
                        }
                        dif.add(new PdfName(this.differences[i]));
                    } else {

                        gap = true;
                    }
                }
                enc.put(PdfName.DIFFERENCES, dif);
                dic.put(PdfName.ENCODING, enc);
            }
        }
        if (this.specialMap != null || this.forceWidthsOutput || !this.builtinFont || (!this.fontSpecific && !stdEncoding)) {
            dic.put(PdfName.FIRSTCHAR, new PdfNumber(firstChar));
            dic.put(PdfName.LASTCHAR, new PdfNumber(lastChar));
            PdfArray wd = new PdfArray();
            for (int k = firstChar; k <= lastChar; k++) {
                if (shortTag[k] == 0) {
                    wd.add(new PdfNumber(0));
                } else {
                    wd.add(new PdfNumber(this.widths[k]));
                }
            }
            dic.put(PdfName.WIDTHS, wd);
        }
        if (!this.builtinFont && fontDescriptor != null) {
            dic.put(PdfName.FONTDESCRIPTOR, fontDescriptor);
        }
        return dic;
    }

    void writeFont(PdfWriter writer, PdfIndirectReference ref, Object[] params) throws DocumentException, IOException {
        int firstChar = ((Integer) params[0]).intValue();
        int lastChar = ((Integer) params[1]).intValue();
        byte[] shortTag = (byte[]) params[2];
        boolean subsetp = (((Boolean) params[3]).booleanValue() && this.subset);
        if (!subsetp || !this.embedded) {
            firstChar = 0;
            lastChar = shortTag.length - 1;
            for (int k = 0; k < shortTag.length; k++) {
                shortTag[k] = 1;
            }
        }
        PdfIndirectReference ind_font = null;
        PdfObject pobj = null;
        PdfIndirectObject obj = null;
        pobj = getFullFontStream();
        if (pobj != null) {
            obj = writer.addToBody(pobj);
            ind_font = obj.getIndirectReference();
        }
        pobj = getFontDescriptor(ind_font);
        if (pobj != null) {
            obj = writer.addToBody(pobj);
            ind_font = obj.getIndirectReference();
        }
        pobj = getFontBaseType(ind_font, firstChar, lastChar, shortTag);
        writer.addToBody(pobj, ref);
    }

    public float getFontDescriptor(int key, float fontSize) {
        switch (key) {
            case 1:
            case 9:
                return this.Ascender * fontSize / 1000.0F;
            case 2:
                return this.CapHeight * fontSize / 1000.0F;
            case 3:
            case 10:
                return this.Descender * fontSize / 1000.0F;
            case 4:
                return this.ItalicAngle;
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
            case 13:
                return this.UnderlinePosition * fontSize / 1000.0F;
            case 14:
                return this.UnderlineThickness * fontSize / 1000.0F;
        }
        return 0.0F;
    }

    public void setFontDescriptor(int key, float value) {
        switch (key) {
            case 1:
            case 9:
                this.Ascender = (int) value;
                break;
            case 3:
            case 10:
                this.Descender = (int) value;
                break;
        }
    }

    public String getPostscriptFontName() {
        return this.FontName;
    }

    public String[][] getFullFontName() {
        return new String[][]{{"", "", "", this.FullName}};
    }

    public String[][] getAllNameEntries() {
        return new String[][]{{"4", "", "", "", this.FullName}};
    }

    public String[][] getFamilyFontName() {
        return new String[][]{{"", "", "", this.FamilyName}};
    }

    public boolean hasKernPairs() {
        return !this.KernPairs.isEmpty();
    }

    public void setPostscriptFontName(String name) {
        this.FontName = name;
    }

    public boolean setKerning(int char1, int char2, int kern) {
        String first = GlyphList.unicodeToName(char1);
        if (first == null) {
            return false;
        }
        String second = GlyphList.unicodeToName(char2);
        if (second == null) {
            return false;
        }
        Object[] obj = this.KernPairs.get(first);
        if (obj == null) {
            obj = new Object[]{second, Integer.valueOf(kern)};
            this.KernPairs.put(first, obj);
            return true;
        }
        for (int k = 0; k < obj.length; k += 2) {
            if (second.equals(obj[k])) {
                obj[k + 1] = Integer.valueOf(kern);
                return true;
            }
        }
        int size = obj.length;
        Object[] obj2 = new Object[size + 2];
        System.arraycopy(obj, 0, obj2, 0, size);
        obj2[size] = second;
        obj2[size + 1] = Integer.valueOf(kern);
        this.KernPairs.put(first, obj2);
        return true;
    }

    protected int[] getRawCharBBox(int c, String name) {
        Object[] metrics;
        if (name == null) {
            metrics = this.CharMetrics.get(Integer.valueOf(c));
        } else {

            if (name.equals(".notdef")) {
                return null;
            }
            metrics = this.CharMetrics.get(name);
        }
        if (metrics != null) {
            return (int[]) metrics[3];
        }
        return null;
    }
}
