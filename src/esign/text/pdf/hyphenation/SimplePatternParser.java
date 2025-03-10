package esign.text.pdf.hyphenation;

import esign.text.ExceptionConverter;
import esign.text.xml.simpleparser.SimpleXMLDocHandler;
import esign.text.xml.simpleparser.SimpleXMLParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

public class SimplePatternParser
        implements SimpleXMLDocHandler, PatternConsumer {

    int currElement;
    PatternConsumer consumer;
    StringBuffer token = new StringBuffer();
    ArrayList<Object> exception;
    char hyphenChar = '-';
    SimpleXMLParser parser;
    static final int ELEM_CLASSES = 1;

    public void parse(InputStream stream, PatternConsumer consumer) {
        this.consumer = consumer;
        try {
            SimpleXMLParser.parse(this, stream);
        } catch (IOException e) {
            throw new ExceptionConverter(e);
        } finally {
            try {
                stream.close();
            } catch (Exception exception) {
            }
        }
    }
    static final int ELEM_EXCEPTIONS = 2;
    static final int ELEM_PATTERNS = 3;
    static final int ELEM_HYPHEN = 4;

    protected static String getPattern(String word) {
        StringBuffer pat = new StringBuffer();
        int len = word.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isDigit(word.charAt(i))) {
                pat.append(word.charAt(i));
            }
        }
        return pat.toString();
    }

    protected ArrayList<Object> normalizeException(ArrayList<Object> ex) {
        ArrayList<Object> res = new ArrayList();
        for (int i = 0; i < ex.size(); i++) {
            Object item = ex.get(i);
            if (item instanceof String) {
                String str = (String) item;
                StringBuffer buf = new StringBuffer();
                for (int j = 0; j < str.length(); j++) {
                    char c = str.charAt(j);
                    if (c != this.hyphenChar) {
                        buf.append(c);
                    } else {
                        res.add(buf.toString());
                        buf.setLength(0);
                        char[] h = new char[1];
                        h[0] = this.hyphenChar;

                        res.add(new Hyphen(new String(h), null, null));
                    }
                }
                if (buf.length() > 0) {
                    res.add(buf.toString());
                }
            } else {
                res.add(item);
            }
        }
        return res;
    }

    protected String getExceptionWord(ArrayList<Object> ex) {
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < ex.size(); i++) {
            Object item = ex.get(i);
            if (item instanceof String) {
                res.append((String) item);
            } else if (((Hyphen) item).noBreak != null) {
                res.append(((Hyphen) item).noBreak);
            }
        }

        return res.toString();
    }

    protected static String getInterletterValues(String pat) {
        StringBuffer il = new StringBuffer();
        String word = pat + "a";
        int len = word.length();
        for (int i = 0; i < len; i++) {
            char c = word.charAt(i);
            if (Character.isDigit(c)) {
                il.append(c);
                i++;
            } else {
                il.append('0');
            }
        }
        return il.toString();
    }

    public void endDocument() {
    }

    public void endElement(String tag) {
        if (this.token.length() > 0) {
            String word = this.token.toString();
            switch (this.currElement) {
                case 1:
                    this.consumer.addClass(word);
                    break;
                case 2:
                    this.exception.add(word);
                    this.exception = normalizeException(this.exception);
                    this.consumer.addException(getExceptionWord(this.exception), (ArrayList<Object>) this.exception
                            .clone());
                    break;
                case 3:
                    this.consumer.addPattern(getPattern(word),
                            getInterletterValues(word));
                    break;
            }

            if (this.currElement != 4) {
                this.token.setLength(0);
            }
        }
        if (this.currElement == 4) {
            this.currElement = 2;
        } else {
            this.currElement = 0;
        }
    }

    public void startDocument() {
    }

    public void startElement(String tag, Map<String, String> h) {
        if (tag.equals("hyphen-char")) {
            String hh = h.get("value");
            if (hh != null && hh.length() == 1) {
                this.hyphenChar = hh.charAt(0);
            }
        } else if (tag.equals("classes")) {
            this.currElement = 1;
        } else if (tag.equals("patterns")) {
            this.currElement = 3;
        } else if (tag.equals("exceptions")) {
            this.currElement = 2;
            this.exception = new ArrayList();
        } else if (tag.equals("hyphen")) {
            if (this.token.length() > 0) {
                this.exception.add(this.token.toString());
            }
            this.exception.add(new Hyphen(h.get("pre"), h
                    .get("no"), h.get("post")));
            this.currElement = 4;
        }
        this.token.setLength(0);
    }

    public void text(String str) {
        StringTokenizer tk = new StringTokenizer(str);
        while (tk.hasMoreTokens()) {
            String word = tk.nextToken();

            switch (this.currElement) {
                case 1:
                    this.consumer.addClass(word);

                case 2:
                    this.exception.add(word);
                    this.exception = normalizeException(this.exception);
                    this.consumer.addException(getExceptionWord(this.exception), (ArrayList<Object>) this.exception
                            .clone());
                    this.exception.clear();

                case 3:
                    this.consumer.addPattern(getPattern(word),
                            getInterletterValues(word));
            }
        }
    }

    public void addClass(String c) {
        System.out.println("class: " + c);
    }

    public void addException(String w, ArrayList<Object> e) {
        System.out.println("exception: " + w + " : " + e.toString());
    }

    public void addPattern(String p, String v) {
        System.out.println("pattern: " + p + " : " + v);
    }
}
