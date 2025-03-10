package esign.text.pdf;

import esign.text.ExceptionConverter;
import esign.text.Utilities;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.fonts.cmaps.AbstractCMap;
import esign.text.pdf.fonts.cmaps.CMapByteCid;
import esign.text.pdf.fonts.cmaps.CMapCache;
import esign.text.pdf.fonts.cmaps.CMapCidUni;
import esign.text.pdf.fonts.cmaps.CMapParserEx;
import esign.text.pdf.fonts.cmaps.CMapSequence;
import esign.text.pdf.fonts.cmaps.CMapToUnicode;
import esign.text.pdf.fonts.cmaps.CidLocation;
import esign.text.pdf.fonts.cmaps.CidLocationFromByte;
import esign.text.pdf.fonts.cmaps.IdentityToUnicode;
import java.io.IOException;
import java.util.Map;

public class CMapAwareDocumentFont
        extends DocumentFont {

    private PdfDictionary fontDic;
    private int spaceWidth;
    private CMapToUnicode toUnicodeCmap;
    private CMapByteCid byteCid;
    private CMapCidUni cidUni;
    private char[] cidbyte2uni;
    private Map<Integer, Integer> uni2cid;

    public CMapAwareDocumentFont(PdfDictionary font) {
        super(font);
        this.fontDic = font;
        initFont();
    }

    public CMapAwareDocumentFont(PRIndirectReference refFont) {
        super(refFont);
        this.fontDic = (PdfDictionary) PdfReader.getPdfObjectRelease(refFont);
        initFont();
    }

    private void initFont() {
        processToUnicode();

        try {
            processUni2Byte();

            this.spaceWidth = super.getWidth(32);
            if (this.spaceWidth == 0) {
                this.spaceWidth = computeAverageWidth();
            }
            if (this.cjkEncoding != null) {
                this.byteCid = CMapCache.getCachedCMapByteCid(this.cjkEncoding);
                this.cidUni = CMapCache.getCachedCMapCidUni(this.uniMap);
            }

        } catch (Exception ex) {
            throw new ExceptionConverter(ex);
        }
    }

    private void processToUnicode() {
        PdfObject toUni = PdfReader.getPdfObjectRelease(this.fontDic.get(PdfName.TOUNICODE));
        if (toUni instanceof PRStream) {
            try {
                byte[] touni = PdfReader.getStreamBytes((PRStream) toUni);
                CidLocationFromByte lb = new CidLocationFromByte(touni);
                this.toUnicodeCmap = new CMapToUnicode();
                CMapParserEx.parseCid("", (AbstractCMap) this.toUnicodeCmap, (CidLocation) lb);
                this.uni2cid = this.toUnicodeCmap.createReverseMapping();
            } catch (IOException e) {
                this.toUnicodeCmap = null;
                this.uni2cid = null;

            }

        } else if (this.isType0) {

            try {
                PdfName encodingName = this.fontDic.getAsName(PdfName.ENCODING);
                if (encodingName == null) {
                    return;
                }
                String enc = PdfName.decodeName(encodingName.toString());
                if (!enc.equals("Identity-H")) {
                    return;
                }
                PdfArray df = (PdfArray) PdfReader.getPdfObjectRelease(this.fontDic.get(PdfName.DESCENDANTFONTS));
                PdfDictionary cidft = (PdfDictionary) PdfReader.getPdfObjectRelease(df.getPdfObject(0));
                PdfDictionary cidinfo = cidft.getAsDict(PdfName.CIDSYSTEMINFO);
                if (cidinfo == null) {
                    return;
                }
                PdfString ordering = cidinfo.getAsString(PdfName.ORDERING);
                if (ordering == null) {
                    return;
                }
                CMapToUnicode touni = IdentityToUnicode.GetMapFromOrdering(ordering.toUnicodeString());
                if (touni == null) {
                    return;
                }
                this.toUnicodeCmap = touni;
                this.uni2cid = this.toUnicodeCmap.createReverseMapping();
            } catch (IOException e) {
                this.toUnicodeCmap = null;
                this.uni2cid = null;
            }
        }
    }

    private void processUni2Byte() throws IOException {
        IntHashtable byte2uni = getByte2Uni();
        int[] e = byte2uni.toOrderedKeys();
        if (e.length == 0) {
            return;
        }
        this.cidbyte2uni = new char[256];
        for (int k = 0; k < e.length; k++) {
            int key = e[k];
            this.cidbyte2uni[key] = (char) byte2uni.get(key);
        }
        if (this.toUnicodeCmap != null) {

            Map<Integer, Integer> dm = this.toUnicodeCmap.createDirectMapping();
            for (Map.Entry<Integer, Integer> kv : dm.entrySet()) {
                if (((Integer) kv.getKey()).intValue() < 256) {
                    this.cidbyte2uni[((Integer) kv.getKey()).intValue()] = (char) ((Integer) kv.getValue()).intValue();
                }
            }
        }
        IntHashtable diffmap = getDiffmap();
        if (diffmap != null) {

            e = diffmap.toOrderedKeys();
            for (int i = 0; i < e.length; i++) {
                int n = diffmap.get(e[i]);
                if (n < 256) {
                    this.cidbyte2uni[n] = (char) e[i];
                }
            }
        }
    }

    private int computeAverageWidth() {
        int count = 0;
        int total = 0;
        for (int i = 0; i < this.widths.length; i++) {
            if (this.widths[i] != 0) {
                total += this.widths[i];
                count++;
            }
        }
        return (count != 0) ? (total / count) : 0;
    }

    public int getWidth(int char1) {
        if (char1 == 32) {
            return (this.spaceWidth != 0) ? this.spaceWidth : this.defaultWidth;
        }
        return super.getWidth(char1);
    }

    private String decodeSingleCID(byte[] bytes, int offset, int len) {
        if (this.toUnicodeCmap != null) {
            if (offset + len > bytes.length) {
                throw new ArrayIndexOutOfBoundsException(MessageLocalization.getComposedMessage("invalid.index.1", offset + len));
            }
            String s = this.toUnicodeCmap.lookup(bytes, offset, len);
            if (s != null) {
                return s;
            }
            if (len != 1 || this.cidbyte2uni == null) {
                return null;
            }
        }
        if (len == 1) {
            if (this.cidbyte2uni == null) {
                return "";
            }
            return new String(this.cidbyte2uni, 0xFF & bytes[offset], 1);
        }

        throw new Error("Multi-byte glyphs not implemented yet");
    }

    public String decode(byte[] cidbytes, int offset, int len) {
        StringBuilder sb = new StringBuilder();
        if (this.toUnicodeCmap == null && this.byteCid != null) {
            CMapSequence seq = new CMapSequence(cidbytes, offset, len);
            String cid = this.byteCid.decodeSequence(seq);
            for (int k = 0; k < cid.length(); k++) {
                int c = this.cidUni.lookup(cid.charAt(k));
                if (c > 0) {
                    sb.append(Utilities.convertFromUtf32(c));
                }
            }
        } else {
            for (int i = offset; i < offset + len; i++) {
                String rslt = decodeSingleCID(cidbytes, i, 1);
                if (rslt == null && i < offset + len - 1) {
                    rslt = decodeSingleCID(cidbytes, i, 2);
                    i++;
                }
                if (rslt != null) {
                    sb.append(rslt);
                }
            }
        }
        return sb.toString();
    }

    public String encode(byte[] bytes, int offset, int len) {
        return decode(bytes, offset, len);
    }
}
