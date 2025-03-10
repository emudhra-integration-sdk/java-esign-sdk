package esign.text.pdf.events;

import esign.text.Document;
import esign.text.DocumentException;
import esign.text.ExceptionConverter;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfAnnotation;
import esign.text.pdf.PdfContentByte;
import esign.text.pdf.PdfFormField;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfPCell;
import esign.text.pdf.PdfPCellEvent;
import esign.text.pdf.PdfPageEventHelper;
import esign.text.pdf.PdfRectangle;
import esign.text.pdf.PdfWriter;
import esign.text.pdf.TextField;
import java.io.IOException;
import java.util.HashMap;

public class FieldPositioningEvents
        extends PdfPageEventHelper
        implements PdfPCellEvent {

    protected HashMap<String, PdfFormField> genericChunkFields = new HashMap<String, PdfFormField>();

    protected PdfFormField cellField = null;

    protected PdfWriter fieldWriter = null;

    protected PdfFormField parent = null;

    public float padding;

    public FieldPositioningEvents() {
    }

    public void addField(String text, PdfFormField field) {
        this.genericChunkFields.put(text, field);
    }

    public FieldPositioningEvents(PdfWriter writer, PdfFormField field) {
        this.cellField = field;
        this.fieldWriter = writer;
    }

    public FieldPositioningEvents(PdfFormField parent, PdfFormField field) {
        this.cellField = field;
        this.parent = parent;
    }

    public FieldPositioningEvents(PdfWriter writer, String text) throws IOException, DocumentException {
        this.fieldWriter = writer;
        TextField tf = new TextField(writer, new Rectangle(0.0F, 0.0F), text);
        tf.setFontSize(14.0F);
        this.cellField = tf.getTextField();
    }

    public FieldPositioningEvents(PdfWriter writer, PdfFormField parent, String text) throws IOException, DocumentException {
        this.parent = parent;
        TextField tf = new TextField(writer, new Rectangle(0.0F, 0.0F), text);
        tf.setFontSize(14.0F);
        this.cellField = tf.getTextField();
    }

    public void setPadding(float padding) {
        this.padding = padding;
    }

    public void setParent(PdfFormField parent) {
        this.parent = parent;
    }

    public void onGenericTag(PdfWriter writer, Document document, Rectangle rect, String text) {
        rect.setBottom(rect.getBottom() - 3.0F);
        PdfFormField field = this.genericChunkFields.get(text);
        if (field == null) {
            TextField tf = new TextField(writer, new Rectangle(rect.getLeft(this.padding), rect.getBottom(this.padding), rect.getRight(this.padding), rect.getTop(this.padding)), text);
            tf.setFontSize(14.0F);
            try {
                field = tf.getTextField();
            } catch (Exception e) {
                throw new ExceptionConverter(e);
            }
        } else {

            field.put(PdfName.RECT, (PdfObject) new PdfRectangle(rect.getLeft(this.padding), rect.getBottom(this.padding), rect.getRight(this.padding), rect.getTop(this.padding)));
        }
        if (this.parent == null) {
            writer.addAnnotation((PdfAnnotation) field);
        } else {
            this.parent.addKid(field);
        }
    }

    public void cellLayout(PdfPCell cell, Rectangle rect, PdfContentByte[] canvases) {
        if (this.cellField == null || (this.fieldWriter == null && this.parent == null)) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("you.have.used.the.wrong.constructor.for.this.fieldpositioningevents.class", new Object[0]));
        }
        this.cellField.put(PdfName.RECT, (PdfObject) new PdfRectangle(rect.getLeft(this.padding), rect.getBottom(this.padding), rect.getRight(this.padding), rect.getTop(this.padding)));
        if (this.parent == null) {
            this.fieldWriter.addAnnotation((PdfAnnotation) this.cellField);
        } else {
            this.parent.addKid(this.cellField);
        }
    }
}
