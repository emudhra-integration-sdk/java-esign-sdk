package esign.text.xml.simpleparser;

import esign.text.error_messages.MessageLocalization;
import esign.text.xml.XMLUtil;
import esign.text.xml.simpleparser.handler.HTMLNewLineHandler;
import esign.text.xml.simpleparser.handler.NeverNewLineHandler;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Stack;

public final class SimpleXMLParser {

    private static final int UNKNOWN = 0;
    private static final int TEXT = 1;
    private static final int TAG_ENCOUNTERED = 2;
    private static final int EXAMIN_TAG = 3;
    private static final int TAG_EXAMINED = 4;
    private static final int IN_CLOSETAG = 5;
    private static final int SINGLE_TAG = 6;
    private static final int CDATA = 7;
    private static final int COMMENT = 8;
    private static final int PI = 9;
    private static final int ENTITY = 10;
    private static final int QUOTE = 11;
    private static final int ATTRIBUTE_KEY = 12;
    private static final int ATTRIBUTE_EQUAL = 13;
    private static final int ATTRIBUTE_VALUE = 14;
    private final Stack<Integer> stack;
    private int character = 0;

    private int previousCharacter = -1;

    private int lines = 1;

    private int columns = 0;

    private boolean eol = false;

    private boolean nowhite = false;

    private int state;

    private final boolean html;

    private final StringBuffer text = new StringBuffer();

    private final StringBuffer entity = new StringBuffer();

    private String tag = null;

    private HashMap<String, String> attributes = null;

    private final SimpleXMLDocHandler doc;

    private final SimpleXMLDocHandlerComment comment;

    private int nested = 0;

    private int quoteCharacter = 34;

    private String attributekey = null;

    private String attributevalue = null;

    private NewLineHandler newLineHandler;

    private SimpleXMLParser(SimpleXMLDocHandler doc, SimpleXMLDocHandlerComment comment, boolean html) {
        this.doc = doc;
        this.comment = comment;
        this.html = html;
        if (html) {
            this.newLineHandler = (NewLineHandler) new HTMLNewLineHandler();
        } else {
            this.newLineHandler = (NewLineHandler) new NeverNewLineHandler();
        }
        this.stack = new Stack<Integer>();
        this.state = html ? 1 : 0;
    }

