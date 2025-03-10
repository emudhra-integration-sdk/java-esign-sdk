package esign.text.pdf;

import esign.text.Chunk;
import esign.text.DocumentException;
import esign.text.Element;
import esign.text.ExceptionConverter;
import esign.text.Font;
import esign.text.Image;
import esign.text.Paragraph;
import esign.text.Phrase;
import esign.text.Rectangle;
import esign.text.Version;
import esign.text.error_messages.MessageLocalization;
import esign.text.io.RASInputStream;
import esign.text.io.RandomAccessSource;
import esign.text.io.RandomAccessSourceFactory;
import esign.text.pdf.security.CertificateInfo;
import esign.text.pdf.security.PdfPKCS7;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.PrivateKey;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfSignatureAppearance {

    public static final int NOT_CERTIFIED = 0;
    public static final int CERTIFIED_NO_CHANGES_ALLOWED = 1;
    public static final int CERTIFIED_FORM_FILLING = 2;
    public static final int CERTIFIED_FORM_FILLING_AND_ANNOTATIONS = 3;
    private int certificationLevel;
    private String reasonCaption;
    private String locationCaption;
    private String reason;
    private String location;
    private Calendar signDate;
    private String signatureCreator;
    private String contact;
    private RandomAccessFile raf;
    private byte[] bout;
    private long[] range;
    private Certificate signCertificate;
    private PdfDictionary cryptoDictionary;
    private SignatureEvent signatureEvent;
    private String fieldName;
    private int page;
    private Rectangle rect;
    private Rectangle pageRect;
    private RenderingMode renderingMode;
    private Image signatureGraphic;
    private boolean acro6Layers;
    private PdfTemplate[] app;
    private boolean reuseAppearance;
    public static final String questionMark = "% DSUnknown\nq\n1 G\n1 g\n0.1 0 0 0.1 9 0 cm\n0 J 0 j 4 M []0 d\n1 i \n0 g\n313 292 m\n313 404 325 453 432 529 c\n478 561 504 597 504 645 c\n504 736 440 760 391 760 c\n286 760 271 681 265 626 c\n265 625 l\n100 625 l\n100 828 253 898 381 898 c\n451 898 679 878 679 650 c\n679 555 628 499 538 435 c\n488 399 467 376 467 292 c\n313 292 l\nh\n308 214 170 -164 re\nf\n0.44 G\n1.2 w\n1 1 0.4 rg\n287 318 m\n287 430 299 479 406 555 c\n451 587 478 623 478 671 c\n478 762 414 786 365 786 c\n260 786 245 707 239 652 c\n239 651 l\n74 651 l\n74 854 227 924 355 924 c\n425 924 653 904 653 676 c\n653 581 602 525 512 461 c\n462 425 441 402 441 318 c\n287 318 l\nh\n282 240 170 -164 re\nB\nQ\n";
    private Image image;
    private float imageScale;
    private String layer2Text;
    private Font layer2Font;
    private int runDirection;
    private String layer4Text;
    private PdfTemplate frm;
    private static final float TOP_SECTION = 0.3F;
    private static final float MARGIN = 2.0F;
    private PdfStamper stamper;
    private PdfStamperImp writer;
    private ByteBuffer sigout;
    private OutputStream originalout;
    private File tempFile;
    public HashMap<PdfName, PdfLiteral> exclusionLocations;
    private int boutLen;
    private boolean preClosed;
    private PdfSigLockDictionary fieldLock;

    PdfSignatureAppearance(PdfStamperImp writer) {
        this.certificationLevel = 0;

        this.reasonCaption = "Reason: ";

        this.locationCaption = "Location: ";

        this.page = 1;

        this.renderingMode = RenderingMode.DESCRIPTION;

        this.signatureGraphic = null;

        this.acro6Layers = true;

        this.app = new PdfTemplate[5];

        this.reuseAppearance = false;

        this.runDirection = 1;

        this.preClosed = false;
        this.writer = writer;
        this.signDate = new GregorianCalendar();
        this.fieldName = getNewSigName();
        this.signatureCreator = Version.getInstance().getVersion();
    }

    public void setCertificationLevel(int certificationLevel) {
        this.certificationLevel = certificationLevel;
    }

    public int getCertificationLevel() {
        return this.certificationLevel;
    }

    public String getReason() {
        return this.reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setReasonCaption(String reasonCaption) {
        this.reasonCaption = reasonCaption;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setLocationCaption(String locationCaption) {
        this.locationCaption = locationCaption;
    }

    public String getSignatureCreator() {
        return this.signatureCreator;
    }

    public void setSignatureCreator(String signatureCreator) {
        this.signatureCreator = signatureCreator;
    }

    public String getContact() {
        return this.contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public Calendar getSignDate() {
        return this.signDate;
    }

    public void setSignDate(Calendar signDate) {
        this.signDate = signDate;
    }

    public InputStream getRangeStream() throws IOException {
        RandomAccessSourceFactory fac = new RandomAccessSourceFactory();
        return (InputStream) new RASInputStream(fac.createRanged(getUnderlyingSource(), this.range));
    }

    private RandomAccessSource getUnderlyingSource() throws IOException {
        RandomAccessSourceFactory fac = new RandomAccessSourceFactory();
        return (this.raf == null) ? fac.createSource(this.bout) : fac.createSource(this.raf);
    }

    public void addDeveloperExtension(PdfDeveloperExtension de) {
        this.writer.addDeveloperExtension(de);
    }

    public PdfDictionary getCryptoDictionary() {
        return this.cryptoDictionary;
    }

    public void setCryptoDictionary(PdfDictionary cryptoDictionary) {
        this.cryptoDictionary = cryptoDictionary;
    }

    public void setCertificate(Certificate signCertificate) {
        this.signCertificate = signCertificate;
    }

    public Certificate getCertificate() {
        return this.signCertificate;
    }

    public SignatureEvent getSignatureEvent() {
        return this.signatureEvent;
    }

    public void setSignatureEvent(SignatureEvent signatureEvent) {
        this.signatureEvent = signatureEvent;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public String getNewSigName() {
        AcroFields af = this.writer.getAcroFields();
        String name = "Signature";
        int step = 0;
        boolean found = false;
        while (!found) {
            step++;
            String n1 = name + step;
            if (af.getFieldItem(n1) != null) {
                continue;
            }
            n1 = n1 + ".";
            found = true;
            for (String element : af.getFields().keySet()) {
                String fn = element;
                if (fn.startsWith(n1)) {
                    found = false;
                }
            }
        }
        name = name + step;
        return name;
    }
    int[] pages = {1};

    public int getPage() {
        return this.page;
    }

    public int[] getPages() {
        return this.pages;
    }

    public Rectangle getRect() {
        return this.rect;
    }

    public Rectangle getPageRect() {
        return this.pageRect;
    }

    public boolean isInvisible() {
        return (this.rect == null || this.rect.getWidth() == 0.0F || this.rect.getHeight() == 0.0F);
    }

    /**
     * Sets the signature to be visible. It creates a new visible signature
     * field.
     *
     * @param pageRect the position and dimension of the field in the page
     * @param pages the page to place the field. The fist page is 1
     * @param fieldName the field name or <CODE>null</CODE> to generate
     * automatically a new field name
     */
    boolean newField;
    Rectangle[] pageRect2;
//    int[] pag;

    public void setVisibleSignature(Rectangle pageRect, int[] pages, String fieldName, List<Rectangle> rList) {
        if (fieldName != null) {
            if (fieldName.indexOf('.') >= 0) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("field.names.cannot.contain.a.dot"));
            }
            AcroFields af = writer.getAcroFields();
            AcroFields.Item item = af.getFieldItem(fieldName);
            if (item != null) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.field.1.already.exists", fieldName));
            }
            this.fieldName = fieldName;
        }
        /*for(int page: pages){
	        if (page < 1 || page > writer.reader.getNumberOfPages())
	            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("invalid.page.number.1", page));
        }*/
        this.pageRect = new Rectangle(pageRect);
        this.pageRect.normalize();
        rect = new Rectangle(this.pageRect.getWidth(), this.pageRect.getHeight());
        this.pages = pages;
        newField = true;
        this.pageRect2 = new Rectangle[rList.size()];
        for (int i = 0; i < rList.size(); i++) {
            pageRect2[i] = rList.get(i);
        }
    }

    public void setVisibleSignature(Rectangle pageRect, int page, String fieldName) {
        if (fieldName != null) {
            if (fieldName.indexOf('.') >= 0) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("field.names.cannot.contain.a.dot", new Object[0]));
            }
            AcroFields af = this.writer.getAcroFields();
            AcroFields.Item item = af.getFieldItem(fieldName);
            if (item != null) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.field.1.already.exists", new Object[]{fieldName}));
            }
            this.fieldName = fieldName;
        }
        if (page < 1 || page > this.writer.reader.getNumberOfPages()) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("invalid.page.number.1", page));
        }
        this.pageRect = new Rectangle(pageRect);
        this.pageRect.normalize();
        this.rect = new Rectangle(this.pageRect.getWidth(), this.pageRect.getHeight());
        this.page = page;
    }

