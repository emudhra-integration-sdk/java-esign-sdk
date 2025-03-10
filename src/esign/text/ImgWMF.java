package esign.text;

import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfContentByte;
import esign.text.pdf.PdfTemplate;
import esign.text.pdf.codec.wmf.InputMeta;
import esign.text.pdf.codec.wmf.MetaDo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ImgWMF
        extends Image {

    ImgWMF(Image image) {
        super(image);
    }

    public ImgWMF(URL url) throws BadElementException, IOException {
        super(url);
        processParameters();
    }

    public ImgWMF(String filename) throws BadElementException, MalformedURLException, IOException {
        this(Utilities.toURL(filename));
    }

    public ImgWMF(byte[] img) throws BadElementException, IOException {
        super((URL) null);
        this.rawData = img;
        this.originalData = img;
        processParameters();
    }

    private void processParameters() throws BadElementException, IOException {
        this.type = 35;
        this.originalType = 6;
        InputStream is = null;
        try {
            String errorID;
            if (this.rawData == null) {
                is = this.url.openStream();
                errorID = this.url.toString();
            } else {

                is = new ByteArrayInputStream(this.rawData);
                errorID = "Byte array";
            }
            InputMeta in = new InputMeta(is);
            if (in.readInt() != -1698247209) {
                throw new BadElementException(MessageLocalization.getComposedMessage("1.is.not.a.valid.placeable.windows.metafile", new Object[]{errorID}));
            }
            in.readWord();
            int left = in.readShort();
            int top = in.readShort();
            int right = in.readShort();
            int bottom = in.readShort();
            int inch = in.readWord();
            this.dpiX = 72;
            this.dpiY = 72;
            this.scaledHeight = (bottom - top) / inch * 72.0F;
            setTop(this.scaledHeight);
            this.scaledWidth = (right - left) / inch * 72.0F;
            setRight(this.scaledWidth);
        } finally {

            if (is != null) {
                is.close();
            }
            this.plainWidth = getWidth();
            this.plainHeight = getHeight();
        }
    }

    public void readWMF(PdfTemplate template) throws IOException, DocumentException {
        setTemplateData(template);
        template.setWidth(getWidth());
        template.setHeight(getHeight());
        InputStream is = null;
        try {
            if (this.rawData == null) {
                is = this.url.openStream();
            } else {

                is = new ByteArrayInputStream(this.rawData);
            }
            MetaDo meta = new MetaDo(is, (PdfContentByte) template);
            meta.readAll();
        } finally {

            if (is != null) {
                is.close();
            }
        }
    }
}
