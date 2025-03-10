package esign.text.xmp.impl;

import esign.text.xmp.XMPException;

class ParseState {

    private String str;
    private int pos = 0;

    public ParseState(String str) {
        this.str = str;
    }

    public int length() {
        return this.str.length();
    }

    public boolean hasNext() {
        return (this.pos < this.str.length());
    }

    public char ch(int index) {
        return (index < this.str.length()) ? this.str
                .charAt(index) : Character.MIN_VALUE;
    }

    public char ch() {
        return (this.pos < this.str.length()) ? this.str
                .charAt(this.pos) : Character.MIN_VALUE;
    }

    public void skip() {
        this.pos++;
    }

    public int pos() {
        return this.pos;
    }

    public int gatherInt(String errorMsg, int maxValue) throws XMPException {
        int value = 0;
        boolean success = false;
        char ch = ch(this.pos);
        while ('0' <= ch && ch <= '9') {

            value = value * 10 + ch - 48;
            success = true;
            this.pos++;
            ch = ch(this.pos);
        }

        if (success) {

            if (value > maxValue) {
                return maxValue;
            }
            if (value < 0) {
                return 0;
            }

            return value;
        }

        throw new XMPException(errorMsg, 5);
    }
}