    private void go(Reader r) throws IOException {
        BufferedReader reader;
        if (r instanceof BufferedReader) {
            reader = (BufferedReader) r;
        } else {
            reader = new BufferedReader(r);
        }
        this.doc.startDocument();

        while (true) {
            if (this.previousCharacter == -1) {
                this.character = reader.read();
            } else {

                this.character = this.previousCharacter;
                this.previousCharacter = -1;
            }

            if (this.character == -1) {
                if (this.html) {
                    if (this.html && this.state == 1) {
                        flush();
                    }
                    this.doc.endDocument();
                } else {
                    throwException(MessageLocalization.getComposedMessage("missing.end.tag", new Object[0]));
                }

                return;
            }

            if (this.character == 10 && this.eol) {
                this.eol = false;
                continue;
            }
            if (this.eol) {
                this.eol = false;
            } else if (this.character == 10) {
                this.lines++;
                this.columns = 0;
            } else if (this.character == 13) {
                this.eol = true;
                this.character = 10;
                this.lines++;
                this.columns = 0;
            } else {
                this.columns++;
            }

            switch (this.state) {

                case 0:
                    if (this.character == 60) {
                        saveState(1);
                        this.state = 2;
                    }

                case 1:
                    if (this.character == 60) {
                        flush();
                        saveState(this.state);
                        this.state = 2;
                        continue;
                    }
                    if (this.character == 38) {
                        saveState(this.state);
                        this.entity.setLength(0);
                        this.state = 10;
                        this.nowhite = true;
                        continue;
                    }
                    if (this.character == 32) {
                        if (this.html && this.nowhite) {
                            this.text.append(' ');
                            this.nowhite = false;
                            continue;
                        }
                        if (this.nowhite) {
                            this.text.append((char) this.character);
                        }
                        this.nowhite = false;
                        continue;
                    }
                    if (Character.isWhitespace((char) this.character)) {
                        if (this.html) {
                            continue;
                        }
                        if (this.nowhite) {
                            this.text.append((char) this.character);
                        }
                        this.nowhite = false;
                        continue;
                    }
                    this.text.append((char) this.character);
                    this.nowhite = true;

                case 2:
                    initTag();
                    if (this.character == 47) {
                        this.state = 5;
                        continue;
                    }
                    if (this.character == 63) {
                        restoreState();
                        this.state = 9;
                        continue;
                    }
                    this.text.append((char) this.character);
                    this.state = 3;

                case 3:
                    if (this.character == 62) {
                        doTag();
                        processTag(true);
                        initTag();
                        this.state = restoreState();
                        continue;
                    }
                    if (this.character == 47) {
                        this.state = 6;
                        continue;
                    }
                    if (this.character == 45 && this.text.toString().equals("!-")) {
                        flush();
                        this.state = 8;
                        continue;
                    }
                    if (this.character == 91 && this.text.toString().equals("![CDATA")) {
                        flush();
                        this.state = 7;
                        continue;
                    }
                    if (this.character == 69 && this.text.toString().equals("!DOCTYP")) {
                        flush();
                        this.state = 9;
                        continue;
                    }
                    if (Character.isWhitespace((char) this.character)) {
                        doTag();
                        this.state = 4;
                        continue;
                    }
                    this.text.append((char) this.character);

                case 4:
                    if (this.character == 62) {
                        processTag(true);
                        initTag();
                        this.state = restoreState();
                        continue;
                    }
                    if (this.character == 47) {
                        this.state = 6;
                        continue;
                    }
                    if (Character.isWhitespace((char) this.character)) {
                        continue;
                    }
                    this.text.append((char) this.character);
                    this.state = 12;

                case 5:
                    if (this.character == 62) {
                        doTag();
                        processTag(false);
                        if (!this.html && this.nested == 0) {
                            return;
                        }
                        this.state = restoreState();
                        continue;
                    }
                    if (!Character.isWhitespace((char) this.character)) {
                        this.text.append((char) this.character);
                    }

                case 6:
                    if (this.character != 62) {
                        throwException(MessageLocalization.getComposedMessage("expected.gt.for.tag.lt.1.gt", new Object[]{this.tag}));
                    }
                    doTag();
                    processTag(true);
                    processTag(false);
                    initTag();
                    if (!this.html && this.nested == 0) {
                        this.doc.endDocument();
                        return;
                    }
                    this.state = restoreState();

                case 7:
                    if (this.character == 62 && this.text
                            .toString().endsWith("]]")) {
                        this.text.setLength(this.text.length() - 2);
                        flush();
                        this.state = restoreState();
                        continue;
                    }
                    this.text.append((char) this.character);

                case 8:
                    if (this.character == 62 && this.text
                            .toString().endsWith("--")) {
                        this.text.setLength(this.text.length() - 2);
                        flush();
                        this.state = restoreState();
                        continue;
                    }
                    this.text.append((char) this.character);

                case 9:
                    if (this.character == 62) {
                        this.state = restoreState();
                        if (this.state == 1) {
                            this.state = 0;
                        }

                    }

                case 10:
                    if (this.character == 59) {
                        this.state = restoreState();
                        String cent = this.entity.toString();
                        this.entity.setLength(0);
                        char ce = EntitiesToUnicode.decodeEntity(cent);
                        if (ce == '\000') {
                            this.text.append('&').append(cent).append(';');
                            continue;
                        }
                        this.text.append(ce);
                        continue;
                    }
                    if ((this.character != 35 && (this.character < 48 || this.character > 57) && (this.character < 97 || this.character > 122) && (this.character < 65 || this.character > 90)) || this.entity
                            .length() >= 7) {
                        this.state = restoreState();
                        this.previousCharacter = this.character;
                        this.text.append('&').append(this.entity.toString());
                        this.entity.setLength(0);
                        continue;
                    }
                    this.entity.append((char) this.character);

                case 11:
                    if (this.html && this.quoteCharacter == 32 && this.character == 62) {
                        flush();
                        processTag(true);
                        initTag();
                        this.state = restoreState();
                        continue;
                    }
                    if (this.html && this.quoteCharacter == 32 && Character.isWhitespace((char) this.character)) {
                        flush();
                        this.state = 4;
                        continue;
                    }
                    if (this.html && this.quoteCharacter == 32) {
                        this.text.append((char) this.character);
                        continue;
                    }
                    if (this.character == this.quoteCharacter) {
                        flush();
                        this.state = 4;
                        continue;
                    }
                    if (" \r\n\t".indexOf(this.character) >= 0) {
                        this.text.append(' ');
                        continue;
                    }
                    if (this.character == 38) {
                        saveState(this.state);
                        this.state = 10;
                        this.entity.setLength(0);
                        continue;
                    }
                    this.text.append((char) this.character);

                case 12:
                    if (Character.isWhitespace((char) this.character)) {
                        flush();
                        this.state = 13;
                        continue;
                    }
                    if (this.character == 61) {
                        flush();
                        this.state = 14;
                        continue;
                    }
                    if (this.html && this.character == 62) {
                        this.text.setLength(0);
                        processTag(true);
                        initTag();
                        this.state = restoreState();
                        continue;
                    }
                    this.text.append((char) this.character);

                case 13:
                    if (this.character == 61) {
                        this.state = 14;
                        continue;
                    }
                    if (Character.isWhitespace((char) this.character)) {
                        continue;
                    }
                    if (this.html && this.character == 62) {
                        this.text.setLength(0);
                        processTag(true);
                        initTag();
                        this.state = restoreState();
                        continue;
                    }
                    if (this.html && this.character == 47) {
                        flush();
                        this.state = 6;
                        continue;
                    }
                    if (this.html) {
                        flush();
                        this.text.append((char) this.character);
                        this.state = 12;
                        continue;
                    }
                    throwException(MessageLocalization.getComposedMessage("error.in.attribute.processing", new Object[0]));

                case 14:
                    if (this.character == 34 || this.character == 39) {
                        this.quoteCharacter = this.character;
                        this.state = 11;
                        continue;
                    }
                    if (Character.isWhitespace((char) this.character)) {
                        continue;
                    }
                    if (this.html && this.character == 62) {
                        flush();
                        processTag(true);
                        initTag();
                        this.state = restoreState();
                        continue;
                    }
                    if (this.html) {
                        this.text.append((char) this.character);
                        this.quoteCharacter = 32;
                        this.state = 11;
                        continue;
                    }
                    throwException(MessageLocalization.getComposedMessage("error.in.attribute.processing", new Object[0]));
            }
        }
    }

