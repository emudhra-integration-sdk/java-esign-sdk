package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;
import esign.text.exceptions.UnsupportedPdfException;
import esign.text.pdf.codec.TIFFFaxDecoder;
import esign.text.pdf.codec.TIFFFaxDecompressor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class FilterHandlers {

    private static final Map<PdfName, FilterHandler> defaults;

    static {
        HashMap<PdfName, FilterHandler> map = new HashMap<PdfName, FilterHandler>();

        map.put(PdfName.FLATEDECODE, new Filter_FLATEDECODE());
        map.put(PdfName.FL, new Filter_FLATEDECODE());
        map.put(PdfName.ASCIIHEXDECODE, new Filter_ASCIIHEXDECODE());
        map.put(PdfName.AHX, new Filter_ASCIIHEXDECODE());
        map.put(PdfName.ASCII85DECODE, new Filter_ASCII85DECODE());
        map.put(PdfName.A85, new Filter_ASCII85DECODE());
        map.put(PdfName.LZWDECODE, new Filter_LZWDECODE());
        map.put(PdfName.CCITTFAXDECODE, new Filter_CCITTFAXDECODE());
        map.put(PdfName.CRYPT, new Filter_DoNothing());
        map.put(PdfName.RUNLENGTHDECODE, new Filter_RUNLENGTHDECODE());

        defaults = Collections.unmodifiableMap(map);
    }

    public static Map<PdfName, FilterHandler> getDefaultFilterHandlers() {
        return defaults;
    }

    public static interface FilterHandler {

        byte[] decode(byte[] param1ArrayOfbyte, PdfName param1PdfName, PdfObject param1PdfObject, PdfDictionary param1PdfDictionary) throws IOException;
    }

    private static class Filter_FLATEDECODE implements FilterHandler {

        public byte[] decode(byte[] b, PdfName filterName, PdfObject decodeParams, PdfDictionary streamDictionary) throws IOException {
            b = PdfReader.FlateDecode(b);
            b = PdfReader.decodePredictor(b, decodeParams);
            return b;
        }

        private Filter_FLATEDECODE() {
        }
    }

    private static class Filter_ASCIIHEXDECODE implements FilterHandler {

        private Filter_ASCIIHEXDECODE() {
        }

        public byte[] decode(byte[] b, PdfName filterName, PdfObject decodeParams, PdfDictionary streamDictionary) throws IOException {
            b = PdfReader.ASCIIHexDecode(b);
            return b;
        }
    }

    private static class Filter_ASCII85DECODE
            implements FilterHandler {

        private Filter_ASCII85DECODE() {
        }

        public byte[] decode(byte[] b, PdfName filterName, PdfObject decodeParams, PdfDictionary streamDictionary) throws IOException {
            b = PdfReader.ASCII85Decode(b);
            return b;
        }
    }

    private static class Filter_LZWDECODE
            implements FilterHandler {

        private Filter_LZWDECODE() {
        }

        public byte[] decode(byte[] b, PdfName filterName, PdfObject decodeParams, PdfDictionary streamDictionary) throws IOException {
            b = PdfReader.LZWDecode(b);
            b = PdfReader.decodePredictor(b, decodeParams);
            return b;
        }
    }

    private static class Filter_CCITTFAXDECODE
            implements FilterHandler {

        private Filter_CCITTFAXDECODE() {
        }

        public byte[] decode(byte[] b, PdfName filterName, PdfObject decodeParams, PdfDictionary streamDictionary) throws IOException {
            PdfNumber wn = (PdfNumber) PdfReader.getPdfObjectRelease(streamDictionary.get(PdfName.WIDTH));
            PdfNumber hn = (PdfNumber) PdfReader.getPdfObjectRelease(streamDictionary.get(PdfName.HEIGHT));
            if (wn == null || hn == null) {
                throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("filter.ccittfaxdecode.is.only.supported.for.images", new Object[0]));
            }
            int width = wn.intValue();
            int height = hn.intValue();

            PdfDictionary param = (decodeParams instanceof PdfDictionary) ? (PdfDictionary) decodeParams : null;
            int k = 0;
            boolean blackIs1 = false;
            boolean byteAlign = false;
            if (param != null) {
                PdfNumber kn = param.getAsNumber(PdfName.K);
                if (kn != null) {
                    k = kn.intValue();
                }
                PdfBoolean bo = param.getAsBoolean(PdfName.BLACKIS1);
                if (bo != null) {
                    blackIs1 = bo.booleanValue();
                }
                bo = param.getAsBoolean(PdfName.ENCODEDBYTEALIGN);
                if (bo != null) {
                    byteAlign = bo.booleanValue();
                }
            }
            byte[] outBuf = new byte[(width + 7) / 8 * height];
            TIFFFaxDecompressor decoder = new TIFFFaxDecompressor();
            if (k == 0 || k > 0) {
                int tiffT4Options = (k > 0) ? 1 : 0;
                tiffT4Options |= byteAlign ? 4 : 0;
                decoder.SetOptions(1, 3, tiffT4Options, 0);
                decoder.decodeRaw(outBuf, b, width, height);
                if (decoder.fails > 0) {
                    byte[] outBuf2 = new byte[(width + 7) / 8 * height];
                    int oldFails = decoder.fails;
                    decoder.SetOptions(1, 2, tiffT4Options, 0);
                    decoder.decodeRaw(outBuf2, b, width, height);
                    if (decoder.fails < oldFails) {
                        outBuf = outBuf2;
                    }
                }
            } else {

                TIFFFaxDecoder deca = new TIFFFaxDecoder(1L, width, height);
                deca.decodeT6(outBuf, b, 0, height, 0L);
            }
            if (!blackIs1) {
                int len = outBuf.length;
                for (int t = 0; t < len; t++) {
                    outBuf[t] = (byte) (outBuf[t] ^ 0xFF);
                }
            }
            b = outBuf;
            return b;
        }
    }

    private static class Filter_DoNothing
            implements FilterHandler {

        private Filter_DoNothing() {
        }

        public byte[] decode(byte[] b, PdfName filterName, PdfObject decodeParams, PdfDictionary streamDictionary) throws IOException {
            return b;
        }
    }

    private static class Filter_RUNLENGTHDECODE
            implements FilterHandler {

        private Filter_RUNLENGTHDECODE() {
        }

        public byte[] decode(byte[] b, PdfName filterName, PdfObject decodeParams, PdfDictionary streamDictionary) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte dupCount = -1;
            for (int i = 0; i < b.length; i++) {
                dupCount = b[i];
                if (dupCount == Byte.MIN_VALUE) {
                    break;
                }
                if (dupCount >= 0 && dupCount <= Byte.MAX_VALUE) {
                    int bytesToCopy = dupCount + 1;
                    baos.write(b, i, bytesToCopy);
                    i += bytesToCopy;
                } else {

                    i++;
                    for (int j = 0; j < 1 - dupCount; j++) {
                        baos.write(b[i]);
                    }
                }
            }

            return baos.toByteArray();
        }
    }
}
