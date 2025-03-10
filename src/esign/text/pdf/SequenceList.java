package esign.text.pdf;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class SequenceList {

    protected static final int COMMA = 1;
    protected static final int MINUS = 2;
    protected static final int NOT = 3;
    protected static final int TEXT = 4;
    protected static final int NUMBER = 5;
    protected static final int END = 6;
    protected static final char EOT = '￿';
    private static final int FIRST = 0;
    private static final int DIGIT = 1;
    private static final int OTHER = 2;
    private static final int DIGIT2 = 3;
    private static final String NOT_OTHER = "-,!0123456789";
    protected char[] text;
    protected int ptr;
    protected int number;
    protected String other;
    protected int low;
    protected int high;
    protected boolean odd;
    protected boolean even;
    protected boolean inverse;

    protected SequenceList(String range) {
        this.ptr = 0;
        this.text = range.toCharArray();
    }

    protected char nextChar() {
        while (true) {
            if (this.ptr >= this.text.length) {
                return Character.MAX_VALUE;
            }
            char c = this.text[this.ptr++];
            if (c > ' ') {
                return c;
            }
        }
    }

    protected void putBack() {
        this.ptr--;
        if (this.ptr < 0) {
            this.ptr = 0;
        }
    }

    protected int getType() {
        StringBuffer buf = new StringBuffer();
        int state = FIRST;
        while (true) {
            char c = nextChar();
            if (c == EOT) {
                if (state == DIGIT) {
                    number = Integer.parseInt(other = buf.toString());
                    return NUMBER;
                } else if (state == OTHER) {
                    other = buf.toString().toLowerCase();
                    return TEXT;
                }
                return END;
            }
            switch (state) {
                case FIRST:
                    switch (c) {
                        case '!':
                            return NOT;
                        case '-':
                            return MINUS;
                        case ',':
                            return COMMA;
                    }
                    buf.append(c);
                    if (c >= '0' && c <= '9') {
                        state = DIGIT;
                    } else {
                        state = OTHER;
                    }
                    break;
                case DIGIT:
                    if (c >= '0' && c <= '9') {
                        buf.append(c);
                    } else {
                        putBack();
                        number = Integer.parseInt(other = buf.toString());
                        return NUMBER;
                    }
                    break;
                case OTHER:
                    if (NOT_OTHER.indexOf(c) < 0) {
                        buf.append(c);
                    } else {
                        putBack();
                        other = buf.toString().toLowerCase();
                        return TEXT;
                    }
                    break;
            }
        }
    }

//    protected int getType() {
//        StringBuffer buf = new StringBuffer();
//        int state = 0;
//        while (true) {
//            char c = nextChar();
//            if (c == Character.MAX_VALUE) {
//                if (state == 1) {
//                    this.number = Integer.parseInt(this.other = buf.toString());
//                    return 5;
//                }
//                if (state == 2) {
//                    this.other = buf.toString().toLowerCase();
//                    return 4;
//                }
//                return 6;
//            }
//            switch (state) {
//                case 0:
//                    switch (c) {
//                        case '!':
//                            return 3;
//                        case '-':
//                            return 2;
//                        case ',':
//                            return 1;
//                    }
//                    buf.append(c);
//                    if (c >= '0' && c <= '9') {
//                        state = 1;
//                        continue;
//                    }
//                    state = 2;
//
//                case 1:
//                    if (c >= '0' && c <= '9') {
//                        buf.append(c);
//                        continue;
//                    }
//                    putBack();
//                    this.number = Integer.parseInt(this.other = buf.toString());
//                    return 5;
//
//                case 2:
//                    if ("-,!0123456789".indexOf(c) < 0) {
//                        buf.append(c);
//                        continue;
//                    }
//                    break;
//            }
//        }
//        putBack();
//        this.other = buf.toString().toLowerCase();
//        return 4;
//    }
    private void otherProc() {
        if (this.other.equals("odd") || this.other.equals("o")) {
            this.odd = true;
            this.even = false;
        } else if (this.other.equals("even") || this.other.equals("e")) {
            this.odd = false;
            this.even = true;
        }
    }

    protected boolean getAttributes() {
        this.low = -1;
        this.high = -1;
        this.odd = this.even = this.inverse = false;
        int state = 2;
        while (true) {
            int type = getType();
            if (type == 6 || type == 1) {
                if (state == 1) {
                    this.high = this.low;
                }
                return (type == 6);
            }
            switch (state) {
                case 2:
                    switch (type) {
                        case 3:
                            this.inverse = true;
                            continue;
                        case 2:
                            state = 3;
                            continue;
                    }
                    if (type == 5) {
                        this.low = this.number;
                        state = 1;
                        continue;
                    }
                    otherProc();

                case 1:
                    switch (type) {
                        case 3:
                            this.inverse = true;
                            state = 2;
                            this.high = this.low;
                            continue;
                        case 2:
                            state = 3;
                            continue;
                    }
                    this.high = this.low;
                    state = 2;
                    otherProc();

                case 3:
                    switch (type) {
                        case 3:
                            this.inverse = true;
                            state = 2;
                            continue;
                        case 2:
                            continue;
                        case 5:
                            this.high = this.number;
                            state = 2;
                            continue;
                    }
                    state = 2;
                    otherProc();
            }
        }
    }

    public static List<Integer> expand(String ranges, int maxNumber) {
        SequenceList parse = new SequenceList(ranges);
        LinkedList<Integer> list = new LinkedList<Integer>();
        boolean sair = false;
        while (!sair) {
            sair = parse.getAttributes();
            if (parse.low == -1 && parse.high == -1 && !parse.even && !parse.odd) {
                continue;
            }
            if (parse.low < 1) {
                parse.low = 1;
            }
            if (parse.high < 1 || parse.high > maxNumber) {
                parse.high = maxNumber;
            }
            if (parse.low > maxNumber) {
                parse.low = maxNumber;
            }

            int inc = 1;
            if (parse.inverse) {
                if (parse.low > parse.high) {
                    int t = parse.low;
                    parse.low = parse.high;
                    parse.high = t;
                }
                for (ListIterator<Integer> it = list.listIterator(); it.hasNext();) {
                    int n = ((Integer) it.next()).intValue();
                    if (parse.even && (n & 0x1) == 1) {
                        continue;
                    }
                    if (parse.odd && (n & 0x1) == 0) {
                        continue;
                    }
                    if (n >= parse.low && n <= parse.high) {
                        it.remove();
                    }
                }
                continue;
            }
            if (parse.low > parse.high) {
                inc = -1;
                if (parse.odd || parse.even) {
                    inc--;
                    if (parse.even) {
                        parse.low &= 0xFFFFFFFE;
                    } else {
                        parse.low -= ((parse.low & 0x1) == 1) ? 0 : 1;
                    }
                }
                int i;
                for (i = parse.low; i >= parse.high; i += inc) {
                    list.add(Integer.valueOf(i));
                }
                continue;
            }
            if (parse.odd || parse.even) {
                inc++;
                if (parse.odd) {
                    parse.low |= 0x1;
                } else {
                    parse.low += ((parse.low & 0x1) == 1) ? 1 : 0;
                }
            }
            int k;
            for (k = parse.low; k <= parse.high; k += inc) {
                list.add(Integer.valueOf(k));
            }
        }

        return list;
    }
}
