package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;
import esign.text.exceptions.InvalidPdfException;
import esign.text.io.RandomAccessSourceFactory;
import java.io.IOException;

public class PRTokeniser {

    private final StringBuilder outBuf = new StringBuilder();

    public enum TokenType {
        NUMBER,
        STRING,
        NAME,
        COMMENT,
        START_ARRAY,
        END_ARRAY,
        START_DIC,
        END_DIC,
        REF,
        OTHER,
        ENDOFFILE;
    }

    public static final boolean[] delims = new boolean[]{
        true, true, false, false, false, false, false, false, false, false,
        true, true, false, true, true, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, true, false, false, false, false, true, false,
        false, true, true, false, false, false, false, false, true, false,
        false, false, false, false, false, false, false, false, false, false,
        false, true, false, true, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, true, false, true, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false, false, false, false,
        false, false, false, false, false, false, false};

    static final String EMPTY = "";

    private final RandomAccessFileOrArray file;

    protected TokenType type;

    protected String stringValue;

    protected int reference;

    protected int generation;

    protected boolean hexString;

    public PRTokeniser(RandomAccessFileOrArray file) {
        this.file = file;
    }

    public void seek(long pos) throws IOException {
        this.file.seek(pos);
    }

    public long getFilePointer() throws IOException {
        return this.file.getFilePointer();
    }

    public void close() throws IOException {
        this.file.close();
    }

    public long length() throws IOException {
        return this.file.length();
    }

    public int read() throws IOException {
        return this.file.read();
    }

    public RandomAccessFileOrArray getSafeFile() {
        return new RandomAccessFileOrArray(this.file);
    }

    public RandomAccessFileOrArray getFile() {
        return this.file;
    }

    public String readString(int size) throws IOException {
        StringBuilder buf = new StringBuilder();

        while (size-- > 0) {
            int ch = read();
            if (ch == -1) {
                break;
            }
            buf.append((char) ch);
        }
        return buf.toString();
    }

    public static final boolean isWhitespace(int ch) {
        return isWhitespace(ch, true);
    }

    public static final boolean isWhitespace(int ch, boolean isWhitespace) {
        return ((isWhitespace && ch == 0) || ch == 9 || ch == 10 || ch == 12 || ch == 13 || ch == 32);
    }

    public static final boolean isDelimiter(int ch) {
        return (ch == 40 || ch == 41 || ch == 60 || ch == 62 || ch == 91 || ch == 93 || ch == 47 || ch == 37);
    }

    public static final boolean isDelimiterWhitespace(int ch) {
        return delims[ch + 1];
    }

    public TokenType getTokenType() {
        return this.type;
    }

    public String getStringValue() {
        return this.stringValue;
    }

    public int getReference() {
        return this.reference;
    }

    public int getGeneration() {
        return this.generation;
    }

    public void backOnePosition(int ch) {
        if (ch != -1) {
            this.file.pushBack((byte) ch);
        }
    }

    public void throwError(String error) throws IOException {
        throw new InvalidPdfException(MessageLocalization.getComposedMessage("1.at.file.pointer.2", new Object[]{error, String.valueOf(this.file.getFilePointer())}));
    }

    public int getHeaderOffset() throws IOException {
        String str = readString(1024);
        int idx = str.indexOf("%PDF-");
        if (idx < 0) {
            idx = str.indexOf("%FDF-");
            if (idx < 0) {
                throw new InvalidPdfException(MessageLocalization.getComposedMessage("pdf.header.not.found", new Object[0]));
            }
        }
        return idx;
    }

    public char checkPdfHeader() throws IOException {
        this.file.seek(0L);
        String str = readString(1024);
        int idx = str.indexOf("%PDF-");
        if (idx != 0) {
            throw new InvalidPdfException(MessageLocalization.getComposedMessage("pdf.header.not.found", new Object[0]));
        }
        return str.charAt(7);
    }

    public void checkFdfHeader() throws IOException {
        this.file.seek(0L);
        String str = readString(1024);
        int idx = str.indexOf("%FDF-");
        if (idx != 0) {
            throw new InvalidPdfException(MessageLocalization.getComposedMessage("fdf.header.not.found", new Object[0]));
        }
    }

    public long getStartxref() throws IOException {
        int arrLength = 1024;
        long fileLength = this.file.length();
        long pos = fileLength - arrLength;
        if (pos < 1L) {
            pos = 1L;
        }
        while (pos > 0L) {
            this.file.seek(pos);
            String str = readString(arrLength);
            int idx = str.lastIndexOf("startxref");
            if (idx >= 0) {
                return pos + idx;
            }
            pos = pos - arrLength + 9L;
        }
        throw new InvalidPdfException(MessageLocalization.getComposedMessage("pdf.startxref.not.found", new Object[0]));
    }

