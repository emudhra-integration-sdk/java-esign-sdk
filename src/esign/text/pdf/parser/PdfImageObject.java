package esign.text.pdf.parser;

import esign.text.Version;
import esign.text.error_messages.MessageLocalization;
import esign.text.exceptions.UnsupportedPdfException;
import esign.text.pdf.FilterHandlers;
import esign.text.pdf.PRStream;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import esign.text.pdf.PdfString;
import esign.text.pdf.codec.PngWriter;
import esign.text.pdf.codec.TiffWriter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class PdfImageObject {

    private PdfDictionary dictionary;
    private byte[] imageBytes;
    private PdfDictionary colorSpaceDic;

    public enum ImageBytesType {
        PNG("png"),
        JPG("jpg"),
        JP2("jp2"),
        CCITT("tif"),
        JBIG2("jbig2");

        private final String fileExtension;

        ImageBytesType(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public String getFileExtension() {
            return this.fileExtension;
        }
    }

    private static class TrackingFilter
            implements FilterHandlers.FilterHandler {

        public PdfName lastFilterName = null;

        public byte[] decode(byte[] b, PdfName filterName, PdfObject decodeParams, PdfDictionary streamDictionary) throws IOException {
            this.lastFilterName = filterName;
            return b;
        }

        private TrackingFilter() {
        }
    }

    private int pngColorType = -1;

    private int pngBitDepth;

    private int width;

    private int height;

    private int bpc;
    private byte[] palette;
    private byte[] icc;
    private int stride;
    private ImageBytesType streamContentType = null;

    public String getFileType() {
        return this.streamContentType.getFileExtension();
    }

    public ImageBytesType getImageBytesType() {
        return this.streamContentType;
    }

    public PdfImageObject(PRStream stream) throws IOException {
        this((PdfDictionary) stream, PdfReader.getStreamBytesRaw(stream), null);
    }

    public PdfImageObject(PRStream stream, PdfDictionary colorSpaceDic) throws IOException {
        this((PdfDictionary) stream, PdfReader.getStreamBytesRaw(stream), colorSpaceDic);
    }

    protected PdfImageObject(PdfDictionary dictionary, byte[] samples, PdfDictionary colorSpaceDic) throws IOException {
        this.dictionary = dictionary;
        this.colorSpaceDic = colorSpaceDic;
        TrackingFilter trackingFilter = new TrackingFilter();
        Map<PdfName, FilterHandlers.FilterHandler> handlers = new HashMap<PdfName, FilterHandlers.FilterHandler>(FilterHandlers.getDefaultFilterHandlers());
        handlers.put(PdfName.JBIG2DECODE, trackingFilter);
        handlers.put(PdfName.DCTDECODE, trackingFilter);
        handlers.put(PdfName.JPXDECODE, trackingFilter);

        this.imageBytes = PdfReader.decodeBytes(samples, dictionary, handlers);

        if (trackingFilter.lastFilterName != null) {
            if (PdfName.JBIG2DECODE.equals(trackingFilter.lastFilterName)) {
                this.streamContentType = ImageBytesType.JBIG2;
            } else if (PdfName.DCTDECODE.equals(trackingFilter.lastFilterName)) {
                this.streamContentType = ImageBytesType.JPG;
            } else if (PdfName.JPXDECODE.equals(trackingFilter.lastFilterName)) {
                this.streamContentType = ImageBytesType.JP2;
            }
        } else {
            decodeImageBytes();
        }

    }

    public PdfObject get(PdfName key) {
        return this.dictionary.get(key);
    }

    public PdfDictionary getDictionary() {
        return this.dictionary;
    }

    private void findColorspace(PdfObject colorspace, boolean allowIndexed) throws IOException {
        if (colorspace == null && this.bpc == 1) {
            this.stride = (this.width * this.bpc + 7) / 8;
            this.pngColorType = 0;
        } else if (PdfName.DEVICEGRAY.equals(colorspace)) {
            this.stride = (this.width * this.bpc + 7) / 8;
            this.pngColorType = 0;
        } else if (PdfName.DEVICERGB.equals(colorspace)) {
            if (this.bpc == 8 || this.bpc == 16) {
                this.stride = (this.width * this.bpc * 3 + 7) / 8;
                this.pngColorType = 2;
            }

        } else if (colorspace instanceof PdfArray) {
            PdfArray ca = (PdfArray) colorspace;
            PdfObject tyca = ca.getDirectObject(0);
            if (PdfName.CALGRAY.equals(tyca)) {
                this.stride = (this.width * this.bpc + 7) / 8;
                this.pngColorType = 0;
            } else if (PdfName.CALRGB.equals(tyca)) {
                if (this.bpc == 8 || this.bpc == 16) {
                    this.stride = (this.width * this.bpc * 3 + 7) / 8;
                    this.pngColorType = 2;
                }

            } else if (PdfName.ICCBASED.equals(tyca)) {
                PRStream pr = (PRStream) ca.getDirectObject(1);
                int n = pr.getAsNumber(PdfName.N).intValue();
                if (n == 1) {
                    this.stride = (this.width * this.bpc + 7) / 8;
                    this.pngColorType = 0;
                    this.icc = PdfReader.getStreamBytes(pr);
                } else if (n == 3) {
                    this.stride = (this.width * this.bpc * 3 + 7) / 8;
                    this.pngColorType = 2;
                    this.icc = PdfReader.getStreamBytes(pr);
                }

            } else if (allowIndexed && PdfName.INDEXED.equals(tyca)) {
                findColorspace(ca.getDirectObject(1), false);
                if (this.pngColorType == 2) {
                    PdfObject id2 = ca.getDirectObject(3);
                    if (id2 instanceof PdfString) {
                        this.palette = ((PdfString) id2).getBytes();
                    } else if (id2 instanceof PRStream) {
                        this.palette = PdfReader.getStreamBytes((PRStream) id2);
                    }
                    this.stride = (this.width * this.bpc + 7) / 8;
                    this.pngColorType = 3;
                }
            }
        }
    }

    private void decodeImageBytes() throws IOException {
        if (this.streamContentType != null) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("Decoding.can't.happen.on.this.type.of.stream.(.1.)", new Object[]{this.streamContentType}));
        }
        this.pngColorType = -1;
        PdfArray decode = this.dictionary.getAsArray(PdfName.DECODE);
        this.width = this.dictionary.getAsNumber(PdfName.WIDTH).intValue();
        this.height = this.dictionary.getAsNumber(PdfName.HEIGHT).intValue();
        this.bpc = this.dictionary.getAsNumber(PdfName.BITSPERCOMPONENT).intValue();
        this.pngBitDepth = this.bpc;
        PdfObject colorspace = this.dictionary.getDirectObject(PdfName.COLORSPACE);
        if (colorspace instanceof PdfName && this.colorSpaceDic != null) {
            PdfObject csLookup = this.colorSpaceDic.getDirectObject((PdfName) colorspace);
            if (csLookup != null) {
                colorspace = csLookup;
            }
        }
        this.palette = null;
        this.icc = null;
        this.stride = 0;
        findColorspace(colorspace, true);
        ByteArrayOutputStream ms = new ByteArrayOutputStream();
        if (this.pngColorType < 0) {
            if (this.bpc != 8) {
                throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("the.color.depth.1.is.not.supported", this.bpc));
            }
            if (!PdfName.DEVICECMYK.equals(colorspace)) {
                if (colorspace instanceof PdfArray) {
                    PdfArray ca = (PdfArray) colorspace;
                    PdfObject tyca = ca.getDirectObject(0);
                    if (!PdfName.ICCBASED.equals(tyca)) {
                        throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("the.color.space.1.is.not.supported", new Object[]{colorspace}));
                    }
                    PRStream pr = (PRStream) ca.getDirectObject(1);
                    int n = pr.getAsNumber(PdfName.N).intValue();
                    if (n != 4) {
                        throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("N.value.1.is.not.supported", n));
                    }
                    this.icc = PdfReader.getStreamBytes(pr);
                } else {

                    throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("the.color.space.1.is.not.supported", new Object[]{colorspace}));
                }
            }
            this.stride = 4 * this.width;
            TiffWriter wr = new TiffWriter();
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldShort(277, 4));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldShort(258, new int[]{8, 8, 8, 8}));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldShort(262, 5));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldLong(256, this.width));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldLong(257, this.height));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldShort(259, 5));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldShort(317, 2));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldLong(278, this.height));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldRational(282, new int[]{300, 1}));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldRational(283, new int[]{300, 1}));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldShort(296, 2));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldAscii(305, Version.getInstance().getVersion()));
            ByteArrayOutputStream comp = new ByteArrayOutputStream();
            TiffWriter.compressLZW(comp, 2, this.imageBytes, this.height, 4, this.stride);
            byte[] buf = comp.toByteArray();
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldImage(buf));
            wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldLong(279, buf.length));
            if (this.icc != null) {
                wr.addField((TiffWriter.FieldBase) new TiffWriter.FieldUndefined(34675, this.icc));
            }
            wr.writeFile(ms);
            this.streamContentType = ImageBytesType.CCITT;
            this.imageBytes = ms.toByteArray();
            return;
        }
        PngWriter png = new PngWriter(ms);
        if (decode != null
                && this.pngBitDepth == 1) {
            if (decode.getAsNumber(0).intValue() == 1 && decode.getAsNumber(1).intValue() == 0) {
                int len = this.imageBytes.length;
                for (int t = 0; t < len; t++) {
                    this.imageBytes[t] = (byte) (this.imageBytes[t] ^ 0xFF);
                }
            }
        }

        png.writeHeader(this.width, this.height, this.pngBitDepth, this.pngColorType);
        if (this.icc != null) {
            png.writeIccProfile(this.icc);
        }
        if (this.palette != null) {
            png.writePalette(this.palette);
        }
        png.writeData(this.imageBytes, this.stride);
        png.writeEnd();
        this.streamContentType = ImageBytesType.PNG;
        this.imageBytes = ms.toByteArray();
    }

    public byte[] getImageAsBytes() {
        return this.imageBytes;
    }

    public BufferedImage getBufferedImage() throws IOException {
        byte[] img = getImageAsBytes();
        if (img == null) {
            return null;
        }
        return ImageIO.read(new ByteArrayInputStream(img));
    }
}
