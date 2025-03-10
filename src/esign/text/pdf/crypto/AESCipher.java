package esign.text.pdf.crypto;

import org.emcastle.crypto.BlockCipher;
import org.emcastle.crypto.CipherParameters;
import org.emcastle.crypto.engines.AESFastEngine;
import org.emcastle.crypto.modes.CBCBlockCipher;
import org.emcastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.emcastle.crypto.params.KeyParameter;
import org.emcastle.crypto.params.ParametersWithIV;

public class AESCipher {

    private PaddedBufferedBlockCipher bp;

    public AESCipher(boolean forEncryption, byte[] key, byte[] iv) {
        AESFastEngine aESFastEngine = new AESFastEngine();
        CBCBlockCipher cBCBlockCipher = new CBCBlockCipher((BlockCipher) aESFastEngine);
        this.bp = new PaddedBufferedBlockCipher((BlockCipher) cBCBlockCipher);
        KeyParameter kp = new KeyParameter(key);
        ParametersWithIV piv = new ParametersWithIV((CipherParameters) kp, iv);
        this.bp.init(forEncryption, (CipherParameters) piv);
    }

    public byte[] update(byte[] inp, int inpOff, int inpLen) {
        int neededLen = this.bp.getUpdateOutputSize(inpLen);
        byte[] outp = null;
        if (neededLen > 0) {
            outp = new byte[neededLen];
        } else {
            neededLen = 0;
        }
        this.bp.processBytes(inp, inpOff, inpLen, outp, 0);
        return outp;
    }

    public byte[] doFinal() {
        int neededLen = this.bp.getOutputSize(0);
        byte[] outp = new byte[neededLen];
        int n = 0;
        try {
            n = this.bp.doFinal(outp, 0);
        } catch (Exception ex) {
            return outp;
        }
        if (n != outp.length) {
            byte[] outp2 = new byte[n];
            System.arraycopy(outp, 0, outp2, 0, n);
            return outp2;
        }

        return outp;
    }
}


