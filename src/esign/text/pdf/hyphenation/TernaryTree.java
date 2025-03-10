package esign.text.pdf.hyphenation;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Stack;

public class TernaryTree
        implements Cloneable, Serializable {

    private static final long serialVersionUID = 5313366505322983510L;
    protected char[] lo;
    protected char[] hi;
    protected char[] eq;
    protected char[] sc;
    protected CharVector kv;
    protected char root;
    protected char freenode;
    protected int length;
    protected static final int BLOCK_SIZE = 2048;

    TernaryTree() {
        init();
    }

    protected void init() {
        this.root = Character.MIN_VALUE;
        this.freenode = '\001';
        this.length = 0;
        this.lo = new char[2048];
        this.hi = new char[2048];
        this.eq = new char[2048];
        this.sc = new char[2048];
        this.kv = new CharVector();
    }

    public void insert(String key, char val) {
        int len = key.length() + 1;

        if (this.freenode + len > this.eq.length) {
            redimNodeArrays(this.eq.length + 2048);
        }
        char[] strkey = new char[len--];
        key.getChars(0, len, strkey, 0);
        strkey[len] = Character.MIN_VALUE;
        this.root = insert(this.root, strkey, 0, val);
    }

    public void insert(char[] key, int start, char val) {
        int len = strlen(key) + 1;
        if (this.freenode + len > this.eq.length) {
            redimNodeArrays(this.eq.length + 2048);
        }
        this.root = insert(this.root, key, start, val);
    }

    private char insert(char p, char[] key, int start, char val) {
        int len = strlen(key, start);
        if (p == '\000') {

            p = this.freenode = (char) (this.freenode + 1);
            this.eq[p] = val;
            this.length++;
            this.hi[p] = Character.MIN_VALUE;
            if (len > 0) {
                this.sc[p] = Character.MAX_VALUE;
                this.lo[p] = (char) this.kv.alloc(len + 1);

                strcpy(this.kv.getArray(), this.lo[p], key, start);
            } else {
                this.sc[p] = Character.MIN_VALUE;
                this.lo[p] = Character.MIN_VALUE;
            }
            return p;
        }

        if (this.sc[p] == Character.MAX_VALUE) {

            char pp = this.freenode = (char) (this.freenode + 1);
            this.lo[pp] = this.lo[p];
            this.eq[pp] = this.eq[p];
            this.lo[p] = Character.MIN_VALUE;
            if (len > 0) {
                this.sc[p] = this.kv.get(this.lo[pp]);
                this.eq[p] = pp;
                this.lo[pp] = (char) (this.lo[pp] + 1);
                if (this.kv.get(this.lo[pp]) == '\000') {

                    this.lo[pp] = Character.MIN_VALUE;
                    this.sc[pp] = Character.MIN_VALUE;
                    this.hi[pp] = Character.MIN_VALUE;
                } else {

                    this.sc[pp] = Character.MAX_VALUE;
                }

            } else {

                this.sc[pp] = Character.MAX_VALUE;
                this.hi[p] = pp;
                this.sc[p] = Character.MIN_VALUE;
                this.eq[p] = val;
                this.length++;
                return p;
            }
        }
        char s = key[start];
        if (s < this.sc[p]) {
            this.lo[p] = insert(this.lo[p], key, start, val);
        } else if (s == this.sc[p]) {
            if (s != '\000') {
                this.eq[p] = insert(this.eq[p], key, start + 1, val);
            } else {

                this.eq[p] = val;
            }
        } else {
            this.hi[p] = insert(this.hi[p], key, start, val);
        }
        return p;
    }

    public static int strcmp(char[] a, int startA, char[] b, int startB) {
        for (; a[startA] == b[startB]; startA++, startB++) {
            if (a[startA] == '\000') {
                return 0;
            }
        }
        return a[startA] - b[startB];
    }

    public static int strcmp(String str, char[] a, int start) {
        int len = str.length();
        int i;
        for (i = 0; i < len; i++) {
            int d = str.charAt(i) - a[start + i];
            if (d != 0) {
                return d;
            }
            if (a[start + i] == '\000') {
                return d;
            }
        }
        if (a[start + i] != '\000') {
            return -a[start + i];
        }
        return 0;
    }

    public static void strcpy(char[] dst, int di, char[] src, int si) {
        while (src[si] != '\000') {
            dst[di++] = src[si++];
        }
        dst[di] = Character.MIN_VALUE;
    }

    public static int strlen(char[] a, int start) {
        int len = 0;
        for (int i = start; i < a.length && a[i] != '\000'; i++) {
            len++;
        }
        return len;
    }

    public static int strlen(char[] a) {
        return strlen(a, 0);
    }

    public int find(String key) {
        int len = key.length();
        char[] strkey = new char[len + 1];
        key.getChars(0, len, strkey, 0);
        strkey[len] = Character.MIN_VALUE;

        return find(strkey, 0);
    }

    public int find(char[] key, int start) {
        char p = this.root;
        int i = start;

        while (p != '\000') {
            if (this.sc[p] == Character.MAX_VALUE) {
                if (strcmp(key, i, this.kv.getArray(), this.lo[p]) == 0) {
                    return this.eq[p];
                }
                return -1;
            }

            char c = key[i];
            int d = c - this.sc[p];
            if (d == 0) {
                if (c == '\000') {
                    return this.eq[p];
                }
                i++;
                p = this.eq[p];
                continue;
            }
            if (d < 0) {
                p = this.lo[p];
                continue;
            }
            p = this.hi[p];
        }

        return -1;
    }

    public boolean knows(String key) {
        return (find(key) >= 0);
    }

    private void redimNodeArrays(int newsize) {
        int len = (newsize < this.lo.length) ? newsize : this.lo.length;
        char[] na = new char[newsize];
        System.arraycopy(this.lo, 0, na, 0, len);
        this.lo = na;
        na = new char[newsize];
        System.arraycopy(this.hi, 0, na, 0, len);
        this.hi = na;
        na = new char[newsize];
        System.arraycopy(this.eq, 0, na, 0, len);
        this.eq = na;
        na = new char[newsize];
        System.arraycopy(this.sc, 0, na, 0, len);
        this.sc = na;
    }

    public int size() {
        return this.length;
    }

    public Object clone() {
        TernaryTree t = new TernaryTree();
        t.lo = (char[]) this.lo.clone();
        t.hi = (char[]) this.hi.clone();
        t.eq = (char[]) this.eq.clone();
        t.sc = (char[]) this.sc.clone();
        t.kv = (CharVector) this.kv.clone();
        t.root = this.root;
        t.freenode = this.freenode;
        t.length = this.length;

        return t;
    }

    protected void insertBalanced(String[] k, char[] v, int offset, int n) {
        if (n < 1) {
            return;
        }
        int m = n >> 1;

        insert(k[m + offset], v[m + offset]);
        insertBalanced(k, v, offset, m);

        insertBalanced(k, v, offset + m + 1, n - m - 1);
    }

    public void balance() {
        int i = 0, n = this.length;
        String[] k = new String[n];
        char[] v = new char[n];
        Iterator iter = new Iterator();
        while (iter.hasMoreElements()) {
            v[i] = iter.getValue();
            k[i++] = iter.nextElement();
        }
        init();
        insertBalanced(k, v, 0, n);
    }

    public void trimToSize() {
        balance();

        redimNodeArrays(this.freenode);

        CharVector kx = new CharVector();
        kx.alloc(1);
        TernaryTree map = new TernaryTree();
        compact(kx, map, this.root);
        this.kv = kx;
        this.kv.trimToSize();
    }

    private void compact(CharVector kx, TernaryTree map, char p) {
        if (p == '\000') {
            return;
        }
        if (this.sc[p] == Character.MAX_VALUE) {
            int k = map.find(this.kv.getArray(), this.lo[p]);
            if (k < 0) {
                k = kx.alloc(strlen(this.kv.getArray(), this.lo[p]) + 1);
                strcpy(kx.getArray(), k, this.kv.getArray(), this.lo[p]);
                map.insert(kx.getArray(), k, (char) k);
            }
            this.lo[p] = (char) k;
        } else {
            compact(kx, map, this.lo[p]);
            if (this.sc[p] != '\000') {
                compact(kx, map, this.eq[p]);
            }
            compact(kx, map, this.hi[p]);
        }
    }

    public Enumeration<String> keys() {
        return new Iterator();
    }

    public class Iterator
            implements Enumeration<String> {

        int cur;

        String curkey;
        Stack<Item> ns;
        StringBuffer ks;

        private class Item
                implements Cloneable {

            char parent;
            char child;

            public Item() {
                this.parent = Character.MIN_VALUE;
                this.child = Character.MIN_VALUE;
            }

            public Item(char p, char c) {
                this.parent = p;
                this.child = c;
            }

            public Item clone() {
                return new Item(this.parent, this.child);
            }
        }

        public Iterator() {
            this.cur = -1;
            this.ns = new Stack<Item>();
            this.ks = new StringBuffer();
            rewind();
        }

        public void rewind() {
            this.ns.removeAllElements();
            this.ks.setLength(0);
            this.cur = TernaryTree.this.root;
            run();
        }

        public String nextElement() {
            String res = this.curkey;
            this.cur = up();
            run();
            return res;
        }

        public char getValue() {
            if (this.cur >= 0) {
                return TernaryTree.this.eq[this.cur];
            }
            return Character.MIN_VALUE;
        }

        public boolean hasMoreElements() {
            return (this.cur != -1);
        }

        private int up() {
            Item i = new Item();
            int res = 0;

            if (this.ns.empty()) {
                return -1;
            }

            if (this.cur != 0 && TernaryTree.this.sc[this.cur] == '\000') {
                return TernaryTree.this.lo[this.cur];
            }

            boolean climb = true;

            while (climb) {
                i = this.ns.pop();
                i.child = (char) (i.child + 1);
                switch (i.child) {
                    case '\001':
                        if (TernaryTree.this.sc[i.parent] != '\000') {
                            res = TernaryTree.this.eq[i.parent];
                            this.ns.push(i.clone());
                            this.ks.append(TernaryTree.this.sc[i.parent]);
                        } else {
                            i.child = (char) (i.child + 1);
                            this.ns.push(i.clone());
                            res = TernaryTree.this.hi[i.parent];
                        }
                        climb = false;
                        continue;

                    case '\002':
                        res = TernaryTree.this.hi[i.parent];
                        this.ns.push(i.clone());
                        if (this.ks.length() > 0) {
                            this.ks.setLength(this.ks.length() - 1);
                        }
                        climb = false;
                        continue;
                }

                if (this.ns.empty()) {
                    return -1;
                }
                climb = true;
            }

            return res;
        }

        private int run() {
            if (this.cur == -1) {
                return -1;
            }

            boolean leaf = false;

            while (true) {
                if (this.cur != 0) {
                    if (TernaryTree.this.sc[this.cur] == Character.MAX_VALUE) {
                        leaf = true;
                    } else {

                        ns.push(new Item((char)cur, '\u0000'));
                        if (TernaryTree.this.sc[this.cur] == '\000') {
                            leaf = true;
                        } else {

                            this.cur = TernaryTree.this.lo[this.cur];
                            continue;
                        }
                    }
                }
                if (leaf) {
                    break;
                }

                this.cur = up();
                if (this.cur == -1) {
                    return -1;
                }
            }

            StringBuffer buf = new StringBuffer(this.ks.toString());
            if (TernaryTree.this.sc[this.cur] == Character.MAX_VALUE) {
                int p = TernaryTree.this.lo[this.cur];
                while (TernaryTree.this.kv.get(p) != '\000') {
                    buf.append(TernaryTree.this.kv.get(p++));
                }
            }
            this.curkey = buf.toString();
            return 0;
        }
    }

    public void printStats() {
        System.out.println("Number of keys = " + Integer.toString(this.length));
        System.out.println("Node count = " + Integer.toString(this.freenode));

        System.out.println("Key Array length = "
                + Integer.toString(this.kv.length()));
    }
}
