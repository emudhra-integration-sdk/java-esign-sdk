package esign.text.pdf.codec;

import esign.text.BadElementException;
import esign.text.ExceptionConverter;
import esign.text.Image;
import esign.text.ImgRaw;
import esign.text.Utilities;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;

public class BmpImage {

    private InputStream inputStream;
    private long bitmapFileSize;
    private long bitmapOffset;
    private long compression;
    private long imageSize;
    private byte[] palette;
    private int imageType;
    private int numBands;
    private boolean isBottomUp;
    private int bitsPerPixel;
    private int redMask;
    private int greenMask;
    private int blueMask;
    private int alphaMask;
    public HashMap<String, Object> properties = new HashMap<String, Object>();

    private long xPelsPerMeter;

    private long yPelsPerMeter;

    private static final int VERSION_2_1_BIT = 0;

    private static final int VERSION_2_4_BIT = 1;

    private static final int VERSION_2_8_BIT = 2;

    private static final int VERSION_2_24_BIT = 3;

    private static final int VERSION_3_1_BIT = 4;

    private static final int VERSION_3_4_BIT = 5;

    private static final int VERSION_3_8_BIT = 6;
    private static final int VERSION_3_24_BIT = 7;
    private static final int VERSION_3_NT_16_BIT = 8;
    private static final int VERSION_3_NT_32_BIT = 9;
    private static final int VERSION_4_1_BIT = 10;
    private static final int VERSION_4_4_BIT = 11;
    private static final int VERSION_4_8_BIT = 12;
    private static final int VERSION_4_16_BIT = 13;
    private static final int VERSION_4_24_BIT = 14;
    private static final int VERSION_4_32_BIT = 15;
    private static final int LCS_CALIBRATED_RGB = 0;
    private static final int LCS_sRGB = 1;
    private static final int LCS_CMYK = 2;
    private static final int BI_RGB = 0;
    private static final int BI_RLE8 = 1;
    private static final int BI_RLE4 = 2;
    private static final int BI_BITFIELDS = 3;
    int width;
    int height;

    BmpImage(InputStream is, boolean noHeader, int size) throws IOException {
        this.bitmapFileSize = size;
        this.bitmapOffset = 0L;
        process(is, noHeader);
    }

    public static Image getImage(URL url) throws IOException {
        InputStream is = null;
        try {
            is = url.openStream();
            Image img = getImage(is);
            img.setUrl(url);
            return img;
        } finally {

            if (is != null) {
                is.close();
            }
        }
    }

    public static Image getImage(InputStream is) throws IOException {
        return getImage(is, false, 0);
    }

    public static Image getImage(InputStream is, boolean noHeader, int size) throws IOException {
        BmpImage bmp = new BmpImage(is, noHeader, size);
        try {
            Image img = bmp.getImage();
            img.setDpi((int) (bmp.xPelsPerMeter * 0.0254D + 0.5D), (int) (bmp.yPelsPerMeter * 0.0254D + 0.5D));
            img.setOriginalType(4);
            return img;
        } catch (BadElementException be) {
            throw new ExceptionConverter(be);
        }
    }

    public static Image getImage(String file) throws IOException {
        return getImage(Utilities.toURL(file));
    }

    public static Image getImage(byte[] data) throws IOException {
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        Image img = getImage(is);
        img.setOriginalData(data);
        return img;
    }

    protected void process(InputStream stream, boolean noHeader) throws IOException {
        if (noHeader || stream instanceof BufferedInputStream) {
            this.inputStream = stream;
        } else {
            this.inputStream = new BufferedInputStream(stream);
        }
        if (!noHeader) {

            if (readUnsignedByte(this.inputStream) != 66
                    || readUnsignedByte(this.inputStream) != 77) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("invalid.magic.value.for.bmp.file", new Object[0]));
            }

            this.bitmapFileSize = readDWord(this.inputStream);

            readWord(this.inputStream);
            readWord(this.inputStream);

