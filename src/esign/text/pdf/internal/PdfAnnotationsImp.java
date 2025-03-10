package esign.text.pdf.internal;

import esign.text.Annotation;
import esign.text.ExceptionConverter;
import esign.text.Rectangle;
import esign.text.pdf.PdfAcroForm;
import esign.text.pdf.PdfAction;
import esign.text.pdf.PdfAnnotation;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfFileSpecification;
import esign.text.pdf.PdfFormField;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfRectangle;
import esign.text.pdf.PdfString;
import esign.text.pdf.PdfTemplate;
import esign.text.pdf.PdfWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;

public class PdfAnnotationsImp {

    protected PdfAcroForm acroForm;
    protected ArrayList<PdfAnnotation> annotations = new ArrayList<PdfAnnotation>();

    protected ArrayList<PdfAnnotation> delayedAnnotations = new ArrayList<PdfAnnotation>();

    public PdfAnnotationsImp(PdfWriter writer) {
        this.acroForm = new PdfAcroForm(writer);
    }

    public boolean hasValidAcroForm() {
        return this.acroForm.isValid();
    }

    public PdfAcroForm getAcroForm() {
        return this.acroForm;
    }

    public void setSigFlags(int f) {
        this.acroForm.setSigFlags(f);
    }

    public void addCalculationOrder(PdfFormField formField) {
        this.acroForm.addCalculationOrder(formField);
    }

    public void addAnnotation(PdfAnnotation annot) {
        if (annot.isForm()) {
            PdfFormField field = (PdfFormField) annot;
            if (field.getParent() == null) {
                addFormFieldRaw(field);
            }
        } else {
            this.annotations.add(annot);
        }
    }

    public void addPlainAnnotation(PdfAnnotation annot) {
        this.annotations.add(annot);
    }

    void addFormFieldRaw(PdfFormField field) {
        this.annotations.add(field);
        ArrayList<PdfFormField> kids = field.getKids();
        if (kids != null) {
            for (int k = 0; k < kids.size(); k++) {
                PdfFormField kid = kids.get(k);
                if (!kid.isUsed()) {
                    addFormFieldRaw(kid);
                }
            }
        }
    }

    public boolean hasUnusedAnnotations() {
        return !this.annotations.isEmpty();
    }

    public void resetAnnotations() {
        this.annotations = this.delayedAnnotations;
        this.delayedAnnotations = new ArrayList<PdfAnnotation>();
    }

    public PdfArray rotateAnnotations(PdfWriter writer, Rectangle pageSize) {
        PdfArray array = new PdfArray();
        int rotation = pageSize.getRotation() % 360;
        int currentPage = writer.getCurrentPageNumber();
        for (int k = 0; k < this.annotations.size(); k++) {
            PdfAnnotation dic = this.annotations.get(k);
            int page = dic.getPlaceInPage();
            if (page > currentPage) {
                this.delayedAnnotations.add(dic);
            } else {

                if (dic.isForm()) {
                    if (!dic.isUsed()) {
                        HashSet<PdfTemplate> templates = dic.getTemplates();
                        if (templates != null) {
                            this.acroForm.addFieldTemplates(templates);
                        }
                    }
                    PdfFormField field = (PdfFormField) dic;
                    if (field.getParent() == null) {
                        this.acroForm.addDocumentField(field.getIndirectReference());
                    }
                }
                if (dic.isAnnotation()) {
                    array.add((PdfObject) dic.getIndirectReference());
                    if (!dic.isUsed()) {
                        PdfRectangle rect;
                        PdfArray tmp = dic.getAsArray(PdfName.RECT);

                        if (tmp.size() == 4) {
                            rect = new PdfRectangle(tmp.getAsNumber(0).floatValue(), tmp.getAsNumber(1).floatValue(), tmp.getAsNumber(2).floatValue(), tmp.getAsNumber(3).floatValue());
                        } else {

                            rect = new PdfRectangle(tmp.getAsNumber(0).floatValue(), tmp.getAsNumber(1).floatValue());
                        }
                        switch (rotation) {
                            case 90:
                                dic.put(PdfName.RECT, (PdfObject) new PdfRectangle(pageSize
                                        .getTop() - rect.bottom(), rect
                                        .left(), pageSize
                                                .getTop() - rect.top(), rect
                                        .right()));
                                break;
                            case 180:
                                dic.put(PdfName.RECT, (PdfObject) new PdfRectangle(pageSize
                                        .getRight() - rect.left(), pageSize
                                        .getTop() - rect.bottom(), pageSize
                                        .getRight() - rect.right(), pageSize
                                        .getTop() - rect.top()));
                                break;
                            case 270:
                                dic.put(PdfName.RECT, (PdfObject) new PdfRectangle(rect
                                        .bottom(), pageSize
                                                .getRight() - rect.left(), rect
                                        .top(), pageSize
                                                .getRight() - rect.right()));
                                break;
                        }
                    }
                }
                if (!dic.isUsed()) {
                    dic.setUsed();
                    try {
                        writer.addToBody((PdfObject) dic, dic.getIndirectReference());
                    } catch (IOException e) {
                        throw new ExceptionConverter(e);
                    }
                }
            }
        }
        return array;
    }

    public static PdfAnnotation convertAnnotation(PdfWriter writer, Annotation annot, Rectangle defaultRect) throws IOException {
        boolean[] sparams;
        String fname;
        String mimetype;
        PdfFileSpecification fs;
        PdfAnnotation ann;
        switch (annot.annotationType()) {
            case 1:
                return writer.createAnnotation(annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction((URL) annot.attributes().get("url")), null);
            case 2:
                return writer.createAnnotation(annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction((String) annot.attributes().get("file")), null);
            case 3:
                return writer.createAnnotation(annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction((String) annot.attributes().get("file"), (String) annot.attributes().get("destination")), null);
            case 7:
                sparams = (boolean[]) annot.attributes().get("parameters");
                fname = (String) annot.attributes().get("file");
                mimetype = (String) annot.attributes().get("mime");

                if (sparams[0]) {
                    fs = PdfFileSpecification.fileEmbedded(writer, fname, fname, null);
                } else {
                    fs = PdfFileSpecification.fileExtern(writer, fname);
                }
                ann = PdfAnnotation.createScreen(writer, new Rectangle(annot.llx(), annot.lly(), annot.urx(), annot.ury()), fname, fs, mimetype, sparams[1]);

                return ann;
            case 4:
                return writer.createAnnotation(annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction((String) annot.attributes().get("file"), ((Integer) annot.attributes().get("page")).intValue()), null);
            case 5:
                return writer.createAnnotation(annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction(((Integer) annot.attributes().get("named")).intValue()), null);
            case 6:
                return writer.createAnnotation(annot.llx(), annot.lly(), annot.urx(), annot.ury(), new PdfAction((String) annot.attributes().get("application"), (String) annot.attributes().get("parameters"), (String) annot.attributes().get("operation"), (String) annot.attributes().get("defaultdir")), null);
        }
        return writer.createAnnotation(defaultRect.getLeft(), defaultRect.getBottom(), defaultRect.getRight(), defaultRect.getTop(), new PdfString(annot.title(), "UnicodeBig"), new PdfString(annot.content(), "UnicodeBig"), null);
    }

}
