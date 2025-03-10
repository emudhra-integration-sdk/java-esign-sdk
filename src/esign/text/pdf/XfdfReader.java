package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;
import esign.text.xml.simpleparser.SimpleXMLDocHandler;
import esign.text.xml.simpleparser.SimpleXMLParser;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class XfdfReader
        implements SimpleXMLDocHandler {

    private boolean foundRoot = false;
    private final Stack<String> fieldNames = new Stack<String>();
    private final Stack<String> fieldValues = new Stack<String>();

    HashMap<String, String> fields;

    protected HashMap<String, List<String>> listFields;

    String fileSpec;

    public XfdfReader(String filename) throws IOException {
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(filename);
            SimpleXMLParser.parse(this, fin);
        } finally {

            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (Exception exception) {
            }
        }
    }

    public XfdfReader(byte[] xfdfIn) throws IOException {
        this(new ByteArrayInputStream(xfdfIn));
    }

    public XfdfReader(InputStream is) throws IOException {
        SimpleXMLParser.parse(this, is);
    }

    public HashMap<String, String> getFields() {
        return this.fields;
    }

    public String getField(String name) {
        return this.fields.get(name);
    }

    public String getFieldValue(String name) {
        String field = this.fields.get(name);
        if (field == null) {
            return null;
        }
        return field;
    }

    public List<String> getListValues(String name) {
        return this.listFields.get(name);
    }

    public String getFileSpec() {
        return this.fileSpec;
    }

    public void startElement(String tag, Map<String, String> h) {
        if (!this.foundRoot) {
            if (!tag.equals("xfdf")) {
                throw new RuntimeException(MessageLocalization.getComposedMessage("root.element.is.not.xfdf.1", new Object[]{tag}));
            }
            this.foundRoot = true;
        }

        if (!tag.equals("xfdf")) {
            if (tag.equals("f")) {
                this.fileSpec = h.get("href");
            } else if (tag.equals("fields")) {
                this.fields = new HashMap<String, String>();
                this.listFields = new HashMap<String, List<String>>();
            } else if (tag.equals("field")) {
                String fName = h.get("name");
                this.fieldNames.push(fName);
            } else if (tag.equals("value")) {
                this.fieldValues.push("");
            }
        }
    }

    public void endElement(String tag) {
        if (tag.equals("value")) {
            String fName = "";
            for (int k = 0; k < this.fieldNames.size(); k++) {
                fName = fName + "." + (String) this.fieldNames.elementAt(k);
            }
            if (fName.startsWith(".")) {
                fName = fName.substring(1);
            }
            String fVal = this.fieldValues.pop();
            String old = this.fields.put(fName, fVal);
            if (old != null) {
                List<String> l = this.listFields.get(fName);
                if (l == null) {
                    l = new ArrayList<String>();
                    l.add(old);
                }
                l.add(fVal);
                this.listFields.put(fName, l);
            }

        } else if (tag.equals("field")
                && !this.fieldNames.isEmpty()) {
            this.fieldNames.pop();
        }
    }

    public void startDocument() {
        this.fileSpec = "";
    }

    public void endDocument() {
    }

    public void text(String str) {
        if (this.fieldNames.isEmpty() || this.fieldValues.isEmpty()) {
            return;
        }
        String val = this.fieldValues.pop();
        val = val + str;
        this.fieldValues.push(val);
    }
}
