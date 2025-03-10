package esign.text;

import esign.text.awt.PdfGraphics2D;
import esign.text.api.Indentable;
import esign.text.api.Spaceable;
import esign.text.error_messages.MessageLocalization;
import esign.text.io.RandomAccessSourceFactory;
import esign.text.pdf.ICC_Profile;
import esign.text.pdf.PRIndirectReference;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfContentByte;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfIndirectReference;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfOCG;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import esign.text.pdf.PdfString;
import esign.text.pdf.PdfTemplate;
import esign.text.pdf.PdfWriter;
import esign.text.pdf.RandomAccessFileOrArray;
import esign.text.pdf.codec.BmpImage;
import esign.text.pdf.codec.CCITTG4Encoder;
import esign.text.pdf.codec.GifImage;
import esign.text.pdf.codec.JBIG2Image;
import esign.text.pdf.codec.PngImage;
import esign.text.pdf.codec.TiffImage;
import esign.text.pdf.interfaces.IAccessibleElement;
import esign.text.pdf.interfaces.IAlternateDescription;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public abstract class Image
        extends Rectangle
        implements Indentable, Spaceable, IAccessibleElement, IAlternateDescription {

    public static final int DEFAULT = 0;
    public static final int RIGHT = 2;
    public static final int LEFT = 0;
    public static final int MIDDLE = 1;
    public static final int TEXTWRAP = 4;
    public static final int UNDERLYING = 8;
    public static final int AX = 0;
    public static final int AY = 1;
    public static final int BX = 2;
    public static final int BY = 3;
    public static final int CX = 4;
    public static final int CY = 5;
    public static final int DX = 6;
    public static final int DY = 7;
    public static final int ORIGINAL_NONE = 0;
    public static final int ORIGINAL_JPEG = 1;
    public static final int ORIGINAL_PNG = 2;
    public static final int ORIGINAL_GIF = 3;
    public static final int ORIGINAL_BMP = 4;
    public static final int ORIGINAL_TIFF = 5;
    public static final int ORIGINAL_WMF = 6;
    public static final int ORIGINAL_PS = 7;
    public static final int ORIGINAL_JPEG2000 = 8;
    public static final int ORIGINAL_JBIG2 = 9;
    protected int type;
    protected URL url;
    protected byte[] rawData;
    protected int bpc = 1;

    protected PdfTemplate[] template = new PdfTemplate[1];

    protected int alignment;

    protected String alt;

    protected float absoluteX = Float.NaN;

    protected float absoluteY = Float.NaN;

    protected float plainWidth;

    protected float plainHeight;

    protected float scaledWidth;

    protected float scaledHeight;

    protected int compressionLevel = -1;

    protected Long mySerialId = getSerialId();

    protected PdfName role = PdfName.FIGURE;
    protected HashMap<PdfName, PdfObject> accessibleAttributes = null;
    private AccessibleElementId id = null;
    private PdfIndirectReference directReference;

    public static Image getInstance(URL url) throws BadElementException, MalformedURLException, IOException {
        return getInstance(url, false);
    }

    public static Image getInstance(URL url, boolean recoverFromImageError) throws BadElementException, MalformedURLException, IOException {
        InputStream is = null;
        RandomAccessSourceFactory randomAccessSourceFactory = new RandomAccessSourceFactory();
        try {
            is = url.openStream();
            int c1 = is.read();
            int c2 = is.read();
            int c3 = is.read();
            int c4 = is.read();
            int c5 = is.read();
            int c6 = is.read();
            int c7 = is.read();
            int c8 = is.read();
            is.close();
            is = null;
            if (c1 == 71 && c2 == 73 && c3 == 70) {
                GifImage gif = new GifImage(url);
                Image img = gif.getImage(1);
                return img;
            }
            if (c1 == 255 && c2 == 216) {
                return new Jpeg(url);
            }
            if (c1 == 0 && c2 == 0 && c3 == 0 && c4 == 12) {
                return new Jpeg2000(url);
            }
            if (c1 == 255 && c2 == 79 && c3 == 255 && c4 == 81) {
                return new Jpeg2000(url);
            }
            if (c1 == PngImage.PNGID[0] && c2 == PngImage.PNGID[1] && c3 == PngImage.PNGID[2] && c4 == PngImage.PNGID[3]) {
                return PngImage.getImage(url);
            }
            if (c1 == 215 && c2 == 205) {
                return new ImgWMF(url);
            }
            if (c1 == 66 && c2 == 77) {
                return BmpImage.getImage(url);
            }
            if ((c1 == 77 && c2 == 77 && c3 == 0 && c4 == 42) || (c1 == 73 && c2 == 73 && c3 == 42 && c4 == 0)) {
                RandomAccessFileOrArray ra = null;
                try {
                    if (url.getProtocol().equals("file")) {
                        String file = url.getFile();
                        file = Utilities.unEscapeURL(file);
                        ra = new RandomAccessFileOrArray(randomAccessSourceFactory.createBestSource(file));
                    } else {
                        ra = new RandomAccessFileOrArray(randomAccessSourceFactory.createSource(url));
                    }
                    Image img = TiffImage.getTiffImage(ra, 1);
                    img.url = url;
                    return img;
                } catch (RuntimeException e) {
                    if (recoverFromImageError) {
                        Image img = TiffImage.getTiffImage(ra, recoverFromImageError, 1);
                        img.url = url;
                        return img;
                    }
                    throw e;
                } finally {
                    if (ra != null) {
                        ra.close();
                    }
                }
            }
            if (c1 == 151 && c2 == 74 && c3 == 66 && c4 == 50 && c5 == 13 && c6 == 10 && c7 == 26 && c8 == 10) {
                RandomAccessFileOrArray ra = null;
                try {
                    if (url.getProtocol().equals("file")) {
                        String file = url.getFile();
                        file = Utilities.unEscapeURL(file);
                        ra = new RandomAccessFileOrArray(randomAccessSourceFactory.createBestSource(file));
                    } else {
                        ra = new RandomAccessFileOrArray(randomAccessSourceFactory.createSource(url));
                    }
                    Image img = JBIG2Image.getJbig2Image(ra, 1);
                    img.url = url;
                    return img;
                } finally {
                    if (ra != null) {
                        ra.close();
                    }
                }
            }
            throw new IOException(MessageLocalization.getComposedMessage("unknown.image.format", new Object[]{url.toString()}));
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static Image getInstance(String filename) throws BadElementException, MalformedURLException, IOException {
        return getInstance(Utilities.toURL(filename));
    }

    public static Image getInstance(String filename, boolean recoverFromImageError) throws IOException, BadElementException {
        return getInstance(Utilities.toURL(filename), recoverFromImageError);
    }

    public static Image getInstance(byte[] imgb) throws BadElementException, MalformedURLException, IOException {
        return getInstance(imgb, false);
    }

    public static Image getInstance(byte[] imgb, boolean recoverFromImageError) throws BadElementException, MalformedURLException, IOException {
        InputStream is = null;
        RandomAccessSourceFactory randomAccessSourceFactory = new RandomAccessSourceFactory();
        try {
            is = new ByteArrayInputStream(imgb);
            int c1 = is.read();
            int c2 = is.read();
            int c3 = is.read();
            int c4 = is.read();
            is.close();
            is = null;
            if (c1 == 71 && c2 == 73 && c3 == 70) {
                GifImage gif = new GifImage(imgb);
                return gif.getImage(1);
            }
            if (c1 == 255 && c2 == 216) {
                return new Jpeg(imgb);
            }
            if (c1 == 0 && c2 == 0 && c3 == 0 && c4 == 12) {
                return new Jpeg2000(imgb);
            }
            if (c1 == 255 && c2 == 79 && c3 == 255 && c4 == 81) {
                return new Jpeg2000(imgb);
            }
            if (c1 == PngImage.PNGID[0] && c2 == PngImage.PNGID[1] && c3 == PngImage.PNGID[2] && c4 == PngImage.PNGID[3]) {
                return PngImage.getImage(imgb);
            }
            if (c1 == 215 && c2 == 205) {
                return new ImgWMF(imgb);
            }
            if (c1 == 66 && c2 == 77) {
                return BmpImage.getImage(imgb);
            }
            if ((c1 == 77 && c2 == 77 && c3 == 0 && c4 == 42) || (c1 == 73 && c2 == 73 && c3 == 42 && c4 == 0)) {
                RandomAccessFileOrArray ra = null;
                try {
                    ra = new RandomAccessFileOrArray(randomAccessSourceFactory.createSource(imgb));
                    Image img = TiffImage.getTiffImage(ra, 1);
                    if (img.getOriginalData() == null) {
                        img.setOriginalData(imgb);
                    }
                    return img;
                } catch (RuntimeException e) {
                    if (recoverFromImageError) {
                        Image img = TiffImage.getTiffImage(ra, recoverFromImageError, 1);
                        if (img.getOriginalData() == null) {
                            img.setOriginalData(imgb);
                        }
                        return img;
                    }
                    throw e;
                } finally {
                    if (ra != null) {
                        ra.close();
                    }
                }
            }
            if (c1 == 151 && c2 == 74 && c3 == 66 && c4 == 50) {
                is = new ByteArrayInputStream(imgb);
                is.skip(4L);
                int c5 = is.read();
                int c6 = is.read();
                int c7 = is.read();
                int c8 = is.read();
                is.close();
                if (c5 == 13 && c6 == 10 && c7 == 26 && c8 == 10) {
                    RandomAccessFileOrArray ra = null;
                    try {
                        ra = new RandomAccessFileOrArray(randomAccessSourceFactory.createSource(imgb));
                        Image img = JBIG2Image.getJbig2Image(ra, 1);
                        if (img.getOriginalData() == null) {
                            img.setOriginalData(imgb);
                        }
                        return img;
                    } finally {
                        if (ra != null) {
                            ra.close();
                        }
                    }
                }
            }
            throw new IOException(MessageLocalization.getComposedMessage("the.byte.array.is.not.a.recognized.imageformat", new Object[0]));
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public static Image getInstance(int width, int height, int components, int bpc, byte[] data) throws BadElementException {
        return getInstance(width, height, components, bpc, data, (int[]) null);
    }

    public static Image getInstance(int width, int height, byte[] data, byte[] globals) {
        return new ImgJBIG2(width, height, data, globals);
    }

    public Image(URL url) {
        super(0.0F, 0.0F);

        this.indentationLeft = 0.0F;

        this.indentationRight = 0.0F;

        this.widthPercentage = 100.0F;

        this.scaleToFitHeight = true;

        this.annotation = null;

        this.originalType = 0;

        this.deflated = false;

        this.dpiX = 0;

        this.dpiY = 0;

        this.XYRatio = 0.0F;

        this.colorspace = -1;

        this.colortransform = 1;

        this.invert = false;

        this.profile = null;

        this.additional = null;

        this.mask = false;
        this.url = url;
        this.alignment = 0;
        this.rotationRadians = 0.0F;
    }

    public static Image getInstance(int width, int height, boolean reverseBits, int typeCCITT, int parameters, byte[] data) throws BadElementException {
        return getInstance(width, height, reverseBits, typeCCITT, parameters, data, (int[]) null);
    }

    public static Image getInstance(int width, int height, boolean reverseBits, int typeCCITT, int parameters, byte[] data, int[] transparency) throws BadElementException {
        if (transparency != null && transparency.length != 2) {
            throw new BadElementException(MessageLocalization.getComposedMessage("transparency.length.must.be.equal.to.2.with.ccitt.images", new Object[0]));
        }
        Image img = new ImgCCITT(width, height, reverseBits, typeCCITT, parameters, data);
        img.transparency = transparency;
        return img;
    }

    public static Image getInstance(int width, int height, int components, int bpc, byte[] data, int[] transparency) throws BadElementException {
        if (transparency != null && transparency.length != components * 2) {
            throw new BadElementException(MessageLocalization.getComposedMessage("transparency.length.must.be.equal.to.componentes.2", new Object[0]));
        }
        if (components == 1 && bpc == 1) {
            byte[] g4 = CCITTG4Encoder.compress(data, width, height);
            return getInstance(width, height, false, 256, 1, g4, transparency);
        }
        Image img = new ImgRaw(width, height, components, bpc, data);
        img.transparency = transparency;
        return img;
    }

    public static Image getInstance(PdfTemplate template) throws BadElementException {
        return new ImgTemplate(template);
    }

    public PdfIndirectReference getDirectReference() {
        return this.directReference;
    }

    public void setDirectReference(PdfIndirectReference directReference) {
        this.directReference = directReference;
    }

    public static Image getInstance(PRIndirectReference ref) throws BadElementException {
        PdfDictionary dic = (PdfDictionary) PdfReader.getPdfObjectRelease((PdfObject) ref);
        int width = ((PdfNumber) PdfReader.getPdfObjectRelease(dic.get(PdfName.WIDTH))).intValue();
        int height = ((PdfNumber) PdfReader.getPdfObjectRelease(dic.get(PdfName.HEIGHT))).intValue();
        Image imask = null;
        PdfObject obj = dic.get(PdfName.SMASK);
        if (obj != null && obj.isIndirect()) {
            imask = getInstance((PRIndirectReference) obj);
        } else {
            obj = dic.get(PdfName.MASK);
            if (obj != null && obj.isIndirect()) {
                PdfObject obj2 = PdfReader.getPdfObjectRelease(obj);
                if (obj2 instanceof PdfDictionary) {
                    imask = getInstance((PRIndirectReference) obj);
                }
            }
        }
        Image img = new ImgRaw(width, height, 1, 1, null);
        img.imageMask = imask;
        img.directReference = (PdfIndirectReference) ref;
        return img;
    }

    public static Image getInstance(Image image) {
        if (image == null) {
            return null;
        }
        try {
            Class<? extends Image> cs = (Class) image.getClass();
            Constructor<? extends Image> constructor = cs.getDeclaredConstructor(new Class[]{Image.class});
            return constructor.newInstance(new Object[]{image});
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public int type() {
        return this.type;
    }

    public boolean isNestable() {
        return true;
    }

    public boolean isJpeg() {
        return (this.type == 32);
    }

    public boolean isImgRaw() {
        return (this.type == 34);
    }

    public boolean isImgTemplate() {
        return (this.type == 35);
    }

    public URL getUrl() {
        return this.url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public byte[] getRawData() {
        return this.rawData;
    }

    public int getBpc() {
        return this.bpc;
    }

    public PdfTemplate getTemplateData() {
        return this.template[0];
    }

    public void setTemplateData(PdfTemplate template) {
        this.template[0] = template;
    }

    public int getAlignment() {
        return this.alignment;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public String getAlt() {
        return this.alt;
    }

    public void setAlt(String alt) {
        this.alt = alt;
        setAccessibleAttribute(PdfName.ALT, (PdfObject) new PdfString(alt));
    }

    public void setAbsolutePosition(float absoluteX, float absoluteY) {
        this.absoluteX = absoluteX;
        this.absoluteY = absoluteY;
    }

    public boolean hasAbsoluteX() {
        return !Float.isNaN(this.absoluteX);
    }

    public float getAbsoluteX() {
        return this.absoluteX;
    }

    public boolean hasAbsoluteY() {
        return !Float.isNaN(this.absoluteY);
    }

    public float getAbsoluteY() {
        return this.absoluteY;
    }

    public float getScaledWidth() {
        return this.scaledWidth;
    }

    public float getScaledHeight() {
        return this.scaledHeight;
    }

    public float getPlainWidth() {
        return this.plainWidth;
    }

    public float getPlainHeight() {
        return this.plainHeight;
    }

    public void scaleAbsolute(Rectangle rectangle) {
        scaleAbsolute(rectangle.getWidth(), rectangle.getHeight());
    }

    public void scaleAbsolute(float newWidth, float newHeight) {
        this.plainWidth = newWidth;
        this.plainHeight = newHeight;
        float[] matrix = matrix();
        this.scaledWidth = matrix[6] - matrix[4];
        this.scaledHeight = matrix[7] - matrix[5];
        setWidthPercentage(0.0F);
    }

    public void scaleAbsoluteWidth(float newWidth) {
        this.plainWidth = newWidth;
        float[] matrix = matrix();
        this.scaledWidth = matrix[6] - matrix[4];
        this.scaledHeight = matrix[7] - matrix[5];
        setWidthPercentage(0.0F);
    }

    public void scaleAbsoluteHeight(float newHeight) {
        this.plainHeight = newHeight;
        float[] matrix = matrix();
        this.scaledWidth = matrix[6] - matrix[4];
        this.scaledHeight = matrix[7] - matrix[5];
        setWidthPercentage(0.0F);
    }

    protected Image(Image image) {
        super(image);
        this.indentationLeft = 0.0F;
        this.indentationRight = 0.0F;
        this.widthPercentage = 100.0F;
        this.scaleToFitHeight = true;
        this.annotation = null;
        this.originalType = 0;
        this.deflated = false;
        this.dpiX = 0;
        this.dpiY = 0;
        this.XYRatio = 0.0F;
        this.colorspace = -1;
        this.colortransform = 1;
        this.invert = false;
        this.profile = null;
        this.additional = null;
        this.mask = false;
        this.type = image.type;
        this.url = image.url;
        this.rawData = image.rawData;
        this.bpc = image.bpc;
        this.template = image.template;
        this.alignment = image.alignment;
        this.alt = image.alt;
        this.absoluteX = image.absoluteX;
        this.absoluteY = image.absoluteY;
        this.plainWidth = image.plainWidth;
        this.plainHeight = image.plainHeight;
        this.scaledWidth = image.scaledWidth;
        this.scaledHeight = image.scaledHeight;
        this.mySerialId = image.mySerialId;
        this.directReference = image.directReference;
        this.rotationRadians = image.rotationRadians;
        this.initialRotation = image.initialRotation;
        this.indentationLeft = image.indentationLeft;
        this.indentationRight = image.indentationRight;
        this.spacingBefore = image.spacingBefore;
        this.spacingAfter = image.spacingAfter;
        this.widthPercentage = image.widthPercentage;
        this.scaleToFitLineWhenOverflow = image.scaleToFitLineWhenOverflow;
        this.scaleToFitHeight = image.scaleToFitHeight;
        this.annotation = image.annotation;
        this.layer = image.layer;
        this.interpolation = image.interpolation;
        this.originalType = image.originalType;
        this.originalData = image.originalData;
        this.deflated = image.deflated;
        this.dpiX = image.dpiX;
        this.dpiY = image.dpiY;
        this.XYRatio = image.XYRatio;
        this.colorspace = image.colorspace;
        this.invert = image.invert;
        this.profile = image.profile;
        this.additional = image.additional;
        this.mask = image.mask;
        this.imageMask = image.imageMask;
        this.smask = image.smask;
        this.transparency = image.transparency;
        this.role = image.role;
        if (image.accessibleAttributes != null) {
            this.accessibleAttributes = new HashMap<PdfName, PdfObject>(image.accessibleAttributes);
        }
        setId(image.getId());
    }

    public void scalePercent(float percent) {
        scalePercent(percent, percent);
    }

    public void scalePercent(float percentX, float percentY) {
        this.plainWidth = getWidth() * percentX / 100.0F;
        this.plainHeight = getHeight() * percentY / 100.0F;
        float[] matrix = matrix();
        this.scaledWidth = matrix[6] - matrix[4];
        this.scaledHeight = matrix[7] - matrix[5];
        setWidthPercentage(0.0F);
    }

    public void scaleToFit(Rectangle rectangle) {
        scaleToFit(rectangle.getWidth(), rectangle.getHeight());
    }

    public void scaleToFit(float fitWidth, float fitHeight) {
        scalePercent(100.0F);
        float percentX = fitWidth * 100.0F / getScaledWidth();
        float percentY = fitHeight * 100.0F / getScaledHeight();
        scalePercent((percentX < percentY) ? percentX : percentY);
        setWidthPercentage(0.0F);
    }

    public float[] matrix() {
        return matrix(1.0F);
    }

    public float[] matrix(float scalePercentage) {
        float[] matrix = new float[8];
        float cosX = (float) Math.cos(this.rotationRadians);
        float sinX = (float) Math.sin(this.rotationRadians);
        matrix[0] = this.plainWidth * cosX * scalePercentage;
        matrix[1] = this.plainWidth * sinX * scalePercentage;
        matrix[2] = -this.plainHeight * sinX * scalePercentage;
        matrix[3] = this.plainHeight * cosX * scalePercentage;
        if (this.rotationRadians < 1.5707963267948966D) {
            matrix[4] = matrix[2];
            matrix[5] = 0.0F;
            matrix[6] = matrix[0];
            matrix[7] = matrix[1] + matrix[3];
        } else if (this.rotationRadians < Math.PI) {
            matrix[4] = matrix[0] + matrix[2];
            matrix[5] = matrix[3];
            matrix[6] = 0.0F;
            matrix[7] = matrix[1];
        } else if (this.rotationRadians < 4.71238898038469D) {
            matrix[4] = matrix[0];
            matrix[5] = matrix[1] + matrix[3];
            matrix[6] = matrix[2];
            matrix[7] = 0.0F;
        } else {
            matrix[4] = 0.0F;
            matrix[5] = matrix[1];
            matrix[6] = matrix[0] + matrix[2];
            matrix[7] = matrix[3];
        }
        return matrix;
    }
    static long serialId = 0L;
    protected float rotationRadians;
    private float initialRotation;
    protected float indentationLeft;
    protected float indentationRight;
    protected float spacingBefore;
    protected float spacingAfter;
    protected float paddingTop;
    private float widthPercentage;
    protected boolean scaleToFitLineWhenOverflow;
    protected boolean scaleToFitHeight;
    protected Annotation annotation;
    protected PdfOCG layer;
    protected boolean interpolation;
    protected int originalType;
    protected byte[] originalData;
    protected boolean deflated;
    protected int dpiX;
    protected int dpiY;
    private float XYRatio;
    protected int colorspace;
    protected int colortransform;
    protected boolean invert;

    public boolean isMask() {
        return this.mask;
    }
    protected ICC_Profile profile;
    private PdfDictionary additional;
    protected boolean mask;
    protected Image imageMask;
    private boolean smask;
    protected int[] transparency;

    protected static synchronized Long getSerialId() {
        serialId++;
        return Long.valueOf(serialId);
    }

    public Long getMySerialId() {
        return this.mySerialId;
    }

    public float getImageRotation() {
        double d = 6.283185307179586D;
        float rot = (float) ((this.rotationRadians - this.initialRotation) % d);
        if (rot < 0.0F) {
            rot = (float) (rot + d);
        }
        return rot;
    }

    public void setRotation(float r) {
        double d = 6.283185307179586D;
        this.rotationRadians = (float) ((r + this.initialRotation) % d);
        if (this.rotationRadians < 0.0F) {
            this.rotationRadians = (float) (this.rotationRadians + d);
        }
        float[] matrix = matrix();
        this.scaledWidth = matrix[6] - matrix[4];
        this.scaledHeight = matrix[7] - matrix[5];
    }

    public void setRotationDegrees(float deg) {
        double d = Math.PI;
        setRotation(deg / 180.0F * (float) d);
    }

    public float getInitialRotation() {
        return this.initialRotation;
    }

    public void setInitialRotation(float initialRotation) {
        float old_rot = this.rotationRadians - this.initialRotation;
        this.initialRotation = initialRotation;
        setRotation(old_rot);
    }

    public float getIndentationLeft() {
        return this.indentationLeft;
    }

    public void setIndentationLeft(float f) {
        this.indentationLeft = f;
    }

    public float getIndentationRight() {
        return this.indentationRight;
    }

    public void setIndentationRight(float f) {
        this.indentationRight = f;
    }

    public float getSpacingBefore() {
        return this.spacingBefore;
    }

    public void setSpacingBefore(float spacing) {
        this.spacingBefore = spacing;
    }

    public float getSpacingAfter() {
        return this.spacingAfter;
    }

    public void setSpacingAfter(float spacing) {
        this.spacingAfter = spacing;
    }

    public float getPaddingTop() {
        return this.paddingTop;
    }

    public void setPaddingTop(float paddingTop) {
        this.paddingTop = paddingTop;
    }

    public float getWidthPercentage() {
        return this.widthPercentage;
    }

    public void setWidthPercentage(float widthPercentage) {
        this.widthPercentage = widthPercentage;
    }

    public boolean isScaleToFitLineWhenOverflow() {
        return this.scaleToFitLineWhenOverflow;
    }

    public void setScaleToFitLineWhenOverflow(boolean scaleToFitLineWhenOverflow) {
        this.scaleToFitLineWhenOverflow = scaleToFitLineWhenOverflow;
    }

    public boolean isScaleToFitHeight() {
        return this.scaleToFitHeight;
    }

    public void setScaleToFitHeight(boolean scaleToFitHeight) {
        this.scaleToFitHeight = scaleToFitHeight;
    }

    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }

    public Annotation getAnnotation() {
        return this.annotation;
    }

    public PdfOCG getLayer() {
        return this.layer;
    }

    public void setLayer(PdfOCG layer) {
        this.layer = layer;
    }

    public boolean isInterpolation() {
        return this.interpolation;
    }

    public void setInterpolation(boolean interpolation) {
        this.interpolation = interpolation;
    }

    public int getOriginalType() {
        return this.originalType;
    }

    public void setOriginalType(int originalType) {
        this.originalType = originalType;
    }

    public byte[] getOriginalData() {
        return this.originalData;
    }

    public void setOriginalData(byte[] originalData) {
        this.originalData = originalData;
    }

    public boolean isDeflated() {
        return this.deflated;
    }

    public void setDeflated(boolean deflated) {
        this.deflated = deflated;
    }

    public int getDpiX() {
        return this.dpiX;
    }

    public int getDpiY() {
        return this.dpiY;
    }

    public void setDpi(int dpiX, int dpiY) {
        this.dpiX = dpiX;
        this.dpiY = dpiY;
    }

    public float getXYRatio() {
        return this.XYRatio;
    }

    public void setXYRatio(float XYRatio) {
        this.XYRatio = XYRatio;
    }

    public int getColorspace() {
        return this.colorspace;
    }

    public void setColorTransform(int c) {
        this.colortransform = c;
    }

    public int getColorTransform() {
        return this.colortransform;
    }

    public boolean isInverted() {
        return this.invert;
    }

    public void setInverted(boolean invert) {
        this.invert = invert;
    }

    public void tagICC(ICC_Profile profile) {
        this.profile = profile;
    }

    public boolean hasICCProfile() {
        return (this.profile != null);
    }

    public ICC_Profile getICCProfile() {
        return this.profile;
    }

    public PdfDictionary getAdditional() {
        return this.additional;
    }

    public void setAdditional(PdfDictionary additional) {
        this.additional = additional;
    }

    public void simplifyColorspace() {
        PdfArray pdfArray1 = new PdfArray();
        if (this.additional == null) {
            return;
        }
        PdfArray value = this.additional.getAsArray(PdfName.COLORSPACE);
        if (value == null) {
            return;
        }
        PdfObject cs = simplifyColorspace(value);
        if (cs.isName()) {
            PdfObject newValue = cs;
        } else {
            pdfArray1 = value;
            PdfName first = value.getAsName(0);
            if (PdfName.INDEXED.equals(first) && value.size() >= 2) {
                PdfArray second = value.getAsArray(1);
                if (second != null) {
                    value.set(1, simplifyColorspace(second));
                }
            }
        }
        this.additional.put(PdfName.COLORSPACE, (PdfObject) pdfArray1);
    }

    private PdfObject simplifyColorspace(PdfArray obj) {
        if (obj == null) {
            return (PdfObject) obj;
        }
        PdfName first = obj.getAsName(0);
        if (PdfName.CALGRAY.equals(first)) {
            return (PdfObject) PdfName.DEVICEGRAY;
        }
        if (PdfName.CALRGB.equals(first)) {
            return (PdfObject) PdfName.DEVICERGB;
        }
        return (PdfObject) obj;
    }

    public void makeMask() throws DocumentException {
        if (!isMaskCandidate()) {
            throw new DocumentException(MessageLocalization.getComposedMessage("this.image.can.not.be.an.image.mask", new Object[0]));
        }
        this.mask = true;
    }

    public boolean isMaskCandidate() {
        if (this.type == 34
                && this.bpc > 255) {
            return true;
        }
        return (this.colorspace == 1);
    }

    public Image getImageMask() {
        return this.imageMask;
    }

    public void setImageMask(Image mask) throws DocumentException {
        if (this.mask) {
            throw new DocumentException(MessageLocalization.getComposedMessage("an.image.mask.cannot.contain.another.image.mask", new Object[0]));
        }
        if (!mask.mask) {
            throw new DocumentException(MessageLocalization.getComposedMessage("the.image.mask.is.not.a.mask.did.you.do.makemask", new Object[0]));
        }
        this.imageMask = mask;
        this.smask = (mask.bpc > 1 && mask.bpc <= 8);
    }

    public boolean isSmask() {
        return this.smask;
    }

    public void setSmask(boolean smask) {
        this.smask = smask;
    }

    public int[] getTransparency() {
        return this.transparency;
    }

    public void setTransparency(int[] transparency) {
        this.transparency = transparency;
    }

    public int getCompressionLevel() {
        return this.compressionLevel;
    }

    public void setCompressionLevel(int compressionLevel) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            this.compressionLevel = -1;
        } else {
            this.compressionLevel = compressionLevel;
        }
    }

    public PdfObject getAccessibleAttribute(PdfName key) {
        if (this.accessibleAttributes != null) {
            return this.accessibleAttributes.get(key);
        }
        return null;
    }

    public void setAccessibleAttribute(PdfName key, PdfObject value) {
        if (this.accessibleAttributes == null) {
            this.accessibleAttributes = new HashMap<PdfName, PdfObject>();
        }
        this.accessibleAttributes.put(key, value);
    }

    public HashMap<PdfName, PdfObject> getAccessibleAttributes() {
        return this.accessibleAttributes;
    }

    public PdfName getRole() {
        return this.role;
    }

    public void setRole(PdfName role) {
        this.role = role;
    }

    public AccessibleElementId getId() {
        if (this.id == null) {
            this.id = new AccessibleElementId();
        }
        return this.id;
    }

    public void setId(AccessibleElementId id) {
        this.id = id;
    }

    public boolean isInline() {
        return true;
    }

    public static Image getInstance(java.awt.Image image, Color color, boolean forceBW) throws BadElementException, IOException {
        if (image instanceof BufferedImage) {
            BufferedImage bi = (BufferedImage) image;
            if (bi.getType() == 12 && bi
                    .getColorModel().getPixelSize() == 1) {
                forceBW = true;
            }
        }

        PixelGrabber pg = new PixelGrabber(image, 0, 0, -1, -1, true);

        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            throw new IOException(MessageLocalization.getComposedMessage("java.awt.image.interrupted.waiting.for.pixels", new Object[0]));
        }
        if ((pg.getStatus() & 0x80) != 0) {
            throw new IOException(MessageLocalization.getComposedMessage("java.awt.image.fetch.aborted.or.errored", new Object[0]));
        }
        int w = pg.getWidth();
        int h = pg.getHeight();
        int[] pixels = (int[]) pg.getPixels();
        if (forceBW) {
            int byteWidth = w / 8 + (((w & 0x7) != 0) ? 1 : 0);
            byte[] arrayOfByte = new byte[byteWidth * h];

            int i = 0;
            int j = h * w;
            int transColor = 1;
            if (color != null) {
                transColor = (color.getRed() + color.getGreen() + color.getBlue() < 384) ? 0 : 1;
            }
            int[] arrayOfInt = null;
            int cbyte = 128;
            int wMarker = 0;
            int currByte = 0;
            if (color != null) {
                for (int k = 0; k < j; k++) {
                    int alpha = pixels[k] >> 24 & 0xFF;
                    if (alpha < 250) {
                        if (transColor == 1) {
                            currByte |= cbyte;
                        }
                    } else if ((pixels[k] & 0x888) != 0) {
                        currByte |= cbyte;
                    }
                    cbyte >>= 1;
                    if (cbyte == 0 || wMarker + 1 >= w) {
                        arrayOfByte[i++] = (byte) currByte;
                        cbyte = 128;
                        currByte = 0;
                    }
                    wMarker++;
                    if (wMarker >= w) {
                        wMarker = 0;
                    }
                }
            } else {
                for (int k = 0; k < j; k++) {
                    if (arrayOfInt == null) {
                        int alpha = pixels[k] >> 24 & 0xFF;
                        if (alpha == 0) {
                            arrayOfInt = new int[2];

                            arrayOfInt[1] = ((pixels[k] & 0x888) != 0) ? 255 : 0;
                            arrayOfInt[0] = ((pixels[k] & 0x888) != 0) ? 255 : 0;
                        }
                    }
                    if ((pixels[k] & 0x888) != 0) {
                        currByte |= cbyte;
                    }
                    cbyte >>= 1;
                    if (cbyte == 0 || wMarker + 1 >= w) {
                        arrayOfByte[i++] = (byte) currByte;
                        cbyte = 128;
                        currByte = 0;
                    }
                    wMarker++;
                    if (wMarker >= w) {
                        wMarker = 0;
                    }
                }
            }
            return getInstance(w, h, 1, 1, arrayOfByte, arrayOfInt);
        }
        byte[] pixelsByte = new byte[w * h * 3];
        byte[] smask = null;

        int index = 0;
        int size = h * w;
        int red = 255;
        int green = 255;
        int blue = 255;
        if (color != null) {
            red = color.getRed();
            green = color.getGreen();
            blue = color.getBlue();
        }
        int[] transparency = null;
        if (color != null) {
            for (int j = 0; j < size; j++) {
                int alpha = pixels[j] >> 24 & 0xFF;
                if (alpha < 250) {
                    pixelsByte[index++] = (byte) red;
                    pixelsByte[index++] = (byte) green;
                    pixelsByte[index++] = (byte) blue;
                } else {
                    pixelsByte[index++] = (byte) (pixels[j] >> 16 & 0xFF);
                    pixelsByte[index++] = (byte) (pixels[j] >> 8 & 0xFF);
                    pixelsByte[index++] = (byte) (pixels[j] & 0xFF);
                }
            }
        } else {
            int transparentPixel = 0;
            smask = new byte[w * h];
            boolean shades = false;
            for (int j = 0; j < size; j++) {
                byte alpha = smask[j] = (byte) (pixels[j] >> 24 & 0xFF);

                if (!shades) {
                    if (alpha != 0 && alpha != -1) {
                        shades = true;
                    } else if (transparency == null) {
                        if (alpha == 0) {
                            transparentPixel = pixels[j] & 0xFFFFFF;
                            transparency = new int[6];
                            transparency[1] = transparentPixel >> 16 & 0xFF;
                            transparency[0] = transparentPixel >> 16 & 0xFF;
                            transparency[3] = transparentPixel >> 8 & 0xFF;
                            transparency[2] = transparentPixel >> 8 & 0xFF;
                            transparency[5] = transparentPixel & 0xFF;
                            transparency[4] = transparentPixel & 0xFF;

                            for (int prevPixel = 0; prevPixel < j; prevPixel++) {
                                if ((pixels[prevPixel] & 0xFFFFFF) == transparentPixel) {

                                    shades = true;
                                    break;
                                }
                            }
                        }
                    } else if ((pixels[j] & 0xFFFFFF) != transparentPixel && alpha == 0) {
                        shades = true;
                    } else if ((pixels[j] & 0xFFFFFF) == transparentPixel && alpha != 0) {
                        shades = true;
                    }
                }
                pixelsByte[index++] = (byte) (pixels[j] >> 16 & 0xFF);
                pixelsByte[index++] = (byte) (pixels[j] >> 8 & 0xFF);
                pixelsByte[index++] = (byte) (pixels[j] & 0xFF);
            }
            if (shades) {
                transparency = null;
            } else {
                smask = null;
            }
        }
        Image img = getInstance(w, h, 3, 8, pixelsByte, transparency);
        if (smask != null) {
            Image sm = getInstance(w, h, 1, 8, smask);
            try {
                sm.makeMask();
                img.setImageMask(sm);
            } catch (DocumentException de) {
                throw new ExceptionConverter(de);
            }
        }
        return img;
    }

    public static Image getInstance(java.awt.Image image, Color color) throws BadElementException, IOException {
        return getInstance(image, color, false);
    }

    public static Image getInstance(PdfWriter writer, java.awt.Image awtImage, float quality) throws BadElementException, IOException {
        return getInstance(new PdfContentByte(writer), awtImage, quality);
    }

    public static Image getInstance(PdfContentByte cb, java.awt.Image awtImage, float quality) throws BadElementException, IOException {
        PixelGrabber pg = new PixelGrabber(awtImage, 0, 0, -1, -1, true);

        try {
            pg.grabPixels();
        } catch (InterruptedException e) {
            throw new IOException(MessageLocalization.getComposedMessage("java.awt.image.interrupted.waiting.for.pixels", new Object[0]));
        }
        if ((pg.getStatus() & 0x80) != 0) {
            throw new IOException(MessageLocalization.getComposedMessage("java.awt.image.fetch.aborted.or.errored", new Object[0]));
        }
        int w = pg.getWidth();
        int h = pg.getHeight();
        PdfTemplate tp = cb.createTemplate(w, h);
        PdfGraphics2D g2d = new PdfGraphics2D((PdfContentByte) tp, w, h, null, false, true, quality);
        g2d.drawImage(awtImage, 0, 0, null);
        g2d.dispose();
        return getInstance(tp);
    }
}