    public static int getHex(int v) {
        if (v >= 48 && v <= 57) {
            return v - 48;
        }
        if (v >= 65 && v <= 70) {
            return v - 65 + 10;
        }
        if (v >= 97 && v <= 102) {
            return v - 97 + 10;
        }
        return -1;
    }

    public void nextValidToken() throws IOException {
        int level = 0;
        String n1 = null;
        String n2 = null;
        long ptr = 0L;
        while (nextToken()) {
            if (this.type == TokenType.COMMENT) {
                continue;
            }
            switch (level) {

                case 0:
                    if (this.type != TokenType.NUMBER) {
                        return;
                    }
                    ptr = this.file.getFilePointer();
                    n1 = this.stringValue;
                    level++;
                    continue;

                case 1:
                    if (this.type != TokenType.NUMBER) {
                        this.file.seek(ptr);
                        this.type = TokenType.NUMBER;
                        this.stringValue = n1;
                        return;
                    }
                    n2 = this.stringValue;
                    level++;
                    continue;
            }

            if (this.type != TokenType.OTHER || !this.stringValue.equals("R")) {
                this.file.seek(ptr);
                this.type = TokenType.NUMBER;
                this.stringValue = n1;
                return;
            }
            this.type = TokenType.REF;
            this.reference = Integer.parseInt(n1);
            this.generation = Integer.parseInt(n2);

            return;
        }

        if (level == 1) {
            this.type = TokenType.NUMBER;
        }
    }

    public boolean nextToken() throws IOException {
        int ch = 0;
        do {
            ch = file.read();
        } while (ch != -1 && isWhitespace(ch));
        if (ch == -1) {
            type = TokenType.ENDOFFILE;
            return false;
        }

        // Note:  We have to initialize stringValue here, after we've looked for the end of the stream,
        // to ensure that we don't lose the value of a token that might end exactly at the end
        // of the stream
        outBuf.setLength(0);
        stringValue = EMPTY;

        switch (ch) {
            case '[':
                type = TokenType.START_ARRAY;
                break;
            case ']':
                type = TokenType.END_ARRAY;
                break;
            case '/': {
                outBuf.setLength(0);
                type = TokenType.NAME;
                while (true) {
                    ch = file.read();
                    if (delims[ch + 1]) {
                        break;
                    }
                    if (ch == '#') {
                        ch = (getHex(file.read()) << 4) + getHex(file.read());
                    }
                    outBuf.append((char) ch);
                }
                backOnePosition(ch);
                break;
            }
            case '>':
                ch = file.read();
                if (ch != '>') {
                    throwError(MessageLocalization.getComposedMessage("greaterthan.not.expected"));
                }
                type = TokenType.END_DIC;
                break;
            case '<': {
                int v1 = file.read();
                if (v1 == '<') {
                    type = TokenType.START_DIC;
                    break;
                }
                outBuf.setLength(0);
                type = TokenType.STRING;
                hexString = true;
                int v2 = 0;
                while (true) {
                    while (isWhitespace(v1)) {
                        v1 = file.read();
                    }
                    if (v1 == '>') {
                        break;
                    }
                    v1 = getHex(v1);
                    if (v1 < 0) {
                        break;
                    }
                    v2 = file.read();
                    while (isWhitespace(v2)) {
                        v2 = file.read();
                    }
                    if (v2 == '>') {
                        ch = v1 << 4;
                        outBuf.append((char) ch);
                        break;
                    }
                    v2 = getHex(v2);
                    if (v2 < 0) {
                        break;
                    }
                    ch = (v1 << 4) + v2;
                    outBuf.append((char) ch);
                    v1 = file.read();
                }
                if (v1 < 0 || v2 < 0) {
                    throwError(MessageLocalization.getComposedMessage("error.reading.string"));
                }
                break;
            }
            case '%':
                type = TokenType.COMMENT;
                do {
                    ch = file.read();
                } while (ch != -1 && ch != '\r' && ch != '\n');
                break;
            case '(': {
                outBuf.setLength(0);
                type = TokenType.STRING;
                hexString = false;
                int nesting = 0;
                while (true) {
                    ch = file.read();
                    if (ch == -1) {
                        break;
                    }
                    if (ch == '(') {
                        ++nesting;
                    } else if (ch == ')') {
                        --nesting;
                    } else if (ch == '\\') {
                        boolean lineBreak = false;
                        ch = file.read();
                        switch (ch) {
                            case 'n':
                                ch = '\n';
                                break;
                            case 'r':
                                ch = '\r';
                                break;
                            case 't':
                                ch = '\t';
                                break;
                            case 'b':
                                ch = '\b';
                                break;
                            case 'f':
                                ch = '\f';
                                break;
                            case '(':
                            case ')':
                            case '\\':
                                break;
                            case '\r':
                                lineBreak = true;
                                ch = file.read();
                                if (ch != '\n') {
                                    backOnePosition(ch);
                                }
                                break;
                            case '\n':
                                lineBreak = true;
                                break;
                            default: {
                                if (ch < '0' || ch > '7') {
                                    break;
                                }
                                int octal = ch - '0';
                                ch = file.read();
                                if (ch < '0' || ch > '7') {
                                    backOnePosition(ch);
                                    ch = octal;
                                    break;
                                }
                                octal = (octal << 3) + ch - '0';
                                ch = file.read();
                                if (ch < '0' || ch > '7') {
                                    backOnePosition(ch);
                                    ch = octal;
                                    break;
                                }
                                octal = (octal << 3) + ch - '0';
                                ch = octal & 0xff;
                                break;
                            }
                        }
                        if (lineBreak) {
                            continue;
                        }
                        if (ch < 0) {
                            break;
                        }
                    } else if (ch == '\r') {
                        ch = file.read();
                        if (ch < 0) {
                            break;
                        }
                        if (ch != '\n') {
                            backOnePosition(ch);
                            ch = '\n';
                        }
                    }
                    if (nesting == -1) {
                        break;
                    }
                    outBuf.append((char) ch);
                }
                if (ch == -1) {
                    throwError(MessageLocalization.getComposedMessage("error.reading.string"));
                }
                break;
            }
            default: {
                outBuf.setLength(0);
                if (ch == '-' || ch == '+' || ch == '.' || (ch >= '0' && ch <= '9')) {
                    type = TokenType.NUMBER;
                    boolean isReal = false;
                    int numberOfMinuses = 0;
                    if (ch == '-') {
                        // Take care of number like "--234". If Acrobat can read them so must we.
                        do {
                            ++numberOfMinuses;
                            ch = file.read();
                        } while (ch == '-');
                        outBuf.append('-');
                    } else {
                        outBuf.append((char) ch);
                        // We don't need to check if the number is real over here
                        // as we need to know that fact only in case if there are any minuses.
                        ch = file.read();
                    }
                    while (ch != -1 && ((ch >= '0' && ch <= '9') || ch == '.')) {
                        if (ch == '.') {
                            isReal = true;
                        }
                        outBuf.append((char) ch);
                        ch = file.read();
                    }
                    if (numberOfMinuses > 1 && !isReal) {
                        // Numbers of integer type and with more than one minus before them
                        // are interpreted by Acrobat as zero.
                        outBuf.setLength(0);
                        outBuf.append('0');
                    }
                } else {
                    type = TokenType.OTHER;
                    do {
                        outBuf.append((char) ch);
                        ch = file.read();
                    } while (!delims[ch + 1]);
                }
                if (ch != -1) {
                    backOnePosition(ch);
                }
                break;
            }
        }
        if (outBuf != null) {
            stringValue = outBuf.toString();
        }
        return true;
    }

