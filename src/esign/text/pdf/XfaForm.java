package esign.text.pdf;

import esign.text.ExceptionConverter;
import esign.text.xml.XmlDomWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XfaForm {

    private Xml2SomTemplate templateSom;
    private Node templateNode;
    private Xml2SomDatasets datasetsSom;
    private Node datasetsNode;
    private AcroFieldsSearch acroFieldsSom;
    private PdfReader reader;
    private boolean xfaPresent;
    private Document domDocument;
    private boolean changed;
    public static final String XFA_DATA_SCHEMA = "http://www.xfa.org/schema/xfa-data/1.0/";

    public XfaForm() {
    }

    public static PdfObject getXfaObject(PdfReader reader) {
        PdfDictionary af = (PdfDictionary) PdfReader.getPdfObjectRelease(reader.getCatalog().get(PdfName.ACROFORM));
        if (af == null) {
            return null;
        }
        return PdfReader.getPdfObjectRelease(af.get(PdfName.XFA));
    }

    public XfaForm(PdfReader reader) throws IOException, ParserConfigurationException, SAXException {
        this.reader = reader;
        PdfObject xfa = getXfaObject(reader);
        if (xfa == null) {
            this.xfaPresent = false;
            return;
        }
        this.xfaPresent = true;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (xfa.isArray()) {
            PdfArray ar = (PdfArray) xfa;
            for (int k = 1; k < ar.size(); k += 2) {
                PdfObject ob = ar.getDirectObject(k);
                if (ob instanceof PRStream) {
                    byte[] b = PdfReader.getStreamBytes((PRStream) ob);
                    bout.write(b);
                }

            }
        } else if (xfa instanceof PRStream) {
            byte[] b = PdfReader.getStreamBytes((PRStream) xfa);
            bout.write(b);
        }
        bout.close();
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        fact.setNamespaceAware(true);
        DocumentBuilder db = fact.newDocumentBuilder();
        this.domDocument = db.parse(new ByteArrayInputStream(bout.toByteArray()));
        extractNodes();
    }

    private void extractNodes() {
        Map<String, Node> xfaNodes = extractXFANodes(this.domDocument);

        if (xfaNodes.containsKey("template")) {
            this.templateNode = xfaNodes.get("template");
            this.templateSom = new Xml2SomTemplate(this.templateNode);
        }
        if (xfaNodes.containsKey("datasets")) {
            this.datasetsNode = xfaNodes.get("datasets");
            Node dataNode = findDataNode(this.datasetsNode);
            this.datasetsSom = new Xml2SomDatasets((dataNode != null) ? dataNode : this.datasetsNode.getFirstChild());
        }
        if (this.datasetsNode == null) {
            createDatasetsNode(this.domDocument.getFirstChild());
        }
    }

    private Node findDataNode(Node datasetsNode) {
        NodeList childNodes = datasetsNode.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeName().equals("xfa:data")) {
                return childNodes.item(i);
            }
        }
        return null;
    }

    public static Map<String, Node> extractXFANodes(Document domDocument) {
        Map<String, Node> xfaNodes = new HashMap<String, Node>();
        Node n = domDocument.getFirstChild();
        while (n.getChildNodes().getLength() == 0) {
            n = n.getNextSibling();
        }
        n = n.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == 1) {
                String s = n.getLocalName();
                xfaNodes.put(s, n);
            }
            n = n.getNextSibling();
        }

        return xfaNodes;
    }

    private void createDatasetsNode(Node n) {
        while (n.getChildNodes().getLength() == 0) {
            n = n.getNextSibling();
        }
        if (n != null) {
            Element e = n.getOwnerDocument().createElement("xfa:datasets");
            e.setAttribute("xmlns:xfa", "http://www.xfa.org/schema/xfa-data/1.0/");
            this.datasetsNode = e;
            n.appendChild(this.datasetsNode);
        }
    }

    public static void setXfa(XfaForm form, PdfReader reader, PdfWriter writer) throws IOException {
        PdfDictionary af = (PdfDictionary) PdfReader.getPdfObjectRelease(reader.getCatalog().get(PdfName.ACROFORM));
        if (af == null) {
            return;
        }
        PdfObject xfa = getXfaObject(reader);
        if (xfa.isArray()) {
            PdfArray ar = (PdfArray) xfa;
            int t = -1;
            int d = -1;
            for (int k = 0; k < ar.size(); k += 2) {
                PdfString s = ar.getAsString(k);
                if ("template".equals(s.toString())) {
                    t = k + 1;
                }
                if ("datasets".equals(s.toString())) {
                    d = k + 1;
                }
            }
            if (t > -1 && d > -1) {
                reader.killXref(ar.getAsIndirectObject(t));
                reader.killXref(ar.getAsIndirectObject(d));
                PdfStream tStream = new PdfStream(serializeDoc(form.templateNode));
                tStream.flateCompress(writer.getCompressionLevel());
                ar.set(t, writer.addToBody(tStream).getIndirectReference());
                PdfStream dStream = new PdfStream(serializeDoc(form.datasetsNode));
                dStream.flateCompress(writer.getCompressionLevel());
                ar.set(d, writer.addToBody(dStream).getIndirectReference());
                af.put(PdfName.XFA, new PdfArray(ar));
                return;
            }
        }
        reader.killXref(af.get(PdfName.XFA));
        PdfStream str = new PdfStream(serializeDoc(form.domDocument));
        str.flateCompress(writer.getCompressionLevel());
        PdfIndirectReference ref = writer.addToBody(str).getIndirectReference();
        af.put(PdfName.XFA, ref);
    }

    public void setXfa(PdfWriter writer) throws IOException {
        setXfa(this, this.reader, writer);
    }

    public static byte[] serializeDoc(Node n) throws IOException {
        XmlDomWriter xw = new XmlDomWriter();
        ByteArrayOutputStream fout = new ByteArrayOutputStream();
        xw.setOutput(fout, null);
        xw.setCanonical(false);
        xw.write(n);
        fout.close();
        return fout.toByteArray();
    }

    public boolean isXfaPresent() {
        return this.xfaPresent;
    }

    public Document getDomDocument() {
        return this.domDocument;
    }

    public String findFieldName(String name, AcroFields af) {
        Map<String, AcroFields.Item> items = af.getFields();
        if (items.containsKey(name)) {
            return name;
        }
        if (this.acroFieldsSom == null) {
            if (items.isEmpty() && this.xfaPresent) {
                this.acroFieldsSom = new AcroFieldsSearch(this.datasetsSom.getName2Node().keySet());
            } else {
                this.acroFieldsSom = new AcroFieldsSearch(items.keySet());
            }
        }
        if (this.acroFieldsSom.getAcroShort2LongName().containsKey(name)) {
            return this.acroFieldsSom.getAcroShort2LongName().get(name);
        }
        return this.acroFieldsSom.inverseSearchGlobal(Xml2Som.splitParts(name));
    }

    public String findDatasetsName(String name) {
        if (this.datasetsSom.getName2Node().containsKey(name)) {
            return name;
        }
        return this.datasetsSom.inverseSearchGlobal(Xml2Som.splitParts(name));
    }

    public Node findDatasetsNode(String name) {
        if (name == null) {
            return null;
        }
        name = findDatasetsName(name);
        if (name == null) {
            return null;
        }
        return this.datasetsSom.getName2Node().get(name);
    }

    public static String getNodeText(Node n) {
        if (n == null) {
            return "";
        }
        return getNodeText(n, "");
    }

    private static String getNodeText(Node n, String name) {
        Node n2 = n.getFirstChild();
        while (n2 != null) {
            if (n2.getNodeType() == 1) {
                name = getNodeText(n2, name);
            } else if (n2.getNodeType() == 3) {
                name = name + n2.getNodeValue();
            }
            n2 = n2.getNextSibling();
        }
        return name;
    }

    public void setNodeText(Node n, String text) {
        if (n == null) {
            return;
        }
        Node nc = null;
        while ((nc = n.getFirstChild()) != null) {
            n.removeChild(nc);
        }
        if (n.getAttributes().getNamedItemNS("http://www.xfa.org/schema/xfa-data/1.0/", "dataNode") != null) {
            n.getAttributes().removeNamedItemNS("http://www.xfa.org/schema/xfa-data/1.0/", "dataNode");
        }
        n.appendChild(this.domDocument.createTextNode(text));
        this.changed = true;
    }

    public void setXfaPresent(boolean xfaPresent) {
        this.xfaPresent = xfaPresent;
    }

    public void setDomDocument(Document domDocument) {
        this.domDocument = domDocument;
        extractNodes();
    }

    public PdfReader getReader() {
        return this.reader;
    }

    public void setReader(PdfReader reader) {
        this.reader = reader;
    }

    public boolean isChanged() {
        return this.changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public static class InverseStore {

        protected ArrayList<String> part = new ArrayList<String>();
        protected ArrayList<Object> follow = new ArrayList();

        public String getDefaultName() {
            InverseStore store = this;
            while (true) {
                Object obj = store.follow.get(0);
                if (obj instanceof String) {
                    return (String) obj;
                }
                store = (InverseStore) obj;
            }
        }

        public boolean isSimilar(String name) {
            int idx = name.indexOf('[');
            name = name.substring(0, idx + 1);
            for (int k = 0; k < this.part.size(); k++) {
                if (((String) this.part.get(k)).startsWith(name)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class Stack2<T>
            extends ArrayList<T> {

        private static final long serialVersionUID = -7451476576174095212L;

        public T peek() {
            if (size() == 0) {
                throw new EmptyStackException();
            }
            return get(size() - 1);
        }

        public T pop() {
            if (size() == 0) {
                throw new EmptyStackException();
            }
            T ret = get(size() - 1);
            remove(size() - 1);
            return ret;
        }

        public T push(T item) {
            add(item);
            return item;
        }

        public boolean empty() {
            return (size() == 0);
        }
    }

    public static class Xml2Som {

        protected ArrayList<String> order;

        protected HashMap<String, Node> name2Node;

        protected HashMap<String, XfaForm.InverseStore> inverseSearch;

        protected XfaForm.Stack2<String> stack;

        protected int anform;

        public static String escapeSom(String s) {
            if (s == null) {
                return "";
            }
            int idx = s.indexOf('.');
            if (idx < 0) {
                return s;
            }
            StringBuffer sb = new StringBuffer();
            int last = 0;
            while (idx >= 0) {
                sb.append(s.substring(last, idx));
                sb.append('\\');
                last = idx;
                idx = s.indexOf('.', idx + 1);
            }
            sb.append(s.substring(last));
            return sb.toString();
        }

        public static String unescapeSom(String s) {
            int idx = s.indexOf('\\');
            if (idx < 0) {
                return s;
            }
            StringBuffer sb = new StringBuffer();
            int last = 0;
            while (idx >= 0) {
                sb.append(s.substring(last, idx));
                last = idx + 1;
                idx = s.indexOf('\\', idx + 1);
            }
            sb.append(s.substring(last));
            return sb.toString();
        }

        protected String printStack() {
            if (this.stack.empty()) {
                return "";
            }
            StringBuffer s = new StringBuffer();
            for (int k = 0; k < this.stack.size(); k++) {
                s.append('.').append(this.stack.get(k));
            }
            return s.substring(1);
        }

        public static String getShortName(String s) {
            int idx = s.indexOf(".#subform[");
            if (idx < 0) {
                return s;
            }
            int last = 0;
            StringBuffer sb = new StringBuffer();
            while (idx >= 0) {
                sb.append(s.substring(last, idx));
                idx = s.indexOf("]", idx + 10);
                if (idx < 0) {
                    return sb.toString();
                }
                last = idx + 1;
                idx = s.indexOf(".#subform[", last);
            }
            sb.append(s.substring(last));
            return sb.toString();
        }

        public void inverseSearchAdd(String unstack) {
            inverseSearchAdd(this.inverseSearch, this.stack, unstack);
        }

        public static void inverseSearchAdd(HashMap<String, XfaForm.InverseStore> inverseSearch, XfaForm.Stack2<String> stack, String unstack) {
            String last = stack.peek();
            XfaForm.InverseStore store = inverseSearch.get(last);
            if (store == null) {
                store = new XfaForm.InverseStore();
                inverseSearch.put(last, store);
            }
            for (int k = stack.size() - 2; k >= 0; k--) {
                XfaForm.InverseStore store2;
                last = stack.get(k);

                int idx = store.part.indexOf(last);
                if (idx < 0) {
                    store.part.add(last);
                    store2 = new XfaForm.InverseStore();
                    store.follow.add(store2);
                } else {

                    store2 = (XfaForm.InverseStore) store.follow.get(idx);
                }
                store = store2;
            }
            store.part.add("");
            store.follow.add(unstack);
        }

        public String inverseSearchGlobal(ArrayList<String> parts) {
            if (parts.isEmpty()) {
                return null;
            }
            XfaForm.InverseStore store = this.inverseSearch.get(parts.get(parts.size() - 1));
            if (store == null) {
                return null;
            }
            for (int k = parts.size() - 2; k >= 0; k--) {
                String part = parts.get(k);
                int idx = store.part.indexOf(part);
                if (idx < 0) {
                    if (store.isSimilar(part)) {
                        return null;
                    }
                    return store.getDefaultName();
                }
                store = (XfaForm.InverseStore) store.follow.get(idx);
            }
            return store.getDefaultName();
        }

        public static XfaForm.Stack2<String> splitParts(String name) {
            while (name.startsWith(".")) {
                name = name.substring(1);
            }
            XfaForm.Stack2<String> parts = new XfaForm.Stack2<String>();
            int last = 0;
            int pos = 0;

            while (true) {
                pos = last;
                while (true) {
                    pos = name.indexOf('.', pos);
                    if (pos < 0) {
                        break;
                    }
                    if (name.charAt(pos - 1) == '\\') {
                        pos++;
                        continue;
                    }
                    break;
                }
                if (pos < 0) {
                    break;
                }
                String str = name.substring(last, pos);
                if (!str.endsWith("]")) {
                    str = str + "[0]";
                }
                parts.add(str);
                last = pos + 1;
            }
            String part = name.substring(last);
            if (!part.endsWith("]")) {
                part = part + "[0]";
            }
            parts.add(part);
            return parts;
        }

        public ArrayList<String> getOrder() {
            return this.order;
        }

        public void setOrder(ArrayList<String> order) {
            this.order = order;
        }

        public HashMap<String, Node> getName2Node() {
            return this.name2Node;
        }

        public void setName2Node(HashMap<String, Node> name2Node) {
            this.name2Node = name2Node;
        }

        public HashMap<String, XfaForm.InverseStore> getInverseSearch() {
            return this.inverseSearch;
        }

        public void setInverseSearch(HashMap<String, XfaForm.InverseStore> inverseSearch) {
            this.inverseSearch = inverseSearch;
        }
    }

    public static class Xml2SomDatasets
            extends Xml2Som {

        public Xml2SomDatasets(Node n) {
            this.order = new ArrayList<String>();
            this.name2Node = new HashMap<String, Node>();
            this.stack = new XfaForm.Stack2<String>();
            this.anform = 0;
            this.inverseSearch = new HashMap<String, XfaForm.InverseStore>();
            processDatasetsInternal(n);
        }

        public Node insertNode(Node n, String shortName) {
            XfaForm.Stack2<String> stack = splitParts(shortName);
            Document doc = n.getOwnerDocument();
            Node n2 = null;
            n = n.getFirstChild();
            while (n.getNodeType() != 1) {
                n = n.getNextSibling();
            }
            for (int k = 0; k < stack.size(); k++) {
                String part = stack.get(k);
                int idx = part.lastIndexOf('[');
                String name = part.substring(0, idx);
                idx = Integer.parseInt(part.substring(idx + 1, part.length() - 1));
                int found = -1;
                for (n2 = n.getFirstChild(); n2 != null; n2 = n2.getNextSibling()) {
                    if (n2.getNodeType() == 1) {
                        String s = escapeSom(n2.getLocalName());

                        found++;
                        if (s.equals(name) && found == idx) {
                            break;
                        }
                    }
                }
                for (; found < idx; found++) {
                    n2 = doc.createElementNS((String) null, name);
                    n2 = n.appendChild(n2);
                    Node attr = doc.createAttributeNS("http://www.xfa.org/schema/xfa-data/1.0/", "dataNode");
                    attr.setNodeValue("dataGroup");
                    n2.getAttributes().setNamedItemNS(attr);
                }
                n = n2;
            }
            inverseSearchAdd(this.inverseSearch, stack, shortName);
            this.name2Node.put(shortName, n2);
            this.order.add(shortName);
            return n2;
        }

        private static boolean hasChildren(Node n) {
            Node dataNodeN = n.getAttributes().getNamedItemNS("http://www.xfa.org/schema/xfa-data/1.0/", "dataNode");
            if (dataNodeN != null) {
                String dataNode = dataNodeN.getNodeValue();
                if ("dataGroup".equals(dataNode)) {
                    return true;
                }
                if ("dataValue".equals(dataNode)) {
                    return false;
                }
            }
            if (!n.hasChildNodes()) {
                return false;
            }
            Node n2 = n.getFirstChild();
            while (n2 != null) {
                if (n2.getNodeType() == 1) {
                    return true;
                }
                n2 = n2.getNextSibling();
            }
            return false;
        }

        private void processDatasetsInternal(Node n) {
            if (n != null) {
                HashMap<String, Integer> ss = new HashMap<String, Integer>();
                Node n2 = n.getFirstChild();
                while (n2 != null) {
                    if (n2.getNodeType() == 1) {
                        String s = escapeSom(n2.getLocalName());
                        Integer i = ss.get(s);
                        if (i == null) {
                            i = Integer.valueOf(0);
                        } else {
                            i = Integer.valueOf(i.intValue() + 1);
                        }
                        ss.put(s, i);
                        this.stack.push(s + "[" + i.toString() + "]");
                        if (hasChildren(n2)) {
                            processDatasetsInternal(n2);
                        }
                        String unstack = printStack();
                        this.order.add(unstack);
                        inverseSearchAdd(unstack);
                        this.name2Node.put(unstack, n2);
                        this.stack.pop();
                    }
                    n2 = n2.getNextSibling();
                }
            }
        }
    }

    public static class AcroFieldsSearch
            extends Xml2Som {

        private HashMap<String, String> acroShort2LongName = new HashMap<String, String>();

        public AcroFieldsSearch(Collection<String> items) {
            for (String string : items) {
                String itemName = string;
                String itemShort = getShortName(itemName);
                this.acroShort2LongName.put(itemShort, itemName);
                inverseSearchAdd(this.inverseSearch, splitParts(itemShort), itemName);
            }
        }

        public HashMap<String, String> getAcroShort2LongName() {
            return this.acroShort2LongName;
        }

        public void setAcroShort2LongName(HashMap<String, String> acroShort2LongName) {
            this.acroShort2LongName = acroShort2LongName;
        }
    }

    public static class Xml2SomTemplate
            extends Xml2Som {

        private boolean dynamicForm;

        private int templateLevel;

        public Xml2SomTemplate(Node n) {
            this.order = new ArrayList<String>();
            this.name2Node = new HashMap<String, Node>();
            this.stack = new XfaForm.Stack2<String>();
            this.anform = 0;
            this.templateLevel = 0;
            this.inverseSearch = new HashMap<String, XfaForm.InverseStore>();
            processTemplate(n, (HashMap<String, Integer>) null);
        }

        public String getFieldType(String s) {
            Node n = this.name2Node.get(s);
            if (n == null) {
                return null;
            }
            if ("exclGroup".equals(n.getLocalName())) {
                return "exclGroup";
            }
            Node ui = n.getFirstChild();
            while (ui != null && (ui.getNodeType() != 1 || !"ui".equals(ui.getLocalName()))) {

                ui = ui.getNextSibling();
            }
            if (ui == null) {
                return null;
            }
            Node type = ui.getFirstChild();
            while (type != null) {
                if (type.getNodeType() == 1 && (!"extras".equals(type.getLocalName()) || !"picture".equals(type.getLocalName()))) {
                    return type.getLocalName();
                }
                type = type.getNextSibling();
            }
            return null;
        }

        private void processTemplate(Node n, HashMap<String, Integer> ff) {
            if (ff == null) {
                ff = new HashMap<String, Integer>();
            }
            HashMap<String, Integer> ss = new HashMap<String, Integer>();
            Node n2 = n.getFirstChild();
            while (n2 != null) {
                if (n2.getNodeType() == 1) {
                    String s = n2.getLocalName();
                    if ("subform".equals(s)) {
                        Integer i;
                        Node name = n2.getAttributes().getNamedItem("name");
                        String nn = "#subform";
                        boolean annon = true;
                        if (name != null) {
                            nn = escapeSom(name.getNodeValue());
                            annon = false;
                        }

                        if (annon) {
                            i = Integer.valueOf(this.anform);
                            this.anform++;
                        } else {

                            i = ss.get(nn);
                            if (i == null) {
                                i = Integer.valueOf(0);
                            } else {
                                i = Integer.valueOf(i.intValue() + 1);
                            }
                            ss.put(nn, i);
                        }
                        this.stack.push(nn + "[" + i.toString() + "]");
                        this.templateLevel++;
                        if (annon) {
                            processTemplate(n2, ff);
                        } else {
                            processTemplate(n2, (HashMap<String, Integer>) null);
                        }
                        this.templateLevel--;
                        this.stack.pop();
                    } else if ("field".equals(s) || "exclGroup".equals(s)) {
                        Node name = n2.getAttributes().getNamedItem("name");
                        if (name != null) {
                            String nn = escapeSom(name.getNodeValue());
                            Integer i = ff.get(nn);
                            if (i == null) {
                                i = Integer.valueOf(0);
                            } else {
                                i = Integer.valueOf(i.intValue() + 1);
                            }
                            ff.put(nn, i);
                            this.stack.push(nn + "[" + i.toString() + "]");
                            String unstack = printStack();
                            this.order.add(unstack);
                            inverseSearchAdd(unstack);
                            this.name2Node.put(unstack, n2);
                            this.stack.pop();
                        }

                    } else if (!this.dynamicForm && this.templateLevel > 0 && "occur".equals(s)) {
                        int initial = 1;
                        int min = 1;
                        int max = 1;
                        Node a = n2.getAttributes().getNamedItem("initial");
                        if (a != null) {
                            try {
                                initial = Integer.parseInt(a.getNodeValue().trim());
                            } catch (Exception exception) {
                            }
                        }
                        a = n2.getAttributes().getNamedItem("min");
                        if (a != null) {
                            try {
                                min = Integer.parseInt(a.getNodeValue().trim());
                            } catch (Exception exception) {
                            }
                        }
                        a = n2.getAttributes().getNamedItem("max");
                        if (a != null) {
                            try {
                                max = Integer.parseInt(a.getNodeValue().trim());
                            } catch (Exception exception) {
                            }
                        }
                        if (initial != min || min != max) {
                            this.dynamicForm = true;
                        }
                    }
                }
                n2 = n2.getNextSibling();
            }
        }

        public boolean isDynamicForm() {
            return this.dynamicForm;
        }

        public void setDynamicForm(boolean dynamicForm) {
            this.dynamicForm = dynamicForm;
        }
    }

    public Xml2SomTemplate getTemplateSom() {
        return this.templateSom;
    }

    public void setTemplateSom(Xml2SomTemplate templateSom) {
        this.templateSom = templateSom;
    }

    public Xml2SomDatasets getDatasetsSom() {
        return this.datasetsSom;
    }

    public void setDatasetsSom(Xml2SomDatasets datasetsSom) {
        this.datasetsSom = datasetsSom;
    }

    public AcroFieldsSearch getAcroFieldsSom() {
        return this.acroFieldsSom;
    }

    public void setAcroFieldsSom(AcroFieldsSearch acroFieldsSom) {
        this.acroFieldsSom = acroFieldsSom;
    }

    public Node getDatasetsNode() {
        return this.datasetsNode;
    }

    public void fillXfaForm(File file) throws IOException {
        fillXfaForm(file, false);
    }

    public void fillXfaForm(File file, boolean readOnly) throws IOException {
        fillXfaForm(new FileInputStream(file), readOnly);
    }

    public void fillXfaForm(InputStream is) throws IOException {
        fillXfaForm(is, false);
    }

    public void fillXfaForm(InputStream is, boolean readOnly) throws IOException {
        fillXfaForm(new InputSource(is), readOnly);
    }

    public void fillXfaForm(InputSource is) throws IOException {
        fillXfaForm(is, false);
    }

    public void fillXfaForm(InputSource is, boolean readOnly) throws IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document newdoc = db.parse(is);
            fillXfaForm(newdoc.getDocumentElement(), readOnly);
        } catch (ParserConfigurationException e) {
            throw new ExceptionConverter(e);
        } catch (SAXException e) {
            throw new ExceptionConverter(e);
        }
    }

    public void fillXfaForm(Node node) {
        fillXfaForm(node, false);
    }

    public void fillXfaForm(Node node, boolean readOnly) {
        if (readOnly) {
            NodeList nodeList = this.domDocument.getElementsByTagName("field");
            for (int i = 0; i < nodeList.getLength(); i++) {
                ((Element) nodeList.item(i)).setAttribute("access", "readOnly");
            }
        }
        NodeList allChilds = this.datasetsNode.getChildNodes();
        int len = allChilds.getLength();
        Node data = null;
        for (int k = 0; k < len; k++) {
            Node n = allChilds.item(k);
            if (n.getNodeType() == 1 && n.getLocalName().equals("data") && "http://www.xfa.org/schema/xfa-data/1.0/".equals(n.getNamespaceURI())) {
                data = n;
                break;
            }
        }
        if (data == null) {
            data = this.datasetsNode.getOwnerDocument().createElementNS("http://www.xfa.org/schema/xfa-data/1.0/", "xfa:data");
            this.datasetsNode.appendChild(data);
        }
        NodeList list = data.getChildNodes();
        if (list.getLength() == 0) {
            data.appendChild(this.domDocument.importNode(node, true));

        } else {

            Node firstNode = getFirstElementNode(data);
            if (firstNode != null) {
                data.replaceChild(this.domDocument.importNode(node, true), firstNode);
            }
        }
        extractNodes();
        setChanged(true);
    }

    private Node getFirstElementNode(Node src) {
        Node result = null;
        NodeList list = src.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() == 1) {
                result = list.item(i);
                break;
            }
        }
        return result;
    }
}
