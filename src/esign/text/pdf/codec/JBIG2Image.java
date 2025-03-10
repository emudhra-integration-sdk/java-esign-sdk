package esign.text.pdf.codec;

import esign.text.ExceptionConverter;
import esign.text.Image;
import esign.text.ImgJBIG2;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.RandomAccessFileOrArray;

public class JBIG2Image {

    public static byte[] getGlobalSegment(RandomAccessFileOrArray ra) {
        try {
            JBIG2SegmentReader sr = new JBIG2SegmentReader(ra);
            sr.read();
            return sr.getGlobal(true);
        } catch (Exception e) {
            return null;
        }
    }

    public static Image getJbig2Image(RandomAccessFileOrArray ra, int page) {
        if (page < 1) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.page.number.must.be.gt.eq.1", new Object[0]));
        }
        try {
            JBIG2SegmentReader sr = new JBIG2SegmentReader(ra);
            sr.read();
            JBIG2SegmentReader.JBIG2Page p = sr.getPage(page);
            return (Image) new ImgJBIG2(p.pageBitmapWidth, p.pageBitmapHeight, p.getData(true), sr.getGlobal(true));
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public static int getNumberOfPages(RandomAccessFileOrArray ra) {
        try {
            JBIG2SegmentReader sr = new JBIG2SegmentReader(ra);
            sr.read();
            return sr.numberOfPages();
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }
}