//    public void setVisibleSignature(Rectangle pageRect, int[] pages, String fieldName, List<Rectangle> rList) {
//        if (fieldName != null) {
//            if (fieldName.indexOf('.') >= 0) {
//                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("field.names.cannot.contain.a.dot"));
//            }
//            AcroFields af = writer.getAcroFields();
//            AcroFields.Item item = af.getFieldItem(fieldName);
//            if (item != null) {
//                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.field.1.already.exists", fieldName));
//            }
//            this.fieldName = fieldName;
//        }
//        /*for(int page: pages){
//	        if (page < 1 || page > writer.reader.getNumberOfPages())
//	            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("invalid.page.number.1", page));
//        }*/
//        this.pageRect = new Rectangle(pageRect);
//        this.pageRect.normalize();
//        rect = new Rectangle(this.pageRect.getWidth(), this.pageRect.getHeight());
//        this.pages = pages;
//        newField = true;
//        this.pageRect2 = new Rectangle[rList.size()];
//        for (int i = 0; i < rList.size(); i++) {
//            pageRect2[i] = rList.get(i);
//        }
//    }
    public void setVisibleSignature(String fieldName) {
        AcroFields af = this.writer.getAcroFields();
        AcroFields.Item item = af.getFieldItem(fieldName);
        if (item == null) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.field.1.does.not.exist", new Object[]{fieldName}));
        }
        PdfDictionary merged = item.getMerged(0);
        if (!PdfName.SIG.equals(PdfReader.getPdfObject(merged.get(PdfName.FT)))) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.field.1.is.not.a.signature.field", new Object[]{fieldName}));
        }
        this.fieldName = fieldName;
        PdfArray r = merged.getAsArray(PdfName.RECT);
        float llx = r.getAsNumber(0).floatValue();
        float lly = r.getAsNumber(1).floatValue();
        float urx = r.getAsNumber(2).floatValue();
        float ury = r.getAsNumber(3).floatValue();
        this.pageRect = new Rectangle(llx, lly, urx, ury);
        this.pageRect.normalize();
        this.page = item.getPage(0).intValue();
        int rotation = this.writer.reader.getPageRotation(this.page);
        Rectangle pageSize = this.writer.reader.getPageSizeWithRotation(this.page);
        switch (rotation) {
            case 90:
                this.pageRect = new Rectangle(this.pageRect.getBottom(), pageSize.getTop() - this.pageRect.getLeft(), this.pageRect.getTop(), pageSize.getTop() - this.pageRect.getRight());
                break;
            case 180:
                this.pageRect = new Rectangle(pageSize.getRight() - this.pageRect.getLeft(), pageSize.getTop() - this.pageRect.getBottom(), pageSize.getRight() - this.pageRect.getRight(), pageSize.getTop() - this.pageRect.getTop());
                break;
            case 270:
                this.pageRect = new Rectangle(pageSize.getRight() - this.pageRect.getBottom(), this.pageRect.getLeft(), pageSize.getRight() - this.pageRect.getTop(), this.pageRect.getRight());
                break;
        }
        if (rotation != 0) {
            this.pageRect.normalize();
        }
        this.rect = new Rectangle(this.pageRect.getWidth(), this.pageRect.getHeight());
    }

    public static interface SignatureEvent {

        void getSignatureDictionary(PdfDictionary param1PdfDictionary);
    }

    public PdfSigLockDictionary getFieldLockDict() {
        return this.fieldLock;
    }

    public enum RenderingMode {
        DESCRIPTION, NAME_AND_DESCRIPTION, GRAPHIC_AND_DESCRIPTION, GRAPHIC, NAME_GRAPHIC_AND_DESCRIPTION;
    }

    public RenderingMode getRenderingMode() {
        return this.renderingMode;
    }

    public void setRenderingMode(RenderingMode renderingMode) {
        this.renderingMode = renderingMode;
    }

    public Image getSignatureGraphic() {
        return this.signatureGraphic;
    }

    public void setSignatureGraphic(Image signatureGraphic) {
        this.signatureGraphic = signatureGraphic;
    }

    public boolean isAcro6Layers() {
        return this.acro6Layers;
    }

    public void setAcro6Layers(boolean acro6Layers) {
        this.acro6Layers = acro6Layers;
    }

    public PdfTemplate getLayer(int layer) {
        if (layer < 0 || layer >= this.app.length) {
            return null;
        }
        PdfTemplate t = this.app[layer];
        if (t == null) {
            t = this.app[layer] = new PdfTemplate(this.writer);
            t.setBoundingBox(this.rect);
            this.writer.addDirectTemplateSimple(t, new PdfName("n" + layer));
        }
        return t;
    }

    public void setReuseAppearance(boolean reuseAppearance) {
        this.reuseAppearance = reuseAppearance;
    }

    public Image getImage() {
        return this.image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public float getImageScale() {
        return this.imageScale;
    }

    public void setImageScale(float imageScale) {
        this.imageScale = imageScale;
    }

    public void setLayer2Text(String text) {
        this.layer2Text = text;
    }

    public String getLayer2Text() {
        return this.layer2Text;
    }

    public Font getLayer2Font() {
        return this.layer2Font;
    }

    public void setFieldLockDict(PdfSigLockDictionary fieldLock) {
        this.fieldLock = fieldLock;
    }

    public void setLayer2Font(Font layer2Font) {
        this.layer2Font = layer2Font;
    }

    public void setRunDirection(int runDirection) {
        if (runDirection < 0 || runDirection > 3) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("invalid.run.direction.1", runDirection));
        }
        this.runDirection = runDirection;
    }

    public int getRunDirection() {
        return this.runDirection;
    }

    public void setLayer4Text(String text) {
        this.layer4Text = text;
    }

    public boolean isPreClosed() {
        return this.preClosed;
    }

    public String getLayer4Text() {
        return this.layer4Text;
    }

    public PdfTemplate getTopLayer() {
        if (this.frm == null) {
            this.frm = new PdfTemplate(this.writer);
            this.frm.setBoundingBox(this.rect);
            this.writer.addDirectTemplateSimple(this.frm, new PdfName("FRM"));
        }
        return this.frm;
    }

    public PdfTemplate getAppearance(int pageno) throws DocumentException {
        if (isInvisible()) {
            PdfTemplate t = new PdfTemplate(this.writer);
            t.setBoundingBox(new Rectangle(0.0F, 0.0F));
            this.writer.addDirectTemplateSimple(t, null);
            return t;
        }
        if (this.app[0] == null && !this.reuseAppearance) {
            createBlankN0();
        }
        if (this.app[1] == null && !this.acro6Layers) {
            PdfTemplate t = this.app[1] = new PdfTemplate(this.writer);
            t.setBoundingBox(new Rectangle(100.0F, 100.0F));
            this.writer.addDirectTemplateSimple(t, new PdfName("n1"));
            t.setLiteral("% DSUnknown\nq\n1 G\n1 g\n0.1 0 0 0.1 9 0 cm\n0 J 0 j 4 M []0 d\n1 i \n0 g\n313 292 m\n313 404 325 453 432 529 c\n478 561 504 597 504 645 c\n504 736 440 760 391 760 c\n286 760 271 681 265 626 c\n265 625 l\n100 625 l\n100 828 253 898 381 898 c\n451 898 679 878 679 650 c\n679 555 628 499 538 435 c\n488 399 467 376 467 292 c\n313 292 l\nh\n308 214 170 -164 re\nf\n0.44 G\n1.2 w\n1 1 0.4 rg\n287 318 m\n287 430 299 479 406 555 c\n451 587 478 623 478 671 c\n478 762 414 786 365 786 c\n260 786 245 707 239 652 c\n239 651 l\n74 651 l\n74 854 227 924 355 924 c\n425 924 653 904 653 676 c\n653 581 602 525 512 461 c\n462 425 441 402 441 318 c\n287 318 l\nh\n282 240 170 -164 re\nB\nQ\n");
        }
        if (this.app[2] == null) {
            String text;
            Font font;
            String signedBy;
            Rectangle sr2;
            float signedSize;
            ColumnText ct2;
            Image im;
            Paragraph p;
            float x, y;
            if (this.layer2Text == null) {
//                StringBuilder buf = new StringBuilder();
                StringBuffer buf = new StringBuffer();
                if (!signerName.isEmpty()) {
                    buf.append("Digitally Signed by " + signerName + "\n");
                } else {
                    buf.append("Document Digitally Signed \n");
                }
                SimpleDateFormat sd = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");
                buf.append("Date: ").append(sd.format(signDate.getTime()));
                if (reason != null) {
                    buf.append('\n').append("Reason: ").append(reason);
                }
                if (location != null) {
                    buf.append('\n').append("Location: ").append(location);
                }
                text = buf.toString();
            } else {
                text = this.layer2Text;
            }
            PdfTemplate t = this.app[2] = new PdfTemplate(this.writer);
            t.setBoundingBox(this.rect);
            this.writer.addDirectTemplateSimple(t, new PdfName("n2"));
            if (this.image != null) {
                if (this.imageScale == 0.0F) {
                    t.addImage(this.image, this.rect.getWidth(), 0.0F, 0.0F, this.rect.getHeight(), 0.0F, 0.0F);
                } else {
                    float usableScale = this.imageScale;
                    if (this.imageScale < 0.0F) {
                        usableScale = Math.min(this.rect.getWidth() / this.image.getWidth(), this.rect.getHeight() / this.image.getHeight());
                    }
                    float w = this.image.getWidth() * usableScale;
                    float h = this.image.getHeight() * usableScale;
                    float f1 = (this.rect.getWidth() - w) / 2.0F;
                    float f2 = (this.rect.getHeight() - h) / 2.0F;
                    t.addImage(this.image, w, 0.0F, 0.0F, h, f1, f2);
                }
            }
            if (this.layer2Font == null) {
                font = new Font();
            } else {
                font = new Font(this.layer2Font);
            }
            float size = font.getSize();
            Rectangle dataRect = null;
            Rectangle signatureRect = null;
            if (this.renderingMode == RenderingMode.NAME_AND_DESCRIPTION || (this.renderingMode == RenderingMode.GRAPHIC_AND_DESCRIPTION || this.renderingMode == RenderingMode.NAME_GRAPHIC_AND_DESCRIPTION && this.signatureGraphic != null)) {
                signatureRect = new Rectangle(2.0F, 2.0F, this.rect.getWidth() / 2.0F - 2.0F, this.rect.getHeight() - 2.0F);
                dataRect = new Rectangle(this.rect.getWidth() / 2.0F + 1.0F, 2.0F, this.rect.getWidth() - 1.0F, this.rect.getHeight() - 2.0F);
                if (this.rect.getHeight() > this.rect.getWidth()) {
                    signatureRect = new Rectangle(2.0F, this.rect.getHeight() / 2.0F, this.rect.getWidth() - 2.0F, this.rect.getHeight());
                    dataRect = new Rectangle(2.0F, 2.0F, this.rect.getWidth() - 2.0F, this.rect.getHeight() / 2.0F - 2.0F);
                }
            } else if (this.renderingMode == RenderingMode.GRAPHIC) {
                if (this.signatureGraphic == null) {
                    throw new IllegalStateException(MessageLocalization.getComposedMessage("a.signature.image.should.be.present.when.rendering.mode.is.graphic.only", new Object[0]));
                }
                signatureRect = new Rectangle(2.0F, 2.0F, this.rect.getWidth() - 2.0F, this.rect.getHeight() - 2.0F);
            } else {
                dataRect = new Rectangle(2.0F, 2.0F, this.rect.getWidth() - 2.0F, this.rect.getHeight() * 0.7F - 2.0F);
            }
            switch (this.renderingMode) {
                case NAME_AND_DESCRIPTION:
                    signedBy = CertificateInfo.getSubjectFields((X509Certificate) this.signCertificate).getField("CN");
                    if (signedBy == null) {
                        signedBy = CertificateInfo.getSubjectFields((X509Certificate) this.signCertificate).getField("E");
                    }
                    if (signedBy == null) {
                        signedBy = "";
                    }
                    sr2 = new Rectangle(signatureRect.getWidth() - 2.0F, signatureRect.getHeight() - 2.0F);
                    signedSize = ColumnText.fitText(font, signedBy, sr2, -1.0F, this.runDirection);
                    ct2 = new ColumnText(t);
                    ct2.setRunDirection(this.runDirection);
                    ct2.setSimpleColumn(new Phrase(signedBy, font), signatureRect.getLeft(), signatureRect.getBottom(), signatureRect.getRight(), signatureRect.getTop(), signedSize, 0);
                    ct2.go();
                    break;
                case GRAPHIC_AND_DESCRIPTION:
                    if (this.signatureGraphic == null) {
                        throw new IllegalStateException(MessageLocalization.getComposedMessage("a.signature.image.should.be.present.when.rendering.mode.is.graphic.and.description", new Object[0]));
                    }
                    ct2 = new ColumnText(t);
                    ct2.setRunDirection(this.runDirection);
                    ct2.setSimpleColumn(signatureRect.getLeft(), signatureRect.getBottom(), signatureRect.getRight(), signatureRect.getTop(), 0.0F, 2);
                    im = Image.getInstance(this.signatureGraphic);
                    im.scaleToFit(signatureRect.getWidth(), signatureRect.getHeight());
                    p = new Paragraph();
                    x = 0.0F;
                    y = -im.getScaledHeight() + 15.0F;
                    x += (signatureRect.getWidth() - im.getScaledWidth()) / 2.0F;
                    y -= (signatureRect.getHeight() - im.getScaledHeight()) / 2.0F;
                    p.add((Element) new Chunk(im, x + (signatureRect.getWidth() - im.getScaledWidth()) / 2.0F, y, false));
                    ct2.addElement((Element) p);
                    ct2.go();
                    break;
                case GRAPHIC:
                    ct2 = new ColumnText(t);
                    ct2.setRunDirection(this.runDirection);
                    ct2.setSimpleColumn(signatureRect.getLeft(), signatureRect.getBottom(), signatureRect.getRight(), signatureRect.getTop(), 0.0F, 2);
                    im = Image.getInstance(this.signatureGraphic);
                    im.scaleToFit(signatureRect.getWidth(), signatureRect.getHeight());
                    p = new Paragraph(signatureRect.getHeight());
                    x = (signatureRect.getWidth() - im.getScaledWidth()) / 2.0F;
                    y = (signatureRect.getHeight() - im.getScaledHeight()) / 2.0F;
                    p.add((Element) new Chunk(im, x, y, false));
                    ct2.addElement((Element) p);
                    ct2.go();
                    break;

                case NAME_GRAPHIC_AND_DESCRIPTION:
                    if (signatureGraphic != null) {
                        image = Image.getInstance(signatureGraphic);
                    }
                    if (image != null) {
                        if (imageScale == 0) {
                            t.addImage(image, rect.getWidth(), 0, 0, rect.getHeight(), 0, 0);
                        } else {
                            float usableScale = imageScale;
                            usableScale = Math.min((rect.getWidth() - imageScale) / image.getWidth(), (rect.getHeight() - imageScale) / image.getHeight());

                            float w = image.getWidth() * usableScale;
                            float h = image.getHeight() * usableScale;
                            x = (rect.getWidth() - w) / 2;
                            y = (rect.getHeight() - h) / 2;
                            t.addImage(image, w, 0, 0, h, x, y);
                        }
                    }

                    sr2 = new Rectangle(signatureRect.getWidth() - MARGIN, signatureRect.getHeight() - MARGIN);
                    signedSize = fitText(font, signerName, sr2, -1, runDirection);
                    ct2 = new ColumnText(t);
                    ct2.setRunDirection(runDirection);
                    ct2.setSimpleColumn(new Phrase(signerName, font), signatureRect.getLeft(), signatureRect.getBottom(), signatureRect.getRight(), signatureRect.getTop(), signedSize, Element.ALIGN_LEFT);

                    ct2.go();
                    break;
                default:
            }
            if (this.renderingMode != RenderingMode.GRAPHIC) {
                if (size <= 0.0F) {
                    Rectangle sr = new Rectangle(dataRect.getWidth(), dataRect.getHeight());
                    size = ColumnText.fitText(font, text, sr, 12.0F, this.runDirection);
                }
                ColumnText ct = new ColumnText(t);
                ct.setRunDirection(this.runDirection);
                ct.setSimpleColumn(new Phrase(text, font), dataRect.getLeft(), dataRect.getBottom(), dataRect.getRight(), dataRect.getTop(), size, 0);
                ct.go();
            }
        }
        if (this.app[3] == null && !this.acro6Layers) {
            PdfTemplate t = this.app[3] = new PdfTemplate(this.writer);
            t.setBoundingBox(new Rectangle(100.0F, 100.0F));
            this.writer.addDirectTemplateSimple(t, new PdfName("n3"));
            t.setLiteral("% DSBlank\n");
        }
        if (this.app[4] == null && !this.acro6Layers) {
            Font font;
            PdfTemplate t = this.app[4] = new PdfTemplate(this.writer);
            t.setBoundingBox(new Rectangle(0.0F, this.rect.getHeight() * 0.7F, this.rect.getRight(), this.rect.getTop()));
            this.writer.addDirectTemplateSimple(t, new PdfName("n4"));
            if (this.layer2Font == null) {
                font = new Font();
            } else {
                font = new Font(this.layer2Font);
            }
            String text = "Signature Not Verified";
            if (this.layer4Text != null) {
                text = this.layer4Text;
            }
            Rectangle sr = new Rectangle(this.rect.getWidth() - 4.0F, this.rect.getHeight() * 0.3F - 4.0F);
            float size = ColumnText.fitText(font, text, sr, 15.0F, this.runDirection);
            ColumnText ct = new ColumnText(t);
            ct.setRunDirection(this.runDirection);
            ct.setSimpleColumn(new Phrase(text, font), 2.0F, 0.0F, this.rect.getWidth() - 2.0F, this.rect.getHeight() - 2.0F, size, 0);
            ct.go();
        }
        int rotation = this.writer.reader.getPageRotation(pageno);

        Rectangle rotated = new Rectangle(this.rect);

        int n = rotation;
        while (n > 0) {
            rotated = rotated.rotate();
            n -= 90;
        }
        if (this.frm == null || (this.frm.getBoundingBox().getRotation() > 0 && rotation == 0) || (this.frm.getBoundingBox().getRotation() == 0 && rotation > 0)) {
            this.frm = new PdfTemplate(this.writer);
            this.frm.setBoundingBox(rotated);
            this.writer.addDirectTemplateSimple(this.frm, new PdfName("FRM"));
            float scale = Math.min(this.rect.getWidth(), this.rect.getHeight()) * 0.9F;
            float x = (this.rect.getWidth() - scale) / 2.0F;
            float y = (this.rect.getHeight() - scale) / 2.0F;
            scale /= 100.0F;
            if (rotation == 90) {
                this.frm.concatCTM(0.0F, 1.0F, -1.0F, 0.0F, this.rect.getHeight(), 0.0F);
            } else if (rotation == 180) {
                this.frm.concatCTM(-1.0F, 0.0F, 0.0F, -1.0F, this.rect.getWidth(), this.rect.getHeight());
            } else if (rotation == 270) {
                this.frm.concatCTM(0.0F, -1.0F, 1.0F, 0.0F, 0.0F, this.rect.getWidth());
            }
            if (this.reuseAppearance) {
                AcroFields af = this.writer.getAcroFields();
                PdfIndirectReference ref = af.getNormalAppearance(getFieldName());
                if (ref != null) {
                    this.frm.addTemplateReference(ref, new PdfName("n0"), 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F);
                } else {
                    this.reuseAppearance = false;
                    if (this.app[0] == null) {
                        createBlankN0();
                    }
                }
            }
            if (!this.reuseAppearance) {
                this.frm.addTemplate(this.app[0], 0.0F, 0.0F);
            }
            if (!this.acro6Layers) {
                this.frm.addTemplate(this.app[1], scale, 0.0F, 0.0F, scale, x, y);
            }
            this.frm.addTemplate(this.app[2], 0.0F, 0.0F);
            if (!this.acro6Layers) {
                this.frm.addTemplate(this.app[3], scale, 0.0F, 0.0F, scale, x, y);
                this.frm.addTemplate(this.app[4], 0.0F, 0.0F);
            }
        }

        PdfTemplate napp = new PdfTemplate(this.writer);
        napp.setBoundingBox(rotated);
        this.writer.addDirectTemplateSimple(napp, null);
        napp.addTemplate(this.frm, 0.0F, 0.0F);
        return napp;
    }

    private void createBlankN0() {
        PdfTemplate t = this.app[0] = new PdfTemplate(this.writer);
        t.setBoundingBox(new Rectangle(100.0F, 100.0F));
        this.writer.addDirectTemplateSimple(t, new PdfName("n0"));
        t.setLiteral("% DSBlank\n");
    }

    public PdfStamper getStamper() {
        return this.stamper;
    }

    void setStamper(PdfStamper stamper) {
        this.stamper = stamper;
    }

    public ByteBuffer getSigout() {
        return this.sigout;
    }

    void setSigout(ByteBuffer sigout) {
        this.sigout = sigout;
    }

    OutputStream getOriginalout() {
        return this.originalout;
    }

    void setOriginalout(OutputStream originalout) {
        this.originalout = originalout;
    }

    public File getTempFile() {
        return this.tempFile;
    }

    void setTempFile(File tempFile) {
        this.tempFile = tempFile;
    }

    public void preClose(HashMap<PdfName, Integer> exclusionSizes) throws IOException, DocumentException {
        if (this.preClosed) {
            throw new DocumentException(MessageLocalization.getComposedMessage("document.already.pre.closed", new Object[0]));
        }
        this.stamper.mergeVerification();
        this.preClosed = true;
        AcroFields af = this.writer.getAcroFields();
        String name = getFieldName();
        boolean fieldExists = af.doesSignatureFieldExist(name);
        PdfIndirectReference refSig = this.writer.getPdfIndirectReference();
        this.writer.setSigFlags(3);
        PdfDictionary fieldLock = null;
        if (fieldExists) {
            PdfDictionary widget = af.getFieldItem(name).getWidget(0);
            this.writer.markUsed(widget);
            fieldLock = widget.getAsDict(PdfName.LOCK);

            if (fieldLock == null && this.fieldLock != null) {
                widget.put(PdfName.LOCK, this.writer.addToBody(this.fieldLock).getIndirectReference());
                fieldLock = this.fieldLock;
            }

            widget.put(PdfName.P, this.writer.getPageReference(getPage()));
            widget.put(PdfName.V, refSig);
            PdfObject obj = PdfReader.getPdfObjectRelease(widget.get(PdfName.F));
            int flags = 0;
            if (obj != null && obj.isNumber()) {
                flags = ((PdfNumber) obj).intValue();
            }
            flags |= 0x80;
            widget.put(PdfName.F, new PdfNumber(flags));
            PdfDictionary ap = new PdfDictionary();
            ap.put(PdfName.N, getAppearance(1).getIndirectReference());
            widget.put(PdfName.AP, ap);
        } else {

//            PdfFormField sigField = PdfFormField.createSignature(this.writer);
//            sigField.setFieldName(name);
//            sigField.put(PdfName.V, refSig);
//            sigField.setFlags(132);
//
//            if (this.fieldLock != null) {
//                sigField.put(PdfName.LOCK, this.writer.addToBody(this.fieldLock).getIndirectReference());
//                fieldLock = this.fieldLock;
//            }
            int[] pagen = getPages();
            for (int i = 0; i < pagen.length; i++) {
                PdfFormField sigField = PdfFormField.createSignature(writer);
                if (name.equals("Signature1")) {
                    sigField.setFieldName("Signature" + (i + 1));
                } else {
                    sigField = PdfFormField.createSignature(writer);
                    int test = Integer.parseInt(name.substring(9));
                    sigField.setFieldName("Signature" + (test + i));
                }

                sigField.put(PdfName.V, refSig);
                sigField.setFlags(PdfAnnotation.FLAGS_PRINT | PdfAnnotation.FLAGS_LOCKED);

                sigField.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, getAppearance(pagen[i]));
                if (!isInvisible()) {
                    if (pageRect2.length == 0) {
                        sigField.setWidget(getPageRect2(i), null);
                    } else {
                        sigField.setWidget(pageRect2[i], null);
                    }
                } else {
                    sigField.setWidget(new Rectangle(0, 0), null);
                }
                //Added on 15th June
                sigField.setPage(1);
                /* for(int page: getPage()){
                	writer.addAnnotation(sigField, page);            	
                }*/
                //end
                writer.addAnnotation(sigField, pagen[i]);
            }

//            if (!isInvisible()) {
//                sigField.setWidget(getPageRect(), (PdfName) null);
//            } else {
//                sigField.setWidget(new Rectangle(0.0F, 0.0F), (PdfName) null);
//            }
//            sigField.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, getAppearance());
//            sigField.setPage(pagen);
//            this.writer.addAnnotation(sigField, pagen);
        }

        this.exclusionLocations = new HashMap<PdfName, PdfLiteral>();
        if (this.cryptoDictionary == null) {
            throw new DocumentException("No crypto dictionary defined.");
        }

        PdfLiteral lit = new PdfLiteral(80);
        this.exclusionLocations.put(PdfName.BYTERANGE, lit);
        this.cryptoDictionary.put(PdfName.BYTERANGE, lit);
        for (Map.Entry<PdfName, Integer> entry : exclusionSizes.entrySet()) {
            PdfName key = entry.getKey();
            Integer v = entry.getValue();
            lit = new PdfLiteral(v.intValue());
            this.exclusionLocations.put(key, lit);
            this.cryptoDictionary.put(key, lit);
        }
        if (this.certificationLevel > 0) {
            addDocMDP(this.cryptoDictionary);
        }
        if (fieldLock != null) {
            addFieldMDP(this.cryptoDictionary, fieldLock);
        }
        if (this.signatureEvent != null) {
            this.signatureEvent.getSignatureDictionary(this.cryptoDictionary);
        }
        this.writer.addToBody(this.cryptoDictionary, refSig, false);

        if (this.certificationLevel > 0) {

            PdfDictionary docmdp = new PdfDictionary();
            docmdp.put(new PdfName("DocMDP"), refSig);
            this.writer.reader.getCatalog().put(new PdfName("Perms"), docmdp);
        }
        this.writer.close(this.stamper.getMoreInfo());

        this.range = new long[this.exclusionLocations.size() * 2];
        long byteRangePosition = ((PdfLiteral) this.exclusionLocations.get(PdfName.BYTERANGE)).getPosition();
        this.exclusionLocations.remove(PdfName.BYTERANGE);
        int idx = 1;
        for (PdfLiteral pdfLiteral : this.exclusionLocations.values()) {
            long n = pdfLiteral.getPosition();
            this.range[idx++] = n;
            this.range[idx++] = pdfLiteral.getPosLength() + n;
        }
        Arrays.sort(this.range, 1, this.range.length - 1);
        for (int k = 3; k < this.range.length - 2; k += 2) {
            this.range[k] = this.range[k] - this.range[k - 1];
        }
        if (this.tempFile == null) {
            this.bout = this.sigout.getBuffer();
            this.boutLen = this.sigout.size();
            this.range[this.range.length - 1] = this.boutLen - this.range[this.range.length - 2];
            ByteBuffer bf = new ByteBuffer();
            bf.append('[');
            for (int i = 0; i < this.range.length; i++) {
                bf.append(this.range[i]).append(' ');
            }
            bf.append(']');
            System.arraycopy(bf.getBuffer(), 0, this.bout, (int) byteRangePosition, bf.size());
        } else {

            try {
                this.raf = new RandomAccessFile(this.tempFile, "rw");
                long len = this.raf.length();
                this.range[this.range.length - 1] = len - this.range[this.range.length - 2];
                ByteBuffer bf = new ByteBuffer();
                bf.append('[');
                for (int i = 0; i < this.range.length; i++) {
                    bf.append(this.range[i]).append(' ');
                }
                bf.append(']');
                this.raf.seek(byteRangePosition);
                this.raf.write(bf.getBuffer(), 0, bf.size());
            } catch (IOException e) {
                try {
                    this.raf.close();
                } catch (Exception exception) {
                }
                try {
                    this.tempFile.delete();
                } catch (Exception exception) {
                }
                throw e;
            }

        }
    }

    private void addDocMDP(PdfDictionary crypto) {
        PdfDictionary reference = new PdfDictionary();
        PdfDictionary transformParams = new PdfDictionary();
        transformParams.put(PdfName.P, new PdfNumber(this.certificationLevel));
        transformParams.put(PdfName.V, new PdfName("1.2"));
        transformParams.put(PdfName.TYPE, PdfName.TRANSFORMPARAMS);
        reference.put(PdfName.TRANSFORMMETHOD, PdfName.DOCMDP);
        reference.put(PdfName.TYPE, PdfName.SIGREF);
        reference.put(PdfName.TRANSFORMPARAMS, transformParams);
        if (this.writer.getPdfVersion().getVersion() < '6') {
            reference.put(new PdfName("DigestValue"), new PdfString("aa"));
            PdfArray loc = new PdfArray();
            loc.add(new PdfNumber(0));
            loc.add(new PdfNumber(0));
            reference.put(new PdfName("DigestLocation"), loc);
            reference.put(new PdfName("DigestMethod"), new PdfName("MD5"));
        }
        reference.put(PdfName.DATA, this.writer.reader.getTrailer().get(PdfName.ROOT));
        PdfArray types = new PdfArray();
        types.add(reference);
        crypto.put(PdfName.REFERENCE, types);
    }

    private void addFieldMDP(PdfDictionary crypto, PdfDictionary fieldLock) {
        PdfDictionary reference = new PdfDictionary();
        PdfDictionary transformParams = new PdfDictionary();
        transformParams.putAll(fieldLock);
        transformParams.put(PdfName.TYPE, PdfName.TRANSFORMPARAMS);
        transformParams.put(PdfName.V, new PdfName("1.2"));
        reference.put(PdfName.TRANSFORMMETHOD, PdfName.FIELDMDP);
        reference.put(PdfName.TYPE, PdfName.SIGREF);
        reference.put(PdfName.TRANSFORMPARAMS, transformParams);
        reference.put(new PdfName("DigestValue"), new PdfString("aa"));
        PdfArray loc = new PdfArray();
        loc.add(new PdfNumber(0));
        loc.add(new PdfNumber(0));
        reference.put(new PdfName("DigestLocation"), loc);
        reference.put(new PdfName("DigestMethod"), new PdfName("MD5"));
        reference.put(PdfName.DATA, this.writer.reader.getTrailer().get(PdfName.ROOT));
        PdfArray types = crypto.getAsArray(PdfName.REFERENCE);
        if (types == null) {
            types = new PdfArray();
        }
        types.add(reference);
        crypto.put(PdfName.REFERENCE, types);
    }

    /**
     * Fits the text to some rectangle adjusting the font size as needed.
     *
     * @param font the font to use
     * @param text the text
     * @param rect the rectangle where the text must fit
     * @param maxFontSize the maximum font size
     * @param runDirection the run direction
     * @return the calculated font size that makes the text fit
     */
    public static float fitText(Font font, String text, Rectangle rect, float maxFontSize, int runDirection) {
        try {
            ColumnText ct = null;
            int status = 0;
            if (maxFontSize <= 0) {
                int cr = 0;
                int lf = 0;
                char t[] = text.toCharArray();
                for (int k = 0; k < t.length; ++k) {
                    if (t[k] == '\n') {
                        ++lf;
                    } else if (t[k] == '\r') {
                        ++cr;
                    }
                }
                int minLines = Math.max(cr, lf) + 1;
                maxFontSize = Math.abs(rect.getHeight()) / minLines - 0.001f;
            }
            font.setSize(maxFontSize);
            Phrase ph = new Phrase(text, font);
            ct = new ColumnText(null);
            ct.setSimpleColumn(ph, rect.getLeft(), rect.getBottom(), rect.getRight(), rect.getTop(), maxFontSize, Element.ALIGN_LEFT);
            ct.setRunDirection(runDirection);
            status = ct.go(true);
            if ((status & ColumnText.NO_MORE_TEXT) != 0) {
                return maxFontSize;
            }
            float precision = 0.1f;
            float min = 0;
            float max = maxFontSize;
            float size = maxFontSize;
            for (int k = 0; k < 50; ++k) { //just in case it doesn't converge
                size = (min + max) / 2;
                ct = new ColumnText(null);
                font.setSize(size);
                ct.setSimpleColumn(new Phrase(text, font), rect.getLeft(), rect.getBottom(), rect.getRight(), rect.getTop(), size, Element.ALIGN_LEFT);
                ct.setRunDirection(runDirection);
                status = ct.go(true);
                if ((status & ColumnText.NO_MORE_TEXT) != 0) {
                    if (max - min < size * precision) {
                        return size;
                    }
                    min = size;
                } else {
                    max = size;
                }
            }
            return size;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public void close(PdfDictionary update) throws IOException, DocumentException {
        try {
            if (!this.preClosed) {
                throw new DocumentException(MessageLocalization.getComposedMessage("preclose.must.be.called.first", new Object[0]));
            }
            ByteBuffer bf = new ByteBuffer();
            for (PdfName key : update.getKeys()) {
                PdfObject obj = update.get(key);
                PdfLiteral lit = this.exclusionLocations.get(key);
                if (lit == null) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.key.1.didn.t.reserve.space.in.preclose", new Object[]{key.toString()}));
                }
                bf.reset();
                obj.toPdf(null, bf);
                if (bf.size() > lit.getPosLength()) {
                    throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.key.1.is.too.big.is.2.reserved.3", new Object[]{key.toString(), String.valueOf(bf.size()), String.valueOf(lit.getPosLength())}));
                }
                if (this.tempFile == null) {
                    System.arraycopy(bf.getBuffer(), 0, this.bout, (int) lit.getPosition(), bf.size());
                    continue;
                }
                this.raf.seek(lit.getPosition());
                this.raf.write(bf.getBuffer(), 0, bf.size());
            }

            if (update.size() != this.exclusionLocations.size()) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.update.dictionary.has.less.keys.than.required", new Object[0]));
            }
            if (this.tempFile == null) {
                this.originalout.write(this.bout, 0, this.boutLen);

            } else if (this.originalout != null) {
                this.raf.seek(0L);
                long length = this.raf.length();
                byte[] buf = new byte[8192];
                while (length > 0L) {
                    int r = this.raf.read(buf, 0, (int) Math.min(buf.length, length));
                    if (r < 0) {
                        throw new EOFException(MessageLocalization.getComposedMessage("unexpected.eof", new Object[0]));
                    }
                    this.originalout.write(buf, 0, r);
                    length -= r;
                }

            }
        } finally {

            this.writer.reader.close();
            if (this.tempFile != null) {
                try {
                    this.raf.close();
                } catch (Exception exception) {
                }
                if (this.originalout != null) {
                    try {
                        this.tempFile.delete();
                    } catch (Exception exception) {
                    }
                }
            }

            if (this.originalout != null) {
                try {
                    this.originalout.close();
                } catch (Exception exception) {
                }
            }
        }
    }
    /**
     * Signature rendering modes
     *
     * @since 5.0.1
     */
    String signerName = "";
//

    public void setSignerName(String signerName) {
        this.signerName = signerName;
    }


    public Rectangle getPageRect2(int i) {
        return pageRect2[i];
    }

}
