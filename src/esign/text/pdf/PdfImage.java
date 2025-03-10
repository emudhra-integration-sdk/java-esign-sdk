package esign.text.pdf;

import esign.text.Image;
import esign.text.error_messages.MessageLocalization;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfImage
        extends PdfStream {

    static final int TRANSFERSIZE = 4096;
    protected PdfName name = null;

    protected Image image = null;

    public PdfImage(Image image, String name, PdfIndirectReference maskRef) throws BadPdfFormatException {
        this.image = image;
        if (name == null) {
            generateImgResName(image);
        } else {
            this.name = new PdfName(name);
        }
        put(PdfName.TYPE, PdfName.XOBJECT);
        put(PdfName.SUBTYPE, PdfName.IMAGE);
        put(PdfName.WIDTH, new PdfNumber(image.getWidth()));
        put(PdfName.HEIGHT, new PdfNumber(image.getHeight()));
        if (image.getLayer() != null) {
            put(PdfName.OC, image.getLayer().getRef());
        }
        if (image.isMask() && (image.getBpc() == 1 || image.getBpc() > 255)) {
            put(PdfName.IMAGEMASK, PdfBoolean.PDFTRUE);
        }
        if (maskRef != null) {
            if (image.isSmask()) {
                put(PdfName.SMASK, maskRef);
            } else {
                put(PdfName.MASK, maskRef);
            }
        }
        if (image.isMask() && image.isInverted()) {
            put(PdfName.DECODE, new PdfLiteral("[1 0]"));
        }
        if (image.isInterpolation()) {
            put(PdfName.INTERPOLATE, PdfBoolean.PDFTRUE);
        }
        InputStream is = null;
        try {
            String errorID;
            int[] transparency = image.getTransparency();
            if (transparency != null && !image.isMask() && maskRef == null) {
                StringBuilder s = new StringBuilder("[");
                for (int k = 0; k < transparency.length; k++) {
                    s.append(transparency[k]).append(" ");
                }
                s.append("]");
                put(PdfName.MASK, new PdfLiteral(s.toString()));
            }

            if (image.isImgRaw()) {

                int colorspace = image.getColorspace();
                this.bytes = image.getRawData();
                put(PdfName.LENGTH, new PdfNumber(this.bytes.length));
                int bpc = image.getBpc();
                if (bpc > 255) {
                    if (!image.isMask()) {
                        put(PdfName.COLORSPACE, PdfName.DEVICEGRAY);
                    }
                    put(PdfName.BITSPERCOMPONENT, new PdfNumber(1));
                    put(PdfName.FILTER, PdfName.CCITTFAXDECODE);
                    int k = bpc - 257;
                    PdfDictionary decodeparms = new PdfDictionary();
                    if (k != 0) {
                        decodeparms.put(PdfName.K, new PdfNumber(k));
                    }
                    if ((colorspace & 0x1) != 0) {
                        decodeparms.put(PdfName.BLACKIS1, PdfBoolean.PDFTRUE);
                    }
                    if ((colorspace & 0x2) != 0) {
                        decodeparms.put(PdfName.ENCODEDBYTEALIGN, PdfBoolean.PDFTRUE);
                    }
                    if ((colorspace & 0x4) != 0) {
                        decodeparms.put(PdfName.ENDOFLINE, PdfBoolean.PDFTRUE);
                    }
                    if ((colorspace & 0x8) != 0) {
                        decodeparms.put(PdfName.ENDOFBLOCK, PdfBoolean.PDFFALSE);
                    }
                    decodeparms.put(PdfName.COLUMNS, new PdfNumber(image.getWidth()));
                    decodeparms.put(PdfName.ROWS, new PdfNumber(image.getHeight()));
                    put(PdfName.DECODEPARMS, decodeparms);
                } else {

                    switch (colorspace) {
                        case 1:
                            put(PdfName.COLORSPACE, PdfName.DEVICEGRAY);
                            if (image.isInverted()) {
                                put(PdfName.DECODE, new PdfLiteral("[1 0]"));
                            }
                            break;
                        case 3:
                            put(PdfName.COLORSPACE, PdfName.DEVICERGB);
                            if (image.isInverted()) {
                                put(PdfName.DECODE, new PdfLiteral("[1 0 1 0 1 0]"));
                            }
                            break;
                        default:
                            put(PdfName.COLORSPACE, PdfName.DEVICECMYK);
                            if (image.isInverted()) {
                                put(PdfName.DECODE, new PdfLiteral("[1 0 1 0 1 0 1 0]"));
                            }
                            break;
                    }
                    PdfDictionary additional = image.getAdditional();
                    if (additional != null) {
                        putAll(additional);
                    }
                    if (image.isMask() && (image.getBpc() == 1 || image.getBpc() > 8)) {
                        remove(PdfName.COLORSPACE);
                    }
                    put(PdfName.BITSPERCOMPONENT, new PdfNumber(image.getBpc()));
                    if (image.isDeflated()) {
                        put(PdfName.FILTER, PdfName.FLATEDECODE);
                    } else {
                        flateCompress(image.getCompressionLevel());
                    }
                }

                return;
            }

            if (image.getRawData() == null) {
                is = image.getUrl().openStream();
                errorID = image.getUrl().toString();
            } else {

                is = new ByteArrayInputStream(image.getRawData());
                errorID = "Byte array";
            }
            switch (image.type()) {
                case 32:
                    put(PdfName.FILTER, PdfName.DCTDECODE);
                    if (image.getColorTransform() == 0) {
                        PdfDictionary decodeparms = new PdfDictionary();
                        decodeparms.put(PdfName.COLORTRANSFORM, new PdfNumber(0));
                        put(PdfName.DECODEPARMS, decodeparms);
                    }
                    switch (image.getColorspace()) {
                        case 1:
                            put(PdfName.COLORSPACE, PdfName.DEVICEGRAY);
                            break;
                        case 3:
                            put(PdfName.COLORSPACE, PdfName.DEVICERGB);
                            break;
                        default:
                            put(PdfName.COLORSPACE, PdfName.DEVICECMYK);
                            if (image.isInverted()) {
                                put(PdfName.DECODE, new PdfLiteral("[1 0 1 0 1 0 1 0]"));
                            }
                            break;
                    }
                    put(PdfName.BITSPERCOMPONENT, new PdfNumber(8));
                    if (image.getRawData() != null) {
                        this.bytes = image.getRawData();
                        put(PdfName.LENGTH, new PdfNumber(this.bytes.length));
                        return;
                    }
                    this.streamBytes = new ByteArrayOutputStream();
                    transferBytes(is, this.streamBytes, -1);
                    break;
                case 33:
                    put(PdfName.FILTER, PdfName.JPXDECODE);
                    if (image.getColorspace() > 0) {
                        switch (image.getColorspace()) {
                            case 1:
                                put(PdfName.COLORSPACE, PdfName.DEVICEGRAY);
                                break;
                            case 3:
                                put(PdfName.COLORSPACE, PdfName.DEVICERGB);
                                break;
                            default:
                                put(PdfName.COLORSPACE, PdfName.DEVICECMYK);
                                break;
                        }
                        put(PdfName.BITSPERCOMPONENT, new PdfNumber(image.getBpc()));
                    }
                    if (image.getRawData() != null) {
                        this.bytes = image.getRawData();
                        put(PdfName.LENGTH, new PdfNumber(this.bytes.length));
                        return;
                    }
                    this.streamBytes = new ByteArrayOutputStream();
                    transferBytes(is, this.streamBytes, -1);
                    break;
                case 36:
                    put(PdfName.FILTER, PdfName.JBIG2DECODE);
                    put(PdfName.COLORSPACE, PdfName.DEVICEGRAY);
                    put(PdfName.BITSPERCOMPONENT, new PdfNumber(1));
                    if (image.getRawData() != null) {
                        this.bytes = image.getRawData();
                        put(PdfName.LENGTH, new PdfNumber(this.bytes.length));
                        return;
                    }
                    this.streamBytes = new ByteArrayOutputStream();
                    transferBytes(is, this.streamBytes, -1);
                    break;
                default:
                    throw new BadPdfFormatException(MessageLocalization.getComposedMessage("1.is.an.unknown.image.format", new Object[]{errorID}));
            }
            if (image.getCompressionLevel() > 0) {
                flateCompress(image.getCompressionLevel());
            }
            put(PdfName.LENGTH, new PdfNumber(this.streamBytes.size()));
        } catch (IOException ioe) {
            throw new BadPdfFormatException(ioe.getMessage());
        } finally {

            if (is != null) {
                try {
                    is.close();
                } catch (Exception exception) {
                }
            }
        }
    }

    public PdfName name() {
        return this.name;
    }

    public Image getImage() {
        return this.image;
    }

    static void transferBytes(InputStream in, OutputStream out, int len) throws IOException {
        byte[] buffer = new byte[4096];
        if (len < 0) {
            len = 2147418112;
        }
        while (len != 0) {
            int size = in.read(buffer, 0, Math.min(len, 4096));
            if (size < 0) {
                return;
            }
            out.write(buffer, 0, size);
            len -= size;
        }
    }

    protected void importAll(PdfImage dup) {
        this.name = dup.name;
        this.compressed = dup.compressed;
        this.compressionLevel = dup.compressionLevel;
        this.streamBytes = dup.streamBytes;
        this.bytes = dup.bytes;
        this.hashMap = dup.hashMap;
    }

    private void generateImgResName(Image img) {
        this.name = new PdfName("img" + Long.toHexString(img.getMySerialId().longValue()));
    }
}
