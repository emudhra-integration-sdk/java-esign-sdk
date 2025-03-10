package esign.text.pdf;

import esign.text.DocWriter;
import esign.text.ExceptionConverter;
import esign.text.error_messages.MessageLocalization;
import esign.text.exceptions.BadPasswordException;
import esign.text.pdf.crypto.AESCipherCBCnoPad;
import esign.text.pdf.crypto.ARCFOUREncryption;
import esign.text.pdf.crypto.IVGenerator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.cert.Certificate;

public class PdfEncryption {

    public static final int STANDARD_ENCRYPTION_40 = 2;
    public static final int STANDARD_ENCRYPTION_128 = 3;
    public static final int AES_128 = 4;
    public static final int AES_256 = 5;
    private static final byte[] pad = new byte[]{40, -65, 78, 94, 78, 117, -118, 65, 100, 0, 78, 86, -1, -6, 1, 8, 46, 46, 0, -74, -48, 104, 62, Byte.MIN_VALUE, 47, 12, -87, -2, 100, 83, 105, 122};

    private static final byte[] salt = new byte[]{115, 65, 108, 84};

    private static final byte[] metadataPad = new byte[]{-1, -1, -1, -1};

    byte[] key;

    int keySize;

    byte[] mkey = new byte[0];

    byte[] ownerKey = new byte[32];

    byte[] userKey = new byte[32];

    byte[] oeKey;

    byte[] ueKey;

    byte[] perms;

    long permissions;

    byte[] documentID;

    private int revision;

    private int keyLength;

    protected PdfPublicKeySecurityHandler publicKeyHandler = null;

    byte[] extra = new byte[5];

    MessageDigest md5;

    private ARCFOUREncryption arcfour = new ARCFOUREncryption();

    private boolean encryptMetadata;

    static long seq = System.currentTimeMillis();

    private boolean embeddedFilesOnly;

    private int cryptoMode;
    private static final int VALIDATION_SALT_OFFSET = 32;
    private static final int KEY_SALT_OFFSET = 40;
    private static final int SALT_LENGHT = 8;
    private static final int OU_LENGHT = 48;

    public PdfEncryption() {
        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
        this.publicKeyHandler = new PdfPublicKeySecurityHandler();
    }

    public PdfEncryption(PdfEncryption enc) {
        this();
        if (enc.key != null) {
            this.key = (byte[]) enc.key.clone();
        }
        this.keySize = enc.keySize;
        this.mkey = (byte[]) enc.mkey.clone();
        this.ownerKey = (byte[]) enc.ownerKey.clone();
        this.userKey = (byte[]) enc.userKey.clone();
        this.permissions = enc.permissions;
        if (enc.documentID != null) {
            this.documentID = (byte[]) enc.documentID.clone();
        }
        this.revision = enc.revision;
        this.keyLength = enc.keyLength;
        this.encryptMetadata = enc.encryptMetadata;
        this.embeddedFilesOnly = enc.embeddedFilesOnly;
        this.publicKeyHandler = enc.publicKeyHandler;

        if (enc.ueKey != null) {
            this.ueKey = (byte[]) enc.ueKey.clone();
        }
        if (enc.oeKey != null) {
            this.oeKey = (byte[]) enc.oeKey.clone();
        }
        if (enc.perms != null) {
            this.perms = (byte[]) enc.perms.clone();
        }
    }

    public void setCryptoMode(int mode, int kl) {
        this.cryptoMode = mode;
        this.encryptMetadata = ((mode & 0x8) != 8);
        this.embeddedFilesOnly = ((mode & 0x18) == 24);
        mode &= 0x7;
        switch (mode) {
            case 0:
                this.encryptMetadata = true;
                this.embeddedFilesOnly = false;
                this.keyLength = 40;
                this.revision = 2;
                return;
            case 1:
                this.embeddedFilesOnly = false;
                if (kl > 0) {
                    this.keyLength = kl;
                } else {
                    this.keyLength = 128;
                }
                this.revision = 3;
                return;
            case 2:
                this.keyLength = 128;
                this.revision = 4;
                return;
            case 3:
                this.keyLength = 256;
                this.keySize = 32;
                this.revision = 5;
                return;
        }
        throw new IllegalArgumentException(MessageLocalization.getComposedMessage("no.valid.encryption.mode", new Object[0]));
    }

