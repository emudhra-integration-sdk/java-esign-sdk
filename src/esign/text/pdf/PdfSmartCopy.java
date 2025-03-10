package esign.text.pdf;

import esign.text.Document;
import esign.text.DocumentException;
import esign.text.ExceptionConverter;
import esign.text.error_messages.MessageLocalization;
import esign.text.log.Counter;
import esign.text.log.CounterFactory;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;

public class PdfSmartCopy
        extends PdfCopy {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfSmartCopy.class);

    private HashMap<ByteStore, PdfIndirectReference> streamMap = null;
    private final HashMap<RefKey, Integer> serialized = new HashMap<RefKey, Integer>();

    protected Counter COUNTER = CounterFactory.getCounter(PdfSmartCopy.class);

    protected Counter getCounter() {
        return this.COUNTER;
    }

    public PdfSmartCopy(Document document, OutputStream os) throws DocumentException {
        super(document, os);
        this.streamMap = new HashMap<ByteStore, PdfIndirectReference>();
    }

    protected PdfIndirectReference copyIndirect(PRIndirectReference in) throws IOException, BadPdfFormatException {
        PdfIndirectReference theRef;
        PdfObject srcObj = PdfReader.getPdfObjectRelease(in);
        ByteStore streamKey = null;
        boolean validStream = false;
        if (srcObj.isStream()) {
            streamKey = new ByteStore((PRStream) srcObj, this.serialized);
            validStream = true;
            PdfIndirectReference streamRef = this.streamMap.get(streamKey);
            if (streamRef != null) {
                return streamRef;
            }
        } else if (srcObj.isDictionary()) {
            streamKey = new ByteStore((PdfDictionary) srcObj, this.serialized);
            validStream = true;
            PdfIndirectReference streamRef = this.streamMap.get(streamKey);
            if (streamRef != null) {
                return streamRef;
            }
        }

        RefKey key = new RefKey(in);
        PdfCopy.IndirectReferences iRef = this.indirects.get(key);
        if (iRef != null) {
            theRef = iRef.getRef();
            if (iRef.getCopied()) {
                return theRef;
            }
        } else {
            theRef = this.body.getPdfIndirectReference();
            iRef = new PdfCopy.IndirectReferences(theRef);
            this.indirects.put(key, iRef);
        }
        if (srcObj.isDictionary()) {
            PdfObject type = PdfReader.getPdfObjectRelease(((PdfDictionary) srcObj).get(PdfName.TYPE));
            if (type != null) {
                if (PdfName.PAGE.equals(type)) {
                    return theRef;
                }
                if (PdfName.CATALOG.equals(type)) {
                    LOGGER.warn(MessageLocalization.getComposedMessage("make.copy.of.catalog.dictionary.is.forbidden", new Object[0]));
                    return null;
                }
            }
        }
        iRef.setCopied();

        if (validStream) {
            this.streamMap.put(streamKey, theRef);
        }

        PdfObject obj = copyObject(srcObj);
        addToBody(obj, theRef);
        return theRef;
    }

    public void freeReader(PdfReader reader) throws IOException {
        this.serialized.clear();
        super.freeReader(reader);
    }

    public void addPage(PdfImportedPage iPage) throws IOException, BadPdfFormatException {
        if (this.currentPdfReaderInstance.getReader() != this.reader) {
            this.serialized.clear();
        }
        super.addPage(iPage);
    }

    static class ByteStore {

        private final byte[] b;
        private final int hash;
        private MessageDigest md5;

        private void serObject(PdfObject obj, int level, ByteBuffer bb, HashMap<RefKey, Integer> serialized) throws IOException {
            if (level <= 0) {
                return;
            }
            if (obj == null) {
                bb.append("$Lnull");
                return;
            }
            PdfIndirectReference ref = null;
            ByteBuffer savedBb = null;

            if (obj.isIndirect()) {
                ref = (PdfIndirectReference) obj;
                RefKey key = new RefKey(ref);
                if (serialized.containsKey(key)) {
                    bb.append(((Integer) serialized.get(key)).intValue());

                    return;
                }
                savedBb = bb;
                bb = new ByteBuffer();
            }

            obj = PdfReader.getPdfObject(obj);
            if (obj.isStream()) {
                bb.append("$B");
                serDic((PdfDictionary) obj, level - 1, bb, serialized);
                if (level > 0) {
                    this.md5.reset();
                    bb.append(this.md5.digest(PdfReader.getStreamBytesRaw((PRStream) obj)));
                }

            } else if (obj.isDictionary()) {
                serDic((PdfDictionary) obj, level - 1, bb, serialized);
            } else if (obj.isArray()) {
                serArray((PdfArray) obj, level - 1, bb, serialized);
            } else if (obj.isString()) {
                bb.append("$S").append(obj.toString());
            } else if (obj.isName()) {
                bb.append("$N").append(obj.toString());
            } else {

                bb.append("$L").append(obj.toString());
            }
            if (savedBb != null) {
                RefKey key = new RefKey(ref);
                if (!serialized.containsKey(key)) {
                    serialized.put(key, Integer.valueOf(calculateHash(bb.getBuffer())));
                }
                savedBb.append(bb);
            }
        }

        private void serDic(PdfDictionary dic, int level, ByteBuffer bb, HashMap<RefKey, Integer> serialized) throws IOException {
            bb.append("$D");
            if (level <= 0) {
                return;
            }
            Object[] keys = dic.getKeys().toArray();
            Arrays.sort(keys);
            for (int k = 0; k < keys.length; k++) {
                if (!keys[k].equals(PdfName.P) || (!dic.get((PdfName) keys[k]).isIndirect() && !dic.get((PdfName) keys[k]).isDictionary())) {

                    serObject((PdfObject) keys[k], level, bb, serialized);
                    serObject(dic.get((PdfName) keys[k]), level, bb, serialized);
                }
            }
        }

        private void serArray(PdfArray array, int level, ByteBuffer bb, HashMap<RefKey, Integer> serialized) throws IOException {
            bb.append("$A");
            if (level <= 0) {
                return;
            }
            for (int k = 0; k < array.size(); k++) {
                serObject(array.getPdfObject(k), level, bb, serialized);
            }
        }

        ByteStore(PRStream str, HashMap<RefKey, Integer> serialized) throws IOException {
            try {
                this.md5 = MessageDigest.getInstance("MD5");
            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
            ByteBuffer bb = new ByteBuffer();
            int level = 100;
            serObject(str, level, bb, serialized);
            this.b = bb.toByteArray();
            this.hash = calculateHash(this.b);
            this.md5 = null;
        }

        ByteStore(PdfDictionary dict, HashMap<RefKey, Integer> serialized) throws IOException {
            try {
                this.md5 = MessageDigest.getInstance("MD5");
            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
            ByteBuffer bb = new ByteBuffer();
            int level = 100;
            serObject(dict, level, bb, serialized);
            this.b = bb.toByteArray();
            this.hash = calculateHash(this.b);
            this.md5 = null;
        }

        private static int calculateHash(byte[] b) {
            int hash = 0;
            int len = b.length;
            for (int k = 0; k < len; k++) {
                hash = hash * 31 + (b[k] & 0xFF);
            }
            return hash;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof ByteStore)) {
                return false;
            }
            if (hashCode() != obj.hashCode()) {
                return false;
            }
            return Arrays.equals(this.b, ((ByteStore) obj).b);
        }

        public int hashCode() {
            return this.hash;
        }
    }
}
