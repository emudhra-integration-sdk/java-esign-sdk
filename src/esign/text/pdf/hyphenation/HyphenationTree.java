package esign.text.pdf.hyphenation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class HyphenationTree
        extends TernaryTree
        implements PatternConsumer {

    private static final long serialVersionUID = -7763254239309429432L;
    protected ByteVector vspace;
    protected HashMap<String, ArrayList<Object>> stoplist;
    protected TernaryTree classmap;
    private transient TernaryTree ivalues;

    public HyphenationTree() {
        this.stoplist = new HashMap<String, ArrayList<Object>>(23);
        this.classmap = new TernaryTree();
        this.vspace = new ByteVector();
        this.vspace.alloc(1);
    }

    protected int packValues(String values) {
        int n = values.length();
        int m = ((n & 0x1) == 1) ? ((n >> 1) + 2) : ((n >> 1) + 1);
        int offset = this.vspace.alloc(m);
        byte[] va = this.vspace.getArray();
        for (int i = 0; i < n; i++) {
            int j = i >> 1;
            byte v = (byte) (values.charAt(i) - 48 + 1 & 0xF);
            if ((i & 0x1) == 1) {
                va[j + offset] = (byte) (va[j + offset] | v);
            } else {
                va[j + offset] = (byte) (v << 4);
            }
        }
        va[m - 1 + offset] = 0;
        return offset;
    }

    protected String unpackValues(int k) {
        StringBuffer buf = new StringBuffer();
        byte v = this.vspace.get(k++);
        while (v != 0) {
            char c = (char) ((v >>> 4) - 1 + 48);
            buf.append(c);
            c = (char) (v & 0xF);
            if (c == '\000') {
                break;
            }
            c = (char) (c - 1 + 48);
            buf.append(c);
            v = this.vspace.get(k++);
        }
        return buf.toString();
    }

    public void loadSimplePatterns(InputStream stream) {
        SimplePatternParser pp = new SimplePatternParser();
        this.ivalues = new TernaryTree();

        pp.parse(stream, this);

        trimToSize();
        this.vspace.trimToSize();
        this.classmap.trimToSize();

        this.ivalues = null;
    }

    public String findPattern(String pat) {
        int k = find(pat);
        if (k >= 0) {
            return unpackValues(k);
        }
        return "";
    }

    protected int hstrcmp(char[] s, int si, char[] t, int ti) {
        for (; s[si] == t[ti]; si++, ti++) {
            if (s[si] == '\000') {
                return 0;
            }
        }
        if (t[ti] == '\000') {
            return 0;
        }
        return s[si] - t[ti];
    }

    protected byte[] getValues(int k) {
        StringBuffer buf = new StringBuffer();
        byte v = this.vspace.get(k++);
        while (v != 0) {
            char c = (char) ((v >>> 4) - 1);
            buf.append(c);
            c = (char) (v & 0xF);
            if (c == '\000') {
                break;
            }
            c = (char) (c - 1);
            buf.append(c);
            v = this.vspace.get(k++);
        }
        byte[] res = new byte[buf.length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = (byte) buf.charAt(i);
        }
        return res;
    }

    protected void searchPatterns(char[] word, int index, byte[] il) {
        int i = index;

        char sp = word[i];
        char p = this.root;

        while (p > '\000' && p < this.sc.length) {
            if (this.sc[p] == Character.MAX_VALUE) {
                if (hstrcmp(word, i, this.kv.getArray(), this.lo[p]) == 0) {
                    byte[] values = getValues(this.eq[p]);
                    int j = index;
                    for (byte value : values) {
                        if (j < il.length && value > il[j]) {
                            il[j] = value;
                        }
                        j++;
                    }
                }
                return;
            }
            int d = sp - this.sc[p];
            if (d == 0) {
                if (sp == '\000') {
                    break;
                }
                sp = word[++i];
                p = this.eq[p];
                char q = p;

                while (q > '\000' && q < this.sc.length
                        && this.sc[q] != Character.MAX_VALUE) {

                    if (this.sc[q] == '\000') {
                        byte[] values = getValues(this.eq[q]);
                        int j = index;
                        for (byte value : values) {
                            if (j < il.length && value > il[j]) {
                                il[j] = value;
                            }
                            j++;
                        }
                        break;
                    }
                    q = this.lo[q];
                }

                continue;
            }

            p = (d < 0) ? this.lo[p] : this.hi[p];
        }
    }

    public Hyphenation hyphenate(String word, int remainCharCount, int pushCharCount) {
        char[] w = word.toCharArray();
        return hyphenate(w, 0, w.length, remainCharCount, pushCharCount);
    }

    public Hyphenation hyphenate(char[] w, int offset, int len, int remainCharCount, int pushCharCount) {
        char[] word = new char[len + 3];

        char[] c = new char[2];
        int iIgnoreAtBeginning = 0;
        int iLength = len;
        boolean bEndOfLetters = false;
        int i;
        for (i = 1; i <= len; i++) {
            c[0] = w[offset + i - 1];
            int nc = this.classmap.find(c, 0);
            if (nc < 0) {
                if (i == 1 + iIgnoreAtBeginning) {

                    iIgnoreAtBeginning++;
                } else {

                    bEndOfLetters = true;
                }
                iLength--;
            } else if (!bEndOfLetters) {
                word[i - iIgnoreAtBeginning] = (char) nc;
            } else {
                return null;
            }
        }

        int origlen = len;
        len = iLength;
        if (len < remainCharCount + pushCharCount) {
            return null;
        }
        int[] result = new int[len + 1];
        int k = 0;

        String sw = new String(word, 1, len);
        if (this.stoplist.containsKey(sw)) {

            ArrayList<Object> hw = this.stoplist.get(sw);
            int j = 0;
            for (i = 0; i < hw.size(); i++) {
                Object o = hw.get(i);

                if (o instanceof String) {
                    j += ((String) o).length();
                    if (j >= remainCharCount && j < len - pushCharCount) {
                        result[k++] = j + iIgnoreAtBeginning;
                    }
                }
            }
        } else {

            word[0] = '.';
            word[len + 1] = '.';
            word[len + 2] = Character.MIN_VALUE;
            byte[] il = new byte[len + 3];
            for (i = 0; i < len + 1; i++) {
                searchPatterns(word, i, il);
            }

            for (i = 0; i < len; i++) {
                if ((il[i + 1] & 0x1) == 1 && i >= remainCharCount && i <= len - pushCharCount) {
                    result[k++] = i + iIgnoreAtBeginning;
                }
            }
        }

        if (k > 0) {

            int[] res = new int[k];
            System.arraycopy(result, 0, res, 0, k);
            return new Hyphenation(new String(w, offset, origlen), res);
        }
        return null;
    }

    public void addClass(String chargroup) {
        if (chargroup.length() > 0) {
            char equivChar = chargroup.charAt(0);
            char[] key = new char[2];
            key[1] = Character.MIN_VALUE;
            for (int i = 0; i < chargroup.length(); i++) {
                key[0] = chargroup.charAt(i);
                this.classmap.insert(key, 0, equivChar);
            }
        }
    }

    public void addException(String word, ArrayList<Object> hyphenatedword) {
        this.stoplist.put(word, hyphenatedword);
    }

    public void addPattern(String pattern, String ivalue) {
        int k = this.ivalues.find(ivalue);
        if (k <= 0) {
            k = packValues(ivalue);
            this.ivalues.insert(ivalue, (char) k);
        }
        insert(pattern, (char) k);
    }

    public void printStats() {
        System.out.println("Value space size = "
                + Integer.toString(this.vspace.length()));
        super.printStats();
    }
}
