package esign.text.pdf;

import esign.text.ExceptionConverter;
import esign.text.pdf.crypto.AESCipher;
import esign.text.pdf.crypto.ARCFOUREncryption;
import esign.text.pdf.crypto.IVGenerator;
import java.io.IOException;
import java.io.OutputStream;

public class OutputStreamEncryption
        extends OutputStream {

    protected OutputStream out;
    protected ARCFOUREncryption arcfour;
    protected AESCipher cipher;
    private byte[] sb = new byte[1];

    private static final int AES_128 = 4;
    private static final int AES_256 = 5;
    private boolean aes;
    private boolean finished;

    public OutputStreamEncryption(OutputStream out, byte[] key, int off, int len, int revision) {
        try {
            this.out = out;
            this.aes = (revision == 4 || revision == 5);
            if (this.aes) {
                byte[] iv = IVGenerator.getIV();
                byte[] nkey = new byte[len];
                System.arraycopy(key, off, nkey, 0, len);
                this.cipher = new AESCipher(true, nkey, iv);
                write(iv);
            } else {

                this.arcfour = new ARCFOUREncryption();
                this.arcfour.prepareARCFOURKey(key, off, len);
            }
        } catch (Exception ex) {
            throw new ExceptionConverter(ex);
        }
    }

    public OutputStreamEncryption(OutputStream out, byte[] key, int revision) {
        this(out, key, 0, key.length, revision);
    }

    public void close() throws IOException {
        finish();
        this.out.close();
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    public void write(int b) throws IOException {
        this.sb[0] = (byte) b;
        write(this.sb, 0, 1);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (this.aes) {
            byte[] b2 = this.cipher.update(b, off, len);
            if (b2 == null || b2.length == 0) {
                return;
            }
            this.out.write(b2, 0, b2.length);
        } else {

            byte[] b2 = new byte[Math.min(len, 4192)];
            while (len > 0) {
                int sz = Math.min(len, b2.length);
                this.arcfour.encryptARCFOUR(b, off, sz, b2, 0);
                this.out.write(b2, 0, sz);
                len -= sz;
                off += sz;
            }
        }
    }

    public void finish() throws IOException {
        if (!this.finished) {
            this.finished = true;
            if (this.aes) {
                byte[] b;
                try {
                    b = this.cipher.doFinal();
                } catch (Exception ex) {
                    throw new ExceptionConverter(ex);
                }
                this.out.write(b, 0, b.length);
            }
        }
    }
}