            this.bitmapOffset = readDWord(this.inputStream);
        }

        long size = readDWord(this.inputStream);

        if (size == 12L) {
            this.width = readWord(this.inputStream);
            this.height = readWord(this.inputStream);
        } else {
            this.width = readLong(this.inputStream);
            this.height = readLong(this.inputStream);
        }

        int planes = readWord(this.inputStream);
        this.bitsPerPixel = readWord(this.inputStream);

        this.properties.put("color_planes", Integer.valueOf(planes));
        this.properties.put("bits_per_pixel", Integer.valueOf(this.bitsPerPixel));

        this.numBands = 3;
        if (this.bitmapOffset == 0L) {
            this.bitmapOffset = size;
        }
        if (size == 12L) {

            this.properties.put("bmp_version", "BMP v. 2.x");

            if (this.bitsPerPixel == 1) {
                this.imageType = 0;
            } else if (this.bitsPerPixel == 4) {
                this.imageType = 1;
            } else if (this.bitsPerPixel == 8) {
                this.imageType = 2;
            } else if (this.bitsPerPixel == 24) {
                this.imageType = 3;
            }

            int numberOfEntries = (int) ((this.bitmapOffset - 14L - size) / 3L);
            int sizeOfPalette = numberOfEntries * 3;
            if (this.bitmapOffset == size) {
                switch (this.imageType) {
                    case 0:
                        sizeOfPalette = 6;
                        break;
                    case 1:
                        sizeOfPalette = 48;
                        break;
                    case 2:
                        sizeOfPalette = 768;
                        break;
                    case 3:
                        sizeOfPalette = 0;
                        break;
                }
                this.bitmapOffset = size + sizeOfPalette;
            }
            readPalette(sizeOfPalette);
        } else {

            this.compression = readDWord(this.inputStream);
            this.imageSize = readDWord(this.inputStream);
            this.xPelsPerMeter = readLong(this.inputStream);
            this.yPelsPerMeter = readLong(this.inputStream);
            long colorsUsed = readDWord(this.inputStream);
            long colorsImportant = readDWord(this.inputStream);

            switch ((int) this.compression) {
                case 0:
                    this.properties.put("compression", "BI_RGB");
                    break;

                case 1:
                    this.properties.put("compression", "BI_RLE8");
                    break;

                case 2:
                    this.properties.put("compression", "BI_RLE4");
                    break;

                case 3:
                    this.properties.put("compression", "BI_BITFIELDS");
                    break;
            }

            this.properties.put("x_pixels_per_meter", Long.valueOf(this.xPelsPerMeter));
            this.properties.put("y_pixels_per_meter", Long.valueOf(this.yPelsPerMeter));
            this.properties.put("colors_used", Long.valueOf(colorsUsed));
            this.properties.put("colors_important", Long.valueOf(colorsImportant));

            if (size == 40L || size == 52L || size == 56L) {
                int numberOfEntries;
                int sizeOfPalette;
                switch ((int) this.compression) {

                    case 0:
                    case 1:
                    case 2:
                        if (this.bitsPerPixel == 1) {
                            this.imageType = 4;
                        } else if (this.bitsPerPixel == 4) {
                            this.imageType = 5;
                        } else if (this.bitsPerPixel == 8) {
                            this.imageType = 6;
                        } else if (this.bitsPerPixel == 24) {
                            this.imageType = 7;
                        } else if (this.bitsPerPixel == 16) {
                            this.imageType = 8;
                            this.redMask = 31744;
                            this.greenMask = 992;
                            this.blueMask = 31;
                            this.properties.put("red_mask", Integer.valueOf(this.redMask));
                            this.properties.put("green_mask", Integer.valueOf(this.greenMask));
                            this.properties.put("blue_mask", Integer.valueOf(this.blueMask));
                        } else if (this.bitsPerPixel == 32) {
                            this.imageType = 9;
                            this.redMask = 16711680;
                            this.greenMask = 65280;
                            this.blueMask = 255;
                            this.properties.put("red_mask", Integer.valueOf(this.redMask));
                            this.properties.put("green_mask", Integer.valueOf(this.greenMask));
                            this.properties.put("blue_mask", Integer.valueOf(this.blueMask));
                        }

                        if (size >= 52L) {
                            this.redMask = (int) readDWord(this.inputStream);
                            this.greenMask = (int) readDWord(this.inputStream);
                            this.blueMask = (int) readDWord(this.inputStream);
                            this.properties.put("red_mask", Integer.valueOf(this.redMask));
                            this.properties.put("green_mask", Integer.valueOf(this.greenMask));
                            this.properties.put("blue_mask", Integer.valueOf(this.blueMask));
                        }

                        if (size == 56L) {
                            this.alphaMask = (int) readDWord(this.inputStream);
                            this.properties.put("alpha_mask", Integer.valueOf(this.alphaMask));
                        }

                        numberOfEntries = (int) ((this.bitmapOffset - 14L - size) / 4L);
                        sizeOfPalette = numberOfEntries * 4;
                        if (this.bitmapOffset == size) {
                            switch (this.imageType) {
                                case 4:
                                    sizeOfPalette = (int) ((colorsUsed == 0L) ? 2L : colorsUsed) * 4;
                                    break;
                                case 5:
                                    sizeOfPalette = (int) ((colorsUsed == 0L) ? 16L : colorsUsed) * 4;
                                    break;
                                case 6:
                                    sizeOfPalette = (int) ((colorsUsed == 0L) ? 256L : colorsUsed) * 4;
                                    break;
                                default:
                                    sizeOfPalette = 0;
                                    break;
                            }
                            this.bitmapOffset = size + sizeOfPalette;
                        }
                        readPalette(sizeOfPalette);

                        this.properties.put("bmp_version", "BMP v. 3.x");
                        break;

                    case 3:
                        if (this.bitsPerPixel == 16) {
                            this.imageType = 8;
                        } else if (this.bitsPerPixel == 32) {
                            this.imageType = 9;
                        }

                        this.redMask = (int) readDWord(this.inputStream);
                        this.greenMask = (int) readDWord(this.inputStream);
                        this.blueMask = (int) readDWord(this.inputStream);

                        if (size == 56L) {
                            this.alphaMask = (int) readDWord(this.inputStream);
                            this.properties.put("alpha_mask", Integer.valueOf(this.alphaMask));
                        }

                        this.properties.put("red_mask", Integer.valueOf(this.redMask));
                        this.properties.put("green_mask", Integer.valueOf(this.greenMask));
                        this.properties.put("blue_mask", Integer.valueOf(this.blueMask));

                        if (colorsUsed != 0L) {

                            sizeOfPalette = (int) colorsUsed * 4;
                            readPalette(sizeOfPalette);
                        }

                        this.properties.put("bmp_version", "BMP v. 3.x NT");
                        break;

                    default:
                        throw new RuntimeException("Invalid compression specified in BMP file.");
                }

            } else if (size == 108L) {

                this.properties.put("bmp_version", "BMP v. 4.x");

                this.redMask = (int) readDWord(this.inputStream);
                this.greenMask = (int) readDWord(this.inputStream);
                this.blueMask = (int) readDWord(this.inputStream);

                this.alphaMask = (int) readDWord(this.inputStream);
                long csType = readDWord(this.inputStream);
                int redX = readLong(this.inputStream);
                int redY = readLong(this.inputStream);
                int redZ = readLong(this.inputStream);
                int greenX = readLong(this.inputStream);
                int greenY = readLong(this.inputStream);
                int greenZ = readLong(this.inputStream);
                int blueX = readLong(this.inputStream);
                int blueY = readLong(this.inputStream);
                int blueZ = readLong(this.inputStream);
                long gammaRed = readDWord(this.inputStream);
                long gammaGreen = readDWord(this.inputStream);
                long gammaBlue = readDWord(this.inputStream);

                if (this.bitsPerPixel == 1) {
                    this.imageType = 10;
                } else if (this.bitsPerPixel == 4) {
                    this.imageType = 11;
                } else if (this.bitsPerPixel == 8) {
                    this.imageType = 12;
                } else if (this.bitsPerPixel == 16) {
                    this.imageType = 13;
                    if ((int) this.compression == 0) {
                        this.redMask = 31744;
                        this.greenMask = 992;
                        this.blueMask = 31;
                    }
                } else if (this.bitsPerPixel == 24) {
                    this.imageType = 14;
                } else if (this.bitsPerPixel == 32) {
                    this.imageType = 15;
                    if ((int) this.compression == 0) {
                        this.redMask = 16711680;
                        this.greenMask = 65280;
                        this.blueMask = 255;
                    }
                }

                this.properties.put("red_mask", Integer.valueOf(this.redMask));
                this.properties.put("green_mask", Integer.valueOf(this.greenMask));
                this.properties.put("blue_mask", Integer.valueOf(this.blueMask));
                this.properties.put("alpha_mask", Integer.valueOf(this.alphaMask));

                int numberOfEntries = (int) ((this.bitmapOffset - 14L - size) / 4L);
                int sizeOfPalette = numberOfEntries * 4;
                if (this.bitmapOffset == size) {
                    switch (this.imageType) {
                        case 10:
                            sizeOfPalette = (int) ((colorsUsed == 0L) ? 2L : colorsUsed) * 4;
                            break;
                        case 11:
                            sizeOfPalette = (int) ((colorsUsed == 0L) ? 16L : colorsUsed) * 4;
                            break;
                        case 12:
                            sizeOfPalette = (int) ((colorsUsed == 0L) ? 256L : colorsUsed) * 4;
                            break;
                        default:
                            sizeOfPalette = 0;
                            break;
                    }
                    this.bitmapOffset = size + sizeOfPalette;
                }
                readPalette(sizeOfPalette);

                switch ((int) csType) {

                    case 0:
                        this.properties.put("color_space", "LCS_CALIBRATED_RGB");
                        this.properties.put("redX", Integer.valueOf(redX));
                        this.properties.put("redY", Integer.valueOf(redY));
                        this.properties.put("redZ", Integer.valueOf(redZ));
                        this.properties.put("greenX", Integer.valueOf(greenX));
                        this.properties.put("greenY", Integer.valueOf(greenY));
                        this.properties.put("greenZ", Integer.valueOf(greenZ));
                        this.properties.put("blueX", Integer.valueOf(blueX));
                        this.properties.put("blueY", Integer.valueOf(blueY));
                        this.properties.put("blueZ", Integer.valueOf(blueZ));
                        this.properties.put("gamma_red", Long.valueOf(gammaRed));
                        this.properties.put("gamma_green", Long.valueOf(gammaGreen));
                        this.properties.put("gamma_blue", Long.valueOf(gammaBlue));

                        throw new RuntimeException("Not implemented yet.");

                    case 1:
                        this.properties.put("color_space", "LCS_sRGB");
                        break;

                    case 2:
                        this.properties.put("color_space", "LCS_CMYK");

                        throw new RuntimeException("Not implemented yet.");
                }

            } else {
                this.properties.put("bmp_version", "BMP v. 5.x");
                throw new RuntimeException("BMP version 5 not implemented yet.");
            }
        }

        if (this.height > 0) {

            this.isBottomUp = true;
        } else {

            this.isBottomUp = false;
            this.height = Math.abs(this.height);
        }

        if (this.bitsPerPixel == 1 || this.bitsPerPixel == 4 || this.bitsPerPixel == 8) {

            this.numBands = 1;

            if (this.imageType == 0 || this.imageType == 1 || this.imageType == 2) {

                int sizep = this.palette.length / 3;

                if (sizep > 256) {
                    sizep = 256;
                }

                byte[] r = new byte[sizep];
                byte[] g = new byte[sizep];
                byte[] b = new byte[sizep];
                for (int i = 0; i < sizep; i++) {
                    int off = 3 * i;
                    b[i] = this.palette[off];
                    g[i] = this.palette[off + 1];
                    r[i] = this.palette[off + 2];
                }
            } else {
                int sizep = this.palette.length / 4;

                if (sizep > 256) {
                    sizep = 256;
                }

                byte[] r = new byte[sizep];
                byte[] g = new byte[sizep];
                byte[] b = new byte[sizep];
                for (int i = 0; i < sizep; i++) {
                    int off = 4 * i;
                    b[i] = this.palette[off];
                    g[i] = this.palette[off + 1];
                    r[i] = this.palette[off + 2];
                }

            }
        } else if (this.bitsPerPixel == 16) {
            this.numBands = 3;
        } else if (this.bitsPerPixel == 32) {
            this.numBands = (this.alphaMask == 0) ? 3 : 4;

        } else {

            this.numBands = 3;
        }
    }

    private byte[] getPalette(int group) {
        if (this.palette == null) {
            return null;
        }
        byte[] np = new byte[this.palette.length / group * 3];
        int e = this.palette.length / group;
        for (int k = 0; k < e; k++) {
            int src = k * group;
            int dest = k * 3;
            np[dest + 2] = this.palette[src++];
            np[dest + 1] = this.palette[src++];
            np[dest] = this.palette[src];
        }
        return np;
    }

    private Image getImage() throws IOException, BadElementException {
        byte[] bdata = null;

        switch (this.imageType) {

            case 0:
                return read1Bit(3);

            case 1:
                return read4Bit(3);

            case 2:
                return read8Bit(3);

            case 3:
                bdata = new byte[this.width * this.height * 3];
                read24Bit(bdata);
                return (Image) new ImgRaw(this.width, this.height, 3, 8, bdata);

            case 4:
                return read1Bit(4);

            case 5:
                switch ((int) this.compression) {
                    case 0:
                        return read4Bit(4);

                    case 2:
                        return readRLE4();
                }

                throw new RuntimeException("Invalid compression specified for BMP file.");

            case 6:
                switch ((int) this.compression) {
                    case 0:
                        return read8Bit(4);

                    case 1:
                        return readRLE8();
                }

                throw new RuntimeException("Invalid compression specified for BMP file.");

            case 7:
                bdata = new byte[this.width * this.height * 3];
                read24Bit(bdata);
                return (Image) new ImgRaw(this.width, this.height, 3, 8, bdata);

            case 8:
                return read1632Bit(false);

            case 9:
                return read1632Bit(true);

            case 10:
                return read1Bit(4);

            case 11:
                switch ((int) this.compression) {

                    case 0:
                        return read4Bit(4);

                    case 2:
                        return readRLE4();
                }

                throw new RuntimeException("Invalid compression specified for BMP file.");

            case 12:
                switch ((int) this.compression) {

                    case 0:
                        return read8Bit(4);

                    case 1:
                        return readRLE8();
                }

                throw new RuntimeException("Invalid compression specified for BMP file.");

            case 13:
                return read1632Bit(false);

            case 14:
                bdata = new byte[this.width * this.height * 3];
                read24Bit(bdata);
                return (Image) new ImgRaw(this.width, this.height, 3, 8, bdata);

            case 15:
                return read1632Bit(true);
        }
        return null;
    }

    private Image indexedModel(byte[] bdata, int bpc, int paletteEntries) throws BadElementException {
        ImgRaw imgRaw = new ImgRaw(this.width, this.height, 1, bpc, bdata);
        PdfArray colorspace = new PdfArray();
        colorspace.add((PdfObject) PdfName.INDEXED);
        colorspace.add((PdfObject) PdfName.DEVICERGB);
        byte[] np = getPalette(paletteEntries);
        int len = np.length;
        colorspace.add((PdfObject) new PdfNumber(len / 3 - 1));
        colorspace.add((PdfObject) new PdfString(np));
        PdfDictionary ad = new PdfDictionary();
        ad.put(PdfName.COLORSPACE, (PdfObject) colorspace);
        imgRaw.setAdditional(ad);
        return (Image) imgRaw;
    }

    private void readPalette(int sizeOfPalette) throws IOException {
        if (sizeOfPalette == 0) {
            return;
        }

        this.palette = new byte[sizeOfPalette];
        int bytesRead = 0;
        while (bytesRead < sizeOfPalette) {
            int r = this.inputStream.read(this.palette, bytesRead, sizeOfPalette - bytesRead);
            if (r < 0) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("incomplete.palette", new Object[0]));
            }
            bytesRead += r;
        }
        this.properties.put("palette", this.palette);
    }

    private Image read1Bit(int paletteEntries) throws IOException, BadElementException {
        byte[] bdata = new byte[(this.width + 7) / 8 * this.height];
        int padding = 0;
        int bytesPerScanline = (int) Math.ceil(this.width / 8.0D);

        int remainder = bytesPerScanline % 4;
        if (remainder != 0) {
            padding = 4 - remainder;
        }

        int imSize = (bytesPerScanline + padding) * this.height;

        byte[] values = new byte[imSize];
        int bytesRead = 0;
        while (bytesRead < imSize) {
            bytesRead += this.inputStream.read(values, bytesRead, imSize - bytesRead);
        }

        if (this.isBottomUp) {

            for (int i = 0; i < this.height; i++) {
                System.arraycopy(values, imSize - (i + 1) * (bytesPerScanline + padding), bdata, i * bytesPerScanline, bytesPerScanline);

            }

        } else {

            for (int i = 0; i < this.height; i++) {
                System.arraycopy(values, i * (bytesPerScanline + padding), bdata, i * bytesPerScanline, bytesPerScanline);
            }
        }

        return indexedModel(bdata, 1, paletteEntries);
    }

    private Image read4Bit(int paletteEntries) throws IOException, BadElementException {
        byte[] bdata = new byte[(this.width + 1) / 2 * this.height];

        int padding = 0;

        int bytesPerScanline = (int) Math.ceil(this.width / 2.0D);
        int remainder = bytesPerScanline % 4;
        if (remainder != 0) {
            padding = 4 - remainder;
        }

        int imSize = (bytesPerScanline + padding) * this.height;

        byte[] values = new byte[imSize];
        int bytesRead = 0;
        while (bytesRead < imSize) {
            bytesRead += this.inputStream.read(values, bytesRead, imSize - bytesRead);
        }

        if (this.isBottomUp) {

            for (int i = 0; i < this.height; i++) {
                System.arraycopy(values, imSize - (i + 1) * (bytesPerScanline + padding), bdata, i * bytesPerScanline, bytesPerScanline);

            }

        } else {

            for (int i = 0; i < this.height; i++) {
                System.arraycopy(values, i * (bytesPerScanline + padding), bdata, i * bytesPerScanline, bytesPerScanline);
            }
        }

        return indexedModel(bdata, 4, paletteEntries);
    }

    private Image read8Bit(int paletteEntries) throws IOException, BadElementException {
        byte[] bdata = new byte[this.width * this.height];

        int padding = 0;

        int bitsPerScanline = this.width * 8;
        if (bitsPerScanline % 32 != 0) {
            padding = (bitsPerScanline / 32 + 1) * 32 - bitsPerScanline;
            padding = (int) Math.ceil(padding / 8.0D);
        }

        int imSize = (this.width + padding) * this.height;

        byte[] values = new byte[imSize];
        int bytesRead = 0;
        while (bytesRead < imSize) {
            bytesRead += this.inputStream.read(values, bytesRead, imSize - bytesRead);
        }

        if (this.isBottomUp) {

            for (int i = 0; i < this.height; i++) {
                System.arraycopy(values, imSize - (i + 1) * (this.width + padding), bdata, i * this.width, this.width);

            }

        } else {

            for (int i = 0; i < this.height; i++) {
                System.arraycopy(values, i * (this.width + padding), bdata, i * this.width, this.width);
            }
        }

        return indexedModel(bdata, 8, paletteEntries);
    }

    private void read24Bit(byte[] bdata) {
        int padding = 0;

        int bitsPerScanline = this.width * 24;
        if (bitsPerScanline % 32 != 0) {
            padding = (bitsPerScanline / 32 + 1) * 32 - bitsPerScanline;
            padding = (int) Math.ceil(padding / 8.0D);
        }

        int imSize = (this.width * 3 + 3) / 4 * 4 * this.height;

        byte[] values = new byte[imSize];
        try {
            int bytesRead = 0;
            while (bytesRead < imSize) {
                int r = this.inputStream.read(values, bytesRead, imSize - bytesRead);

                if (r < 0) {
                    break;
                }
                bytesRead += r;
            }
        } catch (IOException ioe) {
            throw new ExceptionConverter(ioe);
        }

        int l = 0;

        if (this.isBottomUp) {
            int max = this.width * this.height * 3 - 1;

            int count = -padding;
            for (int i = 0; i < this.height; i++) {
                l = max - (i + 1) * this.width * 3 + 1;
                count += padding;
                for (int j = 0; j < this.width; j++) {
                    bdata[l + 2] = values[count++];
                    bdata[l + 1] = values[count++];
                    bdata[l] = values[count++];
                    l += 3;
                }
            }
        } else {
            int count = -padding;
            for (int i = 0; i < this.height; i++) {
                count += padding;
                for (int j = 0; j < this.width; j++) {
                    bdata[l + 2] = values[count++];
                    bdata[l + 1] = values[count++];
                    bdata[l] = values[count++];
                    l += 3;
                }
            }
        }
    }

    private int findMask(int mask) {
        int k = 0;
        for (; k < 32 && (mask & 0x1) != 1; k++) {
            mask >>>= 1;
        }
        return mask;
    }

    private int findShift(int mask) {
        int k = 0;
        for (; k < 32 && (mask & 0x1) != 1; k++) {
            mask >>>= 1;
        }
        return k;
    }

    private Image read1632Bit(boolean is32) throws IOException, BadElementException {
        int red_mask = findMask(this.redMask);
        int red_shift = findShift(this.redMask);
        int red_factor = red_mask + 1;
        int green_mask = findMask(this.greenMask);
        int green_shift = findShift(this.greenMask);
        int green_factor = green_mask + 1;
        int blue_mask = findMask(this.blueMask);
        int blue_shift = findShift(this.blueMask);
        int blue_factor = blue_mask + 1;
        byte[] bdata = new byte[this.width * this.height * 3];

        int padding = 0;

        if (!is32) {

            int bitsPerScanline = this.width * 16;
            if (bitsPerScanline % 32 != 0) {
                padding = (bitsPerScanline / 32 + 1) * 32 - bitsPerScanline;
                padding = (int) Math.ceil(padding / 8.0D);
            }
        }

        int imSize = (int) this.imageSize;
        if (imSize == 0) {
            imSize = (int) (this.bitmapFileSize - this.bitmapOffset);
        }

        int l = 0;

        if (this.isBottomUp) {
            for (int i = this.height - 1; i >= 0; i--) {
                l = this.width * 3 * i;
                for (int j = 0; j < this.width; j++) {
                    int v;
                    if (is32) {
                        v = (int) readDWord(this.inputStream);
                    } else {
                        v = readWord(this.inputStream);
                    }
                    bdata[l++] = (byte) ((v >>> red_shift & red_mask) * 256 / red_factor);
                    bdata[l++] = (byte) ((v >>> green_shift & green_mask) * 256 / green_factor);
                    bdata[l++] = (byte) ((v >>> blue_shift & blue_mask) * 256 / blue_factor);
                }
                for (int m = 0; m < padding; m++) {
                    this.inputStream.read();
                }
            }
        } else {
            for (int i = 0; i < this.height; i++) {
                for (int j = 0; j < this.width; j++) {
                    int v;
                    if (is32) {
                        v = (int) readDWord(this.inputStream);
                    } else {
                        v = readWord(this.inputStream);
                    }
                    bdata[l++] = (byte) ((v >>> red_shift & red_mask) * 256 / red_factor);
                    bdata[l++] = (byte) ((v >>> green_shift & green_mask) * 256 / green_factor);
                    bdata[l++] = (byte) ((v >>> blue_shift & blue_mask) * 256 / blue_factor);
                }
                for (int m = 0; m < padding; m++) {
                    this.inputStream.read();
                }
            }
        }
        return (Image) new ImgRaw(this.width, this.height, 3, 8, bdata);
    }

    private Image readRLE8() throws IOException, BadElementException {
        int imSize = (int) this.imageSize;
        if (imSize == 0) {
            imSize = (int) (this.bitmapFileSize - this.bitmapOffset);
        }

        byte[] values = new byte[imSize];
        int bytesRead = 0;
        while (bytesRead < imSize) {
            bytesRead += this.inputStream.read(values, bytesRead, imSize - bytesRead);
        }

        byte[] val = decodeRLE(true, values);

        imSize = this.width * this.height;

        if (this.isBottomUp) {

            byte[] temp = new byte[val.length];
            int bytesPerScanline = this.width;
            for (int i = 0; i < this.height; i++) {
                System.arraycopy(val, imSize - (i + 1) * bytesPerScanline, temp, i * bytesPerScanline, bytesPerScanline);
            }

            val = temp;
        }
        return indexedModel(val, 8, 4);
    }

    private Image readRLE4() throws IOException, BadElementException {
        int imSize = (int) this.imageSize;
        if (imSize == 0) {
            imSize = (int) (this.bitmapFileSize - this.bitmapOffset);
        }

        byte[] values = new byte[imSize];
        int bytesRead = 0;
        while (bytesRead < imSize) {
            bytesRead += this.inputStream.read(values, bytesRead, imSize - bytesRead);
        }

        byte[] val = decodeRLE(false, values);

        if (this.isBottomUp) {

            byte[] inverted = val;
            val = new byte[this.width * this.height];
            int l = 0;

            for (int i = this.height - 1; i >= 0; i--) {
                int index = i * this.width;
                int lineEnd = l + this.width;
                while (l != lineEnd) {
                    val[l++] = inverted[index++];
                }
            }
        }
        int stride = (this.width + 1) / 2;
        byte[] bdata = new byte[stride * this.height];
        int ptr = 0;
        int sh = 0;
        for (int h = 0; h < this.height; h++) {
            for (int w = 0; w < this.width; w++) {
                if ((w & 0x1) == 0) {
                    bdata[sh + w / 2] = (byte) (val[ptr++] << 4);
                } else {
                    bdata[sh + w / 2] = (byte) (bdata[sh + w / 2] | (byte) (val[ptr++] & 0xF));
                }
            }
            sh += stride;
        }
        return indexedModel(bdata, 4, 4);
    }

    private byte[] decodeRLE(boolean is8, byte[] values) {
        byte[] val = new byte[this.width * this.height];
        try {
            int ptr = 0;
            int x = 0;
            int q = 0;
            for (int y = 0; y < this.height && ptr < values.length;) {
                int count = values[ptr++] & 0xFF;
                if (count != 0) {

                    int bt = values[ptr++] & 0xFF;
                    if (is8) {
                        for (int i = count; i != 0; i--) {
                            val[q++] = (byte) bt;
                        }
                    } else {

                        for (int i = 0; i < count; i++) {
                            val[q++] = (byte) (((i & 0x1) == 1) ? (bt & 0xF) : (bt >>> 4 & 0xF));
                        }
                    }
                    x += count;

                    continue;
                }
                count = values[ptr++] & 0xFF;
                if (count == 1) {
                    break;
                }
                switch (count) {
                    case 0:
                        x = 0;
                        y++;
                        q = y * this.width;
                        continue;

                    case 2:
                        x += values[ptr++] & 0xFF;
                        y += values[ptr++] & 0xFF;
                        q = y * this.width + x;
                        continue;
                }

                if (is8) {
                    for (int i = count; i != 0; i--) {
                        val[q++] = (byte) (values[ptr++] & 0xFF);
                    }
                } else {
                    int bt = 0;
                    for (int i = 0; i < count; i++) {
                        if ((i & 0x1) == 0) {
                            bt = values[ptr++] & 0xFF;
                        }
                        val[q++] = (byte) (((i & 0x1) == 1) ? (bt & 0xF) : (bt >>> 4 & 0xF));
                    }
                }
                x += count;

                if (is8) {
                    if ((count & 0x1) == 1) {
                        ptr++;
                    }
                    continue;
                }
                if ((count & 0x3) == 1 || (count & 0x3) == 2) {
                    ptr++;

                }

            }

        } catch (RuntimeException runtimeException) {
        }

        return val;
    }

    private int readUnsignedByte(InputStream stream) throws IOException {
        return stream.read() & 0xFF;
    }

    private int readUnsignedShort(InputStream stream) throws IOException {
        int b1 = readUnsignedByte(stream);
        int b2 = readUnsignedByte(stream);
        return (b2 << 8 | b1) & 0xFFFF;
    }

    private int readShort(InputStream stream) throws IOException {
        int b1 = readUnsignedByte(stream);
        int b2 = readUnsignedByte(stream);
        return b2 << 8 | b1;
    }

    private int readWord(InputStream stream) throws IOException {
        return readUnsignedShort(stream);
    }

    private long readUnsignedInt(InputStream stream) throws IOException {
        int b1 = readUnsignedByte(stream);
        int b2 = readUnsignedByte(stream);
        int b3 = readUnsignedByte(stream);
        int b4 = readUnsignedByte(stream);
        long l = (b4 << 24 | b3 << 16 | b2 << 8 | b1);
        return l & 0xFFFFFFFFFFFFFFFFL;
    }

    private int readInt(InputStream stream) throws IOException {
        int b1 = readUnsignedByte(stream);
        int b2 = readUnsignedByte(stream);
        int b3 = readUnsignedByte(stream);
        int b4 = readUnsignedByte(stream);
        return b4 << 24 | b3 << 16 | b2 << 8 | b1;
    }

    private long readDWord(InputStream stream) throws IOException {
        return readUnsignedInt(stream);
    }

    private int readLong(InputStream stream) throws IOException {
        return readInt(stream);
    }
}
