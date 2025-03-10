package esign.text.xml;

import esign.text.xml.simpleparser.SimpleXMLDocHandler;
import esign.text.xml.simpleparser.SimpleXMLParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class XmlToTxt
        implements SimpleXMLDocHandler {

    protected StringBuffer buf;

    public static String parse(InputStream is) throws IOException {
        XmlToTxt handler = new XmlToTxt();
        SimpleXMLParser.parse(handler, null, new InputStreamReader(is), true);
        return handler.toString();
    }

    protected XmlToTxt() {
        this.buf = new StringBuffer();
    }

    public String toString() {
        return this.buf.toString();
    }

    public void startElement(String tag, Map<String, String> h) {
    }

    public void endElement(String tag) {
    }

    public void startDocument() {
    }

    public void endDocument() {
    }

    public void text(String str) {
        this.buf.append(str);
    }
}

