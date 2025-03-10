package esign.text.pdf.parser;

import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import esign.text.xml.XMLUtil;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Set;

public class TaggedPdfReaderTool {

    protected PdfReader reader;
    protected PrintWriter out;

    public void convertToXml(PdfReader reader, OutputStream os, String charset) throws IOException {
        this.reader = reader;
        OutputStreamWriter outs = new OutputStreamWriter(os, charset);
        this.out = new PrintWriter(outs);

        PdfDictionary catalog = reader.getCatalog();
        PdfDictionary struct = catalog.getAsDict(PdfName.STRUCTTREEROOT);
        if (struct == null) {
            throw new IOException(MessageLocalization.getComposedMessage("no.structtreeroot.found", new Object[0]));
        }
        inspectChild(struct.getDirectObject(PdfName.K));
        this.out.flush();
        this.out.close();
    }

    public void convertToXml(PdfReader reader, OutputStream os) throws IOException {
        convertToXml(reader, os, "UTF-8");
    }

    public void inspectChild(PdfObject k) throws IOException {
        if (k == null) {
            return;
        }
        if (k instanceof PdfArray) {
            inspectChildArray((PdfArray) k);
        } else if (k instanceof PdfDictionary) {
            inspectChildDictionary((PdfDictionary) k);
        }
    }

    public void inspectChildArray(PdfArray k) throws IOException {
        if (k == null) {
            return;
        }
        for (int i = 0; i < k.size(); i++) {
            inspectChild(k.getDirectObject(i));
        }
    }

    public void inspectChildDictionary(PdfDictionary k) throws IOException {
        inspectChildDictionary(k, false);
    }

    public void inspectChildDictionary(PdfDictionary k, boolean inspectAttributes) throws IOException {
        if (k == null) {
            return;
        }
        PdfName s = k.getAsName(PdfName.S);
        if (s != null) {
            String tagN = PdfName.decodeName(s.toString());
            String tag = fixTagName(tagN);
            this.out.print("<");
            this.out.print(tag);
            if (inspectAttributes) {
                PdfDictionary a = k.getAsDict(PdfName.A);
                if (a != null) {
                    Set<PdfName> keys = a.getKeys();
                    for (PdfName key : keys) {
                        this.out.print(' ');
                        PdfObject value = a.get(key);
                        value = PdfReader.getPdfObject(value);
                        this.out.print(xmlName(key));
                        this.out.print("=\"");
                        this.out.print(value.toString());
                        this.out.print("\"");
                    }
                }
            }
            this.out.print(">");
            PdfObject alt = k.get(PdfName.ALT);
            if (alt != null && alt.toString() != null) {
                this.out.print("<alt><![CDATA[");
                this.out.print(alt.toString().replaceAll("[\\000]*", ""));
                this.out.print("]]></alt>");
            }
            PdfDictionary dict = k.getAsDict(PdfName.PG);
            if (dict != null) {
                parseTag(tagN, k.getDirectObject(PdfName.K), dict);
            }
            inspectChild(k.getDirectObject(PdfName.K));
            this.out.print("</");
            this.out.print(tag);
            this.out.println(">");
        } else {
            inspectChild(k.getDirectObject(PdfName.K));
        }
    }

    protected String xmlName(PdfName name) {
        String xmlName = name.toString().replaceFirst("/", "");

        xmlName = Character.toLowerCase(xmlName.charAt(0)) + xmlName.substring(1);
        return xmlName;
    }

    private static String fixTagName(String tag) {
        StringBuilder sb = new StringBuilder();
        for (int k = 0; k < tag.length(); k++) {
            char c = tag.charAt(k);
            boolean nameStart = (c == ':' || (c >= 'A' && c <= 'Z') || c == '_' || (c >= 'a' && c <= 'z') || (c >= 'À' && c <= 'Ö') || (c >= 'Ø' && c <= 'ö') || (c >= 'ø' && c <= '˿') || (c >= 'Ͱ' && c <= 'ͽ') || (c >= 'Ϳ' && c <= '῿') || (c >= '‌' && c <= '‍') || (c >= '⁰' && c <= '↏') || (c >= 'Ⰰ' && c <= '⿯') || (c >= '、' && c <= '퟿') || (c >= '豈' && c <= '﷏') || (c >= 'ﷰ' && c <= '�'));

            boolean nameMiddle = (c == '-' || c == '.' || (c >= '0' && c <= '9') || c == '·' || (c >= '̀' && c <= 'ͯ') || (c >= '‿' && c <= '⁀') || nameStart);

            if (k == 0) {
                if (!nameStart) {
                    c = '_';
                }
            } else if (!nameMiddle) {
                c = '-';
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public void parseTag(String tag, PdfObject object, PdfDictionary page) throws IOException {
        if (object instanceof PdfNumber) {
            PdfNumber mcid = (PdfNumber) object;
            RenderFilter filter = new MarkedContentRenderFilter(mcid.intValue());
            TextExtractionStrategy strategy = new SimpleTextExtractionStrategy();
            FilteredTextRenderListener listener = new FilteredTextRenderListener(strategy, new RenderFilter[]{filter});

            PdfContentStreamProcessor processor = new PdfContentStreamProcessor(listener);

            processor.processContent(PdfReader.getPageContent(page), page
                    .getAsDict(PdfName.RESOURCES));
            this.out.print(XMLUtil.escapeXML(listener.getResultantText(), true));

        } else if (object instanceof PdfArray) {
            PdfArray arr = (PdfArray) object;
            int n = arr.size();
            for (int i = 0; i < n; i++) {
                parseTag(tag, arr.getPdfObject(i), page);
                if (i < n - 1) {
                    this.out.println();
                }
            }

        } else if (object instanceof PdfDictionary) {
            PdfDictionary mcr = (PdfDictionary) object;
            parseTag(tag, mcr.getDirectObject(PdfName.MCID), mcr
                    .getAsDict(PdfName.PG));
        }
    }
}
