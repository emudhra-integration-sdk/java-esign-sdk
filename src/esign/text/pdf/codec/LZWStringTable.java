package esign.text.pdf.codec;

import java.io.PrintStream;

public class LZWStringTable {

    byte[] strChr_ = new byte[4096];
    short[] strNxt_ = new short[4096];
    int[] strLen_ = new int[4096];
    short[] strHsh_ = new short[9973];

    private static final int RES_CODES = 2;

    private static final short HASH_FREE = -1;

    private static final short NEXT_FIRST = -1;
    private static final int MAXBITS = 12;
    private static final int MAXSTR = 4096;
    private static final short HASHSIZE = 9973;
    private static final short HASHSTEP = 2039;
    short numStrings_;

    public int AddCharString(short index, byte b) {
        if (this.numStrings_ >= 4096) {
            return 65535;
        }

        int hshidx = Hash(index, b);
        while (this.strHsh_[hshidx] != -1) {
            hshidx = (hshidx + 2039) % 9973;
        }
        this.strHsh_[hshidx] = this.numStrings_;
        this.strChr_[this.numStrings_] = b;
        if (index == -1) {

            this.strNxt_[this.numStrings_] = -1;
            this.strLen_[this.numStrings_] = 1;
        } else {

            this.strNxt_[this.numStrings_] = index;
            this.strLen_[this.numStrings_] = this.strLen_[index] + 1;
        }

        this.numStrings_ = (short) (this.numStrings_ + 1);
        return this.numStrings_;
    }

    public short FindCharString(short index, byte b) {
        if (index == -1) {
            return (short) (b & 0xFF);
        }
        int hshidx = Hash(index, b);
        int nxtidx;
        while ((nxtidx = this.strHsh_[hshidx]) != -1) {

            if (this.strNxt_[nxtidx] == index && this.strChr_[nxtidx] == b) {
                return (short) nxtidx;
            }
            hshidx = (hshidx + 2039) % 9973;
        }

        return -1;
    }

    public void ClearTable(int codesize) {
        this.numStrings_ = 0;

        for (int q = 0; q < 9973; q++) {
            this.strHsh_[q] = -1;
        }
        int w = (1 << codesize) + 2;
        for (int i = 0; i < w; i++) {
            AddCharString((short) -1, (byte) i);
        }
    }

    public static int Hash(short index, byte lastbyte) {
        return (((short) (lastbyte << 8) ^ index) & 0xFFFF) % 9973;
    }

    public int expandCode(byte[] buf, int offset, short code, int skipHead) {
        int expandLen;
        if (offset == -2) {
            if (skipHead == 1) {
                skipHead = 0;
            }
        }
        if (code == -1 || skipHead == this.strLen_[code]) {
            return 0;
        }

        int codeLen = this.strLen_[code] - skipHead;
        int bufSpace = buf.length - offset;
        if (bufSpace > codeLen) {
            expandLen = codeLen;
        } else {
            expandLen = bufSpace;
        }
        int skipTail = codeLen - expandLen;

        int idx = offset + expandLen;

        while (idx > offset && code != -1) {

            if (--skipTail < 0) {
                buf[--idx] = this.strChr_[code];
            }
            code = this.strNxt_[code];
        }

        if (codeLen > expandLen) {
            return -expandLen;
        }
        return expandLen;
    }

    public void dump(PrintStream out) {
        for (int i = 258; i < this.numStrings_; i++) {
            out.println(" strNxt_[" + i + "] = " + this.strNxt_[i] + " strChr_ "
                    + Integer.toHexString(this.strChr_[i] & 0xFF) + " strLen_ "
                    + Integer.toHexString(this.strLen_[i]));
        }
    }
}

