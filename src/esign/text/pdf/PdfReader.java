package esign.text.pdf;

import esign.text.DocWriter;
import esign.text.Document;
import esign.text.ExceptionConverter;
import esign.text.PageSize;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import esign.text.exceptions.BadPasswordException;
import esign.text.exceptions.InvalidPdfException;
import esign.text.exceptions.UnsupportedPdfException;
import esign.text.io.RandomAccessSource;
import esign.text.io.RandomAccessSourceFactory;
import esign.text.io.WindowRandomAccessSource;
import esign.text.log.Counter;
import esign.text.log.CounterFactory;
import esign.text.log.Level;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import esign.text.pdf.interfaces.PdfViewerPreferences;
import esign.text.pdf.internal.PdfViewerPreferencesImp;
import esign.text.pdf.security.ExternalDecryptionProcess;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.zip.InflaterInputStream;
import org.emcastle.cert.X509CertificateHolder;
import org.emcastle.cms.CMSEnvelopedData;
import org.emcastle.cms.RecipientInformation;

public class PdfReader
        implements PdfViewerPreferences {

    public static boolean unethicalreading = false;
    public static boolean debugmode = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(PdfReader.class);

    static final PdfName[] pageInhCandidates = new PdfName[]{PdfName.MEDIABOX, PdfName.ROTATE, PdfName.RESOURCES, PdfName.CROPBOX};

    static final byte[] endstream = PdfEncodings.convertToBytes("endstream", (String) null);
    static final byte[] endobj = PdfEncodings.convertToBytes("endobj", (String) null);

    protected PRTokeniser tokens;

    protected long[] xref;

    protected HashMap<Integer, IntHashtable> objStmMark;

    protected LongHashtable objStmToOffset;
    protected boolean newXrefType;
    protected ArrayList<PdfObject> xrefObj;
    PdfDictionary rootPages;
    protected PdfDictionary trailer;
    protected PdfDictionary catalog;
    protected PageRefs pageRefs;
    protected PRAcroForm acroForm = null;
    protected boolean acroFormParsed = false;
    protected boolean encrypted = false;
    protected boolean rebuilt = false;
    protected int freeXref;
    protected boolean tampered = false;
    protected long lastXref;
    protected long eofPos;
    protected char pdfVersion;
    protected PdfEncryption decrypt;
    protected byte[] password = null;
    protected Key certificateKey = null;
    protected Certificate certificate = null;
    protected String certificateKeyProvider = null;
    protected ExternalDecryptionProcess externalDecryptionProcess = null;
    private boolean ownerPasswordUsed = true;
    protected ArrayList<PdfString> strings = new ArrayList<PdfString>();
    protected boolean sharedStreams = true;
    protected boolean consolidateNamedDestinations = false;
    protected boolean remoteToLocalNamedDestinations = false;
    protected int rValue;
    protected long pValue;
    private int objNum;
    private int objGen;
    private long fileLength;
    private boolean hybridXref;
    private int lastXrefPartial = -1;

    private boolean partial;
    private PRIndirectReference cryptoRef;
    private final PdfViewerPreferencesImp viewerPreferences = new PdfViewerPreferencesImp();

    private boolean encryptionError;

    private boolean appendable;

    protected static Counter COUNTER = CounterFactory.getCounter(PdfReader.class);
    private int readDepth;

    public void enableRebuild() {
        this.rebuilt = false;
    }

    protected Counter getCounter() {
        return COUNTER;
    }

    public PdfReader(String filename) throws IOException {
        this(filename, (byte[]) null);
    }

    public PdfReader(String filename, byte[] ownerPassword) throws IOException {
        this(filename, ownerPassword, false);
    }

    public PdfReader(String filename, byte[] ownerPassword, boolean partial) throws IOException {
        this((new RandomAccessSourceFactory())
                .setForceRead(false)
                .setUsePlainRandomAccess(Document.plainRandomAccess)
                .createBestSource(filename), partial, ownerPassword, null, null, null, null, true);
    }

    public PdfReader(byte[] pdfIn) throws IOException {
        this(pdfIn, (byte[]) null);
    }

    public PdfReader(byte[] pdfIn, byte[] ownerPassword) throws IOException {
        this((new RandomAccessSourceFactory())
                .createSource(pdfIn), false, ownerPassword, null, null, null, null, true);
    }

    public PdfReader(String filename, Certificate certificate, Key certificateKey, String certificateKeyProvider) throws IOException {
        this((new RandomAccessSourceFactory())
                .setForceRead(false)
                .setUsePlainRandomAccess(Document.plainRandomAccess)
                .createBestSource(filename), false, null, certificate, certificateKey, certificateKeyProvider, null, true);
    }

    public PdfReader(String filename, Certificate certificate, ExternalDecryptionProcess externalDecryptionProcess) throws IOException {
        this((new RandomAccessSourceFactory())
                .setForceRead(false)
                .setUsePlainRandomAccess(Document.plainRandomAccess)
                .createBestSource(filename), false, null, certificate, null, null, externalDecryptionProcess, true);
    }

    public PdfReader(byte[] pdfIn, Certificate certificate, ExternalDecryptionProcess externalDecryptionProcess) throws IOException {
        this((new RandomAccessSourceFactory())
                .setForceRead(false)
                .setUsePlainRandomAccess(Document.plainRandomAccess)
                .createSource(pdfIn), false, null, certificate, null, null, externalDecryptionProcess, true);
    }

    public PdfReader(InputStream inputStream, Certificate certificate, ExternalDecryptionProcess externalDecryptionProcess) throws IOException {
        this((new RandomAccessSourceFactory()).setForceRead(false).setUsePlainRandomAccess(Document.plainRandomAccess).createSource(inputStream), false, null, certificate, null, null, externalDecryptionProcess, true);
    }

    public PdfReader(URL url) throws IOException {
        this(url, (byte[]) null);
    }

    public PdfReader(URL url, byte[] ownerPassword) throws IOException {
        this((new RandomAccessSourceFactory())
                .createSource(url), false, ownerPassword, null, null, null, null, true);
    }

    public PdfReader(InputStream is, byte[] ownerPassword) throws IOException {
        this((new RandomAccessSourceFactory())
                .createSource(is), false, ownerPassword, null, null, null, null, false);
    }

    public PdfReader(InputStream is) throws IOException {
        this(is, (byte[]) null);
    }

    public PdfReader(RandomAccessFileOrArray raf, byte[] ownerPassword) throws IOException {
        this(raf, ownerPassword, true);
    }

    public PdfReader(RandomAccessFileOrArray raf, byte[] ownerPassword, boolean partial) throws IOException {
        this(raf
                .getByteSource(), partial, ownerPassword, null, null, null, null, false);
    }

    private static PRTokeniser getOffsetTokeniser(RandomAccessSource byteSource) throws IOException {
        PRTokeniser tok = new PRTokeniser(new RandomAccessFileOrArray(byteSource));
        int offset = tok.getHeaderOffset();
        if (offset != 0) {
            WindowRandomAccessSource windowRandomAccessSource = new WindowRandomAccessSource(byteSource, offset);
            tok = new PRTokeniser(new RandomAccessFileOrArray((RandomAccessSource) windowRandomAccessSource));
        }
        return tok;
    }

    public RandomAccessFileOrArray getSafeFile() {
        return this.tokens.getSafeFile();
    }

    protected PdfReaderInstance getPdfReaderInstance(PdfWriter writer) {
        return new PdfReaderInstance(this, writer);
    }

    public int getNumberOfPages() {
        return this.pageRefs.size();
    }

    public PdfDictionary getCatalog() {
        return this.catalog;
    }

    public PRAcroForm getAcroForm() {
        if (!this.acroFormParsed) {
            this.acroFormParsed = true;
            PdfObject form = this.catalog.get(PdfName.ACROFORM);
            if (form != null) {
                try {
                    this.acroForm = new PRAcroForm(this);
                    this.acroForm.readAcroForm((PdfDictionary) getPdfObject(form));
                } catch (Exception e) {
                    this.acroForm = null;
                }
            }
        }
        return this.acroForm;
    }

    public int getPageRotation(int index) {
        return getPageRotation(this.pageRefs.getPageNRelease(index));
    }

    int getPageRotation(PdfDictionary page) {
        PdfNumber rotate = page.getAsNumber(PdfName.ROTATE);
        if (rotate == null) {
            return 0;
        }
        int n = rotate.intValue();
        n %= 360;
        return (n < 0) ? (n + 360) : n;
    }

    public Rectangle getPageSizeWithRotation(int index) {
        return getPageSizeWithRotation(this.pageRefs.getPageNRelease(index));
    }

    public Rectangle getPageSizeWithRotation(PdfDictionary page) {
        Rectangle rect = getPageSize(page);
        int rotation = getPageRotation(page);
        while (rotation > 0) {
            rect = rect.rotate();
            rotation -= 90;
        }
        return rect;
    }

    public Rectangle getPageSize(int index) {
        return getPageSize(this.pageRefs.getPageNRelease(index));
    }

    public Rectangle getPageSize(PdfDictionary page) {
        PdfArray mediaBox = page.getAsArray(PdfName.MEDIABOX);
        return getNormalizedRectangle(mediaBox);
    }

    public Rectangle getCropBox(int index) {
        PdfDictionary page = this.pageRefs.getPageNRelease(index);
        PdfArray cropBox = (PdfArray) getPdfObjectRelease(page.get(PdfName.CROPBOX));
        if (cropBox == null) {
            return getPageSize(page);
        }
        return getNormalizedRectangle(cropBox);
    }

    public Rectangle getBoxSize(int index, String boxName) {
        PdfDictionary page = this.pageRefs.getPageNRelease(index);
        PdfArray box = null;
        if (boxName.equals("trim")) {
            box = (PdfArray) getPdfObjectRelease(page.get(PdfName.TRIMBOX));
        } else if (boxName.equals("art")) {
            box = (PdfArray) getPdfObjectRelease(page.get(PdfName.ARTBOX));
        } else if (boxName.equals("bleed")) {
            box = (PdfArray) getPdfObjectRelease(page.get(PdfName.BLEEDBOX));
        } else if (boxName.equals("crop")) {
            box = (PdfArray) getPdfObjectRelease(page.get(PdfName.CROPBOX));
        } else if (boxName.equals("media")) {
            box = (PdfArray) getPdfObjectRelease(page.get(PdfName.MEDIABOX));
        }
        if (box == null) {
            return null;
        }
        return getNormalizedRectangle(box);
    }

    public HashMap<String, String> getInfo() {
        HashMap<String, String> map = new HashMap<String, String>();
        PdfDictionary info = this.trailer.getAsDict(PdfName.INFO);
        if (info == null) {
            return map;
        }
        for (PdfName element : info.getKeys()) {
            PdfName key = element;
            PdfObject obj = getPdfObject(info.get(key));
            if (obj == null) {
                continue;
            }
            String value = obj.toString();
            switch (obj.type()) {
                case 3:
                    value = ((PdfString) obj).toUnicodeString();
                    break;

                case 4:
                    value = PdfName.decodeName(value);
                    break;
            }

            map.put(PdfName.decodeName(key.toString()), value);
        }
        return map;
    }

    public static Rectangle getNormalizedRectangle(PdfArray box) {
        float llx = ((PdfNumber) getPdfObjectRelease(box.getPdfObject(0))).floatValue();
        float lly = ((PdfNumber) getPdfObjectRelease(box.getPdfObject(1))).floatValue();
        float urx = ((PdfNumber) getPdfObjectRelease(box.getPdfObject(2))).floatValue();
        float ury = ((PdfNumber) getPdfObjectRelease(box.getPdfObject(3))).floatValue();
        return new Rectangle(Math.min(llx, urx), Math.min(lly, ury),
                Math.max(llx, urx), Math.max(lly, ury));
    }

    public boolean isTagged() {
        PdfDictionary markInfo = this.catalog.getAsDict(PdfName.MARKINFO);
        if (markInfo == null) {
            return false;
        }
        if (PdfBoolean.PDFTRUE.equals(markInfo.getAsBoolean(PdfName.MARKED))) {
            return (this.catalog.getAsDict(PdfName.STRUCTTREEROOT) != null);
        }
        return false;
    }

    protected void readPdf() throws IOException {
        this.fileLength = this.tokens.getFile().length();
        this.pdfVersion = this.tokens.checkPdfHeader();
        try {
            readXref();
        } catch (Exception e) {
            try {
                this.rebuilt = true;
                rebuildXref();
                this.lastXref = -1L;
            } catch (Exception ne) {
                throw new InvalidPdfException(MessageLocalization.getComposedMessage("rebuild.failed.1.original.message.2", new Object[]{ne.getMessage(), e.getMessage()}));
            }
        }
        try {
            readDocObj();
        } catch (Exception e) {
            if (e instanceof BadPasswordException) {
                throw new BadPasswordException(e.getMessage());
            }
            if (this.rebuilt || this.encryptionError) {
                throw new InvalidPdfException(e.getMessage());
            }
            this.rebuilt = true;
            this.encrypted = false;
            try {
                rebuildXref();
                this.lastXref = -1L;
                readDocObj();
            } catch (Exception ne) {
                throw new InvalidPdfException(MessageLocalization.getComposedMessage("rebuild.failed.1.original.message.2", new Object[]{ne.getMessage(), e.getMessage()}));
            }
        }
        this.strings.clear();
        readPages();

        removeUnusedObjects();
    }

    protected void readPdfPartial() throws IOException {
        this.fileLength = this.tokens.getFile().length();
        this.pdfVersion = this.tokens.checkPdfHeader();
        try {
            readXref();
        } catch (Exception e) {
            try {
                this.rebuilt = true;
                rebuildXref();
                this.lastXref = -1L;
            } catch (Exception ne) {
                throw new InvalidPdfException(
                        MessageLocalization.getComposedMessage("rebuild.failed.1.original.message.2", new Object[]{
                    ne.getMessage(), e.getMessage()}), ne);
            }
        }
        readDocObjPartial();
        readPages();
    }

    private boolean equalsArray(byte[] ar1, byte[] ar2, int size) {
        for (int k = 0; k < size; k++) {
            if (ar1[k] != ar2[k]) {
                return false;
            }
        }
        return true;
    }

    private void readDecryptedDocObj() throws IOException {
        if (this.encrypted) {
            return;
        }
        PdfObject encDic = this.trailer.get(PdfName.ENCRYPT);
        if (encDic == null || encDic.toString().equals("null")) {
            return;
        }
        this.encryptionError = true;
        byte[] encryptionKey = null;
        this.encrypted = true;
        PdfDictionary enc = (PdfDictionary) getPdfObject(encDic);

        PdfDictionary cfDict = enc.getAsDict(PdfName.CF);
        if (cfDict != null) {
            PdfDictionary stdCFDict = cfDict.getAsDict(PdfName.STDCF);
            if (stdCFDict != null) {
                PdfName authEvent = stdCFDict.getAsName(PdfName.AUTHEVENT);
                if (authEvent != null) {

                    if (authEvent.compareTo(PdfName.EFOPEN) == 0 && !this.ownerPasswordUsed) {
                        return;
                    }
                }
            }
        }

        PdfArray documentIDs = this.trailer.getAsArray(PdfName.ID);
        byte[] documentID = null;
        if (documentIDs != null) {
            PdfObject o = documentIDs.getPdfObject(0);
            this.strings.remove(o);
            String s = o.toString();
            documentID = DocWriter.getISOBytes(s);
            if (documentIDs.size() > 1) {
                this.strings.remove(documentIDs.getPdfObject(1));
            }
        }
        if (documentID == null) {
            documentID = new byte[0];
        }
        byte[] uValue = null;
        byte[] oValue = null;
        int cryptoMode = 0;
        int lengthValue = 0;

        PdfObject filter = getPdfObjectRelease(enc.get(PdfName.FILTER));

        if (filter.equals(PdfName.STANDARD)) {
            PdfDictionary dic;
            PdfObject em, em5;
            String s = enc.get(PdfName.U).toString();
            this.strings.remove(enc.get(PdfName.U));
            uValue = DocWriter.getISOBytes(s);
            s = enc.get(PdfName.O).toString();
            this.strings.remove(enc.get(PdfName.O));
            oValue = DocWriter.getISOBytes(s);
            if (enc.contains(PdfName.OE)) {
                this.strings.remove(enc.get(PdfName.OE));
            }
            if (enc.contains(PdfName.UE)) {
                this.strings.remove(enc.get(PdfName.UE));
            }
            if (enc.contains(PdfName.PERMS)) {
                this.strings.remove(enc.get(PdfName.PERMS));
            }
            PdfObject o = enc.get(PdfName.P);
            if (!o.isNumber()) {
                throw new InvalidPdfException(MessageLocalization.getComposedMessage("illegal.p.value", new Object[0]));
            }
            this.pValue = ((PdfNumber) o).longValue();

            o = enc.get(PdfName.R);
            if (!o.isNumber()) {
                throw new InvalidPdfException(MessageLocalization.getComposedMessage("illegal.r.value", new Object[0]));
            }
            this.rValue = ((PdfNumber) o).intValue();

            switch (this.rValue) {
                case 2:
                    cryptoMode = 0;
                    break;
                case 3:
                    o = enc.get(PdfName.LENGTH);
                    if (!o.isNumber()) {
                        throw new InvalidPdfException(MessageLocalization.getComposedMessage("illegal.length.value", new Object[0]));
                    }
                    lengthValue = ((PdfNumber) o).intValue();
                    if (lengthValue > 128 || lengthValue < 40 || lengthValue % 8 != 0) {
                        throw new InvalidPdfException(MessageLocalization.getComposedMessage("illegal.length.value", new Object[0]));
                    }
                    cryptoMode = 1;
                    break;
                case 4:
                    dic = (PdfDictionary) enc.get(PdfName.CF);
                    if (dic == null) {
                        throw new InvalidPdfException(MessageLocalization.getComposedMessage("cf.not.found.encryption", new Object[0]));
                    }
                    dic = (PdfDictionary) dic.get(PdfName.STDCF);
                    if (dic == null) {
                        throw new InvalidPdfException(MessageLocalization.getComposedMessage("stdcf.not.found.encryption", new Object[0]));
                    }
                    if (PdfName.V2.equals(dic.get(PdfName.CFM))) {
                        cryptoMode = 1;
                    } else if (PdfName.AESV2.equals(dic.get(PdfName.CFM))) {
                        cryptoMode = 2;
                    } else {
                        throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("no.compatible.encryption.found", new Object[0]));
                    }
                    em = enc.get(PdfName.ENCRYPTMETADATA);
                    if (em != null && em.toString().equals("false")) {
                        cryptoMode |= 0x8;
                    }
                    break;
                case 5:
                    cryptoMode = 3;
                    em5 = enc.get(PdfName.ENCRYPTMETADATA);
                    if (em5 != null && em5.toString().equals("false")) {
                        cryptoMode |= 0x8;
                    }
                    break;
                default:
                    throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("unknown.encryption.type.r.eq.1", this.rValue));
            }

        } else if (filter.equals(PdfName.PUBSEC)) {
            PdfDictionary dic;
            X509CertificateHolder certHolder;
            PdfObject em;
            boolean foundRecipient = false;
            byte[] envelopedData = null;
            PdfArray recipients = null;

            PdfObject o = enc.get(PdfName.V);
            if (!o.isNumber()) {
                throw new InvalidPdfException(MessageLocalization.getComposedMessage("illegal.v.value", new Object[0]));
            }
            int vValue = ((PdfNumber) o).intValue();
            switch (vValue) {
                case 1:
                    cryptoMode = 0;
                    lengthValue = 40;
                    recipients = (PdfArray) enc.get(PdfName.RECIPIENTS);
                    break;
                case 2:
                    o = enc.get(PdfName.LENGTH);
                    if (!o.isNumber()) {
                        throw new InvalidPdfException(MessageLocalization.getComposedMessage("illegal.length.value", new Object[0]));
                    }
                    lengthValue = ((PdfNumber) o).intValue();
                    if (lengthValue > 128 || lengthValue < 40 || lengthValue % 8 != 0) {
                        throw new InvalidPdfException(MessageLocalization.getComposedMessage("illegal.length.value", new Object[0]));
                    }
                    cryptoMode = 1;
                    recipients = (PdfArray) enc.get(PdfName.RECIPIENTS);
                    break;
                case 4:
                case 5:
                    dic = (PdfDictionary) enc.get(PdfName.CF);
                    if (dic == null) {
                        throw new InvalidPdfException(MessageLocalization.getComposedMessage("cf.not.found.encryption", new Object[0]));
                    }
                    dic = (PdfDictionary) dic.get(PdfName.DEFAULTCRYPTFILTER);
                    if (dic == null) {
                        throw new InvalidPdfException(MessageLocalization.getComposedMessage("defaultcryptfilter.not.found.encryption", new Object[0]));
                    }
                    if (PdfName.V2.equals(dic.get(PdfName.CFM))) {
                        cryptoMode = 1;
                        lengthValue = 128;
                    } else if (PdfName.AESV2.equals(dic.get(PdfName.CFM))) {
                        cryptoMode = 2;
                        lengthValue = 128;
                    } else if (PdfName.AESV3.equals(dic.get(PdfName.CFM))) {
                        cryptoMode = 3;
                        lengthValue = 256;
                    } else {

                        throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("no.compatible.encryption.found", new Object[0]));
                    }
                    em = dic.get(PdfName.ENCRYPTMETADATA);
                    if (em != null && em.toString().equals("false")) {
                        cryptoMode |= 0x8;
                    }
                    recipients = (PdfArray) dic.get(PdfName.RECIPIENTS);
                    break;
                default:
                    throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("unknown.encryption.type.v.eq.1", vValue));
            }

            try {
                certHolder = new X509CertificateHolder(this.certificate.getEncoded());
            } catch (Exception f) {
                throw new ExceptionConverter(f);
            }
            if (this.externalDecryptionProcess == null) {
                for (int i = 0; i < recipients.size(); i++) {
                    PdfObject recipient = recipients.getPdfObject(i);
                    this.strings.remove(recipient);

                    CMSEnvelopedData data = null;
                    try {
                        data = new CMSEnvelopedData(recipient.getBytes());

                        Iterator<RecipientInformation> recipientCertificatesIt = data.getRecipientInfos().getRecipients().iterator();

                        while (recipientCertificatesIt.hasNext()) {
                            RecipientInformation recipientInfo = recipientCertificatesIt.next();

                            if (recipientInfo.getRID().match(certHolder) && !foundRecipient) {
                                envelopedData = PdfEncryptor.getContent(recipientInfo, (PrivateKey) this.certificateKey, this.certificateKeyProvider);
                                foundRecipient = true;
                            }

                        }
                    } catch (Exception f) {
                        throw new ExceptionConverter(f);
                    }
                }
            } else {
                for (int i = 0; i < recipients.size(); i++) {
                    PdfObject recipient = recipients.getPdfObject(i);
                    this.strings.remove(recipient);

                    CMSEnvelopedData data = null;
                    try {
                        data = new CMSEnvelopedData(recipient.getBytes());

                        RecipientInformation recipientInfo = data.getRecipientInfos().get(this.externalDecryptionProcess.getCmsRecipientId());

                        if (recipientInfo != null) {

                            envelopedData = recipientInfo.getContent(this.externalDecryptionProcess.getCmsRecipient());
                            foundRecipient = true;
                        }
                    } catch (Exception f) {
                        throw new ExceptionConverter(f);
                    }
                }
            }

            if (!foundRecipient || envelopedData == null) {
                throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("bad.certificate.and.key", new Object[0]));
            }

            MessageDigest md = null;

            try {
                if ((cryptoMode & 0x7) == 3) {
                    md = MessageDigest.getInstance("SHA-256");
                } else {
                    md = MessageDigest.getInstance("SHA-1");
                }
                md.update(envelopedData, 0, 20);
                for (int i = 0; i < recipients.size(); i++) {
                    byte[] encodedRecipient = recipients.getPdfObject(i).getBytes();
                    md.update(encodedRecipient);
                }
                if ((cryptoMode & 0x8) != 0) {
                    md.update(new byte[]{-1, -1, -1, -1});
                }
                encryptionKey = md.digest();
            } catch (Exception f) {
                throw new ExceptionConverter(f);
            }
        }

        this.decrypt = new PdfEncryption();
        this.decrypt.setCryptoMode(cryptoMode, lengthValue);

        if (filter.equals(PdfName.STANDARD)) {
            if (this.rValue == 5) {
                this.ownerPasswordUsed = this.decrypt.readKey(enc, this.password);
                this.decrypt.documentID = documentID;
                this.pValue = this.decrypt.getPermissions();
            } else {

                this.decrypt.setupByOwnerPassword(documentID, this.password, uValue, oValue, this.pValue);
                if (!equalsArray(uValue, this.decrypt.userKey, (this.rValue == 3 || this.rValue == 4) ? 16 : 32)) {

                    this.decrypt.setupByUserPassword(documentID, this.password, oValue, this.pValue);
                    if (!equalsArray(uValue, this.decrypt.userKey, (this.rValue == 3 || this.rValue == 4) ? 16 : 32)) {
                        throw new BadPasswordException(MessageLocalization.getComposedMessage("bad.user.password", new Object[0]));
                    }
                } else {

                    this.ownerPasswordUsed = true;
                }
            }
        } else if (filter.equals(PdfName.PUBSEC)) {
            if ((cryptoMode & 0x7) == 3) {
                this.decrypt.setKey(encryptionKey);
            } else {
                this.decrypt.setupByEncryptionKey(encryptionKey, lengthValue);
            }
            this.ownerPasswordUsed = true;
        }

        for (int k = 0; k < this.strings.size(); k++) {
            PdfString str = this.strings.get(k);
            str.decrypt(this);
        }

        if (encDic.isIndirect()) {
            this.cryptoRef = (PRIndirectReference) encDic;
            this.xrefObj.set(this.cryptoRef.getNumber(), null);
        }
        this.encryptionError = false;
    }

    public static PdfObject getPdfObjectRelease(PdfObject obj) {
        PdfObject obj2 = getPdfObject(obj);
        releaseLastXrefPartial(obj);
        return obj2;
    }

    public static PdfObject getPdfObject(PdfObject obj) {
        if (obj == null) {
            return null;
        }
        if (!obj.isIndirect()) {
            return obj;
        }
        try {
            PRIndirectReference ref = (PRIndirectReference) obj;
            int idx = ref.getNumber();
            boolean appendable = (ref.getReader()).appendable;
            obj = ref.getReader().getPdfObject(idx);
            if (obj == null) {
                return null;
            }

            if (appendable) {
                switch (obj.type()) {
                    case 8:
                        obj = new PdfNull();
                        break;
                    case 1:
                        obj = new PdfBoolean(((PdfBoolean) obj).booleanValue());
                        break;
                    case 4:
                        obj = new PdfName(obj.getBytes());
                        break;
                }
                obj.setIndRef(ref);
            }
            return obj;

        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public static PdfObject getPdfObjectRelease(PdfObject obj, PdfObject parent) {
        PdfObject obj2 = getPdfObject(obj, parent);
        releaseLastXrefPartial(obj);
        return obj2;
    }

    public static PdfObject getPdfObject(PdfObject obj, PdfObject parent) {
        if (obj == null) {
            return null;
        }
        if (!obj.isIndirect()) {
            PRIndirectReference ref = null;
            if (parent != null && (ref = parent.getIndRef()) != null && ref.getReader().isAppendable()) {
                switch (obj.type()) {
                    case 8:
                        obj = new PdfNull();
                        break;
                    case 1:
                        obj = new PdfBoolean(((PdfBoolean) obj).booleanValue());
                        break;
                    case 4:
                        obj = new PdfName(obj.getBytes());
                        break;
                }
                obj.setIndRef(ref);
            }
            return obj;
        }
        return getPdfObject(obj);
    }

    public PdfObject getPdfObjectRelease(int idx) {
        PdfObject obj = getPdfObject(idx);
        releaseLastXrefPartial();
        return obj;
    }

    public PdfObject getPdfObject(int idx) {
        try {
            this.lastXrefPartial = -1;
            if (idx < 0 || idx >= this.xrefObj.size()) {
                return null;
            }
            PdfObject obj = this.xrefObj.get(idx);
            if (!this.partial || obj != null) {
                return obj;
            }
            if (idx * 2 >= this.xref.length) {
                return null;
            }
            obj = readSingleObject(idx);
            this.lastXrefPartial = -1;
            if (obj != null) {
                this.lastXrefPartial = idx;
            }
            return obj;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public void resetLastXrefPartial() {
        this.lastXrefPartial = -1;
    }

    public void releaseLastXrefPartial() {
        if (this.partial && this.lastXrefPartial != -1) {
            this.xrefObj.set(this.lastXrefPartial, null);
            this.lastXrefPartial = -1;
        }
    }

    public static void releaseLastXrefPartial(PdfObject obj) {
        if (obj == null) {
            return;
        }
        if (!obj.isIndirect()) {
            return;
        }
        if (!(obj instanceof PRIndirectReference)) {
            return;
        }
        PRIndirectReference ref = (PRIndirectReference) obj;
        PdfReader reader = ref.getReader();
        if (reader.partial && reader.lastXrefPartial != -1 && reader.lastXrefPartial == ref.getNumber()) {
            reader.xrefObj.set(reader.lastXrefPartial, null);
        }
        reader.lastXrefPartial = -1;
    }

    private void setXrefPartialObject(int idx, PdfObject obj) {
        if (!this.partial || idx < 0) {
            return;
        }
        this.xrefObj.set(idx, obj);
    }

    public PRIndirectReference addPdfObject(PdfObject obj) {
        this.xrefObj.add(obj);
        return new PRIndirectReference(this, this.xrefObj.size() - 1);
    }

    protected void readPages() throws IOException {
        this.catalog = this.trailer.getAsDict(PdfName.ROOT);
        if (this.catalog == null) {
            throw new InvalidPdfException(MessageLocalization.getComposedMessage("the.document.has.no.catalog.object", new Object[0]));
        }
        this.rootPages = this.catalog.getAsDict(PdfName.PAGES);
        if (this.rootPages == null || (!PdfName.PAGES.equals(this.rootPages.get(PdfName.TYPE)) && !PdfName.PAGES.equals(this.rootPages.get(new PdfName("Types"))))) {
            if (debugmode) {
                if (LOGGER.isLogging(Level.ERROR)) {
                    LOGGER.error(MessageLocalization.getComposedMessage("the.document.has.no.page.root", new Object[0]));
                }
            } else {

                throw new InvalidPdfException(MessageLocalization.getComposedMessage("the.document.has.no.page.root", new Object[0]));
            }
        }
        this.pageRefs = new PageRefs(this);
    }

    protected void readDocObjPartial() throws IOException {
        this.xrefObj = new ArrayList<PdfObject>(this.xref.length / 2);
        this.xrefObj.addAll(Collections.<PdfObject>nCopies(this.xref.length / 2, null));
        readDecryptedDocObj();
        if (this.objStmToOffset != null) {
            long[] keys = this.objStmToOffset.getKeys();
            for (int k = 0; k < keys.length; k++) {
                long n = keys[k];
                this.objStmToOffset.put(n, this.xref[(int) (n * 2L)]);
                this.xref[(int) (n * 2L)] = -1L;
            }
        }
    }

    protected PdfObject readSingleObject(int k) throws IOException {
        PdfObject obj;
        this.strings.clear();
        int k2 = k * 2;
        long pos = this.xref[k2];
        if (pos < 0L) {
            return null;
        }
        if (this.xref[k2 + 1] > 0L) {
            pos = this.objStmToOffset.get(this.xref[k2 + 1]);
        }
        if (pos == 0L) {
            return null;
        }
        this.tokens.seek(pos);
        this.tokens.nextValidToken();
        if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
            this.tokens.throwError(MessageLocalization.getComposedMessage("invalid.object.number", new Object[0]));
        }
        this.objNum = this.tokens.intValue();
        this.tokens.nextValidToken();
        if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
            this.tokens.throwError(MessageLocalization.getComposedMessage("invalid.generation.number", new Object[0]));
        }
        this.objGen = this.tokens.intValue();
        this.tokens.nextValidToken();
        if (!this.tokens.getStringValue().equals("obj")) {
            this.tokens.throwError(MessageLocalization.getComposedMessage("token.obj.expected", new Object[0]));
        }
        try {
            obj = readPRObject();
            for (int j = 0; j < this.strings.size(); j++) {
                PdfString str = this.strings.get(j);
                str.decrypt(this);
            }
            if (obj.isStream()) {
                checkPRStreamLength((PRStream) obj);
            }
        } catch (IOException e) {
            if (debugmode) {
                if (LOGGER.isLogging(Level.ERROR)) {
                    LOGGER.error(e.getMessage(), e);
                }
                obj = null;
            } else {

                throw e;
            }
        }
        if (this.xref[k2 + 1] > 0L) {
            obj = readOneObjStm((PRStream) obj, (int) this.xref[k2]);
        }
        this.xrefObj.set(k, obj);
        return obj;
    }

    protected PdfObject readOneObjStm(PRStream stream, int idx) throws IOException {
        int first = stream.getAsNumber(PdfName.FIRST).intValue();
        byte[] b = getStreamBytes(stream, this.tokens.getFile());
        PRTokeniser saveTokens = this.tokens;
        this.tokens = new PRTokeniser(new RandomAccessFileOrArray((new RandomAccessSourceFactory()).createSource(b)));
        try {
            PdfObject obj;
            int address = 0;
            boolean ok = true;
            idx++;
            for (int k = 0; k < idx; k++) {
                ok = this.tokens.nextToken();
                if (!ok) {
                    break;
                }
                if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
                    ok = false;
                    break;
                }
                ok = this.tokens.nextToken();
                if (!ok) {
                    break;
                }
                if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
                    ok = false;
                    break;
                }
                address = this.tokens.intValue() + first;
            }
            if (!ok) {
                throw new InvalidPdfException(MessageLocalization.getComposedMessage("error.reading.objstm", new Object[0]));
            }
            this.tokens.seek(address);
            this.tokens.nextToken();

            if (this.tokens.getTokenType() == PRTokeniser.TokenType.NUMBER) {
                obj = new PdfNumber(this.tokens.getStringValue());
            } else {

                this.tokens.seek(address);
                obj = readPRObject();
            }
            return obj;
        } finally {

            this.tokens = saveTokens;
        }
    }

    public double dumpPerc() {
        int total = 0;
        for (int k = 0; k < this.xrefObj.size(); k++) {
            if (this.xrefObj.get(k) != null) {
                total++;
            }
        }
        return total * 100.0D / this.xrefObj.size();
    }

    protected void readDocObj() throws IOException {
        ArrayList<PRStream> streams = new ArrayList<PRStream>();
        this.xrefObj = new ArrayList<PdfObject>(this.xref.length / 2);
        this.xrefObj.addAll(Collections.<PdfObject>nCopies(this.xref.length / 2, null));
//     xrefObj = new ArrayList<PdfObject>(xref.length / 2);
//        xrefObj.addAll(Collections.<PdfObject>nCopies(xref.length / 2, null));

        int k;
        for (k = 2; k < this.xref.length; k += 2) {
            long pos = this.xref[k];
            if (pos > 0L && this.xref[k + 1] <= 0L) {
                PdfObject obj;
                this.tokens.seek(pos);
                this.tokens.nextValidToken();
                if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
                    this.tokens.throwError(MessageLocalization.getComposedMessage("invalid.object.number", new Object[0]));
                }
                this.objNum = this.tokens.intValue();
                this.tokens.nextValidToken();
                if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
                    this.tokens.throwError(MessageLocalization.getComposedMessage("invalid.generation.number", new Object[0]));
                }
                this.objGen = this.tokens.intValue();
                this.tokens.nextValidToken();
                if (!this.tokens.getStringValue().equals("obj")) {
                    this.tokens.throwError(MessageLocalization.getComposedMessage("token.obj.expected", new Object[0]));
                }
                try {
                    obj = readPRObject();
                    if (obj.isStream()) {
                        streams.add((PRStream) obj);
                    }
                } catch (IOException e) {
                    if (debugmode) {
                        if (LOGGER.isLogging(Level.ERROR)) {
                            LOGGER.error(e.getMessage(), e);
                        }
                        obj = null;
                    } else {

                        throw e;
                    }
                }
                this.xrefObj.set(k / 2, obj);
            }
        }
        for (k = 0; k < streams.size(); k++) {
            checkPRStreamLength(streams.get(k));
        }
        readDecryptedDocObj();
        if (this.objStmMark != null) {
            for (Map.Entry<Integer, IntHashtable> entry : this.objStmMark.entrySet()) {
                int n = ((Integer) entry.getKey()).intValue();
                IntHashtable h = entry.getValue();
                readObjStm((PRStream) this.xrefObj.get(n), h);
                this.xrefObj.set(n, null);
            }
            this.objStmMark = null;
        }
        this.xref = null;
    }

    private void checkPRStreamLength(PRStream stream) throws IOException {
        long fileLength = this.tokens.length();
        long start = stream.getOffset();
        boolean calc = false;
        long streamLength = 0L;
        PdfObject obj = getPdfObjectRelease(stream.get(PdfName.LENGTH));
        if (obj != null && obj.type() == 2) {
            streamLength = ((PdfNumber) obj).intValue();
            if (streamLength + start > fileLength - 20L) {
                calc = true;
            } else {
                this.tokens.seek(start + streamLength);
                String line = this.tokens.readString(20);
                if (!line.startsWith("\nendstream")
                        && !line.startsWith("\r\nendstream")
                        && !line.startsWith("\rendstream")
                        && !line.startsWith("endstream")) {
                    calc = true;
                }
            }
        } else {
            calc = true;
        }
        if (calc) {
            long pos;
            byte[] tline = new byte[16];
            this.tokens.seek(start);

            while (true) {
                pos = this.tokens.getFilePointer();
                if (!this.tokens.readLineSegment(tline, false)) {
                    break;
                }
                if (equalsn(tline, endstream)) {
                    streamLength = pos - start;
                    break;
                }
                if (equalsn(tline, endobj)) {
                    this.tokens.seek(pos - 16L);
                    String s = this.tokens.readString(16);
                    int index = s.indexOf("endstream");
                    if (index >= 0) {
                        pos = pos - 16L + index;
                    }
                    streamLength = pos - start;
                    break;
                }
            }
            this.tokens.seek(pos - 2L);
            if (this.tokens.read() == 13) {
                streamLength--;
            }
            this.tokens.seek(pos - 1L);
            if (this.tokens.read() == 10) {
                streamLength--;
            }
            if (streamLength < 0L) {
                streamLength = 0L;
            }
        }
        stream.setLength((int) streamLength);
    }

    protected void readObjStm(PRStream stream, IntHashtable map) throws IOException {
        if (stream == null) {
            return;
        }
        int first = stream.getAsNumber(PdfName.FIRST).intValue();
        int n = stream.getAsNumber(PdfName.N).intValue();
        byte[] b = getStreamBytes(stream, this.tokens.getFile());
        PRTokeniser saveTokens = this.tokens;
        this.tokens = new PRTokeniser(new RandomAccessFileOrArray((new RandomAccessSourceFactory()).createSource(b)));
        try {
            int[] address = new int[n];
            int[] objNumber = new int[n];
            boolean ok = true;
            int k;
            for (k = 0; k < n; k++) {
                ok = this.tokens.nextToken();
                if (!ok) {
                    break;
                }
                if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
                    ok = false;
                    break;
                }
                objNumber[k] = this.tokens.intValue();
                ok = this.tokens.nextToken();
                if (!ok) {
                    break;
                }
                if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
                    ok = false;
                    break;
                }
                address[k] = this.tokens.intValue() + first;
            }
            if (!ok) {
                throw new InvalidPdfException(MessageLocalization.getComposedMessage("error.reading.objstm", new Object[0]));
            }
            for (k = 0; k < n; k++) {
                if (map.containsKey(k)) {
                    PdfObject obj;
                    this.tokens.seek(address[k]);
                    this.tokens.nextToken();

                    if (this.tokens.getTokenType() == PRTokeniser.TokenType.NUMBER) {
                        obj = new PdfNumber(this.tokens.getStringValue());
                    } else {

                        this.tokens.seek(address[k]);
                        obj = readPRObject();
                    }
                    this.xrefObj.set(objNumber[k], obj);
                }
            }
        } finally {

            this.tokens = saveTokens;
        }
    }

    public static PdfObject killIndirect(PdfObject obj) {
        if (obj == null || obj.isNull()) {
            return null;
        }
        PdfObject ret = getPdfObjectRelease(obj);
        if (obj.isIndirect()) {
            PRIndirectReference ref = (PRIndirectReference) obj;
            PdfReader reader = ref.getReader();
            int n = ref.getNumber();
            reader.xrefObj.set(n, null);
            if (reader.partial) {
                reader.xref[n * 2] = -1L;
            }
        }
        return ret;
    }

    private void ensureXrefSize(int size) {
        if (size == 0) {
            return;
        }
        if (this.xref == null) {
            this.xref = new long[size];
        } else if (this.xref.length < size) {
            long[] xref2 = new long[size];
            System.arraycopy(this.xref, 0, xref2, 0, this.xref.length);
            this.xref = xref2;
        }
    }

    protected void readXref() throws IOException {
        this.hybridXref = false;
        this.newXrefType = false;
        this.tokens.seek(this.tokens.getStartxref());
        this.tokens.nextToken();
        if (!this.tokens.getStringValue().equals("startxref")) {
            throw new InvalidPdfException(MessageLocalization.getComposedMessage("startxref.not.found", new Object[0]));
        }
        this.tokens.nextToken();
        if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
            throw new InvalidPdfException(MessageLocalization.getComposedMessage("startxref.is.not.followed.by.a.number", new Object[0]));
        }
        long startxref = this.tokens.longValue();
        this.lastXref = startxref;
        this.eofPos = this.tokens.getFilePointer();
        try {
            if (readXRefStream(startxref)) {
                this.newXrefType = true;

                return;
            }
        } catch (Exception exception) {
        }
        this.xref = null;
        this.tokens.seek(startxref);
        this.trailer = readXrefSection();
        PdfDictionary trailer2 = this.trailer;
        while (true) {
            PdfNumber prev = (PdfNumber) trailer2.get(PdfName.PREV);
            if (prev == null) {
                break;
            }
            if (prev.longValue() == startxref) {
                throw new InvalidPdfException(MessageLocalization.getComposedMessage("trailer.prev.entry.points.to.its.own.cross.reference.section", new Object[0]));
            }
            startxref = prev.longValue();
            this.tokens.seek(startxref);
            trailer2 = readXrefSection();
        }
    }

    protected PdfDictionary readXrefSection() throws IOException {
        this.tokens.nextValidToken();
        if (!this.tokens.getStringValue().equals("xref")) {
            this.tokens.throwError(MessageLocalization.getComposedMessage("xref.subsection.not.found", new Object[0]));
        }
        int start = 0;
        int end = 0;
        long pos = 0L;
        int gen = 0;
        while (true) {
            this.tokens.nextValidToken();
            if (this.tokens.getStringValue().equals("trailer")) {
                break;
            }
            if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
                this.tokens.throwError(MessageLocalization.getComposedMessage("object.number.of.the.first.object.in.this.xref.subsection.not.found", new Object[0]));
            }
            start = this.tokens.intValue();
            this.tokens.nextValidToken();
            if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
                this.tokens.throwError(MessageLocalization.getComposedMessage("number.of.entries.in.this.xref.subsection.not.found", new Object[0]));
            }
            end = this.tokens.intValue() + start;
            if (start == 1) {
                long back = this.tokens.getFilePointer();
                this.tokens.nextValidToken();
                pos = this.tokens.longValue();
                this.tokens.nextValidToken();
                gen = this.tokens.intValue();
                if (pos == 0L && gen == 65535) {
                    start--;
                    end--;
                }
                this.tokens.seek(back);
            }
            ensureXrefSize(end * 2);
            for (int k = start; k < end; k++) {
                this.tokens.nextValidToken();
                pos = this.tokens.longValue();
                this.tokens.nextValidToken();
                gen = this.tokens.intValue();
                this.tokens.nextValidToken();
                int p = k * 2;
                if (this.tokens.getStringValue().equals("n")) {
                    if (this.xref[p] == 0L && this.xref[p + 1] == 0L) {

                        this.xref[p] = pos;
                    }
                } else if (this.tokens.getStringValue().equals("f")) {
                    if (this.xref[p] == 0L && this.xref[p + 1] == 0L) {
                        this.xref[p] = -1L;
                    }
                } else {
                    this.tokens.throwError(MessageLocalization.getComposedMessage("invalid.cross.reference.entry.in.this.xref.subsection", new Object[0]));
                }
            }
        }
        PdfDictionary trailer = (PdfDictionary) readPRObject();
        PdfNumber xrefSize = (PdfNumber) trailer.get(PdfName.SIZE);
        ensureXrefSize(xrefSize.intValue() * 2);
        PdfObject xrs = trailer.get(PdfName.XREFSTM);
        if (xrs != null && xrs.isNumber()) {
            int loc = ((PdfNumber) xrs).intValue();
            try {
                readXRefStream(loc);
                this.newXrefType = true;
                this.hybridXref = true;
            } catch (IOException e) {
                this.xref = null;
                throw e;
            }
        }
        return trailer;
    }

    protected boolean readXRefStream(long ptr) throws IOException {
        PdfArray index;
        this.tokens.seek(ptr);
        int thisStream = 0;
        if (!this.tokens.nextToken()) {
            return false;
        }
        if (this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
            return false;
        }
        thisStream = this.tokens.intValue();
        if (!this.tokens.nextToken() || this.tokens.getTokenType() != PRTokeniser.TokenType.NUMBER) {
            return false;
        }
        if (!this.tokens.nextToken() || !this.tokens.getStringValue().equals("obj")) {
            return false;
        }
        PdfObject object = readPRObject();
        PRStream stm = null;
        if (object.isStream()) {
            stm = (PRStream) object;
            if (!PdfName.XREF.equals(stm.get(PdfName.TYPE))) {
                return false;
            }
        } else {
            return false;
        }
        if (this.trailer == null) {
            this.trailer = new PdfDictionary();
            this.trailer.putAll(stm);
        }
        stm.setLength(((PdfNumber) stm.get(PdfName.LENGTH)).intValue());
        int size = ((PdfNumber) stm.get(PdfName.SIZE)).intValue();

        PdfObject obj = stm.get(PdfName.INDEX);
        if (obj == null) {
            index = new PdfArray();
            index.add(new int[]{0, size});
        } else {

            index = (PdfArray) obj;
        }
        PdfArray w = (PdfArray) stm.get(PdfName.W);
        long prev = -1L;
        obj = stm.get(PdfName.PREV);
        if (obj != null) {
            prev = ((PdfNumber) obj).longValue();
        }

        ensureXrefSize(size * 2);
        if (this.objStmMark == null && !this.partial) {
            this.objStmMark = new HashMap<Integer, IntHashtable>();
        }
        if (this.objStmToOffset == null && this.partial) {
            this.objStmToOffset = new LongHashtable();
        }
        byte[] b = getStreamBytes(stm, this.tokens.getFile());
        int bptr = 0;
        int[] wc = new int[3];
        for (int k = 0; k < 3; k++) {
            wc[k] = w.getAsNumber(k).intValue();
        }
        for (int idx = 0; idx < index.size(); idx += 2) {
            int start = index.getAsNumber(idx).intValue();
            int length = index.getAsNumber(idx + 1).intValue();
            ensureXrefSize((start + length) * 2);
            while (length-- > 0) {
                int type = 1;
                if (wc[0] > 0) {
                    type = 0;
                    for (int m = 0; m < wc[0]; m++) {
                        type = (type << 8) + (b[bptr++] & 0xFF);
                    }
                }
                long field2 = 0L;
                for (int i = 0; i < wc[1]; i++) {
                    field2 = (field2 << 8L) + (b[bptr++] & 0xFF);
                }
                int field3 = 0;
                for (int j = 0; j < wc[2]; j++) {
                    field3 = (field3 << 8) + (b[bptr++] & 0xFF);
                }
                int base = start * 2;
                if (this.xref[base] == 0L && this.xref[base + 1] == 0L) {
                    Integer on;
                    IntHashtable seq;
                    switch (type) {
                        case 0:
                            this.xref[base] = -1L;
                            break;
                        case 1:
                            this.xref[base] = field2;
                            break;
                        case 2:
                            this.xref[base] = field3;
                            this.xref[base + 1] = field2;
                            if (this.partial) {
                                this.objStmToOffset.put(field2, 0L);
                                break;
                            }
                            on = Integer.valueOf((int) field2);
                            seq = this.objStmMark.get(on);
                            if (seq == null) {
                                seq = new IntHashtable();
                                seq.put(field3, 1);
                                this.objStmMark.put(on, seq);
                                break;
                            }
                            seq.put(field3, 1);
                            break;
                    }

                }
                start++;
            }
        }
        thisStream *= 2;
        if (thisStream + 1 < this.xref.length && this.xref[thisStream] == 0L && this.xref[thisStream + 1] == 0L) {
            this.xref[thisStream] = -1L;
        }
        if (prev == -1L) {
            return true;
        }
        return readXRefStream(prev);
    }

    protected void rebuildXref() throws IOException {
        this.hybridXref = false;
        this.newXrefType = false;
        this.tokens.seek(0L);
        long[][] xr = new long[1024][];
        long top = 0L;
        this.trailer = null;
        byte[] line = new byte[64];
        while (true) {
            long pos = this.tokens.getFilePointer();
            if (!this.tokens.readLineSegment(line, true)) {
                break;
            }
            if (line[0] == 116) {
                if (!PdfEncodings.convertToString(line, null).startsWith("trailer")) {
                    continue;
                }
                this.tokens.seek(pos);
                this.tokens.nextToken();
                pos = this.tokens.getFilePointer();
                try {
                    PdfDictionary dic = (PdfDictionary) readPRObject();
                    if (dic.get(PdfName.ROOT) != null) {
                        this.trailer = dic;
                        continue;
                    }
                    this.tokens.seek(pos);
                } catch (Exception e) {
                    this.tokens.seek(pos);
                }
                continue;
            }
            if (line[0] >= 48 && line[0] <= 57) {
                long[] obj = PRTokeniser.checkObjectStart(line);
                if (obj == null) {
                    continue;
                }
                long num = obj[0];
                long gen = obj[1];
                if (num >= xr.length) {
                    long newLength = num * 2L;
                    long[][] xr2 = new long[(int) newLength][];
                    System.arraycopy(xr, 0, xr2, 0, (int) top);
                    xr = xr2;
                }
                if (num >= top) {
                    top = num + 1L;
                }
                if (xr[(int) num] == null || gen >= xr[(int) num][1]) {
                    obj[0] = pos;
                    xr[(int) num] = obj;
                }
            }
        }
        if (this.trailer == null) {
            throw new InvalidPdfException(MessageLocalization.getComposedMessage("trailer.not.found", new Object[0]));
        }
        this.xref = new long[(int) (top * 2L)];
        for (int k = 0; k < top; k++) {
            long[] obj = xr[k];
            if (obj != null) {
                this.xref[k * 2] = obj[0];
            }
        }
    }

    protected PdfDictionary readDictionary() throws IOException {
        PdfDictionary dic = new PdfDictionary();
        while (true) {
            this.tokens.nextValidToken();
            if (this.tokens.getTokenType() == PRTokeniser.TokenType.END_DIC) {
                break;
            }
            if (this.tokens.getTokenType() != PRTokeniser.TokenType.NAME) {
                this.tokens.throwError(MessageLocalization.getComposedMessage("dictionary.key.1.is.not.a.name", new Object[]{this.tokens.getStringValue()}));
            }
            PdfName name = new PdfName(this.tokens.getStringValue(), false);
            PdfObject obj = readPRObject();
            int type = obj.type();
            if (-type == PRTokeniser.TokenType.END_DIC.ordinal()) {
                this.tokens.throwError(MessageLocalization.getComposedMessage("unexpected.gt.gt", new Object[0]));
            }
            if (-type == PRTokeniser.TokenType.END_ARRAY.ordinal()) {
                this.tokens.throwError(MessageLocalization.getComposedMessage("unexpected.close.bracket", new Object[0]));
            }
            dic.put(name, obj);
        }
        return dic;
    }

    protected PdfArray readArray() throws IOException {
        PdfArray array = new PdfArray();
        while (true) {
            PdfObject obj = readPRObject();
            int type = obj.type();
            if (-type == PRTokeniser.TokenType.END_ARRAY.ordinal()) {
                break;
            }
            if (-type == PRTokeniser.TokenType.END_DIC.ordinal()) {
                this.tokens.throwError(MessageLocalization.getComposedMessage("unexpected.gt.gt", new Object[0]));
            }
            array.add(obj);
        }
        return array;
    }

    private PdfReader(RandomAccessSource byteSource, boolean partialRead, byte[] ownerPassword, Certificate certificate, Key certificateKey, String certificateKeyProvider, ExternalDecryptionProcess externalDecryptionProcess, boolean closeSourceOnConstructorError) throws IOException {
        this.readDepth = 0;
        this.certificate = certificate;
        this.certificateKey = certificateKey;
        this.certificateKeyProvider = certificateKeyProvider;
        this.externalDecryptionProcess = externalDecryptionProcess;
        this.password = ownerPassword;
        this.partial = partialRead;
        try {
            this.tokens = getOffsetTokeniser(byteSource);
            if (partialRead) {
                readPdfPartial();
            } else {
                readPdf();
            }
        } catch (IOException e) {
            if (closeSourceOnConstructorError) {
                byteSource.close();
            }
            throw e;
        }
        getCounter().read(this.fileLength);
    }

    public PdfReader(PdfReader reader) {
        this.readDepth = 0;
        this.appendable = reader.appendable;
        this.consolidateNamedDestinations = reader.consolidateNamedDestinations;
        this.encrypted = reader.encrypted;
        this.rebuilt = reader.rebuilt;
        this.sharedStreams = reader.sharedStreams;
        this.tampered = reader.tampered;
        this.password = reader.password;
        this.pdfVersion = reader.pdfVersion;
        this.eofPos = reader.eofPos;
        this.freeXref = reader.freeXref;
        this.lastXref = reader.lastXref;
        this.newXrefType = reader.newXrefType;
        this.tokens = new PRTokeniser(reader.tokens.getSafeFile());
        if (reader.decrypt != null) {
            this.decrypt = new PdfEncryption(reader.decrypt);
        }
        this.pValue = reader.pValue;
        this.rValue = reader.rValue;
        this.xrefObj = new ArrayList<PdfObject>(reader.xrefObj);
        for (int k = 0; k < reader.xrefObj.size(); k++) {
            this.xrefObj.set(k, duplicatePdfObject(reader.xrefObj.get(k), this));
        }
        this.pageRefs = new PageRefs(reader.pageRefs, this);
        this.trailer = (PdfDictionary) duplicatePdfObject(reader.trailer, this);
        this.catalog = this.trailer.getAsDict(PdfName.ROOT);
        this.rootPages = this.catalog.getAsDict(PdfName.PAGES);
        this.fileLength = reader.fileLength;
        this.partial = reader.partial;
        this.hybridXref = reader.hybridXref;
        this.objStmToOffset = reader.objStmToOffset;
        this.xref = reader.xref;
        this.cryptoRef = (PRIndirectReference) duplicatePdfObject(reader.cryptoRef, this);
        this.ownerPasswordUsed = reader.ownerPasswordUsed;
    }

    protected PdfObject readPRObject() throws IOException {
        PdfDictionary dic;
        PdfArray arr;
        PdfString str;
        long pos;
        PdfName cachedName;
        int num;
        PRIndirectReference ref;
        boolean hasNext;
        this.tokens.nextValidToken();
        PRTokeniser.TokenType type = this.tokens.getTokenType();
        switch (type) {
            case START_DIC:
                this.readDepth++;
                dic = readDictionary();
                this.readDepth--;
                pos = this.tokens.getFilePointer();

                do {
                    hasNext = this.tokens.nextToken();
                } while (hasNext && this.tokens.getTokenType() == PRTokeniser.TokenType.COMMENT);

                if (hasNext && this.tokens.getStringValue().equals("stream")) {
                    while (true) {

                        int ch = this.tokens.read();
                        if (ch != 32 && ch != 9 && ch != 0 && ch != 12) {
                            if (ch != 10) {
                                ch = this.tokens.read();
                            }
                            if (ch != 10) {
                                this.tokens.backOnePosition(ch);
                            }
                            PRStream stream = new PRStream(this, this.tokens.getFilePointer());
                            stream.putAll(dic);

                            stream.setObjNum(this.objNum, this.objGen);

                            return stream;
                        }
                    }
                }
                this.tokens.seek(pos);
                return dic;

            case START_ARRAY:
                this.readDepth++;
                arr = readArray();
                this.readDepth--;
                return arr;

            case NUMBER:
                return new PdfNumber(this.tokens.getStringValue());
            case STRING:
                str = (new PdfString(this.tokens.getStringValue(), null)).setHexWriting(this.tokens.isHexString());

                str.setObjNum(this.objNum, this.objGen);
                if (this.strings != null) {
                    this.strings.add(str);
                }
                return str;
            case NAME:
                cachedName = PdfName.staticNames.get(this.tokens.getStringValue());
                if (this.readDepth > 0 && cachedName != null) {
                    return cachedName;
                }

                return new PdfName(this.tokens.getStringValue(), false);

            case REF:
                num = this.tokens.getReference();
                ref = new PRIndirectReference(this, num, this.tokens.getGeneration());
                return ref;
            case ENDOFFILE:
                throw new IOException(MessageLocalization.getComposedMessage("unexpected.end.of.file", new Object[0]));
        }
        String sv = this.tokens.getStringValue();
        if ("null".equals(sv)) {
            if (this.readDepth == 0) {
                return new PdfNull();
            }
            return PdfNull.PDFNULL;
        }
        if ("true".equals(sv)) {
            if (this.readDepth == 0) {
                return new PdfBoolean(true);
            }
            return PdfBoolean.PDFTRUE;
        }
        if ("false".equals(sv)) {
            if (this.readDepth == 0) {
                return new PdfBoolean(false);
            }
            return PdfBoolean.PDFFALSE;
        }
        return new PdfLiteral(-type.ordinal(), this.tokens.getStringValue());
    }

    public static byte[] FlateDecode(byte[] in) {
        byte[] b = FlateDecode(in, true);
        if (b == null) {
            return FlateDecode(in, false);
        }
        return b;
    }

    public static byte[] decodePredictor(byte[] in, PdfObject dicPar) {
        if (dicPar == null || !dicPar.isDictionary()) {
            return in;
        }
        PdfDictionary dic = (PdfDictionary) dicPar;
        PdfObject obj = getPdfObject(dic.get(PdfName.PREDICTOR));
        if (obj == null || !obj.isNumber()) {
            return in;
        }
        int predictor = ((PdfNumber) obj).intValue();
        if (predictor < 10 && predictor != 2) {
            return in;
        }
        int width = 1;
        obj = getPdfObject(dic.get(PdfName.COLUMNS));
        if (obj != null && obj.isNumber()) {
            width = ((PdfNumber) obj).intValue();
        }
        int colors = 1;
        obj = getPdfObject(dic.get(PdfName.COLORS));
        if (obj != null && obj.isNumber()) {
            colors = ((PdfNumber) obj).intValue();
        }
        int bpc = 8;
        obj = getPdfObject(dic.get(PdfName.BITSPERCOMPONENT));
        if (obj != null && obj.isNumber()) {
            bpc = ((PdfNumber) obj).intValue();
        }
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(in));
        ByteArrayOutputStream fout = new ByteArrayOutputStream(in.length);
        int bytesPerPixel = colors * bpc / 8;
        int bytesPerRow = (colors * width * bpc + 7) / 8;
        byte[] curr = new byte[bytesPerRow];
        byte[] prior = new byte[bytesPerRow];
        if (predictor == 2) {
            if (bpc == 8) {
                int numRows = in.length / bytesPerRow;
                for (int row = 0; row < numRows; row++) {
                    int rowStart = row * bytesPerRow;
                    for (int col = 0 + bytesPerPixel; col < bytesPerRow; col++) {
                        in[rowStart + col] = (byte) (in[rowStart + col] + in[rowStart + col - bytesPerPixel]);
                    }
                }
            }
            return in;
        }

        while (true) {
            int i, filter = 0;
            try {
                filter = dataStream.read();
                if (filter < 0) {
                    return fout.toByteArray();
                }
                dataStream.readFully(curr, 0, bytesPerRow);
            } catch (Exception e) {
                return fout.toByteArray();
            }

            switch (filter) {
                case 0:
                    break;
                case 1:
                    for (i = bytesPerPixel; i < bytesPerRow; i++) {
                        curr[i] = (byte) (curr[i] + curr[i - bytesPerPixel]);
                    }
                    break;
                case 2:
                    for (i = 0; i < bytesPerRow; i++) {
                        curr[i] = (byte) (curr[i] + prior[i]);
                    }
                    break;
                case 3:
                    for (i = 0; i < bytesPerPixel; i++) {
                        curr[i] = (byte) (curr[i] + prior[i] / 2);
                    }
                    for (i = bytesPerPixel; i < bytesPerRow; i++) {
                        curr[i] = (byte) (curr[i] + ((curr[i - bytesPerPixel] & 0xFF) + (prior[i] & 0xFF)) / 2);
                    }
                    break;
                case 4:
                    for (i = 0; i < bytesPerPixel; i++) {
                        curr[i] = (byte) (curr[i] + prior[i]);
                    }

                    for (i = bytesPerPixel; i < bytesPerRow; i++) {
                        int ret, a = curr[i - bytesPerPixel] & 0xFF;
                        int b = prior[i] & 0xFF;
                        int c = prior[i - bytesPerPixel] & 0xFF;

                        int p = a + b - c;
                        int pa = Math.abs(p - a);
                        int pb = Math.abs(p - b);
                        int pc = Math.abs(p - c);

                        if (pa <= pb && pa <= pc) {
                            ret = a;
                        } else if (pb <= pc) {
                            ret = b;
                        } else {
                            ret = c;
                        }
                        curr[i] = (byte) (curr[i] + (byte) ret);
                    }
                    break;

                default:
                    throw new RuntimeException(MessageLocalization.getComposedMessage("png.filter.unknown", new Object[0]));
            }
            try {
                fout.write(curr);
            } catch (IOException iOException) {
            }

            byte[] tmp = prior;
            prior = curr;
            curr = tmp;
        }
    }

    public static byte[] FlateDecode(byte[] in, boolean strict) {
        ByteArrayInputStream stream = new ByteArrayInputStream(in);
        InflaterInputStream zip = new InflaterInputStream(stream);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[strict ? 4092 : 1];
        try {
            int n;
            while ((n = zip.read(b)) >= 0) {
                out.write(b, 0, n);
            }
            zip.close();
            out.close();
            return out.toByteArray();
        } catch (Exception e) {
            if (strict) {
                return null;
            }
            return out.toByteArray();
        }
    }

    public static byte[] ASCIIHexDecode(byte[] in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean first = true;
        int n1 = 0;
        for (int k = 0; k < in.length; k++) {
            int ch = in[k] & 0xFF;
            if (ch == 62) {
                break;
            }
            if (!PRTokeniser.isWhitespace(ch)) {

                int n = PRTokeniser.getHex(ch);
                if (n == -1) {
                    throw new RuntimeException(MessageLocalization.getComposedMessage("illegal.character.in.asciihexdecode", new Object[0]));
                }
                if (first) {
                    n1 = n;
                } else {
                    out.write((byte) ((n1 << 4) + n));
                }
                first = !first;
            }
        }
        if (!first) {
            out.write((byte) (n1 << 4));
        }
        return out.toByteArray();
    }

    public static byte[] ASCII85Decode(byte[] in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int state = 0;
        int[] chn = new int[5];
        for (int k = 0; k < in.length; k++) {
            int ch = in[k] & 0xFF;
            if (ch == 126) {
                break;
            }
            if (!PRTokeniser.isWhitespace(ch)) {
                if (ch == 122 && state == 0) {
                    out.write(0);
                    out.write(0);
                    out.write(0);
                    out.write(0);
                } else {

                    if (ch < 33 || ch > 117) {
                        throw new RuntimeException(MessageLocalization.getComposedMessage("illegal.character.in.ascii85decode", new Object[0]));
                    }
                    chn[state] = ch - 33;
                    state++;
                    if (state == 5) {
                        state = 0;
                        int i = 0;
                        for (int j = 0; j < 5; j++) {
                            i = i * 85 + chn[j];
                        }
                        out.write((byte) (i >> 24));
                        out.write((byte) (i >> 16));
                        out.write((byte) (i >> 8));
                        out.write((byte) i);
                    }
                }
            }
        }
        int r = 0;

        if (state == 2) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85 + 614125 + 7225 + 85;
            out.write((byte) (r >> 24));
        } else if (state == 3) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85 + chn[2] * 85 * 85 + 7225 + 85;
            out.write((byte) (r >> 24));
            out.write((byte) (r >> 16));
        } else if (state == 4) {
            r = chn[0] * 85 * 85 * 85 * 85 + chn[1] * 85 * 85 * 85 + chn[2] * 85 * 85 + chn[3] * 85 + 85;
            out.write((byte) (r >> 24));
            out.write((byte) (r >> 16));
            out.write((byte) (r >> 8));
        }
        return out.toByteArray();
    }

    public static byte[] LZWDecode(byte[] in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LZWDecoder lzw = new LZWDecoder();
        lzw.decode(in, out);
        return out.toByteArray();
    }

    public boolean isRebuilt() {
        return this.rebuilt;
    }

    public PdfDictionary getPageN(int pageNum) {
        PdfDictionary dic = this.pageRefs.getPageN(pageNum);
        if (dic == null) {
            return null;
        }
        if (this.appendable) {
            dic.setIndRef(this.pageRefs.getPageOrigRef(pageNum));
        }
        return dic;
    }

    public PdfDictionary getPageNRelease(int pageNum) {
        PdfDictionary dic = getPageN(pageNum);
        this.pageRefs.releasePage(pageNum);
        return dic;
    }

    public void releasePage(int pageNum) {
        this.pageRefs.releasePage(pageNum);
    }

    public void resetReleasePage() {
        this.pageRefs.resetReleasePage();
    }

    public PRIndirectReference getPageOrigRef(int pageNum) {
        return this.pageRefs.getPageOrigRef(pageNum);
    }

    public byte[] getPageContent(int pageNum, RandomAccessFileOrArray file) throws IOException {
        PdfDictionary page = getPageNRelease(pageNum);
        if (page == null) {
            return null;
        }
        PdfObject contents = getPdfObjectRelease(page.get(PdfName.CONTENTS));
        if (contents == null) {
            return new byte[0];
        }
        ByteArrayOutputStream bout = null;
        if (contents.isStream()) {
            return getStreamBytes((PRStream) contents, file);
        }
        if (contents.isArray()) {
            PdfArray array = (PdfArray) contents;
            bout = new ByteArrayOutputStream();
            for (int k = 0; k < array.size(); k++) {
                PdfObject item = getPdfObjectRelease(array.getPdfObject(k));
                if (item != null && item.isStream()) {

                    byte[] b = getStreamBytes((PRStream) item, file);
                    bout.write(b);
                    if (k != array.size() - 1) {
                        bout.write(10);
                    }
                }
            }
            return bout.toByteArray();
        }

        return new byte[0];
    }

    public static byte[] getPageContent(PdfDictionary page) throws IOException {
        if (page == null) {
            return null;
        }
        RandomAccessFileOrArray rf = null;
        try {
            PdfObject contents = getPdfObjectRelease(page.get(PdfName.CONTENTS));
            if (contents == null) {
                return new byte[0];
            }
            if (contents.isStream()) {
                if (rf == null) {
                    rf = ((PRStream) contents).getReader().getSafeFile();
                    rf.reOpen();
                }
                return getStreamBytes((PRStream) contents, rf);
            }
            if (contents.isArray()) {
                PdfArray array = (PdfArray) contents;
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                for (int k = 0; k < array.size(); k++) {
                    PdfObject item = getPdfObjectRelease(array.getPdfObject(k));
                    if (item != null && item.isStream()) {

                        if (rf == null) {
                            rf = ((PRStream) item).getReader().getSafeFile();
                            rf.reOpen();
                        }
                        byte[] b = getStreamBytes((PRStream) item, rf);
                        bout.write(b);
                        if (k != array.size() - 1) {
                            bout.write(10);
                        }
                    }
                }
                return bout.toByteArray();
            }

            return new byte[0];
        } finally {

            try {
                if (rf != null) {
                    rf.close();
                }
            } catch (Exception exception) {
            }
        }
    }

    public PdfDictionary getPageResources(int pageNum) {
        return getPageResources(getPageN(pageNum));
    }

    public PdfDictionary getPageResources(PdfDictionary pageDict) {
        return pageDict.getAsDict(PdfName.RESOURCES);
    }

    public byte[] getPageContent(int pageNum) throws IOException {
        RandomAccessFileOrArray rf = getSafeFile();
        try {
            rf.reOpen();
            return getPageContent(pageNum, rf);
        } finally {

            try {
                rf.close();
            } catch (Exception exception) {
            }
        }
    }

    protected void killXref(PdfObject obj) {
        int xr;
        PdfArray t;
        PdfDictionary dic;
        int i;
        if (obj == null) {
            return;
        }
        if (obj instanceof PdfIndirectReference && !obj.isIndirect()) {
            return;
        }
        switch (obj.type()) {
            case 10:
                xr = ((PRIndirectReference) obj).getNumber();
                obj = this.xrefObj.get(xr);
                this.xrefObj.set(xr, null);
                this.freeXref = xr;
                killXref(obj);
                break;

            case 5:
                t = (PdfArray) obj;
                for (i = 0; i < t.size(); i++) {
                    killXref(t.getPdfObject(i));
                }
                break;
            case 6:
            case 7:
                dic = (PdfDictionary) obj;
                for (PdfName element : dic.getKeys()) {
                    killXref(dic.get(element));
                }
                break;
        }
    }

    public void setPageContent(int pageNum, byte[] content) {
        setPageContent(pageNum, content, -1);
    }

    public void setPageContent(int pageNum, byte[] content, int compressionLevel) {
        setPageContent(pageNum, content, compressionLevel, false);
    }

    public void setPageContent(int pageNum, byte[] content, int compressionLevel, boolean killOldXRefRecursively) {
        PdfDictionary page = getPageN(pageNum);
        if (page == null) {
            return;
        }
        PdfObject contents = page.get(PdfName.CONTENTS);
        this.freeXref = -1;
        if (killOldXRefRecursively) {
            killXref(contents);
        }
        if (this.freeXref == -1) {
            this.xrefObj.add(null);
            this.freeXref = this.xrefObj.size() - 1;
        }
        page.put(PdfName.CONTENTS, new PRIndirectReference(this, this.freeXref));
        this.xrefObj.set(this.freeXref, new PRStream(this, content, compressionLevel));
    }

    public static byte[] decodeBytes(byte[] b, PdfDictionary streamDictionary) throws IOException {
        return decodeBytes(b, streamDictionary, FilterHandlers.getDefaultFilterHandlers());
    }

    public static byte[] decodeBytes(byte[] b, PdfDictionary streamDictionary, Map<PdfName, FilterHandlers.FilterHandler> filterHandlers) throws IOException {
        PdfObject filter = getPdfObjectRelease(streamDictionary.get(PdfName.FILTER));

        ArrayList<PdfObject> filters = new ArrayList<PdfObject>();
        if (filter != null) {
            if (filter.isName()) {
                filters.add(filter);
            } else if (filter.isArray()) {
                filters = ((PdfArray) filter).getArrayList();
            }
        }
        ArrayList<PdfObject> dp = new ArrayList<PdfObject>();
        PdfObject dpo = getPdfObjectRelease(streamDictionary.get(PdfName.DECODEPARMS));
        if (dpo == null || (!dpo.isDictionary() && !dpo.isArray())) {
            dpo = getPdfObjectRelease(streamDictionary.get(PdfName.DP));
        }
        if (dpo != null) {
            if (dpo.isDictionary()) {
                dp.add(dpo);
            } else if (dpo.isArray()) {
                dp = ((PdfArray) dpo).getArrayList();
            }
        }
        for (int j = 0; j < filters.size(); j++) {
            PdfDictionary decodeParams;
            PdfName filterName = (PdfName) filters.get(j);
            FilterHandlers.FilterHandler filterHandler = filterHandlers.get(filterName);
            if (filterHandler == null) {
                throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("the.filter.1.is.not.supported", new Object[]{filterName}));
            }

            if (j < dp.size()) {
                PdfObject dpEntry = getPdfObject(dp.get(j));
                if (dpEntry instanceof PdfDictionary) {
                    decodeParams = (PdfDictionary) dpEntry;
                } else if (dpEntry == null || dpEntry instanceof PdfNull || (dpEntry instanceof PdfLiteral
                        && Arrays.equals("null".getBytes(), ((PdfLiteral) dpEntry).getBytes()))) {
                    decodeParams = null;
                } else {
                    throw new UnsupportedPdfException(MessageLocalization.getComposedMessage("the.decode.parameter.type.1.is.not.supported", new Object[]{dpEntry.getClass().toString()}));
                }
            } else {

                decodeParams = null;
            }
            b = filterHandler.decode(b, filterName, decodeParams, streamDictionary);
        }
        return b;
    }

    public static byte[] getStreamBytes(PRStream stream, RandomAccessFileOrArray file) throws IOException {
        byte[] b = getStreamBytesRaw(stream, file);
        return decodeBytes(b, stream);
    }

    public static byte[] getStreamBytes(PRStream stream) throws IOException {
        RandomAccessFileOrArray rf = stream.getReader().getSafeFile();
        try {
            rf.reOpen();
            return getStreamBytes(stream, rf);
        } finally {

            try {
                rf.close();
            } catch (Exception exception) {
            }
        }
    }

    public static byte[] getStreamBytesRaw(PRStream stream, RandomAccessFileOrArray file) throws IOException {
        byte[] b;
        PdfReader reader = stream.getReader();

        if (stream.getOffset() < 0L) {
            b = stream.getBytes();
        } else {
            b = new byte[stream.getLength()];
            file.seek(stream.getOffset());
            file.readFully(b);
            PdfEncryption decrypt = reader.getDecrypt();
            if (decrypt != null) {
                PdfObject filter = getPdfObjectRelease(stream.get(PdfName.FILTER));
                ArrayList<PdfObject> filters = new ArrayList<PdfObject>();
                if (filter != null) {
                    if (filter.isName()) {
                        filters.add(filter);
                    } else if (filter.isArray()) {
                        filters = ((PdfArray) filter).getArrayList();
                    }
                }
                boolean skip = false;
                for (int k = 0; k < filters.size(); k++) {
                    PdfObject obj = getPdfObjectRelease(filters.get(k));
                    if (obj != null && obj.toString().equals("/Crypt")) {
                        skip = true;
                        break;
                    }
                }
                if (!skip) {
                    decrypt.setHashKey(stream.getObjNum(), stream.getObjGen());
                    b = decrypt.decryptByteArray(b);
                }
            }
        }
        return b;
    }

    public static byte[] getStreamBytesRaw(PRStream stream) throws IOException {
        RandomAccessFileOrArray rf = stream.getReader().getSafeFile();
        try {
            rf.reOpen();
            return getStreamBytesRaw(stream, rf);
        } finally {

            try {
                rf.close();
            } catch (Exception exception) {
            }
        }
    }

    public void eliminateSharedStreams() {
        if (!this.sharedStreams) {
            return;
        }
        this.sharedStreams = false;
        if (this.pageRefs.size() == 1) {
            return;
        }
        ArrayList<PRIndirectReference> newRefs = new ArrayList<PRIndirectReference>();
        ArrayList<PRStream> newStreams = new ArrayList<PRStream>();
        IntHashtable visited = new IntHashtable();
        int k;
        for (k = 1; k <= this.pageRefs.size(); k++) {
            PdfDictionary page = this.pageRefs.getPageN(k);
            if (page != null) {

                PdfObject contents = getPdfObject(page.get(PdfName.CONTENTS));
                if (contents != null) {
                    if (contents.isStream()) {
                        PRIndirectReference ref = (PRIndirectReference) page.get(PdfName.CONTENTS);
                        if (visited.containsKey(ref.getNumber())) {

                            newRefs.add(ref);
                            newStreams.add(new PRStream((PRStream) contents, null));
                        } else {

                            visited.put(ref.getNumber(), 1);
                        }
                    } else if (contents.isArray()) {
                        PdfArray array = (PdfArray) contents;
                        for (int j = 0; j < array.size(); j++) {
                            PRIndirectReference ref = (PRIndirectReference) array.getPdfObject(j);
                            if (visited.containsKey(ref.getNumber())) {
                                newRefs.add(ref);
                                newStreams.add(new PRStream((PRStream) getPdfObject(ref), null));
                            } else {
                                visited.put(ref.getNumber(), 1);
                            }
                        }
                    }
                }
            }
        }
        if (newStreams.isEmpty()) {
            return;
        }
        for (k = 0; k < newStreams.size(); k++) {
            this.xrefObj.add(newStreams.get(k));
            PRIndirectReference ref = newRefs.get(k);
            ref.setNumber(this.xrefObj.size() - 1, 0);
        }
    }

    public boolean isTampered() {
        return this.tampered;
    }

    public void setTampered(boolean tampered) {
        this.tampered = tampered;
        this.pageRefs.keepPages();
    }

    public byte[] getMetadata() throws IOException {
        PdfObject obj = getPdfObject(this.catalog.get(PdfName.METADATA));
        if (!(obj instanceof PRStream)) {
            return null;
        }
        RandomAccessFileOrArray rf = getSafeFile();
        byte[] b = null;
        try {
            rf.reOpen();
            b = getStreamBytes((PRStream) obj, rf);
        } finally {

            try {
                rf.close();
            } catch (Exception exception) {
            }
        }

        return b;
    }

    public long getLastXref() {
        return this.lastXref;
    }

    public int getXrefSize() {
        return this.xrefObj.size();
    }

    public long getEofPos() {
        return this.eofPos;
    }

    public char getPdfVersion() {
        return this.pdfVersion;
    }

    public boolean isEncrypted() {
        return this.encrypted;
    }

    public long getPermissions() {
        return this.pValue;
    }

    public boolean is128Key() {
        return (this.rValue == 3);
    }

    public PdfDictionary getTrailer() {
        return this.trailer;
    }

    PdfEncryption getDecrypt() {
        return this.decrypt;
    }

    static boolean equalsn(byte[] a1, byte[] a2) {
        int length = a2.length;
        for (int k = 0; k < length; k++) {
            if (a1[k] != a2[k]) {
                return false;
            }
        }
        return true;
    }

    static boolean existsName(PdfDictionary dic, PdfName key, PdfName value) {
        PdfObject type = getPdfObjectRelease(dic.get(key));
        if (type == null || !type.isName()) {
            return false;
        }
        PdfName name = (PdfName) type;
        return name.equals(value);
    }

    static String getFontName(PdfDictionary dic) {
        if (dic == null) {
            return null;
        }
        PdfObject type = getPdfObjectRelease(dic.get(PdfName.BASEFONT));
        if (type == null || !type.isName()) {
            return null;
        }
        return PdfName.decodeName(type.toString());
    }

    static String getSubsetPrefix(PdfDictionary dic) {
        if (dic == null) {
            return null;
        }
        String s = getFontName(dic);
        if (s == null) {
            return null;
        }
        if (s.length() < 8 || s.charAt(6) != '+') {
            return null;
        }
        for (int k = 0; k < 6; k++) {
            char c = s.charAt(k);
            if (c < 'A' || c > 'Z') {
                return null;
            }
        }
        return s;
    }

    public int shuffleSubsetNames() {
        int total = 0;
        for (int k = 1; k < this.xrefObj.size(); k++) {
            PdfObject obj = getPdfObjectRelease(k);
            if (obj != null && obj.isDictionary()) {

                PdfDictionary dic = (PdfDictionary) obj;
                if (existsName(dic, PdfName.TYPE, PdfName.FONT)) {
                    if (existsName(dic, PdfName.SUBTYPE, PdfName.TYPE1)
                            || existsName(dic, PdfName.SUBTYPE, PdfName.MMTYPE1)
                            || existsName(dic, PdfName.SUBTYPE, PdfName.TRUETYPE)) {
                        String s = getSubsetPrefix(dic);
                        if (s != null) {

                            String ns = BaseFont.createSubsetPrefix() + s.substring(7);
                            PdfName newName = new PdfName(ns);
                            dic.put(PdfName.BASEFONT, newName);
                            setXrefPartialObject(k, dic);
                            total++;
                            PdfDictionary fd = dic.getAsDict(PdfName.FONTDESCRIPTOR);
                            if (fd != null) {
                                fd.put(PdfName.FONTNAME, newName);
                            }
                        }
                    } else if (existsName(dic, PdfName.SUBTYPE, PdfName.TYPE0)) {
                        String s = getSubsetPrefix(dic);
                        PdfArray arr = dic.getAsArray(PdfName.DESCENDANTFONTS);
                        if (arr != null) {
                            if (!arr.isEmpty()) {
                                PdfDictionary desc = arr.getAsDict(0);
                                String sde = getSubsetPrefix(desc);
                                if (sde != null) {
                                    String ns = BaseFont.createSubsetPrefix();
                                    if (s != null) {
                                        dic.put(PdfName.BASEFONT, new PdfName(ns + s.substring(7)));
                                    }
                                    setXrefPartialObject(k, dic);
                                    PdfName newName = new PdfName(ns + sde.substring(7));
                                    desc.put(PdfName.BASEFONT, newName);
                                    total++;
                                    PdfDictionary fd = desc.getAsDict(PdfName.FONTDESCRIPTOR);
                                    if (fd != null) {
                                        fd.put(PdfName.FONTNAME, newName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return total;
    }

    public int createFakeFontSubsets() {
        int total = 0;
        for (int k = 1; k < this.xrefObj.size(); k++) {
            PdfObject obj = getPdfObjectRelease(k);
            if (obj != null && obj.isDictionary()) {

                PdfDictionary dic = (PdfDictionary) obj;
                if (existsName(dic, PdfName.TYPE, PdfName.FONT)) {
                    if (existsName(dic, PdfName.SUBTYPE, PdfName.TYPE1)
                            || existsName(dic, PdfName.SUBTYPE, PdfName.MMTYPE1)
                            || existsName(dic, PdfName.SUBTYPE, PdfName.TRUETYPE)) {
                        String s = getSubsetPrefix(dic);
                        if (s == null) {
                            s = getFontName(dic);
                            if (s != null) {
                                String ns = BaseFont.createSubsetPrefix() + s;
                                PdfDictionary fd = (PdfDictionary) getPdfObjectRelease(dic.get(PdfName.FONTDESCRIPTOR));
                                if (fd != null) {
                                    if (fd.get(PdfName.FONTFILE) != null || fd.get(PdfName.FONTFILE2) != null || fd
                                            .get(PdfName.FONTFILE3) != null) {
                                        fd = dic.getAsDict(PdfName.FONTDESCRIPTOR);
                                        PdfName newName = new PdfName(ns);
                                        dic.put(PdfName.BASEFONT, newName);
                                        fd.put(PdfName.FONTNAME, newName);
                                        setXrefPartialObject(k, dic);
                                        total++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return total;
    }

    private static PdfArray getNameArray(PdfObject obj) {
        if (obj == null) {
            return null;
        }
        obj = getPdfObjectRelease(obj);
        if (obj == null) {
            return null;
        }
        if (obj.isArray()) {
            return (PdfArray) obj;
        }
        if (obj.isDictionary()) {
            PdfObject arr2 = getPdfObjectRelease(((PdfDictionary) obj).get(PdfName.D));
            if (arr2 != null && arr2.isArray()) {
                return (PdfArray) arr2;
            }
        }
        return null;
    }

    public HashMap<Object, PdfObject> getNamedDestination() {
        return getNamedDestination(false);
    }

    public HashMap<Object, PdfObject> getNamedDestination(boolean keepNames) {
        HashMap<Object, PdfObject> names = getNamedDestinationFromNames(keepNames);
        names.putAll(getNamedDestinationFromStrings());
        return names;
    }

    public HashMap<String, PdfObject> getNamedDestinationFromNames() {
        return (HashMap) new HashMap<Object, PdfObject>(getNamedDestinationFromNames(false));
    }

    public HashMap<Object, PdfObject> getNamedDestinationFromNames(boolean keepNames) {
        HashMap<Object, PdfObject> names = new HashMap<Object, PdfObject>();
        if (this.catalog.get(PdfName.DESTS) != null) {
            PdfDictionary dic = (PdfDictionary) getPdfObjectRelease(this.catalog.get(PdfName.DESTS));
            if (dic == null) {
                return names;
            }
            Set<PdfName> keys = dic.getKeys();
            for (PdfName key : keys) {
                PdfArray arr = getNameArray(dic.get(key));
                if (arr == null) {
                    continue;
                }
                if (keepNames) {
                    names.put(key, arr);
                    continue;
                }
                String name = PdfName.decodeName(key.toString());
                names.put(name, arr);
            }
        }

        return names;
    }

    public HashMap<String, PdfObject> getNamedDestinationFromStrings() {
        if (this.catalog.get(PdfName.NAMES) != null) {
            PdfDictionary dic = (PdfDictionary) getPdfObjectRelease(this.catalog.get(PdfName.NAMES));
            if (dic != null) {
                dic = (PdfDictionary) getPdfObjectRelease(dic.get(PdfName.DESTS));
                if (dic != null) {
                    HashMap<String, PdfObject> names = PdfNameTree.readTree(dic);
                    for (Iterator<Map.Entry<String, PdfObject>> it = names.entrySet().iterator(); it.hasNext();) {
                        Map.Entry<String, PdfObject> entry = it.next();
                        PdfArray arr = getNameArray(entry.getValue());
                        if (arr != null) {
                            entry.setValue(arr);
                            continue;
                        }
                        it.remove();
                    }
                    return names;
                }
            }
        }
        return new HashMap<String, PdfObject>();
    }

    public void removeFields() {
        this.pageRefs.resetReleasePage();
        for (int k = 1; k <= this.pageRefs.size(); k++) {
            PdfDictionary page = this.pageRefs.getPageN(k);
            PdfArray annots = page.getAsArray(PdfName.ANNOTS);
            if (annots == null) {
                this.pageRefs.releasePage(k);
            } else {

                for (int j = 0; j < annots.size(); j++) {
                    PdfObject obj = getPdfObjectRelease(annots.getPdfObject(j));
                    if (obj != null && obj.isDictionary()) {

                        PdfDictionary annot = (PdfDictionary) obj;
                        if (PdfName.WIDGET.equals(annot.get(PdfName.SUBTYPE))) {
                            annots.remove(j--);
                        }
                    }
                }
                if (annots.isEmpty()) {
                    page.remove(PdfName.ANNOTS);
                } else {
                    this.pageRefs.releasePage(k);
                }
            }
        }
        this.catalog.remove(PdfName.ACROFORM);
        this.pageRefs.resetReleasePage();
    }

    public void removeAnnotations() {
        this.pageRefs.resetReleasePage();
        for (int k = 1; k <= this.pageRefs.size(); k++) {
            PdfDictionary page = this.pageRefs.getPageN(k);
            if (page.get(PdfName.ANNOTS) == null) {
                this.pageRefs.releasePage(k);
            } else {
                page.remove(PdfName.ANNOTS);
            }
        }
        this.catalog.remove(PdfName.ACROFORM);
        this.pageRefs.resetReleasePage();
    }

    public ArrayList<PdfAnnotation.PdfImportedLink> getLinks(int page) {
        this.pageRefs.resetReleasePage();
        ArrayList<PdfAnnotation.PdfImportedLink> result = new ArrayList<PdfAnnotation.PdfImportedLink>();
        PdfDictionary pageDic = this.pageRefs.getPageN(page);
        if (pageDic.get(PdfName.ANNOTS) != null) {
            PdfArray annots = pageDic.getAsArray(PdfName.ANNOTS);
            for (int j = 0; j < annots.size(); j++) {
                PdfDictionary annot = (PdfDictionary) getPdfObjectRelease(annots.getPdfObject(j));

                if (PdfName.LINK.equals(annot.get(PdfName.SUBTYPE))) {
                    result.add(new PdfAnnotation.PdfImportedLink(annot));
                }
            }
        }
        this.pageRefs.releasePage(page);
        this.pageRefs.resetReleasePage();
        return result;
    }

    private void iterateBookmarks(PdfObject outlineRef, HashMap<Object, PdfObject> names) {
        while (outlineRef != null) {
            replaceNamedDestination(outlineRef, names);
            PdfDictionary outline = (PdfDictionary) getPdfObjectRelease(outlineRef);
            PdfObject first = outline.get(PdfName.FIRST);
            if (first != null) {
                iterateBookmarks(first, names);
            }
            outlineRef = outline.get(PdfName.NEXT);
        }
    }

    public void makeRemoteNamedDestinationsLocal() {
        if (this.remoteToLocalNamedDestinations) {
            return;
        }
        this.remoteToLocalNamedDestinations = true;
        HashMap<Object, PdfObject> names = getNamedDestination(true);
        if (names.isEmpty()) {
            return;
        }
        for (int k = 1; k <= this.pageRefs.size(); k++) {
            PdfDictionary page = this.pageRefs.getPageN(k);
            PdfObject annotsRef;
            PdfArray annots = (PdfArray) getPdfObject(annotsRef = page.get(PdfName.ANNOTS));
            int annotIdx = this.lastXrefPartial;
            releaseLastXrefPartial();
            if (annots == null) {
                this.pageRefs.releasePage(k);
            } else {

                boolean commitAnnots = false;
                for (int an = 0; an < annots.size(); an++) {
                    PdfObject objRef = annots.getPdfObject(an);
                    if (convertNamedDestination(objRef, names) && !objRef.isIndirect()) {
                        commitAnnots = true;
                    }
                }
                if (commitAnnots) {
                    setXrefPartialObject(annotIdx, annots);
                }
                if (!commitAnnots || annotsRef.isIndirect()) {
                    this.pageRefs.releasePage(k);
                }
            }
        }
    }

    private boolean convertNamedDestination(PdfObject obj, HashMap<Object, PdfObject> names) {
        obj = getPdfObject(obj);
        int objIdx = this.lastXrefPartial;
        releaseLastXrefPartial();
        if (obj != null && obj.isDictionary()) {
            PdfObject ob2 = getPdfObject(((PdfDictionary) obj).get(PdfName.A));
            if (ob2 != null) {
                int obj2Idx = this.lastXrefPartial;
                releaseLastXrefPartial();
                PdfDictionary dic = (PdfDictionary) ob2;
                PdfName type = (PdfName) getPdfObjectRelease(dic.get(PdfName.S));
                if (PdfName.GOTOR.equals(type)) {
                    PdfObject ob3 = getPdfObjectRelease(dic.get(PdfName.D));
                    Object name = null;
                    if (ob3 != null) {
                        if (ob3.isName()) {
                            name = ob3;
                        } else if (ob3.isString()) {
                            name = ob3.toString();
                        }
                        PdfArray dest = (PdfArray) names.get(name);
                        if (dest != null) {
                            dic.remove(PdfName.F);
                            dic.remove(PdfName.NEWWINDOW);
                            dic.put(PdfName.S, PdfName.GOTO);
                            setXrefPartialObject(obj2Idx, ob2);
                            setXrefPartialObject(objIdx, obj);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void consolidateNamedDestinations() {
        if (this.consolidateNamedDestinations) {
            return;
        }
        this.consolidateNamedDestinations = true;
        HashMap<Object, PdfObject> names = getNamedDestination(true);
        if (names.isEmpty()) {
            return;
        }
        for (int k = 1; k <= this.pageRefs.size(); k++) {
            PdfDictionary page = this.pageRefs.getPageN(k);
            PdfObject annotsRef;
            PdfArray annots = (PdfArray) getPdfObject(annotsRef = page.get(PdfName.ANNOTS));
            int annotIdx = this.lastXrefPartial;
            releaseLastXrefPartial();
            if (annots == null) {
                this.pageRefs.releasePage(k);
            } else {

                boolean commitAnnots = false;
                for (int an = 0; an < annots.size(); an++) {
                    PdfObject objRef = annots.getPdfObject(an);
                    if (replaceNamedDestination(objRef, names) && !objRef.isIndirect()) {
                        commitAnnots = true;
                    }
                }
                if (commitAnnots) {
                    setXrefPartialObject(annotIdx, annots);
                }
                if (!commitAnnots || annotsRef.isIndirect()) {
                    this.pageRefs.releasePage(k);
                }
            }
        }
        PdfDictionary outlines = (PdfDictionary) getPdfObjectRelease(this.catalog.get(PdfName.OUTLINES));
        if (outlines == null) {
            return;
        }
        iterateBookmarks(outlines.get(PdfName.FIRST), names);
    }

    private boolean replaceNamedDestination(PdfObject obj, HashMap<Object, PdfObject> names) {
        obj = getPdfObject(obj);
        int objIdx = this.lastXrefPartial;
        releaseLastXrefPartial();
        if (obj != null && obj.isDictionary()) {
            PdfObject ob2 = getPdfObjectRelease(((PdfDictionary) obj).get(PdfName.DEST));
            Object name = null;
            if (ob2 != null) {
                if (ob2.isName()) {
                    name = ob2;
                } else if (ob2.isString()) {
                    name = ob2.toString();
                }
                PdfArray dest = (PdfArray) names.get(name);
                if (dest != null) {
                    ((PdfDictionary) obj).put(PdfName.DEST, dest);
                    setXrefPartialObject(objIdx, obj);
                    return true;
                }

            } else if ((ob2 = getPdfObject(((PdfDictionary) obj).get(PdfName.A))) != null) {
                int obj2Idx = this.lastXrefPartial;
                releaseLastXrefPartial();
                PdfDictionary dic = (PdfDictionary) ob2;
                PdfName type = (PdfName) getPdfObjectRelease(dic.get(PdfName.S));
                if (PdfName.GOTO.equals(type)) {
                    PdfObject ob3 = getPdfObjectRelease(dic.get(PdfName.D));
                    if (ob3 != null) {
                        if (ob3.isName()) {
                            name = ob3;
                        } else if (ob3.isString()) {
                            name = ob3.toString();
                        }
                    }
                    PdfArray dest = (PdfArray) names.get(name);
                    if (dest != null) {
                        dic.put(PdfName.D, dest);
                        setXrefPartialObject(obj2Idx, ob2);
                        setXrefPartialObject(objIdx, obj);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected static PdfDictionary duplicatePdfDictionary(PdfDictionary original, PdfDictionary copy, PdfReader newReader) {
        if (copy == null) {
            copy = new PdfDictionary(original.size());
        }
        for (PdfName element : original.getKeys()) {
            PdfName key = element;
            copy.put(key, duplicatePdfObject(original.get(key), newReader));
        }
        return copy;
    }

    protected static PdfObject duplicatePdfObject(PdfObject original, PdfReader newReader) {
        PRStream pRStream1;
        PdfArray originalArray;
        PRIndirectReference org;
        PRStream stream;
        PdfArray arr;
        Iterator<PdfObject> it;
        if (original == null) {
            return null;
        }
        switch (original.type()) {
            case 6:
                return duplicatePdfDictionary((PdfDictionary) original, null, newReader);

            case 7:
                pRStream1 = (PRStream) original;
                stream = new PRStream(pRStream1, null, newReader);
                duplicatePdfDictionary(pRStream1, stream, newReader);
                return stream;

            case 5:
                originalArray = (PdfArray) original;
                arr = new PdfArray(originalArray.size());
                for (it = originalArray.listIterator(); it.hasNext();) {
                    arr.add(duplicatePdfObject(it.next(), newReader));
                }
                return arr;

            case 10:
                org = (PRIndirectReference) original;
                return new PRIndirectReference(newReader, org.getNumber(), org.getGeneration());
        }

        return original;
    }

    public void close() {
        try {
            this.tokens.close();
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        }
    }

//    protected void removeUnusedNode(PdfObject obj, boolean[] hits) {
//        Stack<Object> state = new Stack();
//        state.push(obj);
//        while (!state.empty()) {
//            Object current = state.pop();
//            if (current == null) {
//                continue;
//            }
//            ArrayList<PdfObject> ar = null;
//            PdfDictionary dic = null;
//            PdfName[] keys = null;
//            Object[] objs = null;
//            int idx = 0;
//            if (current instanceof PdfObject) {
//                PRIndirectReference ref;
//                int num;
//                obj = (PdfObject) current;
//                switch (obj.type()) {
//                    case 6:
//                    case 7:
//                        dic = (PdfDictionary) obj;
//                        keys = new PdfName[dic.size()];
//                        dic.getKeys().toArray(keys);
//                        break;
//                    case 5:
//                        ar = ((PdfArray) obj).getArrayList();
//                        break;
//                    case 10:
//                        ref = (PRIndirectReference) obj;
//                        num = ref.getNumber();
//                        if (!hits[num]) {
//                            hits[num] = true;
//                            state.push(getPdfObjectRelease(ref));
//                        }
//                        continue;
//
//                    default:
//                        continue;
//                }
//            } else {
//                objs = (Object[]) current;
//                if (objs[0] instanceof ArrayList) {
//                    ar = (ArrayList<PdfObject>) objs[0];
//                    idx = ((Integer) objs[1]).intValue();
//                } else {
//
//                    keys = (PdfName[]) objs[0];
//                    dic = (PdfDictionary) objs[1];
//                    idx = ((Integer) objs[2]).intValue();
//                }
//            }
//            if (ar != null) {
//                for (int i = idx; i < ar.size(); i++) {
//                    PdfObject v = ar.get(i);
//                    if (v.isIndirect()) {
//                        int num = ((PRIndirectReference) v).getNumber();
//                        if (num >= this.xrefObj.size() || (!this.partial && this.xrefObj.get(num) == null)) {
//                            ar.set(i, PdfNull.PDFNULL);
//                            continue;
//                        }
//                    }
//                    if (objs == null) {
//                        state.push(new Object[]{ar, Integer.valueOf(i + 1)});
//                    } else {
//                        objs[1] = Integer.valueOf(i + 1);
//                        state.push(objs);
//                    }
//                    state.push(v);
//                }
//
//                continue;
//            }
//            for (int k = idx; k < keys.length; k++) {
//                PdfName key = keys[k];
//                PdfObject v = dic.get(key);
//                if (v != null) {
//                    if (v.isIndirect()) {
//                        int num = ((PRIndirectReference) v).getNumber();
//                        if (num < 0 || num >= this.xrefObj.size() || (!this.partial && this.xrefObj.get(num) == null)) {
//                            dic.put(key, PdfNull.PDFNULL);
//                            continue;
//                        }
//                    }
//                }
//                if (objs == null) {
//                    state.push(new Object[]{keys, dic, Integer.valueOf(k + 1)});
//                } else {
//                    objs[2] = Integer.valueOf(k + 1);
//                    state.push(objs);
//                }
//                state.push(v);
//            }
//        }
//    }
//
//    public int removeUnusedObjects() {
//        boolean[] hits = new boolean[this.xrefObj.size()];
//        removeUnusedNode(this.trailer, hits);
//        int total = 0;
//        if (this.partial) {
//            for (int k = 1; k < hits.length; k++) {
//                if (!hits[k]) {
//                    this.xref[k * 2] = -1L;
//                    this.xref[k * 2 + 1] = 0L;
//                    this.xrefObj.set(k, null);
//                    total++;
//                }
//            }
//        } else {
//
//            for (int k = 1; k < hits.length; k++) {
//                if (!hits[k]) {
//                    this.xrefObj.set(k, null);
//                    total++;
//                }
//            }
//        }
//        return total;
//    }
    
    protected void removeUnusedNode(PdfObject obj, final boolean hits[]) {
        Stack<Object> state = new Stack<Object>();
        state.push(obj);
        while (!state.empty()) {
            Object current = state.pop();
            if (current == null)
                continue;
            ArrayList<PdfObject> ar = null;
            PdfDictionary dic = null;
            PdfName[] keys = null;
            Object[] objs = null;
            int idx = 0;
            if (current instanceof PdfObject) {
                obj = (PdfObject)current;
                switch (obj.type()) {
                    case PdfObject.DICTIONARY:
                    case PdfObject.STREAM:
                        dic = (PdfDictionary)obj;
                        keys = new PdfName[dic.size()];
                        dic.getKeys().toArray(keys);
                        break;
                    case PdfObject.ARRAY:
                         ar = ((PdfArray)obj).getArrayList();
                         break;
                    case PdfObject.INDIRECT:
                        PRIndirectReference ref = (PRIndirectReference)obj;
                        int num = ref.getNumber();
                        if (!hits[num]) {
                            hits[num] = true;
                            state.push(getPdfObjectRelease(ref));
                        }
                        continue;
                    default:
                        continue;
                }
            }
            else {
                objs = (Object[])current;
                if (objs[0] instanceof ArrayList) {
                    ar = (ArrayList<PdfObject>)objs[0];
                    idx = ((Integer)objs[1]).intValue();
                }
                else {
                    keys = (PdfName[])objs[0];
                    dic = (PdfDictionary)objs[1];
                    idx = ((Integer)objs[2]).intValue();
                }
            }
            if (ar != null) {
                for (int k = idx; k < ar.size(); ++k) {
                    PdfObject v = ar.get(k);
                    if (v.isIndirect()) {
                        int num = ((PRIndirectReference)v).getNumber();
                        if (num >= xrefObj.size() || !partial && xrefObj.get(num) == null) {
                            ar.set(k, PdfNull.PDFNULL);
                            continue;
                        }
                    }
                    if (objs == null)
                        state.push(new Object[]{ar, Integer.valueOf(k + 1)});
                    else {
                        objs[1] = Integer.valueOf(k + 1);
                        state.push(objs);
                    }
                    state.push(v);
                    break;
                }
            }
            else {
                for (int k = idx; k < keys.length; ++k) {
                    PdfName key = keys[k];
                    PdfObject v = dic.get(key);
                    if (v.isIndirect()) {
                        int num = ((PRIndirectReference)v).getNumber();
                        if (num < 0 || num >= xrefObj.size() || !partial && xrefObj.get(num) == null) {
                            dic.put(key, PdfNull.PDFNULL);
                            continue;
                        }
                    }
                    if (objs == null)
                        state.push(new Object[]{keys, dic, Integer.valueOf(k + 1)});
                    else {
                        objs[2] = Integer.valueOf(k + 1);
                        state.push(objs);
                    }
                    state.push(v);
                    break;
                }
            }
        }
    }

    /**
     * Removes all the unreachable objects.
     * @return the number of indirect objects removed
     */
    public int removeUnusedObjects() {
        boolean hits[] = new boolean[xrefObj.size()];
        removeUnusedNode(trailer, hits);
        int total = 0;
        if (partial) {
            for (int k = 1; k < hits.length; ++k) {
                if (!hits[k]) {
                    xref[k * 2] = -1;
                    xref[k * 2 + 1] = 0;
                    xrefObj.set(k, null);
                    ++total;
                }
            }
        }
        else {
            for (int k = 1; k < hits.length; ++k) {
                if (!hits[k]) {
                    xrefObj.set(k, null);
                    ++total;
                }
            }
        }
        return total;
    }

    public AcroFields getAcroFields() {
        return new AcroFields(this, null);
    }

    public String getJavaScript(RandomAccessFileOrArray file) throws IOException {
        PdfDictionary names = (PdfDictionary) getPdfObjectRelease(this.catalog.get(PdfName.NAMES));
        if (names == null) {
            return null;
        }
        PdfDictionary js = (PdfDictionary) getPdfObjectRelease(names.get(PdfName.JAVASCRIPT));
        if (js == null) {
            return null;
        }
        HashMap<String, PdfObject> jscript = PdfNameTree.readTree(js);
        String[] sortedNames = new String[jscript.size()];
        sortedNames = (String[]) jscript.keySet().toArray((Object[]) sortedNames);
        Arrays.sort((Object[]) sortedNames);
        StringBuffer buf = new StringBuffer();
        for (int k = 0; k < sortedNames.length; k++) {
            PdfDictionary j = (PdfDictionary) getPdfObjectRelease(jscript.get(sortedNames[k]));
            if (j != null) {

                PdfObject obj = getPdfObjectRelease(j.get(PdfName.JS));
                if (obj != null) {
                    if (obj.isString()) {
                        buf.append(((PdfString) obj).toUnicodeString()).append('\n');
                    } else if (obj.isStream()) {
                        byte[] bytes = getStreamBytes((PRStream) obj, file);
                        if (bytes.length >= 2 && bytes[0] == -2 && bytes[1] == -1) {
                            buf.append(PdfEncodings.convertToString(bytes, "UnicodeBig"));
                        } else {
                            buf.append(PdfEncodings.convertToString(bytes, "PDF"));
                        }
                        buf.append('\n');
                    }
                }
            }
        }
        return buf.toString();
    }

    public String getJavaScript() throws IOException {
        RandomAccessFileOrArray rf = getSafeFile();
        try {
            rf.reOpen();
            return getJavaScript(rf);
        } finally {

            try {
                rf.close();
            } catch (Exception exception) {
            }
        }
    }

    public void selectPages(String ranges) {
        selectPages(SequenceList.expand(ranges, getNumberOfPages()));
    }

    public void selectPages(List<Integer> pagesToKeep) {
        selectPages(pagesToKeep, true);
    }

    protected void selectPages(List<Integer> pagesToKeep, boolean removeUnused) {
        this.pageRefs.selectPages(pagesToKeep);
        if (removeUnused) {
            removeUnusedObjects();
        }

    }

    public void setViewerPreferences(int preferences) {
        this.viewerPreferences.setViewerPreferences(preferences);
        setViewerPreferences(this.viewerPreferences);
    }

    public void addViewerPreference(PdfName key, PdfObject value) {
        this.viewerPreferences.addViewerPreference(key, value);
        setViewerPreferences(this.viewerPreferences);
    }

    public void setViewerPreferences(PdfViewerPreferencesImp vp) {
        vp.addToCatalog(this.catalog);
    }

    public int getSimpleViewerPreferences() {
        return PdfViewerPreferencesImp.getViewerPreferences(this.catalog).getPageLayoutAndMode();
    }

    public boolean isAppendable() {
        return this.appendable;
    }

    public void setAppendable(boolean appendable) {
        this.appendable = appendable;
        if (appendable) {
            getPdfObject(this.trailer.get(PdfName.ROOT));
        }
    }

    public boolean isNewXrefType() {
        return this.newXrefType;
    }

    public long getFileLength() {
        return this.fileLength;
    }

    public boolean isHybridXref() {
        return this.hybridXref;
    }

    static class PageRefs {

        private final PdfReader reader;

        private ArrayList<PRIndirectReference> refsn;

        private int sizep;
        private IntHashtable refsp;
        private int lastPageRead = -1;

        private ArrayList<PdfDictionary> pageInh;

        private boolean keepPages;

        private Set<PdfObject> pagesNodes = new HashSet<PdfObject>();

        private PageRefs(PdfReader reader) throws IOException {
            this.reader = reader;
            if (reader.partial) {
                this.refsp = new IntHashtable();
                PdfNumber npages = (PdfNumber) PdfReader.getPdfObjectRelease(reader.rootPages.get(PdfName.COUNT));
                this.sizep = npages.intValue();
            } else {

                readPages();
            }
        }

        PageRefs(PageRefs other, PdfReader reader) {
            this.reader = reader;
            this.sizep = other.sizep;
            if (other.refsn != null) {
                this.refsn = new ArrayList<PRIndirectReference>(other.refsn);
                for (int k = 0; k < this.refsn.size(); k++) {
                    this.refsn.set(k, (PRIndirectReference) PdfReader.duplicatePdfObject(this.refsn.get(k), reader));
                }
            } else {

                this.refsp = (IntHashtable) other.refsp.clone();
            }
        }

        int size() {
            if (this.refsn != null) {
                return this.refsn.size();
            }
            return this.sizep;
        }

        void readPages() throws IOException {
            if (this.refsn != null) {
                return;
            }
            this.refsp = null;
            this.refsn = new ArrayList<PRIndirectReference>();
            this.pageInh = new ArrayList<PdfDictionary>();
            iteratePages((PRIndirectReference) this.reader.catalog.get(PdfName.PAGES));
            this.pageInh = null;
            this.reader.rootPages.put(PdfName.COUNT, new PdfNumber(this.refsn.size()));
        }

        void reReadPages() throws IOException {
            this.refsn = null;
            readPages();
        }

        public PdfDictionary getPageN(int pageNum) {
            PRIndirectReference ref = getPageOrigRef(pageNum);
            return (PdfDictionary) PdfReader.getPdfObject(ref);
        }

        public PdfDictionary getPageNRelease(int pageNum) {
            PdfDictionary page = getPageN(pageNum);
            releasePage(pageNum);
            return page;
        }

        public PRIndirectReference getPageOrigRefRelease(int pageNum) {
            PRIndirectReference ref = getPageOrigRef(pageNum);
            releasePage(pageNum);
            return ref;
        }

        public PRIndirectReference getPageOrigRef(int pageNum) {
            try {
                pageNum--;
                if (pageNum < 0 || pageNum >= size()) {
                    return null;
                }
                if (this.refsn != null) {
                    return this.refsn.get(pageNum);
                }
                int n = this.refsp.get(pageNum);
                if (n == 0) {
                    PRIndirectReference ref = getSinglePage(pageNum);
                    if (this.reader.lastXrefPartial == -1) {
                        this.lastPageRead = -1;
                    } else {
                        this.lastPageRead = pageNum;
                    }
                    this.reader.lastXrefPartial = -1;
                    this.refsp.put(pageNum, ref.getNumber());
                    if (this.keepPages) {
                        this.lastPageRead = -1;
                    }
                    return ref;
                }

                if (this.lastPageRead != pageNum) {
                    this.lastPageRead = -1;
                }
                if (this.keepPages) {
                    this.lastPageRead = -1;
                }
                return new PRIndirectReference(this.reader, n);

            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
        }

        void keepPages() {
            if (this.refsp == null || this.keepPages) {
                return;
            }
            this.keepPages = true;
            this.refsp.clear();
        }

        public void releasePage(int pageNum) {
            if (this.refsp == null) {
                return;
            }
            pageNum--;
            if (pageNum < 0 || pageNum >= size()) {
                return;
            }
            if (pageNum != this.lastPageRead) {
                return;
            }
            this.lastPageRead = -1;
            this.reader.lastXrefPartial = this.refsp.get(pageNum);
            this.reader.releaseLastXrefPartial();
            this.refsp.remove(pageNum);
        }

        public void resetReleasePage() {
            if (this.refsp == null) {
                return;
            }
            this.lastPageRead = -1;
        }

        void insertPage(int pageNum, PRIndirectReference ref) {
            pageNum--;
            if (this.refsn != null) {
                if (pageNum >= this.refsn.size()) {
                    this.refsn.add(ref);
                } else {
                    this.refsn.add(pageNum, ref);
                }
            } else {
                this.sizep++;
                this.lastPageRead = -1;
                if (pageNum >= size()) {
                    this.refsp.put(size(), ref.getNumber());
                } else {

                    IntHashtable refs2 = new IntHashtable((this.refsp.size() + 1) * 2);
                    for (Iterator<IntHashtable.Entry> it = this.refsp.getEntryIterator(); it.hasNext();) {
                        IntHashtable.Entry entry = it.next();
                        int p = entry.getKey();
                        refs2.put((p >= pageNum) ? (p + 1) : p, entry.getValue());
                    }
                    refs2.put(pageNum, ref.getNumber());
                    this.refsp = refs2;
                }
            }
        }

        private void pushPageAttributes(PdfDictionary nodePages) {
            PdfDictionary dic = new PdfDictionary();
            if (!this.pageInh.isEmpty()) {
                dic.putAll(this.pageInh.get(this.pageInh.size() - 1));
            }
            for (int k = 0; k < PdfReader.pageInhCandidates.length; k++) {
                PdfObject obj = nodePages.get(PdfReader.pageInhCandidates[k]);
                if (obj != null) {
                    dic.put(PdfReader.pageInhCandidates[k], obj);
                }
            }
            this.pageInh.add(dic);
        }

        private void popPageAttributes() {
            this.pageInh.remove(this.pageInh.size() - 1);
        }

        private void iteratePages(PRIndirectReference rpage) throws IOException {
            if (!this.pagesNodes.add(PdfReader.getPdfObject(rpage))) {
                throw new InvalidPdfException(MessageLocalization.getComposedMessage("illegal.pages.tree", new Object[0]));
            }
            PdfDictionary page = (PdfDictionary) PdfReader.getPdfObject(rpage);
            if (page == null) {
                return;
            }
            PdfArray kidsPR = page.getAsArray(PdfName.KIDS);

            if (kidsPR == null) {
                page.put(PdfName.TYPE, PdfName.PAGE);
                PdfDictionary dic = this.pageInh.get(this.pageInh.size() - 1);

                for (PdfName element : dic.getKeys()) {
                    PdfName key = element;
                    if (page.get(key) == null) {
                        page.put(key, dic.get(key));
                    }
                }
                if (page.get(PdfName.MEDIABOX) == null) {
                    PdfArray arr = new PdfArray(new float[]{0.0F, 0.0F, PageSize.LETTER.getRight(), PageSize.LETTER.getTop()});
                    page.put(PdfName.MEDIABOX, arr);
                }
                this.refsn.add(rpage);
            } else {

                page.put(PdfName.TYPE, PdfName.PAGES);
                pushPageAttributes(page);
                for (int k = 0; k < kidsPR.size(); k++) {
                    PdfObject obj = kidsPR.getPdfObject(k);
                    if (!obj.isIndirect()) {
                        while (k < kidsPR.size()) {
                            kidsPR.remove(k);
                        }
                        break;
                    }
                    iteratePages((PRIndirectReference) obj);
                }
                popPageAttributes();
            }
        }

        protected PRIndirectReference getSinglePage(int n) {
            PdfDictionary acc = new PdfDictionary();
            PdfDictionary top = this.reader.rootPages;
            int base = 0;
            while (true) {
                for (int k = 0; k < PdfReader.pageInhCandidates.length; k++) {
                    PdfObject obj = top.get(PdfReader.pageInhCandidates[k]);
                    if (obj != null) {
                        acc.put(PdfReader.pageInhCandidates[k], obj);
                    }
                }
                PdfArray kids = (PdfArray) PdfReader.getPdfObjectRelease(top.get(PdfName.KIDS));
                for (Iterator<PdfObject> it = kids.listIterator(); it.hasNext();) {
                    PRIndirectReference ref = (PRIndirectReference) it.next();
                    PdfDictionary dic = (PdfDictionary) PdfReader.getPdfObject(ref);
                    int last = this.reader.lastXrefPartial;
                    PdfObject count = PdfReader.getPdfObjectRelease(dic.get(PdfName.COUNT));
                    this.reader.lastXrefPartial = last;
                    int acn = 1;
                    if (count != null && count.type() == 2) {
                        acn = ((PdfNumber) count).intValue();
                    }
                    if (n < base + acn) {
                        if (count == null) {
                            dic.mergeDifferent(acc);
                            return ref;
                        }
                        this.reader.releaseLastXrefPartial();
                        top = dic;
                        break;
                    }
                    this.reader.releaseLastXrefPartial();
                    base += acn;
                }
            }
        }

        private void selectPages(List<Integer> pagesToKeep) {
            IntHashtable pg = new IntHashtable();
            ArrayList<Integer> finalPages = new ArrayList<Integer>();
            int psize = size();
            for (Integer pi : pagesToKeep) {
                int p = pi.intValue();
                if (p >= 1 && p <= psize && pg.put(p, 1) == 0) {
                    finalPages.add(pi);
                }
            }
            if (this.reader.partial) {
                for (int j = 1; j <= psize; j++) {
                    getPageOrigRef(j);
                    resetReleasePage();
                }
            }
            PRIndirectReference parent = (PRIndirectReference) this.reader.catalog.get(PdfName.PAGES);
            PdfDictionary topPages = (PdfDictionary) PdfReader.getPdfObject(parent);
            ArrayList<PRIndirectReference> newPageRefs = new ArrayList<PRIndirectReference>(finalPages.size());
            PdfArray kids = new PdfArray();
            for (int k = 0; k < finalPages.size(); k++) {
                int p = ((Integer) finalPages.get(k)).intValue();
                PRIndirectReference pref = getPageOrigRef(p);
                resetReleasePage();
                kids.add(pref);
                newPageRefs.add(pref);
                getPageN(p).put(PdfName.PARENT, parent);
            }
            AcroFields af = this.reader.getAcroFields();
            boolean removeFields = (af.getFields().size() > 0);
            for (int i = 1; i <= psize; i++) {
                if (!pg.containsKey(i)) {
                    if (removeFields) {
                        af.removeFieldsFromPage(i);
                    }
                    PRIndirectReference pref = getPageOrigRef(i);
                    int nref = pref.getNumber();
                    this.reader.xrefObj.set(nref, null);
                    if (this.reader.partial) {
                        this.reader.xref[nref * 2] = -1L;
                        this.reader.xref[nref * 2 + 1] = 0L;
                    }
                }
            }
            topPages.put(PdfName.COUNT, new PdfNumber(finalPages.size()));
            topPages.put(PdfName.KIDS, kids);
            this.refsp = null;
            this.refsn = newPageRefs;
        }
    }

    PdfIndirectReference getCryptoRef() {
        if (this.cryptoRef == null) {
            return null;
        }
        return new PdfIndirectReference(0, this.cryptoRef.getNumber(), this.cryptoRef.getGeneration());
    }

    public boolean hasUsageRights() {
        PdfDictionary perms = this.catalog.getAsDict(PdfName.PERMS);
        if (perms == null) {
            return false;
        }
        return (perms.contains(PdfName.UR) || perms.contains(PdfName.UR3));
    }

    public void removeUsageRights() {
        PdfDictionary perms = this.catalog.getAsDict(PdfName.PERMS);
        if (perms == null) {
            return;
        }
        perms.remove(PdfName.UR);
        perms.remove(PdfName.UR3);
        if (perms.size() == 0) {
            this.catalog.remove(PdfName.PERMS);
        }
    }

    public int getCertificationLevel() {
        PdfDictionary dic = this.catalog.getAsDict(PdfName.PERMS);
        if (dic == null) {
            return 0;
        }
        dic = dic.getAsDict(PdfName.DOCMDP);
        if (dic == null) {
            return 0;
        }
        PdfArray arr = dic.getAsArray(PdfName.REFERENCE);
        if (arr == null || arr.size() == 0) {
            return 0;
        }
        dic = arr.getAsDict(0);
        if (dic == null) {
            return 0;
        }
        dic = dic.getAsDict(PdfName.TRANSFORMPARAMS);
        if (dic == null) {
            return 0;
        }
        PdfNumber p = dic.getAsNumber(PdfName.P);
        if (p == null) {
            return 0;
        }
        return p.intValue();
    }

    public final boolean isOpenedWithFullPermissions() {
        return (!this.encrypted || this.ownerPasswordUsed || unethicalreading);
    }

    public int getCryptoMode() {
        if (this.decrypt == null) {
            return -1;
        }
        return this.decrypt.getCryptoMode();
    }

    public boolean isMetadataEncrypted() {
        if (this.decrypt == null) {
            return false;
        }
        return this.decrypt.isMetadataEncrypted();
    }

    public byte[] computeUserPassword() {
        if (!this.encrypted || !this.ownerPasswordUsed) {
            return null;
        }
        return this.decrypt.computeUserPassword(this.password);
    }
}
