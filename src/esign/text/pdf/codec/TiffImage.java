package esign.text.pdf.codec;

import esign.text.ExceptionConverter;
import esign.text.Image;
import esign.text.ImgRaw;
import esign.text.Jpeg;
import esign.text.error_messages.MessageLocalization;
import esign.text.exceptions.InvalidImageException;
import esign.text.pdf.ICC_Profile;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfString;
import esign.text.pdf.RandomAccessFileOrArray;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

public class TiffImage {

    public static int getNumberOfPages(RandomAccessFileOrArray s) {
        try {
            return TIFFDirectory.getNumDirectories(s);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    static int getDpi(TIFFField fd, int resolutionUnit) {
        if (fd == null) {
            return 0;
        }
        long[] res = fd.getAsRational(0);
        float frac = (float) res[0] / (float) res[1];
        int dpi = 0;
        switch (resolutionUnit) {
            case 1:
            case 2:
                dpi = (int) (frac + 0.5D);
                break;
            case 3:
                dpi = (int) (frac * 2.54D + 0.5D);
                break;
        }
        return dpi;
    }

    public static Image getTiffImage(RandomAccessFileOrArray s, boolean recoverFromImageError, int page, boolean direct) {
        if (page < 1) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.page.number.must.be.gt.eq.1", new Object[0]));
        }
        try {
            TIFFField t4OptionsField, t6OptionsField;
            TIFFDirectory dir = new TIFFDirectory(s, page - 1);
            if (dir.isTagPresent(322)) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("tiles.are.not.supported", new Object[0]));
            }
            int compression = (int) dir.getFieldAsLong(259);
            switch (compression) {
                case 2:
                case 3:
                case 4:
                case 32771:
                    break;
                default:
                    return getTiffImageColor(dir, s);
            }
            float rotation = 0.0F;
            if (dir.isTagPresent(274)) {
                int rot = (int) dir.getFieldAsLong(274);
                if (rot == 3 || rot == 4) {
                    rotation = 3.1415927F;
                } else if (rot == 5 || rot == 8) {
                    rotation = 1.5707964F;
                } else if (rot == 6 || rot == 7) {
                    rotation = -1.5707964F;
                }
            }
            Image img = null;
            long tiffT4Options = 0L;
            long tiffT6Options = 0L;
            long fillOrder = 1L;
            int h = (int) dir.getFieldAsLong(257);
            int w = (int) dir.getFieldAsLong(256);
            int dpiX = 0;
            int dpiY = 0;
            float XYRatio = 0.0F;
            int resolutionUnit = 2;
            if (dir.isTagPresent(296)) {
                resolutionUnit = (int) dir.getFieldAsLong(296);
            }
            dpiX = getDpi(dir.getField(282), resolutionUnit);
            dpiY = getDpi(dir.getField(283), resolutionUnit);
            if (resolutionUnit == 1) {
                if (dpiY != 0) {
                    XYRatio = dpiX / dpiY;
                }
                dpiX = 0;
                dpiY = 0;
            }
            int rowsStrip = h;
            if (dir.isTagPresent(278)) {
                rowsStrip = (int) dir.getFieldAsLong(278);
            }
            if (rowsStrip <= 0 || rowsStrip > h) {
                rowsStrip = h;
            }
            long[] offset = getArrayLongShort(dir, 273);
            long[] size = getArrayLongShort(dir, 279);
            if ((size == null || (size.length == 1 && (size[0] == 0L || size[0] + offset[0] > s.length()))) && h == rowsStrip) {
                size = new long[]{s.length() - (int) offset[0]};
            }
            boolean reverse = false;
            TIFFField fillOrderField = dir.getField(266);
            if (fillOrderField != null) {
                fillOrder = fillOrderField.getAsLong(0);
            }
            reverse = (fillOrder == 2L);
            int params = 0;
            if (dir.isTagPresent(262)) {
                long photo = dir.getFieldAsLong(262);
                if (photo == 1L) {
                    params |= 0x1;
                }
            }
            int imagecomp = 0;
            switch (compression) {
                case 2:
                case 32771:
                    imagecomp = 257;
                    params |= 0xA;
                    break;
                case 3:
                    imagecomp = 257;
                    params |= 0xC;
                    t4OptionsField = dir.getField(292);
                    if (t4OptionsField != null) {
                        tiffT4Options = t4OptionsField.getAsLong(0);
                        if ((tiffT4Options & 0x1L) != 0L) {
                            imagecomp = 258;
                        }
                        if ((tiffT4Options & 0x4L) != 0L) {
                            params |= 0x2;
                        }
                    }
                    break;
                case 4:
                    imagecomp = 256;
                    t6OptionsField = dir.getField(293);
                    if (t6OptionsField != null) {
                        tiffT6Options = t6OptionsField.getAsLong(0);
                    }
                    break;
            }
            if (direct && rowsStrip == h) {
                byte[] im = new byte[(int) size[0]];
                s.seek(offset[0]);
                s.readFully(im);
                img = Image.getInstance(w, h, false, imagecomp, params, im);
                img.setInverted(true);
            } else {

                int rowsLeft = h;
                CCITTG4Encoder g4 = new CCITTG4Encoder(w);
                for (int k = 0; k < offset.length; k++) {
                    byte[] im = new byte[(int) size[k]];
                    s.seek(offset[k]);
                    s.readFully(im);
                    int height = Math.min(rowsStrip, rowsLeft);
                    TIFFFaxDecoder decoder = new TIFFFaxDecoder(fillOrder, w, height);
                    decoder.setRecoverFromImageError(recoverFromImageError);
                    byte[] outBuf = new byte[(w + 7) / 8 * height];
                    switch (compression) {
                        case 2:
                        case 32771:
                            decoder.decode1D(outBuf, im, 0, height);
                            g4.fax4Encode(outBuf, height);
                            break;
                        case 3:
                            try {
                                decoder.decode2D(outBuf, im, 0, height, tiffT4Options);
                            } catch (RuntimeException e) {

                                tiffT4Options ^= 0x4L;
                                try {
                                    decoder.decode2D(outBuf, im, 0, height, tiffT4Options);
                                } catch (RuntimeException e2) {
                                    if (!recoverFromImageError) {
                                        throw e;
                                    }
                                    if (rowsStrip == 1) {
                                        throw e;
                                    }

                                    im = new byte[(int) size[0]];
                                    s.seek(offset[0]);
                                    s.readFully(im);
                                    img = Image.getInstance(w, h, false, imagecomp, params, im);
                                    img.setInverted(true);
                                    img.setDpi(dpiX, dpiY);
                                    img.setXYRatio(XYRatio);
                                    img.setOriginalType(5);
                                    if (rotation != 0.0F) {
                                        img.setInitialRotation(rotation);
                                    }
                                    return img;
                                }
                            }
                            g4.fax4Encode(outBuf, height);
                            break;
                        case 4:
                            try {
                                decoder.decodeT6(outBuf, im, 0, height, tiffT6Options);
                            } catch (InvalidImageException e) {
                                if (!recoverFromImageError) {
                                    throw e;
                                }
                            }

                            g4.fax4Encode(outBuf, height);
                            break;
                    }
                    rowsLeft -= rowsStrip;
                }
                byte[] g4pic = g4.close();
                img = Image.getInstance(w, h, false, 256, params & 0x1, g4pic);
            }
            img.setDpi(dpiX, dpiY);
            img.setXYRatio(XYRatio);
            if (dir.isTagPresent(34675)) {
                try {
                    TIFFField fd = dir.getField(34675);
                    ICC_Profile icc_prof = ICC_Profile.getInstance(fd.getAsBytes());
                    if (icc_prof.getNumComponents() == 1) {
                        img.tagICC(icc_prof);
                    }
                } catch (RuntimeException runtimeException) {
                }
            }

            img.setOriginalType(5);
            if (rotation != 0.0F) {
                img.setInitialRotation(rotation);
            }
            return img;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public static Image getTiffImage(RandomAccessFileOrArray s, boolean recoverFromImageError, int page) {
        return getTiffImage(s, recoverFromImageError, page, false);
    }

    public static Image getTiffImage(RandomAccessFileOrArray s, int page) {
        return getTiffImage(s, page, false);
    }

    public static Image getTiffImage(RandomAccessFileOrArray s, int page, boolean direct) {
        return getTiffImage(s, false, page, direct);
    }

    protected static Image getTiffImageColor(TIFFDirectory dir, RandomAccessFileOrArray s) {
        try {
            ImgRaw imgRaw = null;
            int compression = (int) dir.getFieldAsLong(259);
            int predictor = 1;
            TIFFLZWDecoder lzwDecoder = null;
            switch (compression) {
                case 1:
                case 5:
                case 6:
                case 7:
                case 8:
                case 32773:
                case 32946:
                    break;
                default:
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.compression.1.is.not.supported", compression));
            }
            int photometric = (int) dir.getFieldAsLong(262);
            switch (photometric) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 5:
                    break;
                default:
                    if (compression != 6 && compression != 7) {
                        throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.photometric.1.is.not.supported", photometric));
                    }
                    break;
            }
            float rotation = 0.0F;
            if (dir.isTagPresent(274)) {
                int rot = (int) dir.getFieldAsLong(274);
                if (rot == 3 || rot == 4) {
                    rotation = 3.1415927F;
                } else if (rot == 5 || rot == 8) {
                    rotation = 1.5707964F;
                } else if (rot == 6 || rot == 7) {
                    rotation = -1.5707964F;
                }
            }
            if (dir.isTagPresent(284) && dir
                    .getFieldAsLong(284) == 2L) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("planar.images.are.not.supported", new Object[0]));
            }
            int extraSamples = 0;
            if (dir.isTagPresent(338)) {
                extraSamples = 1;
            }
            int samplePerPixel = 1;
            if (dir.isTagPresent(277)) {
                samplePerPixel = (int) dir.getFieldAsLong(277);
            }
            int bitsPerSample = 1;
            if (dir.isTagPresent(258)) {
                bitsPerSample = (int) dir.getFieldAsLong(258);
            }
            switch (bitsPerSample) {
                case 1:
                case 2:
                case 4:
                case 8:
                    break;
                default:
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("bits.per.sample.1.is.not.supported", bitsPerSample));
            }
            Image img = null;

            int h = (int) dir.getFieldAsLong(257);
            int w = (int) dir.getFieldAsLong(256);
            int dpiX = 0;
            int dpiY = 0;
            int resolutionUnit = 2;
            if (dir.isTagPresent(296)) {
                resolutionUnit = (int) dir.getFieldAsLong(296);
            }
            dpiX = getDpi(dir.getField(282), resolutionUnit);
            dpiY = getDpi(dir.getField(283), resolutionUnit);
            int fillOrder = 1;
            boolean reverse = false;
            TIFFField fillOrderField = dir.getField(266);
            if (fillOrderField != null) {
                fillOrder = fillOrderField.getAsInt(0);
            }
            reverse = (fillOrder == 2);
            int rowsStrip = h;
            if (dir.isTagPresent(278)) {
                rowsStrip = (int) dir.getFieldAsLong(278);
            }
            if (rowsStrip <= 0 || rowsStrip > h) {
                rowsStrip = h;
            }
            long[] offset = getArrayLongShort(dir, 273);
            long[] size = getArrayLongShort(dir, 279);
            if ((size == null || (size.length == 1 && (size[0] == 0L || size[0] + offset[0] > s.length()))) && h == rowsStrip) {
                size = new long[]{s.length() - (int) offset[0]};
            }
            if (compression == 5 || compression == 32946 || compression == 8) {
                TIFFField predictorField = dir.getField(317);
                if (predictorField != null) {
                    predictor = predictorField.getAsInt(0);
                    if (predictor != 1 && predictor != 2) {
                        throw new RuntimeException(MessageLocalization.getComposedMessage("illegal.value.for.predictor.in.tiff.file", new Object[0]));
                    }
                    if (predictor == 2 && bitsPerSample != 8) {
                        throw new RuntimeException(MessageLocalization.getComposedMessage("1.bit.samples.are.not.supported.for.horizontal.differencing.predictor", bitsPerSample));
                    }
                }
            }
            if (compression == 5) {
                lzwDecoder = new TIFFLZWDecoder(w, predictor, samplePerPixel);
            }
            int rowsLeft = h;
            ByteArrayOutputStream stream = null;
            ByteArrayOutputStream mstream = null;
            DeflaterOutputStream zip = null;
            DeflaterOutputStream mzip = null;
            if (extraSamples > 0) {
                mstream = new ByteArrayOutputStream();
                mzip = new DeflaterOutputStream(mstream);
            }

            CCITTG4Encoder g4 = null;
            if (bitsPerSample == 1 && samplePerPixel == 1 && photometric != 3) {
                g4 = new CCITTG4Encoder(w);
            } else {

                stream = new ByteArrayOutputStream();
                if (compression != 6 && compression != 7) {
                    zip = new DeflaterOutputStream(stream);
                }
            }
            if (compression == 6) {

                if (!dir.isTagPresent(513)) {
                    throw new IOException(MessageLocalization.getComposedMessage("missing.tag.s.for.ojpeg.compression", new Object[0]));
                }
                int jpegOffset = (int) dir.getFieldAsLong(513);
                int jpegLength = (int) s.length() - jpegOffset;

                if (dir.isTagPresent(514)) {
                    jpegLength = (int) dir.getFieldAsLong(514) + (int) size[0];
                }

                byte[] jpeg = new byte[Math.min(jpegLength, (int) s.length() - jpegOffset)];

                s.seek(jpegOffset);
                s.readFully(jpeg);
                Jpeg jpeg1 = new Jpeg(jpeg);
            } else if (compression == 7) {
                if (size.length > 1) {
                    throw new IOException(MessageLocalization.getComposedMessage("compression.jpeg.is.only.supported.with.a.single.strip.this.image.has.1.strips", size.length));
                }
                byte[] jpeg = new byte[(int) size[0]];
                s.seek(offset[0]);
                s.readFully(jpeg);

                TIFFField jpegtables = dir.getField(347);
                if (jpegtables != null) {
                    byte[] temp = jpegtables.getAsBytes();
                    int tableoffset = 0;
                    int tablelength = temp.length;

                    if (temp[0] == -1 && temp[1] == -40) {
                        tableoffset = 2;
                        tablelength -= 2;
                    }

                    if (temp[temp.length - 2] == -1 && temp[temp.length - 1] == -39) {
                        tablelength -= 2;
                    }
                    byte[] tables = new byte[tablelength];
                    System.arraycopy(temp, tableoffset, tables, 0, tablelength);

                    byte[] jpegwithtables = new byte[jpeg.length + tables.length];
                    System.arraycopy(jpeg, 0, jpegwithtables, 0, 2);
                    System.arraycopy(tables, 0, jpegwithtables, 2, tables.length);
                    System.arraycopy(jpeg, 2, jpegwithtables, tables.length + 2, jpeg.length - 2);
                    jpeg = jpegwithtables;
                }
                Jpeg jpeg1 = new Jpeg(jpeg);
                if (photometric == 2) {
                    jpeg1.setColorTransform(0);
                }
            } else {

                for (int k = 0; k < offset.length; k++) {
                    byte[] im = new byte[(int) size[k]];
                    s.seek(offset[k]);
                    s.readFully(im);
                    int height = Math.min(rowsStrip, rowsLeft);
                    byte[] outBuf = null;
                    if (compression != 1) {
                        outBuf = new byte[(w * bitsPerSample * samplePerPixel + 7) / 8 * height];
                    }
                    if (reverse) {
                        TIFFFaxDecoder.reverseBits(im);
                    }
                    switch (compression) {
                        case 8:
                        case 32946:
                            inflate(im, outBuf);
                            applyPredictor(outBuf, predictor, w, height, samplePerPixel);
                            break;
                        case 1:
                            outBuf = im;
                            break;
                        case 32773:
                            decodePackbits(im, outBuf);
                            break;
                        case 5:
                            lzwDecoder.decode(im, outBuf, height);
                            break;
                    }
                    if (bitsPerSample == 1 && samplePerPixel == 1 && photometric != 3) {
                        g4.fax4Encode(outBuf, height);

                    } else if (extraSamples > 0) {
                        ProcessExtraSamples(zip, mzip, outBuf, samplePerPixel, bitsPerSample, w, height);
                    } else {
                        zip.write(outBuf);
                    }
                    rowsLeft -= rowsStrip;
                }
                if (bitsPerSample == 1 && samplePerPixel == 1 && photometric != 3) {
                    img = Image.getInstance(w, h, false, 256, (photometric == 1) ? 1 : 0, g4
                            .close());
                } else {

                    zip.close();
                    imgRaw = new ImgRaw(w, h, samplePerPixel - extraSamples, bitsPerSample, stream.toByteArray());
                    imgRaw.setDeflated(true);
                }
            }
            imgRaw.setDpi(dpiX, dpiY);
            if (compression != 6 && compression != 7) {
                if (dir.isTagPresent(34675)) {
                    try {
                        TIFFField fd = dir.getField(34675);
                        ICC_Profile icc_prof = ICC_Profile.getInstance(fd.getAsBytes());
                        if (samplePerPixel - extraSamples == icc_prof.getNumComponents()) {
                            imgRaw.tagICC(icc_prof);
                        }
                    } catch (RuntimeException runtimeException) {
                    }
                }

                if (dir.isTagPresent(320)) {
                    TIFFField fd = dir.getField(320);
                    char[] rgb = fd.getAsChars();
                    byte[] palette = new byte[rgb.length];
                    int gColor = rgb.length / 3;
                    int bColor = gColor * 2;
                    for (int k = 0; k < gColor; k++) {
                        palette[k * 3] = (byte) (rgb[k] >>> 8);
                        palette[k * 3 + 1] = (byte) (rgb[k + gColor] >>> 8);
                        palette[k * 3 + 2] = (byte) (rgb[k + bColor] >>> 8);
                    }

                    boolean colormapBroken = true;
                    int i;
                    for (i = 0; i < palette.length; i++) {
                        if (palette[i] != 0) {
                            colormapBroken = false;
                            break;
                        }
                    }
                    if (colormapBroken) {
                        for (i = 0; i < gColor; i++) {
                            palette[i * 3] = (byte) rgb[i];
                            palette[i * 3 + 1] = (byte) rgb[i + gColor];
                            palette[i * 3 + 2] = (byte) rgb[i + bColor];
                        }
                    }
                    PdfArray indexed = new PdfArray();
                    indexed.add((PdfObject) PdfName.INDEXED);
                    indexed.add((PdfObject) PdfName.DEVICERGB);
                    indexed.add((PdfObject) new PdfNumber(gColor - 1));
                    indexed.add((PdfObject) new PdfString(palette));
                    PdfDictionary additional = new PdfDictionary();
                    additional.put(PdfName.COLORSPACE, (PdfObject) indexed);
                    imgRaw.setAdditional(additional);
                }
                imgRaw.setOriginalType(5);
            }
            if (photometric == 0) {
                imgRaw.setInverted(true);
            }
            if (rotation != 0.0F) {
                imgRaw.setInitialRotation(rotation);
            }
            if (extraSamples > 0) {
                mzip.close();
                Image mimg = Image.getInstance(w, h, 1, bitsPerSample, mstream.toByteArray());
                mimg.makeMask();
                mimg.setDeflated(true);
                imgRaw.setImageMask(mimg);
            }
            return (Image) imgRaw;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    static Image ProcessExtraSamples(DeflaterOutputStream zip, DeflaterOutputStream mzip, byte[] outBuf, int samplePerPixel, int bitsPerSample, int width, int height) throws IOException {
        if (bitsPerSample == 8) {
            byte[] mask = new byte[width * height];
            int mptr = 0;
            int optr = 0;
            int total = width * height * samplePerPixel;
            int k;
            for (k = 0; k < total; k += samplePerPixel) {
                for (int s = 0; s < samplePerPixel - 1; s++) {
                    outBuf[optr++] = outBuf[k + s];
                }
                mask[mptr++] = outBuf[k + samplePerPixel - 1];
            }
            zip.write(outBuf, 0, optr);
            mzip.write(mask, 0, mptr);
        } else {

            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("extra.samples.are.not.supported", new Object[0]));
        }
        return null;
    }

    static long[] getArrayLongShort(TIFFDirectory dir, int tag) {
        long[] offset;
        TIFFField field = dir.getField(tag);
        if (field == null) {
            return null;
        }
        if (field.getType() == 4) {
            offset = field.getAsLongs();
        } else {
            char[] temp = field.getAsChars();
            offset = new long[temp.length];
            for (int k = 0; k < temp.length; k++) {
                offset[k] = temp[k];
            }
        }
        return offset;
    }

    public static void decodePackbits(byte[] data, byte[] dst) {
        int srcCount = 0, dstCount = 0;

        try {
            while (dstCount < dst.length) {
                byte b = data[srcCount++];
                if (b >= 0 && b <= Byte.MAX_VALUE) {

                    for (int i = 0; i < b + 1; i++) {
                        dst[dstCount++] = data[srcCount++];
                    }
                    continue;
                }
                if (b <= -1 && b >= -127) {

                    byte repeat = data[srcCount++];
                    for (int i = 0; i < -b + 1; i++) {
                        dst[dstCount++] = repeat;
                    }
                    continue;
                }
                srcCount++;
            }

        } catch (Exception exception) {
        }
    }

    public static void inflate(byte[] deflated, byte[] inflated) {
        Inflater inflater = new Inflater();
        inflater.setInput(deflated);
        try {
            inflater.inflate(inflated);
        } catch (DataFormatException dfe) {
            throw new ExceptionConverter(dfe);
        }
    }

    public static void applyPredictor(byte[] uncompData, int predictor, int w, int h, int samplesPerPixel) {
        if (predictor != 2) {
            return;
        }
        for (int j = 0; j < h; j++) {
            int count = samplesPerPixel * (j * w + 1);
            for (int i = samplesPerPixel; i < w * samplesPerPixel; i++) {
                uncompData[count] = (byte) (uncompData[count] + uncompData[count - samplesPerPixel]);
                count++;
            }
        }
    }
}
