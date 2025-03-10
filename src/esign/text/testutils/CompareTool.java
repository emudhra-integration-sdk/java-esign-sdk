package esign.text.testutils;

import esign.text.BaseColor;
import esign.text.DocumentException;
import esign.text.Rectangle;
import esign.text.io.RandomAccessSourceFactory;
import esign.text.pdf.PRStream;
import esign.text.pdf.PRTokeniser;
import esign.text.pdf.PdfAnnotation;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfBoolean;
import esign.text.pdf.PdfContentByte;
import esign.text.pdf.PdfContentParser;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfIndirectReference;
import esign.text.pdf.PdfLiteral;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import esign.text.pdf.PdfStamper;
import esign.text.pdf.PdfString;
import esign.text.pdf.RandomAccessFileOrArray;
import esign.text.pdf.RefKey;
import esign.text.pdf.parser.ContentByteUtils;
import esign.text.pdf.parser.ImageRenderInfo;
import esign.text.pdf.parser.InlineImageInfo;
import esign.text.pdf.parser.InlineImageUtils;
import esign.text.pdf.parser.PdfContentStreamProcessor;
import esign.text.pdf.parser.RenderListener;
import esign.text.pdf.parser.SimpleTextExtractionStrategy;
import esign.text.pdf.parser.TaggedPdfReaderTool;
import esign.text.pdf.parser.TextExtractionStrategy;
import esign.text.pdf.parser.TextRenderInfo;
import esign.text.xml.XMLUtil;
import esign.text.xmp.XMPException;
import esign.text.xmp.XMPMeta;
import esign.text.xmp.XMPMetaFactory;
import esign.text.xmp.XMPUtils;
import esign.text.xmp.options.SerializeOptions;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class CompareTool {

    private String gsExec;
    private String compareExec;

    private class ObjectPath {

        protected RefKey baseCmpObject;
        protected RefKey baseOutObject;
        protected Stack<PathItem> path = new Stack<PathItem>();
        protected Stack<Pair<RefKey>> indirects = new Stack<Pair<RefKey>>();

        public ObjectPath() {
        }

        protected ObjectPath(RefKey baseCmpObject, RefKey baseOutObject) {
            this.baseCmpObject = baseCmpObject;
            this.baseOutObject = baseOutObject;
        }

        private ObjectPath(RefKey baseCmpObject, RefKey baseOutObject, Stack<PathItem> path) {
            this.baseCmpObject = baseCmpObject;
            this.baseOutObject = baseOutObject;
            this.path = path;
        }

        private class Pair<T> {

            private T first;
            private T second;

            public Pair(T first, T second) {
                this.first = first;
                this.second = second;
            }

            public int hashCode() {
                return this.first.hashCode() * 31 + this.second.hashCode();
            }

            public boolean equals(Object obj) {
                return (obj instanceof Pair && this.first.equals(((Pair) obj).first) && this.second.equals(((Pair) obj).second));
            }
        }

        private abstract class PathItem {

            private PathItem() {
            }

            protected abstract Node toXmlNode(Document param2Document);
        }

        private class DictPathItem extends PathItem {

            public DictPathItem(String key) {
                this.key = key;
            }
            String key;

            public String toString() {
                return "Dict key: " + this.key;
            }

            public int hashCode() {
                return this.key.hashCode();
            }

            public boolean equals(Object obj) {
                return (obj instanceof DictPathItem && this.key.equals(((DictPathItem) obj).key));
            }

            protected Node toXmlNode(Document document) {
                Node element = document.createElement("dictKey");
                element.appendChild(document.createTextNode(this.key));
                return element;
            }
        }

        private class ArrayPathItem extends PathItem {

            int index;

            public ArrayPathItem(int index) {
                this.index = index;
            }

            public String toString() {
                return "Array index: " + String.valueOf(this.index);
            }

            public int hashCode() {
                return this.index;
            }

            public boolean equals(Object obj) {
                return (obj instanceof ArrayPathItem && this.index == ((ArrayPathItem) obj).index);
            }

            protected Node toXmlNode(Document document) {
                Node element = document.createElement("arrayIndex");
                element.appendChild(document.createTextNode(String.valueOf(this.index)));
                return element;
            }
        }

        private class OffsetPathItem extends PathItem {

            int offset;

            public OffsetPathItem(int offset) {
                this.offset = offset;
            }

            public String toString() {
                return "Offset: " + String.valueOf(this.offset);
            }

            public int hashCode() {
                return this.offset;
            }

            public boolean equals(Object obj) {
                return (obj instanceof OffsetPathItem && this.offset == ((OffsetPathItem) obj).offset);
            }

            protected Node toXmlNode(Document document) {
                Node element = document.createElement("offset");
                element.appendChild(document.createTextNode(String.valueOf(this.offset)));
                return element;
            }
        }

        public ObjectPath resetDirectPath(RefKey baseCmpObject, RefKey baseOutObject) {
            ObjectPath newPath = new ObjectPath(baseCmpObject, baseOutObject);
            newPath.indirects = (Stack<Pair<RefKey>>) this.indirects.clone();
            newPath.indirects.add(new Pair<RefKey>(baseCmpObject, baseOutObject));
            return newPath;
        }

        public boolean isComparing(RefKey baseCmpObject, RefKey baseOutObject) {
            return this.indirects.contains(new Pair<RefKey>(baseCmpObject, baseOutObject));
        }

        public void pushArrayItemToPath(int index) {
            this.path.add(new ArrayPathItem(index));
        }

        public void pushDictItemToPath(String key) {
            this.path.add(new DictPathItem(key));
        }

        public void pushOffsetToPath(int offset) {
            this.path.add(new OffsetPathItem(offset));
        }

        public void pop() {
            this.path.pop();
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Base cmp object: %s obj. Base out object: %s obj", new Object[]{this.baseCmpObject, this.baseOutObject}));
            for (PathItem pathItem : this.path) {
                sb.append("\n");
                sb.append(pathItem.toString());
            }
            return sb.toString();
        }

        public int hashCode() {
            int hashCode1 = (this.baseCmpObject != null) ? this.baseCmpObject.hashCode() : 1;
            int hashCode2 = (this.baseOutObject != null) ? this.baseOutObject.hashCode() : 1;
            int hashCode = hashCode1 * 31 + hashCode2;
            for (PathItem pathItem : this.path) {
                hashCode *= 31;
                hashCode += pathItem.hashCode();
            }
            return hashCode;
        }

        public boolean equals(Object obj) {
            return (obj instanceof ObjectPath && this.baseCmpObject.equals(((ObjectPath) obj).baseCmpObject) && this.baseOutObject.equals(((ObjectPath) obj).baseOutObject) && this.path
                    .equals(((ObjectPath) obj).path));
        }

        protected Object clone() {
            return new ObjectPath(this.baseCmpObject, this.baseOutObject, (Stack<PathItem>) this.path.clone());
        }

        public Node toXmlNode(Document document) {
            Element element = document.createElement("path");
            Element baseNode = document.createElement("base");
            baseNode.setAttribute("cmp", this.baseCmpObject.toString() + " obj");
            baseNode.setAttribute("out", this.baseOutObject.toString() + " obj");
            element.appendChild(baseNode);
            for (PathItem pathItem : this.path) {
                element.appendChild(pathItem.toXmlNode(document));
            }
            return element;
        }
    }

    protected class CompareResult {

        protected Map<CompareTool.ObjectPath, String> differences = new LinkedHashMap<CompareTool.ObjectPath, String>();
        protected int messageLimit = 1;

        public CompareResult(int messageLimit) {
            this.messageLimit = messageLimit;
        }

        public boolean isOk() {
            return (this.differences.size() == 0);
        }

        public int getErrorCount() {
            return this.differences.size();
        }

        protected boolean isMessageLimitReached() {
            return (this.differences.size() >= this.messageLimit);
        }

        public String getReport() {
            StringBuilder sb = new StringBuilder();
            boolean firstEntry = true;
            for (Map.Entry<CompareTool.ObjectPath, String> entry : this.differences.entrySet()) {
                if (!firstEntry) {
                    sb.append("-----------------------------").append("\n");
                }
                CompareTool.ObjectPath diffPath = entry.getKey();
                sb.append(entry.getValue()).append("\n").append(diffPath.toString()).append("\n");
                firstEntry = false;
            }
            return sb.toString();
        }

        protected void addError(CompareTool.ObjectPath path, String message) {
            if (this.differences.size() < this.messageLimit) {
                this.differences.put((CompareTool.ObjectPath) path.clone(), message);
            }
        }

        public void writeReportToXml(OutputStream stream) throws ParserConfigurationException, TransformerException {
            Document xmlReport = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            Element root = xmlReport.createElement("report");
            Element errors = xmlReport.createElement("errors");
            errors.setAttribute("count", String.valueOf(this.differences.size()));
            root.appendChild(errors);
            for (Map.Entry<CompareTool.ObjectPath, String> entry : this.differences.entrySet()) {
                Node errorNode = xmlReport.createElement("error");
                Node message = xmlReport.createElement("message");
                message.appendChild(xmlReport.createTextNode(entry.getValue()));
                Node path = ((CompareTool.ObjectPath) entry.getKey()).toXmlNode(xmlReport);
                errorNode.appendChild(message);
                errorNode.appendChild(path);
                errors.appendChild(errorNode);
            }
            xmlReport.appendChild(root);

            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes");
            DOMSource source = new DOMSource(xmlReport);
            StreamResult result = new StreamResult(stream);
            transformer.transform(source, result);
        }
    }

    private final String gsParams = " -dNOPAUSE -dBATCH -sDEVICE=png16m -r150 -sOutputFile=<outputfile> <inputfile>";
    private final String compareParams = " \"<image1>\" \"<image2>\" \"<difference>\"";

    private static final String cannotOpenTargetDirectory = "Cannot open target directory for <filename>.";

    private static final String gsFailed = "GhostScript failed for <filename>.";

    private static final String unexpectedNumberOfPages = "Unexpected number of pages for <filename>.";

    private static final String differentPages = "File <filename> differs on page <pagenumber>.";

    private static final String undefinedGsPath = "Path to GhostScript is not specified. Please use -DgsExec=<path_to_ghostscript> (e.g. -DgsExec=\"C:/Program Files/gs/gs9.14/bin/gswin32c.exe\")";
    private static final String ignoredAreasPrefix = "ignored_areas_";
    private String cmpPdf;
    private String cmpPdfName;
    private String cmpImage;
    private String outPdf;
    private String outPdfName;
    private String outImage;
    List<PdfDictionary> outPages;
    List<RefKey> outPagesRef;
    List<PdfDictionary> cmpPages;
    List<RefKey> cmpPagesRef;
    private int compareByContentErrorsLimit = 1;
    private boolean generateCompareByContentXmlReport = false;
    private String xmlReportName = "report";
    private double floatComparisonError = 0.0D;

    private boolean absoluteError = true;

    public CompareTool() {
        this.gsExec = System.getProperty("gsExec");
        if (this.gsExec == null) {
            this.gsExec = System.getenv("gsExec");
        }
        this.compareExec = System.getProperty("compareExec");
        if (this.compareExec == null) {
            this.compareExec = System.getenv("compareExec");
        }
    }

    private String compare(String outPath, String differenceImagePrefix, Map<Integer, List<Rectangle>> ignoredAreas) throws IOException, InterruptedException, DocumentException {
        return compare(outPath, differenceImagePrefix, ignoredAreas, (List<Integer>) null);
    }

    private String compare(String outPath, String differenceImagePrefix, Map<Integer, List<Rectangle>> ignoredAreas, List<Integer> equalPages) throws IOException, InterruptedException, DocumentException {
        if (this.gsExec == null) {
            return "Path to GhostScript is not specified. Please use -DgsExec=<path_to_ghostscript> (e.g. -DgsExec=\"C:/Program Files/gs/gs9.14/bin/gswin32c.exe\")";
        }
        if (!(new File(this.gsExec)).exists()) {
            return (new File(this.gsExec)).getAbsolutePath() + " does not exist";
        }
        if (!outPath.endsWith("/")) {
            outPath = outPath + "/";
        }
        File targetDir = new File(outPath);

        if (!targetDir.exists()) {
            targetDir.mkdirs();
        } else {
            File[] imageFiles = targetDir.listFiles(new PngFileFilter());
            for (File file : imageFiles) {
                file.delete();
            }
            File[] cmpImageFiles = targetDir.listFiles(new CmpPngFileFilter());
            for (File file : cmpImageFiles) {
                file.delete();
            }
        }

        File diffFile = new File(outPath + differenceImagePrefix);
        if (diffFile.exists()) {
            diffFile.delete();
        }

        if (ignoredAreas != null && !ignoredAreas.isEmpty()) {
            PdfReader cmpReader = new PdfReader(this.cmpPdf);
            PdfReader outReader = new PdfReader(this.outPdf);
            PdfStamper outStamper = new PdfStamper(outReader, new FileOutputStream(outPath + "ignored_areas_" + this.outPdfName));
            PdfStamper cmpStamper = new PdfStamper(cmpReader, new FileOutputStream(outPath + "ignored_areas_" + this.cmpPdfName));

            for (Map.Entry<Integer, List<Rectangle>> entry : ignoredAreas.entrySet()) {
                int pageNumber = ((Integer) entry.getKey()).intValue();
                List<Rectangle> rectangles = entry.getValue();

                if (rectangles != null && !rectangles.isEmpty()) {
                    PdfContentByte outCB = outStamper.getOverContent(pageNumber);
                    PdfContentByte cmpCB = cmpStamper.getOverContent(pageNumber);

                    for (Rectangle rect : rectangles) {
                        rect.setBackgroundColor(BaseColor.BLACK);
                        outCB.rectangle(rect);
                        cmpCB.rectangle(rect);
                    }
                }
            }

            outStamper.close();
            cmpStamper.close();

            outReader.close();
            cmpReader.close();

            init(outPath + "ignored_areas_" + this.outPdfName, outPath + "ignored_areas_" + this.cmpPdfName);
        }

        if (targetDir.exists()) {
            getClass();
            String gsParams = " -dNOPAUSE -dBATCH -sDEVICE=png16m -r150 -sOutputFile=<outputfile> <inputfile>".replace("<outputfile>", outPath + this.cmpImage).replace("<inputfile>", this.cmpPdf);
            Process p = runProcess(this.gsExec, gsParams);
            BufferedReader bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line;
            while ((line = bri.readLine()) != null) {
                System.out.println(line);
            }
            bri.close();
            while ((line = bre.readLine()) != null) {
                System.out.println(line);
            }
            bre.close();
            if (p.waitFor() == 0) {
                getClass();
                gsParams = " -dNOPAUSE -dBATCH -sDEVICE=png16m -r150 -sOutputFile=<outputfile> <inputfile>".replace("<outputfile>", outPath + this.outImage).replace("<inputfile>", this.outPdf);
                p = runProcess(this.gsExec, gsParams);
                bri = new BufferedReader(new InputStreamReader(p.getInputStream()));
                bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                while ((line = bri.readLine()) != null) {
                    System.out.println(line);
                }
                bri.close();
                while ((line = bre.readLine()) != null) {
                    System.out.println(line);
                }
                bre.close();
                int exitValue = p.waitFor();

                if (exitValue == 0) {
                    File[] imageFiles = targetDir.listFiles(new PngFileFilter());
                    File[] cmpImageFiles = targetDir.listFiles(new CmpPngFileFilter());
                    boolean bUnexpectedNumberOfPages = false;
                    if (imageFiles.length != cmpImageFiles.length) {
                        bUnexpectedNumberOfPages = true;
                    }
                    int cnt = Math.min(imageFiles.length, cmpImageFiles.length);
                    if (cnt < 1) {
                        return "No files for comparing!!!\nThe result or sample pdf file is not processed by GhostScript.";
                    }
                    Arrays.sort(imageFiles, new ImageNameComparator());
                    Arrays.sort(cmpImageFiles, new ImageNameComparator());
                    String differentPagesFail = null;
                    for (int i = 0; i < cnt; i++) {
                        if (equalPages == null || !equalPages.contains(Integer.valueOf(i))) {

                            System.out.print("Comparing page " + Integer.toString(i + 1) + " (" + imageFiles[i].getAbsolutePath() + ")...");
                            FileInputStream is1 = new FileInputStream(imageFiles[i]);
                            FileInputStream is2 = new FileInputStream(cmpImageFiles[i]);
                            boolean cmpResult = compareStreams(is1, is2);
                            is1.close();
                            is2.close();
                            if (!cmpResult) {
                                if (this.compareExec != null && (new File(this.compareExec)).exists()) {
                                    getClass();
                                    String compareParams = " \"<image1>\" \"<image2>\" \"<difference>\"".replace("<image1>", imageFiles[i].getAbsolutePath()).replace("<image2>", cmpImageFiles[i].getAbsolutePath()).replace("<difference>", outPath + differenceImagePrefix + Integer.toString(i + 1) + ".png");
                                    p = runProcess(this.compareExec, compareParams);
                                    bre = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                                    while ((line = bre.readLine()) != null) {
                                        System.out.println(line);
                                    }
                                    bre.close();
                                    int cmpExitValue = p.waitFor();
                                    if (cmpExitValue == 0) {
                                        if (differentPagesFail == null) {
                                            differentPagesFail = "File <filename> differs on page <pagenumber>.".replace("<filename>", this.outPdf).replace("<pagenumber>", Integer.toString(i + 1));
                                            differentPagesFail = differentPagesFail + "\nPlease, examine " + outPath + differenceImagePrefix + Integer.toString(i + 1) + ".png for more details.";
                                        } else {
                                            differentPagesFail = "File " + this.outPdf + " differs.\nPlease, examine difference images for more details.";
                                        }
                                    } else {

                                        differentPagesFail = "File <filename> differs on page <pagenumber>.".replace("<filename>", this.outPdf).replace("<pagenumber>", Integer.toString(i + 1));
                                    }
                                } else {
                                    differentPagesFail = "File <filename> differs on page <pagenumber>.".replace("<filename>", this.outPdf).replace("<pagenumber>", Integer.toString(i + 1));
                                    differentPagesFail = differentPagesFail + "\nYou can optionally specify path to ImageMagick compare tool (e.g. -DcompareExec=\"C:/Program Files/ImageMagick-6.5.4-2/compare.exe\") to visualize differences.";
                                    break;
                                }
                                System.out.println(differentPagesFail);
                            } else {
                                System.out.println("done.");
                            }
                        }
                    }
                    if (differentPagesFail != null) {
                        return differentPagesFail;
                    }
                    if (bUnexpectedNumberOfPages) {
                        return "Unexpected number of pages for <filename>.".replace("<filename>", this.outPdf);
                    }
                } else {
                    return "GhostScript failed for <filename>.".replace("<filename>", this.outPdf);
                }
            } else {
                return "GhostScript failed for <filename>.".replace("<filename>", this.cmpPdf);
            }
        } else {
            return "Cannot open target directory for <filename>.".replace("<filename>", this.outPdf);
        }

        return null;
    }

    private Process runProcess(String execPath, String params) throws IOException, InterruptedException {
        StringTokenizer st = new StringTokenizer(params);
        String[] cmdArray = new String[st.countTokens() + 1];
        cmdArray[0] = execPath;
        for (int i = 1; st.hasMoreTokens(); i++) {
            cmdArray[i] = st.nextToken();
        }
        Process p = Runtime.getRuntime().exec(cmdArray);

        return p;
    }

    public String compare(String outPdf, String cmpPdf, String outPath, String differenceImagePrefix, Map<Integer, List<Rectangle>> ignoredAreas) throws IOException, InterruptedException, DocumentException {
        init(outPdf, cmpPdf);
        return compare(outPath, differenceImagePrefix, ignoredAreas);
    }

    public String compare(String outPdf, String cmpPdf, String outPath, String differenceImagePrefix) throws IOException, InterruptedException, DocumentException {
        return compare(outPdf, cmpPdf, outPath, differenceImagePrefix, null);
    }

    public CompareTool setCompareByContentErrorsLimit(int compareByContentMaxErrorCount) {
        this.compareByContentErrorsLimit = compareByContentMaxErrorCount;
        return this;
    }

    public void setGenerateCompareByContentXmlReport(boolean generateCompareByContentXmlReport) {
        this.generateCompareByContentXmlReport = generateCompareByContentXmlReport;
    }

    public CompareTool setFloatAbsoluteError(float error) {
        this.floatComparisonError = error;
        this.absoluteError = true;
        return this;
    }

    public CompareTool setFloatRelativeError(float error) {
        this.floatComparisonError = error;
        this.absoluteError = false;
        return this;
    }

    public String getXmlReportName() {
        return this.xmlReportName;
    }

    public void setXmlReportName(String xmlReportName) {
        this.xmlReportName = xmlReportName;
    }

    protected String compareByContent(String outPath, String differenceImagePrefix, Map<Integer, List<Rectangle>> ignoredAreas) throws DocumentException, InterruptedException, IOException {
        System.out.print("[itext] INFO  Comparing by content..........");
        PdfReader outReader = new PdfReader(this.outPdf);
        this.outPages = new ArrayList<PdfDictionary>();
        this.outPagesRef = new ArrayList<RefKey>();
        loadPagesFromReader(outReader, this.outPages, this.outPagesRef);

        PdfReader cmpReader = new PdfReader(this.cmpPdf);
        this.cmpPages = new ArrayList<PdfDictionary>();
        this.cmpPagesRef = new ArrayList<RefKey>();
        loadPagesFromReader(cmpReader, this.cmpPages, this.cmpPagesRef);

        if (this.outPages.size() != this.cmpPages.size()) {
            return compare(outPath, differenceImagePrefix, ignoredAreas);
        }
        CompareResult compareResult = new CompareResult(this.compareByContentErrorsLimit);
        List<Integer> equalPages = new ArrayList<Integer>(this.cmpPages.size());
        for (int i = 0; i < this.cmpPages.size(); i++) {
            ObjectPath currentPath = new ObjectPath(this.cmpPagesRef.get(i), this.outPagesRef.get(i));
            if (compareDictionariesExtended(this.outPages.get(i), this.cmpPages.get(i), currentPath, compareResult)) {
                equalPages.add(Integer.valueOf(i));
            }
        }
        PdfObject outStructTree = outReader.getCatalog().get(PdfName.STRUCTTREEROOT);
        PdfObject cmpStructTree = cmpReader.getCatalog().get(PdfName.STRUCTTREEROOT);
        RefKey outStructTreeRef = (outStructTree == null) ? null : new RefKey((PdfIndirectReference) outStructTree);
        RefKey cmpStructTreeRef = (cmpStructTree == null) ? null : new RefKey((PdfIndirectReference) cmpStructTree);
        compareObjects(outStructTree, cmpStructTree, new ObjectPath(outStructTreeRef, cmpStructTreeRef), compareResult);

        PdfObject outOcProperties = outReader.getCatalog().get(PdfName.OCPROPERTIES);
        PdfObject cmpOcProperties = cmpReader.getCatalog().get(PdfName.OCPROPERTIES);
        RefKey outOcPropertiesRef = (outOcProperties instanceof PdfIndirectReference) ? new RefKey((PdfIndirectReference) outOcProperties) : null;
        RefKey cmpOcPropertiesRef = (cmpOcProperties instanceof PdfIndirectReference) ? new RefKey((PdfIndirectReference) cmpOcProperties) : null;
        compareObjects(outOcProperties, cmpOcProperties, new ObjectPath(outOcPropertiesRef, cmpOcPropertiesRef), compareResult);

        outReader.close();
        cmpReader.close();

        if (this.generateCompareByContentXmlReport) {
            try {
                compareResult.writeReportToXml(new FileOutputStream(outPath + "/" + this.xmlReportName + ".xml"));
            } catch (Exception exception) {
            }
        }

        if (equalPages.size() == this.cmpPages.size() && compareResult.isOk()) {
            System.out.println("OK");
            System.out.flush();
            return null;
        }
        System.out.println("Fail");
        System.out.flush();
        String compareByContentReport = "Compare by content report:\n" + compareResult.getReport();
        System.out.println(compareByContentReport);
        System.out.flush();
        String message = compare(outPath, differenceImagePrefix, ignoredAreas, equalPages);
        if (message == null || message.length() == 0) {
            return "Compare by content fails. No visual differences";
        }
        return message;
    }

    public String compareByContent(String outPdf, String cmpPdf, String outPath, String differenceImagePrefix, Map<Integer, List<Rectangle>> ignoredAreas) throws DocumentException, InterruptedException, IOException {
        init(outPdf, cmpPdf);
        return compareByContent(outPath, differenceImagePrefix, ignoredAreas);
    }

    public String compareByContent(String outPdf, String cmpPdf, String outPath, String differenceImagePrefix) throws DocumentException, InterruptedException, IOException {
        return compareByContent(outPdf, cmpPdf, outPath, differenceImagePrefix, null);
    }

    private void loadPagesFromReader(PdfReader reader, List<PdfDictionary> pages, List<RefKey> pagesRef) {
        PdfObject pagesDict = reader.getCatalog().get(PdfName.PAGES);
        addPagesFromDict(pagesDict, pages, pagesRef);
    }

    private void addPagesFromDict(PdfObject dictRef, List<PdfDictionary> pages, List<RefKey> pagesRef) {
        PdfDictionary dict = (PdfDictionary) PdfReader.getPdfObject(dictRef);
        if (dict.isPages()) {
            PdfArray kids = dict.getAsArray(PdfName.KIDS);
            if (kids == null) {
                return;
            }
            for (PdfObject kid : kids) {
                addPagesFromDict(kid, pages, pagesRef);
            }
        } else if (dict.isPage()) {
            pages.add(dict);
            pagesRef.add(new RefKey((PdfIndirectReference) dictRef));
        }
    }

    private boolean compareObjects(PdfObject outObj, PdfObject cmpObj, ObjectPath currentPath, CompareResult compareResult) throws IOException {
        PdfObject outDirectObj = PdfReader.getPdfObject(outObj);
        PdfObject cmpDirectObj = PdfReader.getPdfObject(cmpObj);

        if (cmpDirectObj == null && outDirectObj == null) {
            return true;
        }
        if (outDirectObj == null) {
            compareResult.addError(currentPath, "Expected object was not found.");
            return false;
        }
        if (cmpDirectObj == null) {
            compareResult.addError(currentPath, "Found object which was not expected to be found.");
            return false;
        }
        if (cmpDirectObj.type() != outDirectObj.type()) {
            compareResult.addError(currentPath, String.format("Types do not match. Expected: %s. Found: %s.", new Object[]{cmpDirectObj.getClass().getSimpleName(), outDirectObj.getClass().getSimpleName()}));
            return false;
        }

        if (cmpObj.isIndirect() && outObj.isIndirect()) {
            if (currentPath.isComparing(new RefKey((PdfIndirectReference) cmpObj), new RefKey((PdfIndirectReference) outObj))) {
                return true;
            }
            currentPath = currentPath.resetDirectPath(new RefKey((PdfIndirectReference) cmpObj), new RefKey((PdfIndirectReference) outObj));
        }

        if (cmpDirectObj.isDictionary() && ((PdfDictionary) cmpDirectObj).isPage()) {
            if (!outDirectObj.isDictionary() || !((PdfDictionary) outDirectObj).isPage()) {
                if (compareResult != null && currentPath != null) {
                    compareResult.addError(currentPath, "Expected a page. Found not a page.");
                }
                return false;
            }
            RefKey cmpRefKey = new RefKey((PdfIndirectReference) cmpObj);
            RefKey outRefKey = new RefKey((PdfIndirectReference) outObj);

            if (this.cmpPagesRef.contains(cmpRefKey) && this.cmpPagesRef.indexOf(cmpRefKey) == this.outPagesRef.indexOf(outRefKey)) {
                return true;
            }
            if (compareResult != null && currentPath != null) {
                compareResult.addError(currentPath, String.format("The dictionaries refer to different pages. Expected page number: %s. Found: %s", new Object[]{
                    Integer.valueOf(this.cmpPagesRef.indexOf(cmpRefKey)), Integer.valueOf(this.outPagesRef.indexOf(outRefKey))}));
            }
            return false;
        }

        if (cmpDirectObj.isDictionary()) {
            if (!compareDictionariesExtended((PdfDictionary) outDirectObj, (PdfDictionary) cmpDirectObj, currentPath, compareResult)) {
                return false;
            }
        } else if (cmpDirectObj.isStream()) {
            if (!compareStreamsExtended((PRStream) outDirectObj, (PRStream) cmpDirectObj, currentPath, compareResult)) {
                return false;
            }
        } else if (cmpDirectObj.isArray()) {
            if (!compareArraysExtended((PdfArray) outDirectObj, (PdfArray) cmpDirectObj, currentPath, compareResult)) {
                return false;
            }
        } else if (cmpDirectObj.isName()) {
            if (!compareNamesExtended((PdfName) outDirectObj, (PdfName) cmpDirectObj, currentPath, compareResult)) {
                return false;
            }
        } else if (cmpDirectObj.isNumber()) {
            if (!compareNumbersExtended((PdfNumber) outDirectObj, (PdfNumber) cmpDirectObj, currentPath, compareResult)) {
                return false;
            }
        } else if (cmpDirectObj.isString()) {
            if (!compareStringsExtended((PdfString) outDirectObj, (PdfString) cmpDirectObj, currentPath, compareResult)) {
                return false;
            }
        } else if (cmpDirectObj.isBoolean()) {
            if (!compareBooleansExtended((PdfBoolean) outDirectObj, (PdfBoolean) cmpDirectObj, currentPath, compareResult)) {
                return false;
            }
        } else if (cmpDirectObj instanceof PdfLiteral) {
            if (!compareLiteralsExtended((PdfLiteral) outDirectObj, (PdfLiteral) cmpDirectObj, currentPath, compareResult)) {
                return false;
            }
        } else if (!outDirectObj.isNull() || !cmpDirectObj.isNull()) {

            throw new UnsupportedOperationException();
        }
        return true;
    }

    public boolean compareDictionaries(PdfDictionary outDict, PdfDictionary cmpDict) throws IOException {
        return compareDictionariesExtended(outDict, cmpDict, null, null);
    }

    private boolean compareDictionariesExtended(PdfDictionary outDict, PdfDictionary cmpDict, ObjectPath currentPath, CompareResult compareResult) throws IOException {
        if ((cmpDict != null && outDict == null) || (outDict != null && cmpDict == null)) {
            compareResult.addError(currentPath, "One of the dictionaries is null, the other is not.");
            return false;
        }
        boolean dictsAreSame = true;

        Set<PdfName> mergedKeys = new TreeSet<PdfName>(cmpDict.getKeys());
        mergedKeys.addAll(outDict.getKeys());

        for (PdfName key : mergedKeys) {
            if (key.compareTo(PdfName.PARENT) == 0 || key.compareTo(PdfName.P) == 0 || (outDict.isStream() && cmpDict.isStream() && (key.equals(PdfName.FILTER) || key.equals(PdfName.LENGTH)))) {
                continue;
            }
            if (key.compareTo(PdfName.BASEFONT) == 0 || key.compareTo(PdfName.FONTNAME) == 0) {
                PdfObject cmpObj = cmpDict.getDirectObject(key);
                if (cmpObj.isName() && cmpObj.toString().indexOf('+') > 0) {
                    PdfObject outObj = outDict.getDirectObject(key);
                    if (!outObj.isName() || outObj.toString().indexOf('+') == -1) {
                        if (compareResult != null && currentPath != null) {
                            compareResult.addError(currentPath, String.format("PdfDictionary %s entry: Expected: %s. Found: %s", new Object[]{key.toString(), cmpObj.toString(), outObj.toString()}));
                        }
                        dictsAreSame = false;
                    }
                    String cmpName = cmpObj.toString().substring(cmpObj.toString().indexOf('+'));
                    String outName = outObj.toString().substring(outObj.toString().indexOf('+'));
                    if (!cmpName.equals(outName)) {
                        if (compareResult != null && currentPath != null) {
                            compareResult.addError(currentPath, String.format("PdfDictionary %s entry: Expected: %s. Found: %s", new Object[]{key.toString(), cmpObj.toString(), outObj.toString()}));
                        }
                        dictsAreSame = false;
                    }

                    continue;
                }
            }
            if (this.floatComparisonError != 0.0D && cmpDict.isPage() && outDict.isPage() && key.equals(PdfName.CONTENTS)) {
                if (!compareContentStreamsByParsingExtended(outDict.getDirectObject(key), cmpDict.getDirectObject(key), (PdfDictionary) outDict
                        .getDirectObject(PdfName.RESOURCES), (PdfDictionary) cmpDict.getDirectObject(PdfName.RESOURCES), currentPath, compareResult)) {
                    dictsAreSame = false;
                }

                continue;
            }
            if (currentPath != null) {
                currentPath.pushDictItemToPath(key.toString());
            }
            dictsAreSame = (compareObjects(outDict.get(key), cmpDict.get(key), currentPath, compareResult) && dictsAreSame);
            if (currentPath != null) {
                currentPath.pop();
            }
            if (!dictsAreSame && (currentPath == null || compareResult == null || compareResult.isMessageLimitReached())) {
                return false;
            }
        }
        return dictsAreSame;
    }

    public boolean compareContentStreamsByParsing(PdfObject outObj, PdfObject cmpObj) throws IOException {
        return compareContentStreamsByParsingExtended(outObj, cmpObj, null, null, null, null);
    }

    public boolean compareContentStreamsByParsing(PdfObject outObj, PdfObject cmpObj, PdfDictionary outResources, PdfDictionary cmpResources) throws IOException {
        return compareContentStreamsByParsingExtended(outObj, cmpObj, outResources, cmpResources, null, null);
    }

    private boolean compareContentStreamsByParsingExtended(PdfObject outObj, PdfObject cmpObj, PdfDictionary outResources, PdfDictionary cmpResources, ObjectPath currentPath, CompareResult compareResult) throws IOException {
        if (outObj.type() != outObj.type()) {
            compareResult.addError(currentPath, String.format("PdfObject. Types are different. Expected: %s. Found: %s", new Object[]{
                Integer.valueOf(cmpObj.type()), Integer.valueOf(outObj.type())}));
            return false;
        }

        if (outObj.isArray()) {
            PdfArray outArr = (PdfArray) outObj;
            PdfArray cmpArr = (PdfArray) cmpObj;
            if (cmpArr.size() != outArr.size()) {
                compareResult.addError(currentPath, String.format("PdfArray. Sizes are different. Expected: %s. Found: %s", new Object[]{
                    Integer.valueOf(cmpArr.size()), Integer.valueOf(outArr.size())}));
                return false;
            }
            for (int i = 0; i < cmpArr.size(); i++) {
                if (!compareContentStreamsByParsingExtended(outArr.getPdfObject(i), cmpArr.getPdfObject(i), outResources, cmpResources, currentPath, compareResult)) {
                    return false;
                }
            }
        }

        PRTokeniser cmpTokeniser = new PRTokeniser(new RandomAccessFileOrArray((new RandomAccessSourceFactory()).createSource(ContentByteUtils.getContentBytesFromContentObject(cmpObj))));

        PRTokeniser outTokeniser = new PRTokeniser(new RandomAccessFileOrArray((new RandomAccessSourceFactory()).createSource(ContentByteUtils.getContentBytesFromContentObject(outObj))));

        PdfContentParser cmpPs = new PdfContentParser(cmpTokeniser);
        PdfContentParser outPs = new PdfContentParser(outTokeniser);

        ArrayList<PdfObject> cmpOperands = new ArrayList<PdfObject>();
        ArrayList<PdfObject> outOperands = new ArrayList<PdfObject>();

        while (cmpPs.parse(cmpOperands).size() > 0) {
            outPs.parse(outOperands);
            if (cmpOperands.size() != outOperands.size()) {
                compareResult.addError(currentPath, String.format("PdfObject. Different commands lengths. Expected: %s. Found: %s", new Object[]{
                    Integer.valueOf(cmpOperands.size()), Integer.valueOf(outOperands.size())}));
                return false;
            }
            if (cmpOperands.size() == 1 && compareLiterals((PdfLiteral) cmpOperands.get(0), new PdfLiteral("BI")) && compareLiterals((PdfLiteral) outOperands.get(0), new PdfLiteral("BI"))) {
                PRStream cmpStr = (PRStream) cmpObj;
                PRStream outStr = (PRStream) outObj;
                if (null != outStr.getDirectObject(PdfName.RESOURCES) && null != cmpStr.getDirectObject(PdfName.RESOURCES)) {
                    outResources = (PdfDictionary) outStr.getDirectObject(PdfName.RESOURCES);
                    cmpResources = (PdfDictionary) cmpStr.getDirectObject(PdfName.RESOURCES);
                }
                if (!compareInlineImagesExtended(outPs, cmpPs, outResources, cmpResources, currentPath, compareResult)) {
                    return false;
                }
                continue;
            }
            for (int i = 0; i < cmpOperands.size(); i++) {
                if (!compareObjects(outOperands.get(i), cmpOperands.get(i), currentPath, compareResult)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean compareInlineImagesExtended(PdfContentParser outPs, PdfContentParser cmpPs, PdfDictionary outDict, PdfDictionary cmpDict, ObjectPath currentPath, CompareResult compareResult) throws IOException {
        InlineImageInfo cmpInfo = InlineImageUtils.parseInlineImage(cmpPs, cmpDict);
        InlineImageInfo outInfo = InlineImageUtils.parseInlineImage(outPs, outDict);
        return (compareObjects((PdfObject) outInfo.getImageDictionary(), (PdfObject) cmpInfo.getImageDictionary(), currentPath, compareResult)
                && Arrays.equals(outInfo.getSamples(), cmpInfo.getSamples()));
    }

    public boolean compareStreams(PRStream outStream, PRStream cmpStream) throws IOException {
        return compareStreamsExtended(outStream, cmpStream, null, null);
    }

    private boolean compareStreamsExtended(PRStream outStream, PRStream cmpStream, ObjectPath currentPath, CompareResult compareResult) throws IOException {
        boolean decodeStreams = PdfName.FLATEDECODE.equals(outStream.get(PdfName.FILTER));
        byte[] outStreamBytes = PdfReader.getStreamBytesRaw(outStream);
        byte[] cmpStreamBytes = PdfReader.getStreamBytesRaw(cmpStream);
        if (decodeStreams) {
            outStreamBytes = PdfReader.decodeBytes(outStreamBytes, (PdfDictionary) outStream);
            cmpStreamBytes = PdfReader.decodeBytes(cmpStreamBytes, (PdfDictionary) cmpStream);
        }
        if (this.floatComparisonError != 0.0D && PdfName.XOBJECT
                .equals(cmpStream.getDirectObject(PdfName.TYPE)) && PdfName.XOBJECT
                .equals(outStream.getDirectObject(PdfName.TYPE)) && PdfName.FORM
                .equals(cmpStream.getDirectObject(PdfName.SUBTYPE)) && PdfName.FORM
                .equals(outStream.getDirectObject(PdfName.SUBTYPE))) {
            return (compareContentStreamsByParsingExtended((PdfObject) outStream, (PdfObject) cmpStream, outStream.getAsDict(PdfName.RESOURCES), cmpStream.getAsDict(PdfName.RESOURCES), currentPath, compareResult)
                    && compareDictionariesExtended((PdfDictionary) outStream, (PdfDictionary) cmpStream, currentPath, compareResult));
        }
        if (Arrays.equals(outStreamBytes, cmpStreamBytes)) {
            return compareDictionariesExtended((PdfDictionary) outStream, (PdfDictionary) cmpStream, currentPath, compareResult);
        }
        if (cmpStreamBytes.length != outStreamBytes.length) {
            if (compareResult != null && currentPath != null) {
                compareResult.addError(currentPath, String.format("PRStream. Lengths are different. Expected: %s. Found: %s", new Object[]{Integer.valueOf(cmpStreamBytes.length), Integer.valueOf(outStreamBytes.length)}));
            }
        } else {
            for (int i = 0; i < cmpStreamBytes.length; i++) {
                if (cmpStreamBytes[i] != outStreamBytes[i]) {
                    int l = Math.max(0, i - 10);
                    int r = Math.min(cmpStreamBytes.length, i + 10);
                    if (compareResult != null && currentPath != null) {
                        currentPath.pushOffsetToPath(i);
                        compareResult.addError(currentPath, String.format("PRStream. The bytes differ at index %s. Expected: %s (%s). Found: %s (%s)", new Object[]{
                            Integer.valueOf(i), new String(new byte[]{cmpStreamBytes[i]}), (new String(cmpStreamBytes, l, r - l)).replaceAll("\\n", "\\\\n"), new String(new byte[]{outStreamBytes[i]}), (new String(outStreamBytes, l, r - l))
                            .replaceAll("\\n", "\\\\n")}));
                        currentPath.pop();
                    }
                }
            }
        }
        return false;
    }

    public boolean compareArrays(PdfArray outArray, PdfArray cmpArray) throws IOException {
        return compareArraysExtended(outArray, cmpArray, null, null);
    }

    private boolean compareArraysExtended(PdfArray outArray, PdfArray cmpArray, ObjectPath currentPath, CompareResult compareResult) throws IOException {
        if (outArray == null) {
            if (compareResult != null && currentPath != null) {
                compareResult.addError(currentPath, "Found null. Expected PdfArray.");
            }
            return false;
        }
        if (outArray.size() != cmpArray.size()) {
            if (compareResult != null && currentPath != null) {
                compareResult.addError(currentPath, String.format("PdfArrays. Lengths are different. Expected: %s. Found: %s.", new Object[]{Integer.valueOf(cmpArray.size()), Integer.valueOf(outArray.size())}));
            }
            return false;
        }
        boolean arraysAreEqual = true;
        for (int i = 0; i < cmpArray.size(); i++) {
            if (currentPath != null) {
                currentPath.pushArrayItemToPath(i);
            }
            arraysAreEqual = (compareObjects(outArray.getPdfObject(i), cmpArray.getPdfObject(i), currentPath, compareResult) && arraysAreEqual);
            if (currentPath != null) {
                currentPath.pop();
            }
            if (!arraysAreEqual && (currentPath == null || compareResult == null || compareResult.isMessageLimitReached())) {
                return false;
            }
        }
        return arraysAreEqual;
    }

    public boolean compareNames(PdfName outName, PdfName cmpName) {
        return (cmpName.compareTo(outName) == 0);
    }

    private boolean compareNamesExtended(PdfName outName, PdfName cmpName, ObjectPath currentPath, CompareResult compareResult) {
        if (cmpName.compareTo(outName) == 0) {
            return true;
        }
        if (compareResult != null && currentPath != null) {
            compareResult.addError(currentPath, String.format("PdfName. Expected: %s. Found: %s", new Object[]{cmpName.toString(), outName.toString()}));
        }
        return false;
    }

    public boolean compareNumbers(PdfNumber outNumber, PdfNumber cmpNumber) {
        double difference = Math.abs(outNumber.doubleValue() - cmpNumber.doubleValue());
        if (!this.absoluteError && cmpNumber.doubleValue() != 0.0D) {
            difference /= cmpNumber.doubleValue();
        }
        return (difference <= this.floatComparisonError);
    }

    private boolean compareNumbersExtended(PdfNumber outNumber, PdfNumber cmpNumber, ObjectPath currentPath, CompareResult compareResult) {
        if (compareNumbers(outNumber, cmpNumber)) {
            return true;
        }
        if (compareResult != null && currentPath != null) {
            compareResult.addError(currentPath, String.format("PdfNumber. Expected: %s. Found: %s", new Object[]{cmpNumber, outNumber}));
        }
        return false;
    }

    public boolean compareStrings(PdfString outString, PdfString cmpString) {
        return Arrays.equals(cmpString.getBytes(), outString.getBytes());
    }

    private boolean compareStringsExtended(PdfString outString, PdfString cmpString, ObjectPath currentPath, CompareResult compareResult) {
        if (Arrays.equals(cmpString.getBytes(), outString.getBytes())) {
            return true;
        }
        String cmpStr = cmpString.toUnicodeString();
        String outStr = outString.toUnicodeString();
        if (cmpStr.length() != outStr.length()) {
            if (compareResult != null && currentPath != null) {
                compareResult.addError(currentPath, String.format("PdfString. Lengths are different. Expected: %s. Found: %s", new Object[]{Integer.valueOf(cmpStr.length()), Integer.valueOf(outStr.length())}));
            }
        } else {
            for (int i = 0; i < cmpStr.length(); i++) {
                if (cmpStr.charAt(i) != outStr.charAt(i)) {
                    int l = Math.max(0, i - 10);
                    int r = Math.min(cmpStr.length(), i + 10);
                    if (compareResult != null && currentPath != null) {
                        currentPath.pushOffsetToPath(i);
                        compareResult.addError(currentPath, String.format("PdfString. Characters differ at position %s. Expected: %s (%s). Found: %s (%s).", new Object[]{
                            Integer.valueOf(i), Character.toString(cmpStr.charAt(i)), cmpStr.substring(l, r).replace("\n", "\\n"),
                            Character.toString(outStr.charAt(i)), outStr.substring(l, r).replace("\n", "\\n")}));
                        currentPath.pop();
                    }
                    break;
                }
            }
        }
        return false;
    }

    public boolean compareLiterals(PdfLiteral outLiteral, PdfLiteral cmpLiteral) {
        return Arrays.equals(cmpLiteral.getBytes(), outLiteral.getBytes());
    }

    private boolean compareLiteralsExtended(PdfLiteral outLiteral, PdfLiteral cmpLiteral, ObjectPath currentPath, CompareResult compareResult) {
        if (compareLiterals(outLiteral, cmpLiteral)) {
            return true;
        }
        if (compareResult != null && currentPath != null) {
            compareResult.addError(currentPath, String.format("PdfLiteral. Expected: %s. Found: %s", new Object[]{cmpLiteral, outLiteral}));
        }
        return false;
    }

    public boolean compareBooleans(PdfBoolean outBoolean, PdfBoolean cmpBoolean) {
        return Arrays.equals(cmpBoolean.getBytes(), outBoolean.getBytes());
    }

    private boolean compareBooleansExtended(PdfBoolean outBoolean, PdfBoolean cmpBoolean, ObjectPath currentPath, CompareResult compareResult) {
        if (cmpBoolean.booleanValue() == outBoolean.booleanValue()) {
            return true;
        }
        if (compareResult != null && currentPath != null) {
            compareResult.addError(currentPath, String.format("PdfBoolean. Expected: %s. Found: %s.", new Object[]{Boolean.valueOf(cmpBoolean.booleanValue()), Boolean.valueOf(outBoolean.booleanValue())}));
        }
        return false;
    }

    public String compareXmp(byte[] xmp1, byte[] xmp2) {
        return compareXmp(xmp1, xmp2, false);
    }

    public String compareXmp(byte[] xmp1, byte[] xmp2, boolean ignoreDateAndProducerProperties) {
        try {
            if (ignoreDateAndProducerProperties) {
                XMPMeta xmpMeta = XMPMetaFactory.parseFromBuffer(xmp1);

                XMPUtils.removeProperties(xmpMeta, "http://ns.adobe.com/xap/1.0/", "CreateDate", true, true);
                XMPUtils.removeProperties(xmpMeta, "http://ns.adobe.com/xap/1.0/", "ModifyDate", true, true);
                XMPUtils.removeProperties(xmpMeta, "http://ns.adobe.com/xap/1.0/", "MetadataDate", true, true);
                XMPUtils.removeProperties(xmpMeta, "http://ns.adobe.com/pdf/1.3/", "Producer", true, true);

                xmp1 = XMPMetaFactory.serializeToBuffer(xmpMeta, new SerializeOptions(8192));

                xmpMeta = XMPMetaFactory.parseFromBuffer(xmp2);
                XMPUtils.removeProperties(xmpMeta, "http://ns.adobe.com/xap/1.0/", "CreateDate", true, true);
                XMPUtils.removeProperties(xmpMeta, "http://ns.adobe.com/xap/1.0/", "ModifyDate", true, true);
                XMPUtils.removeProperties(xmpMeta, "http://ns.adobe.com/xap/1.0/", "MetadataDate", true, true);
                XMPUtils.removeProperties(xmpMeta, "http://ns.adobe.com/pdf/1.3/", "Producer", true, true);

                xmp2 = XMPMetaFactory.serializeToBuffer(xmpMeta, new SerializeOptions(8192));
            }

            if (!compareXmls(xmp1, xmp2)) {
                return "The XMP packages different!";
            }
        } catch (XMPException xmpExc) {
            return "XMP parsing failure!";
        } catch (IOException ioExc) {
            return "XMP parsing failure!";
        } catch (ParserConfigurationException parseExc) {
            return "XMP parsing failure!";
        } catch (SAXException parseExc) {
            return "XMP parsing failure!";
        }
        return null;
    }

    public String compareXmp(String outPdf, String cmpPdf) {
        return compareXmp(outPdf, cmpPdf, false);
    }

    public String compareXmp(String outPdf, String cmpPdf, boolean ignoreDateAndProducerProperties) {
        init(outPdf, cmpPdf);
        PdfReader cmpReader = null;
        PdfReader outReader = null;
        try {
            cmpReader = new PdfReader(this.cmpPdf);
            outReader = new PdfReader(this.outPdf);
            byte[] cmpBytes = cmpReader.getMetadata(), outBytes = outReader.getMetadata();
            return compareXmp(cmpBytes, outBytes, ignoreDateAndProducerProperties);
        } catch (IOException e) {
            return "XMP parsing failure!";
        } finally {

            if (cmpReader != null) {
                cmpReader.close();
            }
            if (outReader != null) {
                outReader.close();
            }
        }
    }

    public boolean compareXmls(byte[] xml1, byte[] xml2) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setCoalescing(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setIgnoringComments(true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc1 = db.parse(new ByteArrayInputStream(xml1));
        doc1.normalizeDocument();

        Document doc2 = db.parse(new ByteArrayInputStream(xml2));
        doc2.normalizeDocument();

        return doc2.isEqualNode(doc1);
    }

    public String compareDocumentInfo(String outPdf, String cmpPdf) throws IOException {
        System.out.print("[itext] INFO  Comparing document info.......");
        String message = null;
        PdfReader outReader = new PdfReader(outPdf);
        PdfReader cmpReader = new PdfReader(cmpPdf);
        String[] cmpInfo = convertInfo(cmpReader.getInfo());
        String[] outInfo = convertInfo(outReader.getInfo());
        for (int i = 0; i < cmpInfo.length; i++) {
            if (!cmpInfo[i].equals(outInfo[i])) {
                message = "Document info fail";
                break;
            }
        }
        outReader.close();
        cmpReader.close();

        if (message == null) {
            System.out.println("OK");
        } else {
            System.out.println("Fail");
        }
        System.out.flush();
        return message;
    }

    private boolean linksAreSame(PdfAnnotation.PdfImportedLink cmpLink, PdfAnnotation.PdfImportedLink outLink) {
        if (cmpLink.getDestinationPage() != outLink.getDestinationPage()) {
            return false;
        }
        if (!cmpLink.getRect().toString().equals(outLink.getRect().toString())) {
            return false;
        }
        Map<PdfName, PdfObject> cmpParams = cmpLink.getParameters();
        Map<PdfName, PdfObject> outParams = outLink.getParameters();
        if (cmpParams.size() != outParams.size()) {
            return false;
        }
        for (Map.Entry<PdfName, PdfObject> cmpEntry : cmpParams.entrySet()) {
            PdfObject cmpObj = cmpEntry.getValue();
            if (!outParams.containsKey(cmpEntry.getKey())) {
                return false;
            }
            PdfObject outObj = outParams.get(cmpEntry.getKey());
            if (cmpObj.type() != outObj.type()) {
                return false;
            }
            switch (cmpObj.type()) {
                case 1:
                case 2:
                case 3:
                case 4:
                case 8:
                    if (!cmpObj.toString().equals(outObj.toString())) {
                        return false;
                    }
            }

        }
        return true;
    }

    public String compareLinks(String outPdf, String cmpPdf) throws IOException {
        System.out.print("[itext] INFO  Comparing link annotations....");
        String message = null;
        PdfReader outReader = new PdfReader(outPdf);
        PdfReader cmpReader = new PdfReader(cmpPdf);
        for (int i = 0; i < outReader.getNumberOfPages() && i < cmpReader.getNumberOfPages(); i++) {
            List<PdfAnnotation.PdfImportedLink> outLinks = outReader.getLinks(i + 1);
            List<PdfAnnotation.PdfImportedLink> cmpLinks = cmpReader.getLinks(i + 1);
            if (cmpLinks.size() != outLinks.size()) {
                message = String.format("Different number of links on page %d.", new Object[]{Integer.valueOf(i + 1)});
                break;
            }
            for (int j = 0; j < cmpLinks.size(); j++) {
                if (!linksAreSame(cmpLinks.get(j), outLinks.get(j))) {
                    message = String.format("Different links on page %d.\n%s\n%s", new Object[]{Integer.valueOf(i + 1), ((PdfAnnotation.PdfImportedLink) cmpLinks.get(j)).toString(), ((PdfAnnotation.PdfImportedLink) outLinks.get(j)).toString()});
                    break;
                }
            }
        }
        outReader.close();
        cmpReader.close();
        if (message == null) {
            System.out.println("OK");
        } else {
            System.out.println("Fail");
        }
        System.out.flush();
        return message;
    }

    public String compareTagStructures(String outPdf, String cmpPdf) throws IOException, ParserConfigurationException, SAXException {
        System.out.print("[itext] INFO  Comparing tag structures......");

        String outXml = outPdf.replace(".pdf", ".xml");
        String cmpXml = outPdf.replace(".pdf", ".cmp.xml");

        String message = null;
        PdfReader reader = new PdfReader(outPdf);
        FileOutputStream xmlOut1 = new FileOutputStream(outXml);
        (new CmpTaggedPdfReaderTool()).convertToXml(reader, xmlOut1);
        reader.close();
        reader = new PdfReader(cmpPdf);
        FileOutputStream xmlOut2 = new FileOutputStream(cmpXml);
        (new CmpTaggedPdfReaderTool()).convertToXml(reader, xmlOut2);
        reader.close();
        if (!compareXmls(outXml, cmpXml)) {
            message = "The tag structures are different.";
        }
        xmlOut1.close();
        xmlOut2.close();
        if (message == null) {
            System.out.println("OK");
        } else {
            System.out.println("Fail");
        }
        System.out.flush();
        return message;
    }

    private String[] convertInfo(HashMap<String, String> info) {
        String[] convertedInfo = {"", "", "", ""};
        for (Map.Entry<String, String> entry : info.entrySet()) {
            if ("title".equalsIgnoreCase(entry.getKey())) {
                convertedInfo[0] = entry.getValue();
                continue;
            }
            if ("author".equalsIgnoreCase(entry.getKey())) {
                convertedInfo[1] = entry.getValue();
                continue;
            }
            if ("subject".equalsIgnoreCase(entry.getKey())) {
                convertedInfo[2] = entry.getValue();
                continue;
            }
            if ("keywords".equalsIgnoreCase(entry.getKey())) {
                convertedInfo[3] = entry.getValue();
            }
        }
        return convertedInfo;
    }

    public boolean compareXmls(String xml1, String xml2) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setCoalescing(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setIgnoringComments(true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc1 = db.parse(new File(xml1));
        doc1.normalizeDocument();

        Document doc2 = db.parse(new File(xml2));
        doc2.normalizeDocument();

        return doc2.isEqualNode(doc1);
    }

    private void init(String outPdf, String cmpPdf) {
        this.outPdf = outPdf;
        this.cmpPdf = cmpPdf;
        this.outPdfName = (new File(outPdf)).getName();
        this.cmpPdfName = (new File(cmpPdf)).getName();
        this.outImage = this.outPdfName + "-%03d.png";
        if (this.cmpPdfName.startsWith("cmp_")) {
            this.cmpImage = this.cmpPdfName + "-%03d.png";
        } else {
            this.cmpImage = "cmp_" + this.cmpPdfName + "-%03d.png";
        }

    }

    private boolean compareStreams(InputStream is1, InputStream is2) throws IOException {
        int len1;
        byte[] buffer1 = new byte[65536];
        byte[] buffer2 = new byte[65536];

        do {
            len1 = is1.read(buffer1);
            int len2 = is2.read(buffer2);
            if (len1 != len2) {
                return false;
            }
            if (!Arrays.equals(buffer1, buffer2)) {
                return false;
            }
        } while (len1 != -1);

        return true;
    }

    class PngFileFilter
            implements FileFilter {

        public boolean accept(File pathname) {
            String ap = pathname.getAbsolutePath();
            boolean b1 = ap.endsWith(".png");
            boolean b2 = ap.contains("cmp_");
            return (b1 && !b2 && ap.contains(CompareTool.this.outPdfName));
        }
    }

    class CmpPngFileFilter implements FileFilter {

        public boolean accept(File pathname) {
            String ap = pathname.getAbsolutePath();
            boolean b1 = ap.endsWith(".png");
            boolean b2 = ap.contains("cmp_");
            return (b1 && b2 && ap.contains(CompareTool.this.cmpPdfName));
        }
    }

    class ImageNameComparator implements Comparator<File> {

        public int compare(File f1, File f2) {
            String f1Name = f1.getAbsolutePath();
            String f2Name = f2.getAbsolutePath();
            return f1Name.compareTo(f2Name);
        }
    }

    class CmpTaggedPdfReaderTool
            extends TaggedPdfReaderTool {

        Map<PdfDictionary, Map<Integer, String>> parsedTags = new HashMap<PdfDictionary, Map<Integer, String>>();

        public void parseTag(String tag, PdfObject object, PdfDictionary page) throws IOException {
            if (object instanceof PdfNumber) {

                if (!this.parsedTags.containsKey(page)) {
                    CompareTool.CmpMarkedContentRenderFilter listener = new CompareTool.CmpMarkedContentRenderFilter();

                    PdfContentStreamProcessor processor = new PdfContentStreamProcessor(listener);

                    processor.processContent(PdfReader.getPageContent(page), page
                            .getAsDict(PdfName.RESOURCES));

                    this.parsedTags.put(page, listener.getParsedTagContent());
                }

                String tagContent = "";
                if (((Map) this.parsedTags.get(page)).containsKey(Integer.valueOf(((PdfNumber) object).intValue()))) {
                    tagContent = (String) ((Map) this.parsedTags.get(page)).get(Integer.valueOf(((PdfNumber) object).intValue()));
                }
                this.out.print(XMLUtil.escapeXML(tagContent, true));
            } else {

                super.parseTag(tag, object, page);
            }
        }

        public void inspectChildDictionary(PdfDictionary k) throws IOException {
            inspectChildDictionary(k, true);
        }
    }

    class CmpMarkedContentRenderFilter
            implements RenderListener {

        Map<Integer, TextExtractionStrategy> tagsByMcid = new HashMap<Integer, TextExtractionStrategy>();

        public Map<Integer, String> getParsedTagContent() {
            Map<Integer, String> content = new HashMap<Integer, String>();
            for (Iterator<Integer> iterator = this.tagsByMcid.keySet().iterator(); iterator.hasNext();) {
                int id = ((Integer) iterator.next()).intValue();
                content.put(Integer.valueOf(id), ((TextExtractionStrategy) this.tagsByMcid.get(Integer.valueOf(id))).getResultantText());
            }

            return content;
        }

        public void beginTextBlock() {
            for (Iterator<Integer> iterator = this.tagsByMcid.keySet().iterator(); iterator.hasNext();) {
                int id = ((Integer) iterator.next()).intValue();
                ((TextExtractionStrategy) this.tagsByMcid.get(Integer.valueOf(id))).beginTextBlock();
            }

        }

        public void renderText(TextRenderInfo renderInfo) {
            Integer mcid = renderInfo.getMcid();
            if (mcid != null && this.tagsByMcid.containsKey(mcid)) {
                ((TextExtractionStrategy) this.tagsByMcid.get(mcid)).renderText(renderInfo);
            } else if (mcid != null) {
                this.tagsByMcid.put(mcid, new SimpleTextExtractionStrategy());
                ((TextExtractionStrategy) this.tagsByMcid.get(mcid)).renderText(renderInfo);
            }
        }

        public void endTextBlock() {
            for (Iterator<Integer> iterator = this.tagsByMcid.keySet().iterator(); iterator.hasNext();) {
                int id = ((Integer) iterator.next()).intValue();
                ((TextExtractionStrategy) this.tagsByMcid.get(Integer.valueOf(id))).endTextBlock();
            }

        }

        public void renderImage(ImageRenderInfo renderInfo) {
        }
    }
}