    public long longValue() {
        return Long.parseLong(this.stringValue);
    }

    public int intValue() {
        return Integer.parseInt(this.stringValue);
    }

    public boolean readLineSegment(byte[] input) throws IOException {
        return readLineSegment(input, true);
    }

    public boolean readLineSegment(byte[] input, boolean isNullWhitespace) throws IOException {
        int c = -1;
        boolean eol = false;
        int ptr = 0;
        int len = input.length;

        if (ptr < len) {
            while (isWhitespace(c = read(), isNullWhitespace));
        }
        while (!eol && ptr < len) {
            long cur;
            switch (c) {
                case -1:
                case 10:
                    eol = true;
                    break;
                case 13:
                    eol = true;
                    cur = getFilePointer();
                    if (read() != 10) {
                        seek(cur);
                    }
                    break;
                default:
                    input[ptr++] = (byte) c;
                    break;
            }

            if (eol || len <= ptr) {
                break;
            }
            c = read();
        }

        if (ptr >= len) {
            eol = false;
            while (!eol) {
                long cur;
                switch (c = read()) {
                    case -1:
                    case 10:
                        eol = true;

                    case 13:
                        eol = true;
                        cur = getFilePointer();
                        if (read() != 10) {
                            seek(cur);
                        }
                }

            }
        }
        if (c == -1 && ptr == 0) {
            return false;
        }
        if (ptr + 2 <= len) {
            input[ptr++] = 32;
            input[ptr] = 88;
        }
        return true;
    }

    public static long[] checkObjectStart(byte[] line) {
        try {
            PRTokeniser tk = new PRTokeniser(new RandomAccessFileOrArray((new RandomAccessSourceFactory()).createSource(line)));
            int num = 0;
            int gen = 0;
            if (!tk.nextToken() || tk.getTokenType() != TokenType.NUMBER) {
                return null;
            }
            num = tk.intValue();
            if (!tk.nextToken() || tk.getTokenType() != TokenType.NUMBER) {
                return null;
            }
            gen = tk.intValue();
            if (!tk.nextToken()) {
                return null;
            }
            if (!tk.getStringValue().equals("obj")) {
                return null;
            }
            return new long[]{num, gen};
        } catch (Exception exception) {

            return null;
        }
    }

    public boolean isHexString() {
        return this.hexString;
    }
}
