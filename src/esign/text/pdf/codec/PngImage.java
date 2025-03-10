package esign.text.pdf.codec;

import esign.text.ExceptionConverter;
import esign.text.Image;
import esign.text.ImgRaw;
import esign.text.Utilities;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.ByteBuffer;
import esign.text.pdf.ICC_Profile;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfLiteral;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import esign.text.pdf.PdfString;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class PngImage {

    public static final int[] PNGID = new int[]{137, 80, 78, 71, 13, 10, 26, 10};

    public static final String IHDR = "IHDR";

    public static final String PLTE = "PLTE";

    public static final String IDAT = "IDAT";

    public static final String IEND = "IEND";

    public static final String tRNS = "tRNS";

    public static final String pHYs = "pHYs";

    public static final String gAMA = "gAMA";

    public static final String cHRM = "cHRM";

    public static final String sRGB = "sRGB";

    public static final String iCCP = "iCCP";

    private static final int TRANSFERSIZE = 4096;

    private static final int PNG_FILTER_NONE = 0;

    private static final int PNG_FILTER_SUB = 1;

    private static final int PNG_FILTER_UP = 2;

    private static final int PNG_FILTER_AVERAGE = 3;

    private static final int PNG_FILTER_PAETH = 4;

    private static final PdfName[] intents = new PdfName[]{PdfName.PERCEPTUAL, PdfName.RELATIVECOLORIMETRIC, PdfName.SATURATION, PdfName.ABSOLUTECOLORIMETRIC};

    InputStream is;

    DataInputStream dataStream;
    int width;
    int height;
    int bitDepth;
    int colorType;
    int compressionMethod;
    int filterMethod;
    int interlaceMethod;
    PdfDictionary additional = new PdfDictionary();
    byte[] image;
    byte[] smask;
    byte[] trans;
    NewByteArrayOutputStream idat = new NewByteArrayOutputStream();
    int dpiX;
    int dpiY;
    float XYRatio;
    boolean genBWMask;
    boolean palShades;
    int transRedGray = -1;
    int transGreen = -1;
    int transBlue = -1;
    int inputBands;
    int bytesPerPixel;
    byte[] colorTable;
    float gamma = 1.0F;
    boolean hasCHRM = false;
    float xW;
    float yW;
    float xR;
    float yR;
    float xG;
    float yG;
    float xB;
    float yB;
    PdfName intent;
    ICC_Profile icc_profile;

    PngImage(InputStream is) {
        this.is = is;
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
        PngImage png = new PngImage(is);
        return png.getImage();
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

    boolean checkMarker(String s) {
        if (s.length() != 4) {
            return false;
        }
        for (int k = 0; k < 4; k++) {
            char c = s.charAt(k);
            if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z')) {
                return false;
            }
        }
        return true;
    }

    void readPng() throws IOException {
        for (int i = 0; i < PNGID.length; i++) {
            if (PNGID[i] != this.is.read()) {
                throw new IOException(MessageLocalization.getComposedMessage("file.is.not.a.valid.png", new Object[0]));
            }
        }
        byte[] buffer = new byte[4096];
        while (true) {
            int len = getInt(this.is);
            String marker = getString(this.is);
            if (len < 0 || !checkMarker(marker)) {
                throw new IOException(MessageLocalization.getComposedMessage("corrupted.png.file", new Object[0]));
            }
            if ("IDAT".equals(marker)) {
                while (len != 0) {
                    int size = this.is.read(buffer, 0, Math.min(len, 4096));
                    if (size < 0) {
                        return;
                    }
                    this.idat.write(buffer, 0, size);
                    len -= size;
                }
            } else if ("tRNS".equals(marker)) {
                switch (this.colorType) {
                    case 0:
                        if (len >= 2) {
                            len -= 2;
                            int gray = getWord(this.is);
                            if (this.bitDepth == 16) {
                                this.transRedGray = gray;
                                break;
                            }
                            this.additional.put(PdfName.MASK, (PdfObject) new PdfLiteral("[" + gray + " " + gray + "]"));
                        }
                        break;
                    case 2:
                        if (len >= 6) {
                            len -= 6;
                            int red = getWord(this.is);
                            int green = getWord(this.is);
                            int blue = getWord(this.is);
                            if (this.bitDepth == 16) {
                                this.transRedGray = red;
                                this.transGreen = green;
                                this.transBlue = blue;
                                break;
                            }
                            this.additional.put(PdfName.MASK, (PdfObject) new PdfLiteral("[" + red + " " + red + " " + green + " " + green + " " + blue + " " + blue + "]"));
                        }
                        break;
                    case 3:
                        if (len > 0) {
                            this.trans = new byte[len];
                            for (int k = 0; k < len; k++) {
                                this.trans[k] = (byte) this.is.read();
                            }
                            len = 0;
                        }
                        break;
                }
                Utilities.skip(this.is, len);
            } else if ("IHDR".equals(marker)) {
                this.width = getInt(this.is);
                this.height = getInt(this.is);

                this.bitDepth = this.is.read();
                this.colorType = this.is.read();
                this.compressionMethod = this.is.read();
                this.filterMethod = this.is.read();
                this.interlaceMethod = this.is.read();
            } else if ("PLTE".equals(marker)) {
                if (this.colorType == 3) {
                    PdfArray colorspace = new PdfArray();
                    colorspace.add((PdfObject) PdfName.INDEXED);
                    colorspace.add(getColorspace());
                    colorspace.add((PdfObject) new PdfNumber(len / 3 - 1));
                    ByteBuffer colortable = new ByteBuffer();
                    while (len-- > 0) {
                        colortable.append_i(this.is.read());
                    }
                    colorspace.add((PdfObject) new PdfString(this.colorTable = colortable.toByteArray()));
                    this.additional.put(PdfName.COLORSPACE, (PdfObject) colorspace);
                } else {

                    Utilities.skip(this.is, len);
                }
            } else if ("pHYs".equals(marker)) {
                int dx = getInt(this.is);
                int dy = getInt(this.is);
                int unit = this.is.read();
                if (unit == 1) {
                    this.dpiX = (int) (dx * 0.0254F + 0.5F);
                    this.dpiY = (int) (dy * 0.0254F + 0.5F);

                } else if (dy != 0) {
                    this.XYRatio = dx / dy;
                }
            } else if ("cHRM".equals(marker)) {
                this.xW = getInt(this.is) / 100000.0F;
                this.yW = getInt(this.is) / 100000.0F;
                this.xR = getInt(this.is) / 100000.0F;
                this.yR = getInt(this.is) / 100000.0F;
                this.xG = getInt(this.is) / 100000.0F;
                this.yG = getInt(this.is) / 100000.0F;
                this.xB = getInt(this.is) / 100000.0F;
                this.yB = getInt(this.is) / 100000.0F;
                this.hasCHRM = (Math.abs(this.xW) >= 1.0E-4F && Math.abs(this.yW) >= 1.0E-4F && Math.abs(this.xR) >= 1.0E-4F && Math.abs(this.yR) >= 1.0E-4F && Math.abs(this.xG) >= 1.0E-4F && Math.abs(this.yG) >= 1.0E-4F && Math.abs(this.xB) >= 1.0E-4F && Math.abs(this.yB) >= 1.0E-4F);
            } else if ("sRGB".equals(marker)) {
                int ri = this.is.read();
                this.intent = intents[ri];
                this.gamma = 2.2F;
                this.xW = 0.3127F;
                this.yW = 0.329F;
                this.xR = 0.64F;
                this.yR = 0.33F;
                this.xG = 0.3F;
                this.yG = 0.6F;
                this.xB = 0.15F;
                this.yB = 0.06F;
                this.hasCHRM = true;
            } else if ("gAMA".equals(marker)) {
                int gm = getInt(this.is);
                if (gm != 0) {
                    this.gamma = 100000.0F / gm;
                    if (!this.hasCHRM) {
                        this.xW = 0.3127F;
                        this.yW = 0.329F;
                        this.xR = 0.64F;
                        this.yR = 0.33F;
                        this.xG = 0.3F;
                        this.yG = 0.6F;
                        this.xB = 0.15F;
                        this.yB = 0.06F;
                        this.hasCHRM = true;
                    }

                }
            } else if ("iCCP".equals(marker)) {
                while (true) {
                    len--;
                    if (this.is.read() == 0) {
                        this.is.read();
                        len--;
                        byte[] icccom = new byte[len];
                        int p = 0;
                        while (len > 0) {
                            int r = this.is.read(icccom, p, len);
                            if (r < 0) {
                                throw new IOException(MessageLocalization.getComposedMessage("premature.end.of.file", new Object[0]));
                            }
                            p += r;
                            len -= r;
                        }
                        byte[] iccp = PdfReader.FlateDecode(icccom, true);
                        icccom = null;
                        try {
                            this.icc_profile = ICC_Profile.getInstance(iccp);
                            break;
                        } catch (RuntimeException e) {
                            this.icc_profile = null;
                        }
                    } else {
                        continue;
                    }

                    Utilities.skip(this.is, 4);
                }
            } else {
                if ("IEND".equals(marker)) {
                    break;
                }
                Utilities.skip(this.is, len);
            }
            Utilities.skip(this.is, 4);
        }
    }

    PdfObject getColorspace() {
        if (this.icc_profile != null) {
            if ((this.colorType & 0x2) == 0) {
                return (PdfObject) PdfName.DEVICEGRAY;
            }
            return (PdfObject) PdfName.DEVICERGB;
        }
        if (this.gamma == 1.0F && !this.hasCHRM) {
            if ((this.colorType & 0x2) == 0) {
                return (PdfObject) PdfName.DEVICEGRAY;
            }
            return (PdfObject) PdfName.DEVICERGB;
        }

        PdfArray array = new PdfArray();
        PdfDictionary dic = new PdfDictionary();
        if ((this.colorType & 0x2) == 0) {
            if (this.gamma == 1.0F) {
                return (PdfObject) PdfName.DEVICEGRAY;
            }
            array.add((PdfObject) PdfName.CALGRAY);
            dic.put(PdfName.GAMMA, (PdfObject) new PdfNumber(this.gamma));
            dic.put(PdfName.WHITEPOINT, (PdfObject) new PdfLiteral("[1 1 1]"));
            array.add((PdfObject) dic);
        } else {
            PdfArray pdfArray = new PdfArray();
            PdfLiteral pdfLiteral = new PdfLiteral("[1 1 1]");
            array.add((PdfObject) PdfName.CALRGB);
            if (this.gamma != 1.0F) {
                PdfArray gm = new PdfArray();
                PdfNumber n = new PdfNumber(this.gamma);
                gm.add((PdfObject) n);
                gm.add((PdfObject) n);
                gm.add((PdfObject) n);
                dic.put(PdfName.GAMMA, (PdfObject) gm);
            }
            if (this.hasCHRM) {
                float z = this.yW * ((this.xG - this.xB) * this.yR - (this.xR - this.xB) * this.yG + (this.xR - this.xG) * this.yB);
                float YA = this.yR * ((this.xG - this.xB) * this.yW - (this.xW - this.xB) * this.yG + (this.xW - this.xG) * this.yB) / z;
                float XA = YA * this.xR / this.yR;
                float ZA = YA * ((1.0F - this.xR) / this.yR - 1.0F);
                float YB = -this.yG * ((this.xR - this.xB) * this.yW - (this.xW - this.xB) * this.yR + (this.xW - this.xR) * this.yB) / z;
                float XB = YB * this.xG / this.yG;
                float ZB = YB * ((1.0F - this.xG) / this.yG - 1.0F);
                float YC = this.yB * ((this.xR - this.xG) * this.yW - (this.xW - this.xG) * this.yW + (this.xW - this.xR) * this.yG) / z;
                float XC = YC * this.xB / this.yB;
                float ZC = YC * ((1.0F - this.xB) / this.yB - 1.0F);
                float XW = XA + XB + XC;
                float YW = 1.0F;
                float ZW = ZA + ZB + ZC;
                PdfArray wpa = new PdfArray();
                wpa.add((PdfObject) new PdfNumber(XW));
                wpa.add((PdfObject) new PdfNumber(YW));
                wpa.add((PdfObject) new PdfNumber(ZW));
                pdfArray = wpa;
                PdfArray matrix = new PdfArray();
                matrix.add((PdfObject) new PdfNumber(XA));
                matrix.add((PdfObject) new PdfNumber(YA));
                matrix.add((PdfObject) new PdfNumber(ZA));
                matrix.add((PdfObject) new PdfNumber(XB));
                matrix.add((PdfObject) new PdfNumber(YB));
                matrix.add((PdfObject) new PdfNumber(ZB));
                matrix.add((PdfObject) new PdfNumber(XC));
                matrix.add((PdfObject) new PdfNumber(YC));
                matrix.add((PdfObject) new PdfNumber(ZC));
                dic.put(PdfName.MATRIX, (PdfObject) matrix);
            }
            dic.put(PdfName.WHITEPOINT, (PdfObject) pdfArray);
            array.add((PdfObject) dic);
        }
        return (PdfObject) array;
    }

    Image getImage() throws IOException {
        readPng();
        try {
            ImgRaw imgRaw = null;
            int pal0 = 0;
            int palIdx = 0;
            this.palShades = false;
            if (this.trans != null) {
                for (int k = 0; k < this.trans.length; k++) {
                    int n = this.trans[k] & 0xFF;
                    if (n == 0) {
                        pal0++;
                        palIdx = k;
                    }
                    if (n != 0 && n != 255) {
                        this.palShades = true;
                        break;
                    }
                }
            }
            if ((this.colorType & 0x4) != 0) {
                this.palShades = true;
            }
            this.genBWMask = (!this.palShades && (pal0 > 1 || this.transRedGray >= 0));
            if (!this.palShades && !this.genBWMask && pal0 == 1) {
                this.additional.put(PdfName.MASK, (PdfObject) new PdfLiteral("[" + palIdx + " " + palIdx + "]"));
            }
            boolean needDecode = (this.interlaceMethod == 1 || this.bitDepth == 16 || (this.colorType & 0x4) != 0 || this.palShades || this.genBWMask);
            switch (this.colorType) {
                case 0:
                    this.inputBands = 1;
                    break;
                case 2:
                    this.inputBands = 3;
                    break;
                case 3:
                    this.inputBands = 1;
                    break;
                case 4:
                    this.inputBands = 2;
                    break;
                case 6:
                    this.inputBands = 4;
                    break;
            }
            if (needDecode) {
                decodeIdat();
            }
            int components = this.inputBands;
            if ((this.colorType & 0x4) != 0) {
                components--;
            }
            int bpc = this.bitDepth;
            if (bpc == 16) {
                bpc = 8;
            }
            if (this.image != null) {
                if (this.colorType == 3) {
                    imgRaw = new ImgRaw(this.width, this.height, components, bpc, this.image);
                } else {
                    Image img = Image.getInstance(this.width, this.height, components, bpc, this.image);
                    imgRaw = new ImgRaw(img);
                }
            } else {
                imgRaw = new ImgRaw(this.width, this.height, components, bpc, this.idat.toByteArray());
                imgRaw.setDeflated(true);
                PdfDictionary decodeparms = new PdfDictionary();
                decodeparms.put(PdfName.BITSPERCOMPONENT, (PdfObject) new PdfNumber(this.bitDepth));
                decodeparms.put(PdfName.PREDICTOR, (PdfObject) new PdfNumber(15));
                decodeparms.put(PdfName.COLUMNS, (PdfObject) new PdfNumber(this.width));
                decodeparms.put(PdfName.COLORS, (PdfObject) new PdfNumber((this.colorType == 3 || (this.colorType & 0x2) == 0) ? 1 : 3));
                this.additional.put(PdfName.DECODEPARMS, (PdfObject) decodeparms);
            }
            if (this.additional.get(PdfName.COLORSPACE) == null) {
                this.additional.put(PdfName.COLORSPACE, getColorspace());
            }
            if (this.intent != null) {
                this.additional.put(PdfName.INTENT, (PdfObject) this.intent);
            }
            if (this.additional.size() > 0) {
                imgRaw.setAdditional(this.additional);
            }
            if (this.icc_profile != null) {
                imgRaw.tagICC(this.icc_profile);
            }
            if (this.palShades) {
                Image im2 = Image.getInstance(this.width, this.height, 1, 8, this.smask);
                im2.makeMask();
                imgRaw.setImageMask(im2);
            }
            if (this.genBWMask) {
                Image im2 = Image.getInstance(this.width, this.height, 1, 1, this.smask);
                im2.makeMask();
                imgRaw.setImageMask(im2);
            }
            imgRaw.setDpi(this.dpiX, this.dpiY);
            imgRaw.setXYRatio(this.XYRatio);
            imgRaw.setOriginalType(2);
            return (Image) imgRaw;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    void decodeIdat() {
        int nbitDepth = this.bitDepth;
        if (nbitDepth == 16) {
            nbitDepth = 8;
        }
        int size = -1;
        this.bytesPerPixel = (this.bitDepth == 16) ? 2 : 1;
        switch (this.colorType) {
            case 0:
                size = (nbitDepth * this.width + 7) / 8 * this.height;
                break;
            case 2:
                size = this.width * 3 * this.height;
                this.bytesPerPixel *= 3;
                break;
            case 3:
                if (this.interlaceMethod == 1) {
                    size = (nbitDepth * this.width + 7) / 8 * this.height;
                }
                this.bytesPerPixel = 1;
                break;
            case 4:
                size = this.width * this.height;
                this.bytesPerPixel *= 2;
                break;
            case 6:
                size = this.width * 3 * this.height;
                this.bytesPerPixel *= 4;
                break;
        }
        if (size >= 0) {
            this.image = new byte[size];
        }
        if (this.palShades) {
            this.smask = new byte[this.width * this.height];
        } else if (this.genBWMask) {
            this.smask = new byte[(this.width + 7) / 8 * this.height];
        }
        ByteArrayInputStream bai = new ByteArrayInputStream(this.idat.getBuf(), 0, this.idat.size());
        InputStream infStream = new InflaterInputStream(bai, new Inflater());
        this.dataStream = new DataInputStream(infStream);

        if (this.interlaceMethod != 1) {
            decodePass(0, 0, 1, 1, this.width, this.height);
        } else {

            decodePass(0, 0, 8, 8, (this.width + 7) / 8, (this.height + 7) / 8);
            decodePass(4, 0, 8, 8, (this.width + 3) / 8, (this.height + 7) / 8);
            decodePass(0, 4, 4, 8, (this.width + 3) / 4, (this.height + 3) / 8);
            decodePass(2, 0, 4, 4, (this.width + 1) / 4, (this.height + 3) / 4);
            decodePass(0, 2, 2, 4, (this.width + 1) / 2, (this.height + 1) / 4);
            decodePass(1, 0, 2, 2, this.width / 2, (this.height + 1) / 2);
            decodePass(0, 1, 1, 2, this.width, this.height / 2);
        }
    }

    void decodePass(int xOffset, int yOffset, int xStep, int yStep, int passWidth, int passHeight) {
        if (passWidth == 0 || passHeight == 0) {
            return;
        }

        int bytesPerRow = (this.inputBands * passWidth * this.bitDepth + 7) / 8;
        byte[] curr = new byte[bytesPerRow];
        byte[] prior = new byte[bytesPerRow];

        int srcY = 0, dstY = yOffset;
        for (; srcY < passHeight;
                srcY++, dstY += yStep) {

            int filter = 0;
            try {
                filter = this.dataStream.read();
                this.dataStream.readFully(curr, 0, bytesPerRow);
            } catch (Exception exception) {
            }

            switch (filter) {
                case 0:
                    break;
                case 1:
                    decodeSubFilter(curr, bytesPerRow, this.bytesPerPixel);
                    break;
                case 2:
                    decodeUpFilter(curr, prior, bytesPerRow);
                    break;
                case 3:
                    decodeAverageFilter(curr, prior, bytesPerRow, this.bytesPerPixel);
                    break;
                case 4:
                    decodePaethFilter(curr, prior, bytesPerRow, this.bytesPerPixel);
                    break;

                default:
                    throw new RuntimeException(MessageLocalization.getComposedMessage("png.filter.unknown", new Object[0]));
            }

            processPixels(curr, xOffset, xStep, dstY, passWidth);

            byte[] tmp = prior;
            prior = curr;
            curr = tmp;
        }
    }

    void processPixels(byte[] curr, int xOffset, int step, int y, int width) {
        int[] out = getPixel(curr);
        int sizes = 0;
        switch (this.colorType) {
            case 0:
            case 3:
            case 4:
                sizes = 1;
                break;
            case 2:
            case 6:
                sizes = 3;
                break;
        }
        if (this.image != null) {
            int dstX = xOffset;
            int yStride = (sizes * this.width * ((this.bitDepth == 16) ? 8 : this.bitDepth) + 7) / 8;
            for (int srcX = 0; srcX < width; srcX++) {
                setPixel(this.image, out, this.inputBands * srcX, sizes, dstX, y, this.bitDepth, yStride);
                dstX += step;
            }
        }
        if (this.palShades) {
            if ((this.colorType & 0x4) != 0) {
                if (this.bitDepth == 16) {
                    for (int k = 0; k < width; k++) {
                        out[k * this.inputBands + sizes] = out[k * this.inputBands + sizes] >>> 8;
                    }
                }
                int yStride = this.width;
                int dstX = xOffset;
                for (int srcX = 0; srcX < width; srcX++) {
                    setPixel(this.smask, out, this.inputBands * srcX + sizes, 1, dstX, y, 8, yStride);
                    dstX += step;
                }
            } else {

                int yStride = this.width;
                int[] v = new int[1];
                int dstX = xOffset;
                for (int srcX = 0; srcX < width; srcX++) {
                    int idx = out[srcX];
                    if (idx < this.trans.length) {
                        v[0] = this.trans[idx];
                    } else {
                        v[0] = 255;
                    }
                    setPixel(this.smask, v, 0, 1, dstX, y, 8, yStride);
                    dstX += step;
                }

            }
        } else if (this.genBWMask) {
            int srcX;
            int dstX;
            int yStride;
            int[] v;
            switch (this.colorType) {
                case 3:
                    yStride = (this.width + 7) / 8;
                    v = new int[1];
                    dstX = xOffset;
                    for (srcX = 0; srcX < width; srcX++) {
                        int idx = out[srcX];
                        v[0] = (idx < this.trans.length && this.trans[idx] == 0) ? 1 : 0;
                        setPixel(this.smask, v, 0, 1, dstX, y, 1, yStride);
                        dstX += step;
                    }
                    break;

                case 0:
                    yStride = (this.width + 7) / 8;
                    v = new int[1];
                    dstX = xOffset;
                    for (srcX = 0; srcX < width; srcX++) {
                        int g = out[srcX];
                        v[0] = (g == this.transRedGray) ? 1 : 0;
                        setPixel(this.smask, v, 0, 1, dstX, y, 1, yStride);
                        dstX += step;
                    }
                    break;

                case 2:
                    yStride = (this.width + 7) / 8;
                    v = new int[1];
                    dstX = xOffset;
                    for (srcX = 0; srcX < width; srcX++) {
                        int markRed = this.inputBands * srcX;
                        v[0] = (out[markRed] == this.transRedGray && out[markRed + 1] == this.transGreen && out[markRed + 2] == this.transBlue) ? 1 : 0;

                        setPixel(this.smask, v, 0, 1, dstX, y, 1, yStride);
                        dstX += step;
                    }
                    break;
            }
        }
    }

    static int getPixel(byte[] image, int x, int y, int bitDepth, int bytesPerRow) {
        if (bitDepth == 8) {
            int i = bytesPerRow * y + x;
            return image[i] & 0xFF;
        }

        int pos = bytesPerRow * y + x / 8 / bitDepth;
        int v = image[pos] >> 8 - bitDepth * x % 8 / bitDepth - bitDepth;
        return v & (1 << bitDepth) - 1;
    }

    static void setPixel(byte[] image, int[] data, int offset, int size, int x, int y, int bitDepth, int bytesPerRow) {
        if (bitDepth == 8) {
            int pos = bytesPerRow * y + size * x;
            for (int k = 0; k < size; k++) {
                image[pos + k] = (byte) data[k + offset];
            }
        } else if (bitDepth == 16) {
            int pos = bytesPerRow * y + size * x;
            for (int k = 0; k < size; k++) {
                image[pos + k] = (byte) (data[k + offset] >>> 8);
            }
        } else {
            int pos = bytesPerRow * y + x / 8 / bitDepth;
            int v = data[offset] << 8 - bitDepth * x % 8 / bitDepth - bitDepth;
            image[pos] = (byte) (image[pos] | v);
        }
    }

    int[] getPixel(byte[] curr) {
        int k;
        int[] out = new int[curr.length * 8 / this.bitDepth];
        switch (this.bitDepth) {
            case 8:
                out = new int[curr.length];
                for (k = 0; k < out.length; k++) {
                    out[k] = curr[k] & 0xFF;
                }
                return out;

            case 16:
                out = new int[curr.length / 2];
                for (k = 0; k < out.length; k++) {
                    out[k] = ((curr[k * 2] & 0xFF) << 8) + (curr[k * 2 + 1] & 0xFF);
                }
                return out;
        }

        int idx = 0;
        int passes = 8 / this.bitDepth;
        int mask = (1 << this.bitDepth) - 1;
        for (int i = 0; i < curr.length; i++) {
            for (int j = passes - 1; j >= 0; j--) {
                out[idx++] = curr[i] >>> this.bitDepth * j & mask;
            }
        }
        return out;
    }

    private static void decodeSubFilter(byte[] curr, int count, int bpp) {
        for (int i = bpp; i < count; i++) {

            int val = curr[i] & 0xFF;
            val += curr[i - bpp] & 0xFF;

            curr[i] = (byte) val;
        }
    }

    private static void decodeUpFilter(byte[] curr, byte[] prev, int count) {
        for (int i = 0; i < count; i++) {
            int raw = curr[i] & 0xFF;
            int prior = prev[i] & 0xFF;

            curr[i] = (byte) (raw + prior);
        }
    }

    private static void decodeAverageFilter(byte[] curr, byte[] prev, int count, int bpp) {
        int i;
        for (i = 0; i < bpp; i++) {
            int raw = curr[i] & 0xFF;
            int priorRow = prev[i] & 0xFF;

            curr[i] = (byte) (raw + priorRow / 2);
        }

        for (i = bpp; i < count; i++) {
            int raw = curr[i] & 0xFF;
            int priorPixel = curr[i - bpp] & 0xFF;
            int priorRow = prev[i] & 0xFF;

            curr[i] = (byte) (raw + (priorPixel + priorRow) / 2);
        }
    }

    private static int paethPredictor(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);

        if (pa <= pb && pa <= pc) {
            return a;
        }
        if (pb <= pc) {
            return b;
        }
        return c;
    }

    private static void decodePaethFilter(byte[] curr, byte[] prev, int count, int bpp) {
        int i;
        for (i = 0; i < bpp; i++) {
            int raw = curr[i] & 0xFF;
            int priorRow = prev[i] & 0xFF;

            curr[i] = (byte) (raw + priorRow);
        }

        for (i = bpp; i < count; i++) {
            int raw = curr[i] & 0xFF;
            int priorPixel = curr[i - bpp] & 0xFF;
            int priorRow = prev[i] & 0xFF;
            int priorRowPixel = prev[i - bpp] & 0xFF;

            curr[i] = (byte) (raw + paethPredictor(priorPixel, priorRow, priorRowPixel));
        }
    }

    static class NewByteArrayOutputStream
            extends ByteArrayOutputStream {

        public byte[] getBuf() {
            return this.buf;
        }
    }

    public static final int getInt(InputStream is) throws IOException {
        return (is.read() << 24) + (is.read() << 16) + (is.read() << 8) + is.read();
    }

    public static final int getWord(InputStream is) throws IOException {
        return (is.read() << 8) + is.read();
    }

    public static final String getString(InputStream is) throws IOException {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < 4; i++) {
            buf.append((char) is.read());
        }
        return buf.toString();
    }
}
