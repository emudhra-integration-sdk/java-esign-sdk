package esign.text.pdf.parser;

import esign.text.pdf.PRIndirectReference;
import esign.text.pdf.PRStream;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ListIterator;

public class ContentByteUtils {

    public static byte[] getContentBytesFromContentObject(PdfObject contentObject) throws IOException {
        byte[] result;
        PRIndirectReference ref;
        PdfObject directObject;
        PRStream stream;
        ByteArrayOutputStream allBytes;
        PdfArray contentArray;
        ListIterator<PdfObject> iter;
        switch (contentObject.type()) {

            case 10:
                ref = (PRIndirectReference) contentObject;
                directObject = PdfReader.getPdfObjectRelease((PdfObject) ref);
                result = getContentBytesFromContentObject(directObject);

                return result;
            case 7:
                stream = (PRStream) PdfReader.getPdfObjectRelease(contentObject);
                result = PdfReader.getStreamBytes(stream);
                return result;
            case 5:
                allBytes = new ByteArrayOutputStream();
                contentArray = (PdfArray) contentObject;
                iter = contentArray.listIterator();
                while (iter.hasNext()) {
                    PdfObject element = iter.next();
                    allBytes.write(getContentBytesFromContentObject(element));
                    allBytes.write(32);
                }
                result = allBytes.toByteArray();
                return result;
        }
        String msg = "Unable to handle Content of type " + contentObject.getClass();
        throw new IllegalStateException(msg);
    }

    public static byte[] getContentBytesForPage(PdfReader reader, int pageNum) throws IOException {
        PdfDictionary pageDictionary = reader.getPageN(pageNum);
        PdfObject contentObject = pageDictionary.get(PdfName.CONTENTS);
        if (contentObject == null) {
            return new byte[0];
        }
        byte[] contentBytes = getContentBytesFromContentObject(contentObject);
        return contentBytes;
    }
}
