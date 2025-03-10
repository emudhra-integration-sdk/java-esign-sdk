package esign.text.pdf.crypto;

import org.emcastle.crypto.BlockCipher;
import org.emcastle.crypto.CipherParameters;
import org.emcastle.crypto.engines.AESFastEngine;
import org.emcastle.crypto.modes.CBCBlockCipher;
import org.emcastle.crypto.params.KeyParameter;

public class AESCipherCBCnoPad {

    private BlockCipher cbc;

    public AESCipherCBCnoPad(boolean forEncryption, byte[] key) {
        AESFastEngine aESFastEngine = new AESFastEngine();
        this.cbc = (BlockCipher) new CBCBlockCipher((BlockCipher) aESFastEngine);
        KeyParameter kp = new KeyParameter(key);
        this.cbc.init(forEncryption, (CipherParameters) kp);
    }

    public byte[] processBlock(byte[] inp, int inpOff, int inpLen) {
        if (inpLen % this.cbc.getBlockSize() != 0) {
            throw new IllegalArgumentException("Not multiple of block: " + inpLen);
        }
        byte[] outp = new byte[inpLen];
        int baseOffset = 0;
        while (inpLen > 0) {
            this.cbc.processBlock(inp, inpOff, outp, baseOffset);
            inpLen -= this.cbc.getBlockSize();
            baseOffset += this.cbc.getBlockSize();
            inpOff += this.cbc.getBlockSize();
        }
        return outp;
    }
}
