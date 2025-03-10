package esign.text.pdf;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

public class PdfLister {

    PrintStream out;

    public PdfLister(PrintStream out) {
        this.out = out;
    }

    public void listAnyObject(PdfObject object) {
        switch (object.type()) {
            case 5:
                listArray((PdfArray) object);
                return;
            case 6:
                listDict((PdfDictionary) object);
                return;
            case 3:
                this.out.println("(" + object.toString() + ")");
                return;
        }
        this.out.println(object.toString());
    }

    public void listDict(PdfDictionary dictionary) {
        this.out.println("<<");

        for (PdfName key : dictionary.getKeys()) {
            PdfObject value = dictionary.get(key);
            this.out.print(key.toString());
            this.out.print(' ');
            listAnyObject(value);
        }
        this.out.println(">>");
    }

    public void listArray(PdfArray array) {
        this.out.println('[');
        for (Iterator<PdfObject> i = array.listIterator(); i.hasNext();) {
            PdfObject item = i.next();
            listAnyObject(item);
        }
        this.out.println(']');
    }

    public void listStream(PRStream stream, PdfReaderInstance reader) {
        try {
            listDict(stream);
            this.out.println("startstream");
            byte[] b = PdfReader.getStreamBytes(stream);

            int len = b.length - 1;
            for (int k = 0; k < len; k++) {
                if (b[k] == 13 && b[k + 1] != 10) {
                    b[k] = 10;
                }
            }
            this.out.println(new String(b));
            this.out.println("endstream");
        } catch (IOException e) {
            System.err.println("I/O exception: " + e);
        }
    }

    public void listPage(PdfImportedPage iPage) {
        Iterator<PdfObject> i;
        int pageNum = iPage.getPageNumber();
        PdfReaderInstance readerInst = iPage.getPdfReaderInstance();
        PdfReader reader = readerInst.getReader();

        PdfDictionary page = reader.getPageN(pageNum);
        listDict(page);
        PdfObject obj = PdfReader.getPdfObject(page.get(PdfName.CONTENTS));
        if (obj == null) {
            return;
        }
        switch (obj.type) {
            case 7:
                listStream((PRStream) obj, readerInst);
                break;
            case 5:
                for (i = ((PdfArray) obj).listIterator(); i.hasNext();) {
                    PdfObject o = PdfReader.getPdfObject(i.next());
                    listStream((PRStream) o, readerInst);
                    this.out.println("-----------");
                }
                break;
        }
    }
}