    private int restoreState() {
        if (!this.stack.empty()) {
            return ((Integer) this.stack.pop()).intValue();
        }
        return 0;
    }

    private void saveState(int s) {
        this.stack.push(Integer.valueOf(s));
    }

    private void flush() {
        switch (this.state) {
            case 1:
            case 7:
                if (this.text.length() > 0) {
                    this.doc.text(this.text.toString());
                }
                break;
            case 8:
                if (this.comment != null) {
                    this.comment.comment(this.text.toString());
                }
                break;
            case 12:
                this.attributekey = this.text.toString();
                if (this.html) {
                    this.attributekey = this.attributekey.toLowerCase();
                }
                break;
            case 11:
            case 14:
                this.attributevalue = this.text.toString();
                this.attributes.put(this.attributekey, this.attributevalue);
                break;
        }

        this.text.setLength(0);
    }

    private void initTag() {
        this.tag = null;
        this.attributes = new HashMap<String, String>();
    }

    private void doTag() {
        if (this.tag == null) {
            this.tag = this.text.toString();
        }
        if (this.html) {
            this.tag = this.tag.toLowerCase();
        }
        this.text.setLength(0);
    }

    private void processTag(boolean start) {
        if (start) {
            this.nested++;
            this.doc.startElement(this.tag, this.attributes);
        } else {

            if (this.newLineHandler.isNewLineTag(this.tag)) {
                this.nowhite = false;
            }
            this.nested--;
            this.doc.endElement(this.tag);
        }
    }

    private void throwException(String s) throws IOException {
        throw new IOException(MessageLocalization.getComposedMessage("1.near.line.2.column.3", new Object[]{s, String.valueOf(this.lines), String.valueOf(this.columns)}));
    }

    public static void parse(SimpleXMLDocHandler doc, SimpleXMLDocHandlerComment comment, Reader r, boolean html) throws IOException {
        SimpleXMLParser parser = new SimpleXMLParser(doc, comment, html);
        parser.go(r);
    }

    public static void parse(SimpleXMLDocHandler doc, InputStream in) throws IOException {
        byte[] b4 = new byte[4];
        int count = in.read(b4);
        if (count != 4) {
            throw new IOException(MessageLocalization.getComposedMessage("insufficient.length", new Object[0]));
        }
        String encoding = XMLUtil.getEncodingName(b4);
        String decl = null;
        if (encoding.equals("UTF-8")) {
            StringBuffer sb = new StringBuffer();
            int c;
            while ((c = in.read()) != -1
                    && c != 62) {
                sb.append((char) c);
            }
            decl = sb.toString();
        } else if (encoding.equals("CP037")) {
            ByteArrayOutputStream bi = new ByteArrayOutputStream();
            int c;
            while ((c = in.read()) != -1
                    && c != 110) {
                bi.write(c);
            }
            decl = new String(bi.toByteArray(), "CP037");
        }
        if (decl != null) {
            decl = getDeclaredEncoding(decl);
            if (decl != null) {
                encoding = decl;
            }
        }
        parse(doc, new InputStreamReader(in, IanaEncodings.getJavaEncoding(encoding)));
    }

    private static String getDeclaredEncoding(String decl) {
        if (decl == null) {
            return null;
        }
        int idx = decl.indexOf("encoding");
        if (idx < 0) {
            return null;
        }
        int idx1 = decl.indexOf('"', idx);
        int idx2 = decl.indexOf('\'', idx);
        if (idx1 == idx2) {
            return null;
        }
        if ((idx1 < 0 && idx2 > 0) || (idx2 > 0 && idx2 < idx1)) {
            int idx3 = decl.indexOf('\'', idx2 + 1);
            if (idx3 < 0) {
                return null;
            }
            return decl.substring(idx2 + 1, idx3);
        }
        if ((idx2 < 0 && idx1 > 0) || (idx1 > 0 && idx1 < idx2)) {
            int idx3 = decl.indexOf('"', idx1 + 1);
            if (idx3 < 0) {
                return null;
            }
            return decl.substring(idx1 + 1, idx3);
        }
        return null;
    }

    public static void parse(SimpleXMLDocHandler doc, Reader r) throws IOException {
        parse(doc, null, r, false);
    }

    @Deprecated
    public static String escapeXML(String s, boolean onlyASCII) {
        return XMLUtil.escapeXML(s, onlyASCII);
    }
}