    public int getCryptoMode() {
        return this.cryptoMode;
    }

    public boolean isMetadataEncrypted() {
        return this.encryptMetadata;
    }

    public long getPermissions() {
        return this.permissions;
    }

    public boolean isEmbeddedFilesOnly() {
        return this.embeddedFilesOnly;
    }

    private byte[] padPassword(byte[] userPassword) {
        byte[] userPad = new byte[32];
        if (userPassword == null) {
            System.arraycopy(pad, 0, userPad, 0, 32);
        } else {
            System.arraycopy(userPassword, 0, userPad, 0, Math.min(userPassword.length, 32));

            if (userPassword.length < 32) {
                System.arraycopy(pad, 0, userPad, userPassword.length, 32 - userPassword.length);
            }
        }

        return userPad;
    }

    private byte[] computeOwnerKey(byte[] userPad, byte[] ownerPad) {
        byte[] ownerKey = new byte[32];
        byte[] digest = this.md5.digest(ownerPad);
        if (this.revision == 3 || this.revision == 4) {
            byte[] mkey = new byte[this.keyLength / 8];

            for (int k = 0; k < 50; k++) {
                this.md5.update(digest, 0, mkey.length);
                System.arraycopy(this.md5.digest(), 0, digest, 0, mkey.length);
            }
            System.arraycopy(userPad, 0, ownerKey, 0, 32);
            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < mkey.length; j++) {
                    mkey[j] = (byte) (digest[j] ^ i);
                }
                this.arcfour.prepareARCFOURKey(mkey);
                this.arcfour.encryptARCFOUR(ownerKey);
            }
        } else {
            this.arcfour.prepareARCFOURKey(digest, 0, 5);
            this.arcfour.encryptARCFOUR(userPad, ownerKey);
        }
        return ownerKey;
    }

    private void setupGlobalEncryptionKey(byte[] documentID, byte[] userPad, byte[] ownerKey, long permissions) {
        this.documentID = documentID;
        this.ownerKey = ownerKey;
        this.permissions = permissions;

        this.mkey = new byte[this.keyLength / 8];

        this.md5.reset();
        this.md5.update(userPad);
        this.md5.update(ownerKey);

        byte[] ext = new byte[4];
        ext[0] = (byte) (int) permissions;
        ext[1] = (byte) (int) (permissions >> 8L);
        ext[2] = (byte) (int) (permissions >> 16L);
        ext[3] = (byte) (int) (permissions >> 24L);
        this.md5.update(ext, 0, 4);
        if (documentID != null) {
            this.md5.update(documentID);
        }
        if (!this.encryptMetadata) {
            this.md5.update(metadataPad);
        }
        byte[] digest = new byte[this.mkey.length];
        System.arraycopy(this.md5.digest(), 0, digest, 0, this.mkey.length);

        if (this.revision == 3 || this.revision == 4) {
            for (int k = 0; k < 50; k++) {
                System.arraycopy(this.md5.digest(digest), 0, digest, 0, this.mkey.length);
            }
        }
        System.arraycopy(digest, 0, this.mkey, 0, this.mkey.length);
    }

    private void setupUserKey() {
        if (this.revision == 3 || this.revision == 4) {
            this.md5.update(pad);
            byte[] digest = this.md5.digest(this.documentID);
            System.arraycopy(digest, 0, this.userKey, 0, 16);
            for (int k = 16; k < 32; k++) {
                this.userKey[k] = 0;
            }
            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < this.mkey.length; j++) {
                    digest[j] = (byte) (this.mkey[j] ^ i);
                }
                this.arcfour.prepareARCFOURKey(digest, 0, this.mkey.length);
                this.arcfour.encryptARCFOUR(this.userKey, 0, 16);
            }
        } else {
            this.arcfour.prepareARCFOURKey(this.mkey);
            this.arcfour.encryptARCFOUR(pad, this.userKey);
        }
    }

    public void setupAllKeys(byte[] userPassword, byte[] ownerPassword, int permissions) {
        if (ownerPassword == null || ownerPassword.length == 0) {
            ownerPassword = this.md5.digest(createDocumentId());
        }
        permissions |= (this.revision == 3 || this.revision == 4 || this.revision == 5) ? -3904 : -64;

        permissions &= 0xFFFFFFFC;
        this.permissions = permissions;
        if (this.revision == 5) {
            try {
                if (userPassword == null) {
                    userPassword = new byte[0];
                }
                this.documentID = createDocumentId();
                byte[] uvs = IVGenerator.getIV(8);
                byte[] uks = IVGenerator.getIV(8);
                this.key = IVGenerator.getIV(32);

                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(userPassword, 0, Math.min(userPassword.length, 127));
                md.update(uvs);
                this.userKey = new byte[48];
                md.digest(this.userKey, 0, 32);
                System.arraycopy(uvs, 0, this.userKey, 32, 8);
                System.arraycopy(uks, 0, this.userKey, 40, 8);

                md.update(userPassword, 0, Math.min(userPassword.length, 127));
                md.update(uks);
                AESCipherCBCnoPad ac = new AESCipherCBCnoPad(true, md.digest());
                this.ueKey = ac.processBlock(this.key, 0, this.key.length);

                byte[] ovs = IVGenerator.getIV(8);
                byte[] oks = IVGenerator.getIV(8);
                md.update(ownerPassword, 0, Math.min(ownerPassword.length, 127));
                md.update(ovs);
                md.update(this.userKey);
                this.ownerKey = new byte[48];
                md.digest(this.ownerKey, 0, 32);
                System.arraycopy(ovs, 0, this.ownerKey, 32, 8);
                System.arraycopy(oks, 0, this.ownerKey, 40, 8);

                md.update(ownerPassword, 0, Math.min(ownerPassword.length, 127));
                md.update(oks);
                md.update(this.userKey);
                ac = new AESCipherCBCnoPad(true, md.digest());
                this.oeKey = ac.processBlock(this.key, 0, this.key.length);

                byte[] permsp = IVGenerator.getIV(16);
                permsp[0] = (byte) permissions;
                permsp[1] = (byte) (permissions >> 8);
                permsp[2] = (byte) (permissions >> 16);
                permsp[3] = (byte) (permissions >> 24);
                permsp[4] = -1;
                permsp[5] = -1;
                permsp[6] = -1;
                permsp[7] = -1;
                permsp[8] = this.encryptMetadata ? (byte) 84 : (byte) 70;
                permsp[9] = 97;
                permsp[10] = 100;
                permsp[11] = 98;
                ac = new AESCipherCBCnoPad(true, this.key);
                this.perms = ac.processBlock(permsp, 0, permsp.length);
            } catch (Exception ex) {
                throw new ExceptionConverter(ex);
            }

        } else {

            byte[] userPad = padPassword(userPassword);
            byte[] ownerPad = padPassword(ownerPassword);

            this.ownerKey = computeOwnerKey(userPad, ownerPad);
            this.documentID = createDocumentId();
            setupByUserPad(this.documentID, userPad, this.ownerKey, permissions);
        }
    }

    public boolean readKey(PdfDictionary enc, byte[] password) throws BadPasswordException {
        try {
            if (password == null) {
                password = new byte[0];
            }
            byte[] oValue = DocWriter.getISOBytes(enc.get(PdfName.O).toString());
            byte[] uValue = DocWriter.getISOBytes(enc.get(PdfName.U).toString());
            byte[] oeValue = DocWriter.getISOBytes(enc.get(PdfName.OE).toString());
            byte[] ueValue = DocWriter.getISOBytes(enc.get(PdfName.UE).toString());
            byte[] perms = DocWriter.getISOBytes(enc.get(PdfName.PERMS).toString());
            PdfNumber pValue = (PdfNumber) enc.get(PdfName.P);

            this.oeKey = oeValue;
            this.ueKey = ueValue;
            this.perms = perms;

            this.ownerKey = oValue;
            this.userKey = uValue;

            this.permissions = pValue.longValue();

            boolean isUserPass = false;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(password, 0, Math.min(password.length, 127));
            md.update(oValue, 32, 8);
            md.update(uValue, 0, 48);
            byte[] hash = md.digest();
            boolean isOwnerPass = compareArray(hash, oValue, 32);
            if (isOwnerPass) {
                md.update(password, 0, Math.min(password.length, 127));
                md.update(oValue, 40, 8);
                md.update(uValue, 0, 48);
                hash = md.digest();
                AESCipherCBCnoPad aESCipherCBCnoPad = new AESCipherCBCnoPad(false, hash);
                this.key = aESCipherCBCnoPad.processBlock(oeValue, 0, oeValue.length);
            } else {

                md.update(password, 0, Math.min(password.length, 127));
                md.update(uValue, 32, 8);
                hash = md.digest();
                isUserPass = compareArray(hash, uValue, 32);
                if (!isUserPass) {
                    throw new BadPasswordException(MessageLocalization.getComposedMessage("bad.user.password", new Object[0]));
                }
                md.update(password, 0, Math.min(password.length, 127));
                md.update(uValue, 40, 8);
                hash = md.digest();
                AESCipherCBCnoPad aESCipherCBCnoPad = new AESCipherCBCnoPad(false, hash);
                this.key = aESCipherCBCnoPad.processBlock(ueValue, 0, ueValue.length);
            }
            AESCipherCBCnoPad ac = new AESCipherCBCnoPad(false, this.key);
            byte[] decPerms = ac.processBlock(perms, 0, perms.length);
            if (decPerms[9] != 97 || decPerms[10] != 100 || decPerms[11] != 98) {
                throw new BadPasswordException(MessageLocalization.getComposedMessage("bad.user.password", new Object[0]));
            }
            this.permissions = (decPerms[0] & 0xFF | (decPerms[1] & 0xFF) << 8 | (decPerms[2] & 0xFF) << 16 | (decPerms[2] & 0xFF) << 24);

            this.encryptMetadata = (decPerms[8] == 84);
            return isOwnerPass;
        } catch (BadPasswordException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ExceptionConverter(ex);
        }
    }

    private static boolean compareArray(byte[] a, byte[] b, int len) {
        for (int k = 0; k < len; k++) {
            if (a[k] != b[k]) {
                return false;
            }
        }
        return true;
    }

    public static byte[] createDocumentId() {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
        long time = System.currentTimeMillis();
        long mem = Runtime.getRuntime().freeMemory();
        String s = time + "+" + mem + "+" + seq++;
        return md5.digest(s.getBytes());
    }

    public void setupByUserPassword(byte[] documentID, byte[] userPassword, byte[] ownerKey, long permissions) {
        setupByUserPad(documentID, padPassword(userPassword), ownerKey, permissions);
    }

    private void setupByUserPad(byte[] documentID, byte[] userPad, byte[] ownerKey, long permissions) {
        setupGlobalEncryptionKey(documentID, userPad, ownerKey, permissions);
        setupUserKey();
    }

    public void setupByOwnerPassword(byte[] documentID, byte[] ownerPassword, byte[] userKey, byte[] ownerKey, long permissions) {
        setupByOwnerPad(documentID, padPassword(ownerPassword), userKey, ownerKey, permissions);
    }

    private void setupByOwnerPad(byte[] documentID, byte[] ownerPad, byte[] userKey, byte[] ownerKey, long permissions) {
        byte[] userPad = computeOwnerKey(ownerKey, ownerPad);

        setupGlobalEncryptionKey(documentID, userPad, ownerKey, permissions);

        setupUserKey();
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public void setupByEncryptionKey(byte[] key, int keylength) {
        this.mkey = new byte[keylength / 8];
        System.arraycopy(key, 0, this.mkey, 0, this.mkey.length);
    }

    public void setHashKey(int number, int generation) {
        if (this.revision == 5) {
            return;
        }
        this.md5.reset();
        this.extra[0] = (byte) number;
        this.extra[1] = (byte) (number >> 8);
        this.extra[2] = (byte) (number >> 16);
        this.extra[3] = (byte) generation;
        this.extra[4] = (byte) (generation >> 8);
        this.md5.update(this.mkey);
        this.md5.update(this.extra);
        if (this.revision == 4) {
            this.md5.update(salt);
        }
        this.key = this.md5.digest();
        this.keySize = this.mkey.length + 5;
        if (this.keySize > 16) {
            this.keySize = 16;
        }
    }

    public static PdfObject createInfoId(byte[] id, boolean modified) throws IOException {
        ByteBuffer buf = new ByteBuffer(90);
        if (id.length == 0) {
            id = createDocumentId();
        }
        buf.append('[').append('<');
        int k;
        for (k = 0; k < id.length; k++) {
            buf.appendHex(id[k]);
        }
        buf.append('>').append('<');
        if (modified) {
            id = createDocumentId();
        }
        for (k = 0; k < id.length; k++) {
            buf.appendHex(id[k]);
        }
        buf.append('>').append(']');
        buf.close();
        return new PdfLiteral(buf.toByteArray());
    }

    public PdfDictionary getEncryptionDictionary() {
        PdfDictionary dic = new PdfDictionary();

        if (this.publicKeyHandler.getRecipientsSize() > 0) {
            PdfArray recipients = null;

            dic.put(PdfName.FILTER, PdfName.PUBSEC);
            dic.put(PdfName.R, new PdfNumber(this.revision));

            try {
                recipients = this.publicKeyHandler.getEncodedRecipients();
            } catch (Exception f) {
                throw new ExceptionConverter(f);
            }

            if (this.revision == 2) {
                dic.put(PdfName.V, new PdfNumber(1));
                dic.put(PdfName.SUBFILTER, PdfName.ADBE_PKCS7_S4);
                dic.put(PdfName.RECIPIENTS, recipients);
            } else if (this.revision == 3 && this.encryptMetadata) {
                dic.put(PdfName.V, new PdfNumber(2));
                dic.put(PdfName.LENGTH, new PdfNumber(128));
                dic.put(PdfName.SUBFILTER, PdfName.ADBE_PKCS7_S4);
                dic.put(PdfName.RECIPIENTS, recipients);
            } else {
                if (this.revision == 5) {
                    dic.put(PdfName.R, new PdfNumber(5));
                    dic.put(PdfName.V, new PdfNumber(5));
                } else {

                    dic.put(PdfName.R, new PdfNumber(4));
                    dic.put(PdfName.V, new PdfNumber(4));
                }
                dic.put(PdfName.SUBFILTER, PdfName.ADBE_PKCS7_S5);

                PdfDictionary stdcf = new PdfDictionary();
                stdcf.put(PdfName.RECIPIENTS, recipients);
                if (!this.encryptMetadata) {
                    stdcf.put(PdfName.ENCRYPTMETADATA, PdfBoolean.PDFFALSE);
                }
                if (this.revision == 4) {
                    stdcf.put(PdfName.CFM, PdfName.AESV2);
                    stdcf.put(PdfName.LENGTH, new PdfNumber(128));
                } else if (this.revision == 5) {
                    stdcf.put(PdfName.CFM, PdfName.AESV3);
                    stdcf.put(PdfName.LENGTH, new PdfNumber(256));
                } else {

                    stdcf.put(PdfName.CFM, PdfName.V2);
                }
                PdfDictionary cf = new PdfDictionary();
                cf.put(PdfName.DEFAULTCRYPTFILTER, stdcf);
                dic.put(PdfName.CF, cf);
                if (this.embeddedFilesOnly) {
                    dic.put(PdfName.EFF, PdfName.DEFAULTCRYPTFILTER);
                    dic.put(PdfName.STRF, PdfName.IDENTITY);
                    dic.put(PdfName.STMF, PdfName.IDENTITY);
                } else {

                    dic.put(PdfName.STRF, PdfName.DEFAULTCRYPTFILTER);
                    dic.put(PdfName.STMF, PdfName.DEFAULTCRYPTFILTER);
                }
            }

            MessageDigest md = null;
            byte[] encodedRecipient = null;

            try {
                if (this.revision == 5) {
                    md = MessageDigest.getInstance("SHA-256");
                } else {
                    md = MessageDigest.getInstance("SHA-1");
                }
                md.update(this.publicKeyHandler.getSeed());
                for (int i = 0; i < this.publicKeyHandler.getRecipientsSize(); i++) {
                    encodedRecipient = this.publicKeyHandler.getEncodedRecipient(i);
                    md.update(encodedRecipient);
                }
                if (!this.encryptMetadata) {
                    md.update(new byte[]{-1, -1, -1, -1});
                }
            } catch (Exception f) {
                throw new ExceptionConverter(f);
            }

            byte[] mdResult = md.digest();

            if (this.revision == 5) {
                this.key = mdResult;
            } else {
                setupByEncryptionKey(mdResult, this.keyLength);
            }
        } else {
            dic.put(PdfName.FILTER, PdfName.STANDARD);
            dic.put(PdfName.O, new PdfLiteral(
                    StringUtils.escapeString(this.ownerKey)));
            dic.put(PdfName.U, new PdfLiteral(
                    StringUtils.escapeString(this.userKey)));
            dic.put(PdfName.P, new PdfNumber(this.permissions));
            dic.put(PdfName.R, new PdfNumber(this.revision));

            if (this.revision == 2) {
                dic.put(PdfName.V, new PdfNumber(1));
            } else if (this.revision == 3 && this.encryptMetadata) {
                dic.put(PdfName.V, new PdfNumber(2));
                dic.put(PdfName.LENGTH, new PdfNumber(128));

            } else if (this.revision == 5) {
                if (!this.encryptMetadata) {
                    dic.put(PdfName.ENCRYPTMETADATA, PdfBoolean.PDFFALSE);
                }
                dic.put(PdfName.OE, new PdfLiteral(
                        StringUtils.escapeString(this.oeKey)));
                dic.put(PdfName.UE, new PdfLiteral(
                        StringUtils.escapeString(this.ueKey)));
                dic.put(PdfName.PERMS, new PdfLiteral(
                        StringUtils.escapeString(this.perms)));
                dic.put(PdfName.V, new PdfNumber(this.revision));
                dic.put(PdfName.LENGTH, new PdfNumber(256));
                PdfDictionary stdcf = new PdfDictionary();
                stdcf.put(PdfName.LENGTH, new PdfNumber(32));
                if (this.embeddedFilesOnly) {
                    stdcf.put(PdfName.AUTHEVENT, PdfName.EFOPEN);
                    dic.put(PdfName.EFF, PdfName.STDCF);
                    dic.put(PdfName.STRF, PdfName.IDENTITY);
                    dic.put(PdfName.STMF, PdfName.IDENTITY);
                } else {

                    stdcf.put(PdfName.AUTHEVENT, PdfName.DOCOPEN);
                    dic.put(PdfName.STRF, PdfName.STDCF);
                    dic.put(PdfName.STMF, PdfName.STDCF);
                }
                stdcf.put(PdfName.CFM, PdfName.AESV3);
                PdfDictionary cf = new PdfDictionary();
                cf.put(PdfName.STDCF, stdcf);
                dic.put(PdfName.CF, cf);
            } else {

                if (!this.encryptMetadata) {
                    dic.put(PdfName.ENCRYPTMETADATA, PdfBoolean.PDFFALSE);
                }
                dic.put(PdfName.R, new PdfNumber(4));
                dic.put(PdfName.V, new PdfNumber(4));
                dic.put(PdfName.LENGTH, new PdfNumber(128));
                PdfDictionary stdcf = new PdfDictionary();
                stdcf.put(PdfName.LENGTH, new PdfNumber(16));
                if (this.embeddedFilesOnly) {
                    stdcf.put(PdfName.AUTHEVENT, PdfName.EFOPEN);
                    dic.put(PdfName.EFF, PdfName.STDCF);
                    dic.put(PdfName.STRF, PdfName.IDENTITY);
                    dic.put(PdfName.STMF, PdfName.IDENTITY);
                } else {

                    stdcf.put(PdfName.AUTHEVENT, PdfName.DOCOPEN);
                    dic.put(PdfName.STRF, PdfName.STDCF);
                    dic.put(PdfName.STMF, PdfName.STDCF);
                }
                if (this.revision == 4) {
                    stdcf.put(PdfName.CFM, PdfName.AESV2);
                } else {
                    stdcf.put(PdfName.CFM, PdfName.V2);
                }
                PdfDictionary cf = new PdfDictionary();
                cf.put(PdfName.STDCF, stdcf);
                dic.put(PdfName.CF, cf);
            }
        }

        return dic;
    }

    public PdfObject getFileID(boolean modified) throws IOException {
        return createInfoId(this.documentID, modified);
    }

    public OutputStreamEncryption getEncryptionStream(OutputStream os) {
        return new OutputStreamEncryption(os, this.key, 0, this.keySize, this.revision);
    }

    public int calculateStreamSize(int n) {
        if (this.revision == 4 || this.revision == 5) {
            return (n & 0x7FFFFFF0) + 32;
        }
        return n;
    }

    public byte[] encryptByteArray(byte[] b) {
        try {
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            OutputStreamEncryption os2 = getEncryptionStream(ba);
            os2.write(b);
            os2.finish();
            return ba.toByteArray();
        } catch (IOException ex) {
            throw new ExceptionConverter(ex);
        }
    }

    public StandardDecryption getDecryptor() {
        return new StandardDecryption(this.key, 0, this.keySize, this.revision);
    }

    public byte[] decryptByteArray(byte[] b) {
        try {
            ByteArrayOutputStream ba = new ByteArrayOutputStream();
            StandardDecryption dec = getDecryptor();
            byte[] b2 = dec.update(b, 0, b.length);
            if (b2 != null) {
                ba.write(b2);
            }
            b2 = dec.finish();
            if (b2 != null) {
                ba.write(b2);
            }
            return ba.toByteArray();
        } catch (IOException ex) {
            throw new ExceptionConverter(ex);
        }
    }

    public void addRecipient(Certificate cert, int permission) {
        this.documentID = createDocumentId();
        this.publicKeyHandler.addRecipient(new PdfPublicKeyRecipient(cert, permission));
    }

    public byte[] computeUserPassword(byte[] ownerPassword) {
        byte[] userPad = null;
        if (this.publicKeyHandler.getRecipientsSize() == 0 && 2 <= this.revision && this.revision <= 4) {

            userPad = computeOwnerKey(this.ownerKey, padPassword(ownerPassword));
            for (int i = 0; i < userPad.length;) {
                boolean match = true;
                for (int j = 0; j < userPad.length - i; j++) {
                    if (userPad[i + j] != pad[j]) {
                        match = false;
                        break;
                    }
                }
                if (!match) {
                    i++;
                    continue;
                }
                byte[] userPassword = new byte[i];
                System.arraycopy(userPad, 0, userPassword, 0, i);
                return userPassword;
            }
        }
        return userPad;
    }
}
