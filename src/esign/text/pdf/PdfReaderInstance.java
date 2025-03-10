package esign.text.pdf;

import esign.text.error_messages.MessageLocalization;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

class PdfReaderInstance {

    static final PdfLiteral IDENTITYMATRIX = new PdfLiteral("[1 0 0 1 0 0]");
    static final PdfNumber ONE = new PdfNumber(1);
    int[] myXref;
    PdfReader reader;
    RandomAccessFileOrArray file;
    HashMap<Integer, PdfImportedPage> importedPages = new HashMap<Integer, PdfImportedPage>();
    PdfWriter writer;
    HashSet<Integer> visited = new HashSet<Integer>();
    ArrayList<Integer> nextRound = new ArrayList<Integer>();

    PdfReaderInstance(PdfReader reader, PdfWriter writer) {
        this.reader = reader;
        this.writer = writer;
        this.file = reader.getSafeFile();
        this.myXref = new int[reader.getXrefSize()];
    }

    PdfReader getReader() {
        return this.reader;
    }

    PdfImportedPage getImportedPage(int pageNumber) {
        if (!this.reader.isOpenedWithFullPermissions()) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("pdfreader.not.opened.with.owner.password", new Object[0]));
        }
        if (pageNumber < 1 || pageNumber > this.reader.getNumberOfPages()) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("invalid.page.number.1", pageNumber));
        }
        Integer i = Integer.valueOf(pageNumber);
        PdfImportedPage pageT = this.importedPages.get(i);
        if (pageT == null) {
            pageT = new PdfImportedPage(this, this.writer, pageNumber);
            this.importedPages.put(i, pageT);
        }
        return pageT;
    }

    int getNewObjectNumber(int number, int generation) {
        if (this.myXref[number] == 0) {
            this.myXref[number] = this.writer.getIndirectReferenceNumber();
            this.nextRound.add(Integer.valueOf(number));
        }
        return this.myXref[number];
    }

    RandomAccessFileOrArray getReaderFile() {
        return this.file;
    }

    PdfObject getResources(int pageNumber) {
        PdfObject obj = PdfReader.getPdfObjectRelease(this.reader.getPageNRelease(pageNumber).get(PdfName.RESOURCES));
        return obj;
    }

    PdfStream getFormXObject(int pageNumber, int compressionLevel) throws IOException {
        PRStream stream;
        PdfDictionary page = this.reader.getPageNRelease(pageNumber);
        PdfObject contents = PdfReader.getPdfObjectRelease(page.get(PdfName.CONTENTS));
        PdfDictionary dic = new PdfDictionary();
        byte[] bout = null;
        if (contents != null) {
            if (contents.isStream()) {
                dic.putAll((PRStream) contents);
            } else {
                bout = this.reader.getPageContent(pageNumber, this.file);
            }
        } else {
            bout = new byte[0];
        }
        dic.put(PdfName.RESOURCES, PdfReader.getPdfObjectRelease(page.get(PdfName.RESOURCES)));
        dic.put(PdfName.TYPE, PdfName.XOBJECT);
        dic.put(PdfName.SUBTYPE, PdfName.FORM);
        PdfImportedPage impPage = this.importedPages.get(Integer.valueOf(pageNumber));
        dic.put(PdfName.BBOX, new PdfRectangle(impPage.getBoundingBox()));
        PdfArray matrix = impPage.getMatrix();
        if (matrix == null) {
            dic.put(PdfName.MATRIX, IDENTITYMATRIX);
        } else {
            dic.put(PdfName.MATRIX, matrix);
        }
        dic.put(PdfName.FORMTYPE, ONE);

        if (bout == null) {
            stream = new PRStream((PRStream) contents, dic);
        } else {

            stream = new PRStream(this.reader, bout, compressionLevel);
            stream.putAll(dic);
        }
        return stream;
    }

    void writeAllVisited() throws IOException {
        while (!this.nextRound.isEmpty()) {
            ArrayList<Integer> vec = this.nextRound;
            this.nextRound = new ArrayList<Integer>();
            for (int k = 0; k < vec.size(); k++) {
                Integer i = vec.get(k);
                if (!this.visited.contains(i)) {
                    this.visited.add(i);
                    int n = i.intValue();
                    this.writer.addToBody(this.reader.getPdfObjectRelease(n), this.myXref[n]);
                }
            }
        }
    }

    public void writeAllPages() throws IOException {
        try {
            this.file.reOpen();
            for (PdfImportedPage element : this.importedPages.values()) {
                PdfImportedPage ip = element;
                if (ip.isToCopy()) {
                    this.writer.addToBody(ip.getFormXObject(this.writer.getCompressionLevel()), ip.getIndirectReference());
                    ip.setCopied();
                }
            }
            writeAllVisited();
        } finally {

            try {

                this.file.close();
            } catch (Exception exception) {
            }
        }
    }
}
