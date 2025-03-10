package com.emudhra.esign;

import esign.text.*;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfTemplate;
import java.net.URL;

public class ImgTemplate
        extends Image {

    public ImgTemplate(Image image) {
        super(image);
    }

    public ImgTemplate(PdfTemplate template) throws BadElementException {
        super((URL) null);
        if (template == null) {
            throw new BadElementException(MessageLocalization.getComposedMessage("the.template.can.not.be.null", new Object[0]));
        }
        if (template.getType() == 3) {
            throw new BadElementException(MessageLocalization.getComposedMessage("a.pattern.can.not.be.used.as.a.template.to.create.an.image", new Object[0]));
        }
        this.type = 35;
        this.scaledHeight = template.getHeight();
        setTop(this.scaledHeight);
        this.scaledWidth = template.getWidth();
        setRight(this.scaledWidth);
        setTemplateData(template);
        this.plainWidth = getWidth();
        this.plainHeight = getHeight();
    }
}
