package esign.text.pdf;

import esign.text.BaseColor;
import esign.text.DocumentException;
import esign.text.ExceptionConverter;
import esign.text.Image;
import esign.text.Rectangle;
import esign.text.error_messages.MessageLocalization;
import esign.text.io.RASInputStream;
import esign.text.io.RandomAccessSource;
import esign.text.io.RandomAccessSourceFactory;
import esign.text.io.WindowRandomAccessSource;
import esign.text.pdf.codec.Base64;
import esign.text.pdf.security.PdfPKCS7;
import esign.text.xml.XmlToTxt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Node;

public class AcroFields {

    PdfReader reader;
    PdfWriter writer;
    Map<String, Item> fields;
    private int topFirst;
    private HashMap<String, int[]> sigNames;
    private boolean append;
    public static final int DA_FONT = 0;
    public static final int DA_SIZE = 1;
    public static final int DA_COLOR = 2;
    private HashMap<Integer, BaseFont> extensionFonts = new HashMap<Integer, BaseFont>();

    private XfaForm xfa;

    public static final int FIELD_TYPE_NONE = 0;

    public static final int FIELD_TYPE_PUSHBUTTON = 1;

    public static final int FIELD_TYPE_CHECKBOX = 2;

    public static final int FIELD_TYPE_RADIOBUTTON = 3;

    public static final int FIELD_TYPE_TEXT = 4;

    public static final int FIELD_TYPE_LIST = 5;

    public static final int FIELD_TYPE_COMBO = 6;

    public static final int FIELD_TYPE_SIGNATURE = 7;

    private boolean lastWasString;

    private boolean generateAppearances = true;

    private HashMap<String, BaseFont> localFonts = new HashMap<String, BaseFont>();
    private float extraMarginLeft;
    private float extraMarginTop;
    private ArrayList<BaseFont> substitutionFonts;
    private ArrayList<String> orderedSignatureNames;

    AcroFields(PdfReader reader, PdfWriter writer) {
        this.reader = reader;
        this.writer = writer;
        try {
            this.xfa = new XfaForm(reader);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
        if (writer instanceof PdfStamperImp) {
            this.append = ((PdfStamperImp) writer).isAppend();
        }
        fill();
    }

    void fill() {
        this.fields = new LinkedHashMap<String, Item>();
        PdfDictionary top = (PdfDictionary) PdfReader.getPdfObjectRelease(this.reader.getCatalog().get(PdfName.ACROFORM));
        if (top == null) {
            return;
        }
        PdfBoolean needappearances = top.getAsBoolean(PdfName.NEEDAPPEARANCES);
        if (needappearances == null || !needappearances.booleanValue()) {
            setGenerateAppearances(true);
        } else {
            setGenerateAppearances(false);
        }
        PdfArray arrfds = (PdfArray) PdfReader.getPdfObjectRelease(top.get(PdfName.FIELDS));
        if (arrfds == null || arrfds.size() == 0) {
            return;
        }
        for (int k = 1; k <= this.reader.getNumberOfPages(); k++) {
            PdfDictionary page = this.reader.getPageNRelease(k);
            PdfArray annots = (PdfArray) PdfReader.getPdfObjectRelease(page.get(PdfName.ANNOTS), page);
            if (annots != null) {
                for (int i = 0; i < annots.size(); i++) {
                    PdfDictionary annot = annots.getAsDict(i);
                    if (annot == null) {
                        PdfReader.releaseLastXrefPartial(annots.getAsIndirectObject(i));

                    } else if (!PdfName.WIDGET.equals(annot.getAsName(PdfName.SUBTYPE))) {
                        PdfReader.releaseLastXrefPartial(annots.getAsIndirectObject(i));
                    } else {

                        PdfDictionary widget = annot;
                        PdfDictionary dic = new PdfDictionary();
                        dic.putAll(annot);
                        String name = "";
                        PdfDictionary value = null;
                        PdfObject lastV = null;
                        while (annot != null) {
                            dic.mergeDifferent(annot);
                            PdfString t = annot.getAsString(PdfName.T);
                            if (t != null) {
                                name = t.toUnicodeString() + "." + name;
                            }
                            if (lastV == null && annot.get(PdfName.V) != null) {
                                lastV = PdfReader.getPdfObjectRelease(annot.get(PdfName.V));
                            }
                            if (value == null && t != null) {
                                value = annot;
                                if (annot.get(PdfName.V) == null && lastV != null) {
                                    value.put(PdfName.V, lastV);
                                }
                            }
                            annot = annot.getAsDict(PdfName.PARENT);
                        }
                        if (name.length() > 0) {
                            name = name.substring(0, name.length() - 1);
                        }
                        Item item = this.fields.get(name);
                        if (item == null) {
                            item = new Item();
                            this.fields.put(name, item);
                        }
                        if (value == null) {
                            item.addValue(widget);
                        } else {
                            item.addValue(value);
                        }
                        item.addWidget(widget);
                        item.addWidgetRef(annots.getAsIndirectObject(i));
                        if (top != null) {
                            dic.mergeDifferent(top);
                        }
                        item.addMerged(dic);
                        item.addPage(k);
                        item.addTabOrder(i);
                    }
                }
            }
        }
        PdfNumber sigFlags = top.getAsNumber(PdfName.SIGFLAGS);
        if (sigFlags == null || (sigFlags.intValue() & 0x1) != 1) {
            return;
        }
        for (int j = 0; j < arrfds.size(); j++) {
            PdfDictionary annot = arrfds.getAsDict(j);
            if (annot == null) {
                PdfReader.releaseLastXrefPartial(arrfds.getAsIndirectObject(j));

            } else if (!PdfName.WIDGET.equals(annot.getAsName(PdfName.SUBTYPE))) {
                PdfReader.releaseLastXrefPartial(arrfds.getAsIndirectObject(j));
            } else {

                PdfArray kids = (PdfArray) PdfReader.getPdfObjectRelease(annot.get(PdfName.KIDS));
                if (kids == null) {

                    PdfDictionary dic = new PdfDictionary();
                    dic.putAll(annot);
                    PdfString t = annot.getAsString(PdfName.T);
                    if (t != null) {

                        String name = t.toUnicodeString();
                        if (!this.fields.containsKey(name)) {

                            Item item = new Item();
                            this.fields.put(name, item);
                            item.addValue(dic);
                            item.addWidget(dic);
                            item.addWidgetRef(arrfds.getAsIndirectObject(j));
                            item.addMerged(dic);
                            item.addPage(-1);
                            item.addTabOrder(-1);
                        }
                    }
                }
            }
        }
    }

    public String[] getAppearanceStates(String fieldName) {
        Item fd = this.fields.get(fieldName);
        if (fd == null) {
            return null;
        }
        HashSet<String> names = new LinkedHashSet<String>();
        PdfDictionary vals = fd.getValue(0);
        PdfString stringOpt = vals.getAsString(PdfName.OPT);

        if (stringOpt != null) {
            names.add(stringOpt.toUnicodeString());
        } else {

            PdfArray arrayOpt = vals.getAsArray(PdfName.OPT);
            if (arrayOpt != null) {
                for (int i = 0; i < arrayOpt.size(); i++) {
                    PdfArray pdfArray;
                    PdfObject pdfObject = arrayOpt.getDirectObject(i);
                    PdfString valStr = null;

                    switch (pdfObject.type()) {
                        case 5:
                            pdfArray = (PdfArray) pdfObject;
                            valStr = pdfArray.getAsString(1);
                            break;
                        case 3:
                            valStr = (PdfString) pdfObject;
                            break;
                    }

                    if (valStr != null) {
                        names.add(valStr.toUnicodeString());
                    }
                }
            }
        }
        for (int k = 0; k < fd.size(); k++) {
            PdfDictionary dic = fd.getWidget(k);
            dic = dic.getAsDict(PdfName.AP);
            if (dic != null) {

                dic = dic.getAsDict(PdfName.N);
                if (dic != null) {
                    for (PdfName element : dic.getKeys()) {
                        String name = PdfName.decodeName(((PdfName) element).toString());
                        names.add(name);
                    }
                }
            }
        }
        String[] out = new String[names.size()];
        return (String[]) names.toArray((Object[]) out);
    }

    private String[] getListOption(String fieldName, int idx) {
        Item fd = getFieldItem(fieldName);
        if (fd == null) {
            return null;
        }
        PdfArray ar = fd.getMerged(0).getAsArray(PdfName.OPT);
        if (ar == null) {
            return null;
        }
        String[] ret = new String[ar.size()];
        for (int k = 0; k < ar.size(); k++) {
            PdfObject obj = ar.getDirectObject(k);
            try {
                if (obj.isArray()) {
                    obj = ((PdfArray) obj).getDirectObject(idx);
                }
                if (obj.isString()) {
                    ret[k] = ((PdfString) obj).toUnicodeString();
                } else {
                    ret[k] = obj.toString();
                }
            } catch (Exception e) {
                ret[k] = "";
            }
        }
        return ret;
    }

    public String[] getListOptionExport(String fieldName) {
        return getListOption(fieldName, 0);
    }

    public String[] getListOptionDisplay(String fieldName) {
        return getListOption(fieldName, 1);
    }

    public boolean setListOption(String fieldName, String[] exportValues, String[] displayValues) {
        if (exportValues == null && displayValues == null) {
            return false;
        }
        if (exportValues != null && displayValues != null && exportValues.length != displayValues.length) {
            throw new IllegalArgumentException(MessageLocalization.getComposedMessage("the.export.and.the.display.array.must.have.the.same.size", new Object[0]));
        }
        int ftype = getFieldType(fieldName);
        if (ftype != 6 && ftype != 5) {
            return false;
        }
        Item fd = this.fields.get(fieldName);
        String[] sing = null;
        if (exportValues == null && displayValues != null) {
            sing = displayValues;
        } else if (exportValues != null && displayValues == null) {
            sing = exportValues;
        }
        PdfArray opt = new PdfArray();
        if (sing != null) {
            for (int k = 0; k < sing.length; k++) {
                opt.add(new PdfString(sing[k], "UnicodeBig"));
            }
        } else {
            for (int k = 0; k < exportValues.length; k++) {
                PdfArray a = new PdfArray();
                a.add(new PdfString(exportValues[k], "UnicodeBig"));
                a.add(new PdfString(displayValues[k], "UnicodeBig"));
                opt.add(a);
            }
        }
        fd.writeToAll(PdfName.OPT, opt, 5);
        return true;
    }

    public int getFieldType(String fieldName) {
        Item fd = getFieldItem(fieldName);
        if (fd == null) {
            return 0;
        }
        PdfDictionary merged = fd.getMerged(0);
        PdfName type = merged.getAsName(PdfName.FT);
        if (type == null) {
            return 0;
        }
        int ff = 0;
        PdfNumber ffo = merged.getAsNumber(PdfName.FF);
        if (ffo != null) {
            ff = ffo.intValue();
        }
        if (PdfName.BTN.equals(type)) {
            if ((ff & 0x10000) != 0) {
                return 1;
            }
            if ((ff & 0x8000) != 0) {
                return 3;
            }
            return 2;
        }
        if (PdfName.TX.equals(type)) {
            return 4;
        }
        if (PdfName.CH.equals(type)) {
            if ((ff & 0x20000) != 0) {
                return 6;
            }
            return 5;
        }
        if (PdfName.SIG.equals(type)) {
            return 7;
        }
        return 0;
    }

    public void exportAsFdf(FdfWriter writer) {
        for (Map.Entry<String, Item> entry : this.fields.entrySet()) {
            Item item = entry.getValue();
            String name = entry.getKey();
            PdfObject v = item.getMerged(0).get(PdfName.V);
            if (v == null) {
                continue;
            }
            String value = getField(name);
            if (this.lastWasString) {
                writer.setFieldAsString(name, value);
                continue;
            }
            writer.setFieldAsName(name, value);
        }
    }

    public boolean renameField(String oldName, String newName) {
        int idx1 = oldName.lastIndexOf('.') + 1;
        int idx2 = newName.lastIndexOf('.') + 1;
        if (idx1 != idx2) {
            return false;
        }
        if (!oldName.substring(0, idx1).equals(newName.substring(0, idx2))) {
            return false;
        }
        if (this.fields.containsKey(newName)) {
            return false;
        }
        Item item = this.fields.get(oldName);
        if (item == null) {
            return false;
        }
        newName = newName.substring(idx2);
        PdfString ss = new PdfString(newName, "UnicodeBig");

        item.writeToAll(PdfName.T, ss, 5);
        item.markUsed(this, 4);

        this.fields.remove(oldName);
        this.fields.put(newName, item);

        return true;
    }

    public static Object[] splitDAelements(String da) {
        try {
            PRTokeniser tk = new PRTokeniser(new RandomAccessFileOrArray((new RandomAccessSourceFactory()).createSource(PdfEncodings.convertToBytes(da, (String) null))));
            ArrayList<String> stack = new ArrayList<String>();
            Object[] ret = new Object[3];
            while (tk.nextToken()) {
                if (tk.getTokenType() == PRTokeniser.TokenType.COMMENT) {
                    continue;
                }
                if (tk.getTokenType() == PRTokeniser.TokenType.OTHER) {
                    String operator = tk.getStringValue();
                    if (operator.equals("Tf")) {
                        if (stack.size() >= 2) {
                            ret[0] = stack.get(stack.size() - 2);
                            ret[1] = new Float(stack.get(stack.size() - 1));
                        }

                    } else if (operator.equals("g")) {
                        if (stack.size() >= 1) {
                            float gray = (new Float(stack.get(stack.size() - 1))).floatValue();
                            if (gray != 0.0F) {
                                ret[2] = new GrayColor(gray);
                            }
                        }
                    } else if (operator.equals("rg")) {
                        if (stack.size() >= 3) {
                            float red = (new Float(stack.get(stack.size() - 3))).floatValue();
                            float green = (new Float(stack.get(stack.size() - 2))).floatValue();
                            float blue = (new Float(stack.get(stack.size() - 1))).floatValue();
                            ret[2] = new BaseColor(red, green, blue);
                        }

                    } else if (operator.equals("k")
                            && stack.size() >= 4) {
                        float cyan = (new Float(stack.get(stack.size() - 4))).floatValue();
                        float magenta = (new Float(stack.get(stack.size() - 3))).floatValue();
                        float yellow = (new Float(stack.get(stack.size() - 2))).floatValue();
                        float black = (new Float(stack.get(stack.size() - 1))).floatValue();
                        ret[2] = new CMYKColor(cyan, magenta, yellow, black);
                    }

                    stack.clear();
                    continue;
                }
                stack.add(tk.getStringValue());
            }
            return ret;
        } catch (IOException ioe) {
            throw new ExceptionConverter(ioe);
        }
    }

    public void decodeGenericDictionary(PdfDictionary merged, BaseField tx) throws IOException, DocumentException {
        int flags = 0;

        PdfString da = merged.getAsString(PdfName.DA);
        if (da != null) {
            boolean fontfallback = false;
            Object[] dab = splitDAelements(da.toUnicodeString());
            if (dab[1] != null) {
                tx.setFontSize(((Float) dab[1]).floatValue());
            }
            if (dab[2] != null) {
                tx.setTextColor((BaseColor) dab[2]);
            }
            if (dab[0] != null) {
                PdfDictionary dr = merged.getAsDict(PdfName.DR);
                if (dr != null) {
                    PdfDictionary font = dr.getAsDict(PdfName.FONT);
                    if (font != null) {
                        PdfObject po = font.get(new PdfName((String) dab[0]));
                        if (po != null && po.type() == 10) {
                            PRIndirectReference por = (PRIndirectReference) po;
                            BaseFont bp = new DocumentFont((PRIndirectReference) po, dr.getAsDict(PdfName.ENCODING));
                            tx.setFont(bp);
                            Integer porkey = Integer.valueOf(por.getNumber());
                            BaseFont porf = this.extensionFonts.get(porkey);
                            if (porf == null
                                    && !this.extensionFonts.containsKey(porkey)) {
                                PdfDictionary fo = (PdfDictionary) PdfReader.getPdfObject(po);
                                PdfDictionary fd = fo.getAsDict(PdfName.FONTDESCRIPTOR);
                                if (fd != null) {
                                    PRStream prs = (PRStream) PdfReader.getPdfObject(fd.get(PdfName.FONTFILE2));
                                    if (prs == null) {
                                        prs = (PRStream) PdfReader.getPdfObject(fd.get(PdfName.FONTFILE3));
                                    }
                                    if (prs == null) {
                                        this.extensionFonts.put(porkey, null);
                                    } else {

                                        try {
                                            porf = BaseFont.createFont("font.ttf", "Identity-H", true, false, PdfReader.getStreamBytes(prs), null);
                                        } catch (Exception exception) {
                                        }

                                        this.extensionFonts.put(porkey, porf);
                                    }
                                }
                            }

                            if (tx instanceof TextField) {
                                ((TextField) tx).setExtensionFont(porf);
                            }
                        } else {
                            fontfallback = true;
                        }

                    } else {

                        fontfallback = true;
                    }
                } else {

                    fontfallback = true;
                }
            }
            if (fontfallback) {
                BaseFont bf = this.localFonts.get(dab[0]);
                if (bf == null) {
                    String[] fn = stdFieldFontNames.get(dab[0]);
                    if (fn != null) {
                        try {
                            String enc = "winansi";
                            if (fn.length > 1) {
                                enc = fn[1];
                            }
                            bf = BaseFont.createFont(fn[0], enc, false);
                            tx.setFont(bf);
                        } catch (Exception exception) {
                        }

                    }
                } else {

                    tx.setFont(bf);
                }
            }
        }
        PdfDictionary mk = merged.getAsDict(PdfName.MK);
        if (mk != null) {
            PdfArray ar = mk.getAsArray(PdfName.BC);
            BaseColor border = getMKColor(ar);
            tx.setBorderColor(border);
            if (border != null) {
                tx.setBorderWidth(1.0F);
            }
            ar = mk.getAsArray(PdfName.BG);
            tx.setBackgroundColor(getMKColor(ar));
            PdfNumber rotation = mk.getAsNumber(PdfName.R);
            if (rotation != null) {
                tx.setRotation(rotation.intValue());
            }
        }
        PdfNumber nfl = merged.getAsNumber(PdfName.F);
        flags = 0;
        tx.setVisibility(2);
        if (nfl != null) {
            flags = nfl.intValue();
            if ((flags & 0x4) != 0 && (flags & 0x2) != 0) {
                tx.setVisibility(1);
            } else if ((flags & 0x4) != 0 && (flags & 0x20) != 0) {
                tx.setVisibility(3);
            } else if ((flags & 0x4) != 0) {
                tx.setVisibility(0);
            }
        }
        nfl = merged.getAsNumber(PdfName.FF);
        flags = 0;
        if (nfl != null) {
            flags = nfl.intValue();
        }
        tx.setOptions(flags);
        if ((flags & 0x1000000) != 0) {
            PdfNumber maxLen = merged.getAsNumber(PdfName.MAXLEN);
            int len = 0;
            if (maxLen != null) {
                len = maxLen.intValue();
            }
            tx.setMaxCharacterLength(len);
        }

        nfl = merged.getAsNumber(PdfName.Q);
        if (nfl != null) {
            if (nfl.intValue() == 1) {
                tx.setAlignment(1);
            } else if (nfl.intValue() == 2) {
                tx.setAlignment(2);
            }
        }
        PdfDictionary bs = merged.getAsDict(PdfName.BS);
        if (bs != null) {
            PdfNumber w = bs.getAsNumber(PdfName.W);
            if (w != null) {
                tx.setBorderWidth(w.floatValue());
            }
            PdfName s = bs.getAsName(PdfName.S);
            if (PdfName.D.equals(s)) {
                tx.setBorderStyle(1);
            } else if (PdfName.B.equals(s)) {
                tx.setBorderStyle(2);
            } else if (PdfName.I.equals(s)) {
                tx.setBorderStyle(3);
            } else if (PdfName.U.equals(s)) {
                tx.setBorderStyle(4);
            }
        } else {
            PdfArray bd = merged.getAsArray(PdfName.BORDER);
            if (bd != null) {
                if (bd.size() >= 3) {
                    tx.setBorderWidth(bd.getAsNumber(2).floatValue());
                }
                if (bd.size() >= 4) {
                    tx.setBorderStyle(1);
                }
            }
        }
    }

    PdfAppearance getAppearance(PdfDictionary merged, String[] values, String fieldName) throws IOException, DocumentException {
        PdfName fieldType = merged.getAsName(PdfName.FT);

        if (PdfName.BTN.equals(fieldType)) {
            PdfNumber fieldFlags = merged.getAsNumber(PdfName.FF);
            boolean isRadio = (fieldFlags != null && (fieldFlags.intValue() & 0x8000) != 0);
            RadioCheckField field = new RadioCheckField(this.writer, null, null, null);
            decodeGenericDictionary(merged, field);

            PdfArray rect = merged.getAsArray(PdfName.RECT);
            Rectangle box = PdfReader.getNormalizedRectangle(rect);
            if (field.getRotation() == 90 || field.getRotation() == 270) {
                box = box.rotate();
            }
            field.setBox(box);
            if (!isRadio) {
                field.setCheckType(3);
            }
            return field.getAppearance(isRadio, !merged.getAsName(PdfName.AS).equals(PdfName.Off));
        }

        this.topFirst = 0;
        String text = (values.length > 0) ? values[0] : null;

        TextField tx = null;
        if (this.fieldCache == null || !this.fieldCache.containsKey(fieldName)) {
            tx = new TextField(this.writer, null, null);
            tx.setExtraMargin(this.extraMarginLeft, this.extraMarginTop);
            tx.setBorderWidth(0.0F);
            tx.setSubstitutionFonts(this.substitutionFonts);
            decodeGenericDictionary(merged, tx);

            PdfArray rect = merged.getAsArray(PdfName.RECT);
            Rectangle box = PdfReader.getNormalizedRectangle(rect);
            if (tx.getRotation() == 90 || tx.getRotation() == 270) {
                box = box.rotate();
            }
            tx.setBox(box);
            if (this.fieldCache != null) {
                this.fieldCache.put(fieldName, tx);
            }
        } else {
            tx = this.fieldCache.get(fieldName);
            tx.setWriter(this.writer);
        }
        if (PdfName.TX.equals(fieldType)) {
            if (values.length > 0 && values[0] != null) {
                tx.setText(values[0]);
            }
            return tx.getAppearance();
        }
        if (!PdfName.CH.equals(fieldType)) {
            throw new DocumentException(MessageLocalization.getComposedMessage("an.appearance.was.requested.without.a.variable.text.field", new Object[0]));
        }
        PdfArray opt = merged.getAsArray(PdfName.OPT);
        int flags = 0;
        PdfNumber nfl = merged.getAsNumber(PdfName.FF);
        if (nfl != null) {
            flags = nfl.intValue();
        }
        if ((flags & 0x20000) != 0 && opt == null) {
            tx.setText(text);
            return tx.getAppearance();
        }
        if (opt != null) {
            String[] choices = new String[opt.size()];
            String[] choicesExp = new String[opt.size()];
            int k;
            for (k = 0; k < opt.size(); k++) {
                PdfObject obj = opt.getPdfObject(k);
                if (obj.isString()) {
                    choicesExp[k] = ((PdfString) obj).toUnicodeString();
                    choices[k] = ((PdfString) obj).toUnicodeString();
                } else {

                    PdfArray a = (PdfArray) obj;
                    choicesExp[k] = a.getAsString(0).toUnicodeString();
                    choices[k] = a.getAsString(1).toUnicodeString();
                }
            }
            if ((flags & 0x20000) != 0) {
                for (k = 0; k < choices.length; k++) {
                    if (text.equals(choicesExp[k])) {
                        text = choices[k];
                        break;
                    }
                }
                tx.setText(text);
                return tx.getAppearance();
            }
            ArrayList<Integer> indexes = new ArrayList<Integer>();
            for (int i = 0; i < choicesExp.length; i++) {
                for (int j = 0; j < values.length; j++) {
                    String val = values[j];
                    if (val != null && val.equals(choicesExp[i])) {
                        indexes.add(Integer.valueOf(i));
                        break;
                    }
                }
            }
            tx.setChoices(choices);
            tx.setChoiceExports(choicesExp);
            tx.setChoiceSelections(indexes);
        }
        PdfAppearance app = tx.getListAppearance();
        this.topFirst = tx.getTopFirst();
        return app;
    }

    PdfAppearance getAppearance(PdfDictionary merged, String text, String fieldName) throws IOException, DocumentException {
        String[] valueArr = new String[1];
        valueArr[0] = text;
        return getAppearance(merged, valueArr, fieldName);
    }

    BaseColor getMKColor(PdfArray ar) {
        if (ar == null) {
            return null;
        }
        switch (ar.size()) {
            case 1:
                return new GrayColor(ar.getAsNumber(0).floatValue());
            case 3:
                return new BaseColor(ExtendedColor.normalize(ar.getAsNumber(0).floatValue()), ExtendedColor.normalize(ar.getAsNumber(1).floatValue()), ExtendedColor.normalize(ar.getAsNumber(2).floatValue()));
            case 4:
                return new CMYKColor(ar.getAsNumber(0).floatValue(), ar.getAsNumber(1).floatValue(), ar.getAsNumber(2).floatValue(), ar.getAsNumber(3).floatValue());
        }
        return null;
    }

    public String getFieldRichValue(String name) {
        if (this.xfa.isXfaPresent()) {
            return null;
        }

        Item item = this.fields.get(name);
        if (item == null) {
            return null;
        }

        PdfDictionary merged = item.getMerged(0);
        PdfString rich = merged.getAsString(PdfName.RV);

        String markup = null;
        if (rich != null) {
            markup = rich.toString();
        }

        return markup;
    }

    public String getField(String name) {
        if (this.xfa.isXfaPresent()) {
            name = this.xfa.findFieldName(name, this);
            if (name == null) {
                return null;
            }
            name = XfaForm.Xml2Som.getShortName(name);
            return XfaForm.getNodeText(this.xfa.findDatasetsNode(name));
        }
        Item item = this.fields.get(name);
        if (item == null) {
            return null;
        }
        this.lastWasString = false;
        PdfDictionary mergedDict = item.getMerged(0);

        PdfObject v = PdfReader.getPdfObject(mergedDict.get(PdfName.V));
        if (v == null) {
            return "";
        }
        if (v instanceof PRStream) {

            try {
                byte[] valBytes = PdfReader.getStreamBytes((PRStream) v);
                return new String(valBytes);
            } catch (IOException e) {
                throw new ExceptionConverter(e);
            }
        }

        PdfName type = mergedDict.getAsName(PdfName.FT);
        if (PdfName.BTN.equals(type)) {
            PdfNumber ff = mergedDict.getAsNumber(PdfName.FF);
            int flags = 0;
            if (ff != null) {
                flags = ff.intValue();
            }
            if ((flags & 0x10000) != 0) {
                return "";
            }
            String value = "";
            if (v instanceof PdfName) {
                value = PdfName.decodeName(v.toString());
            } else if (v instanceof PdfString) {
                value = ((PdfString) v).toUnicodeString();
            }
            PdfArray opts = item.getValue(0).getAsArray(PdfName.OPT);
            if (opts != null) {
                int idx = 0;
                try {
                    idx = Integer.parseInt(value);
                    PdfString ps = opts.getAsString(idx);
                    value = ps.toUnicodeString();
                    this.lastWasString = true;
                } catch (Exception exception) {
                }
            }

            return value;
        }
        if (v instanceof PdfString) {
            this.lastWasString = true;
            return ((PdfString) v).toUnicodeString();
        }
        if (v instanceof PdfName) {
            return PdfName.decodeName(v.toString());
        }
        return "";
    }

    public String[] getListSelection(String name) {
        String s = getField(name);
        Item item = this.fields.get(name);
        PdfArray values = item.getMerged(0).getAsArray(PdfName.I);
        String[] ret = new String[values.size()];
        String[] options = getListOptionExport(name);
        if (s == null) {
            ret = new String[0];
        } else {

            ret = new String[]{s};
        }

        if (item == null) {
            return ret;
        }

        if (values == null) {
            return ret;
        }

        int idx = 0;
        for (Iterator<PdfObject> i = values.listIterator(); i.hasNext();) {
            PdfNumber n = (PdfNumber) i.next();
            ret[idx++] = options[n.intValue()];
        }
        return ret;
    }

    public boolean setFieldProperty(String field, String name, Object value, int[] inst) {
        if (this.writer == null) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("this.acrofields.instance.is.read.only", new Object[0]));
        }
        try {
            Item item = this.fields.get(field);
            if (item == null) {
                return false;
            }
            InstHit hit = new InstHit(inst);

            if (name.equalsIgnoreCase("textfont")) {
                for (int k = 0; k < item.size(); k++) {
                    if (hit.isHit(k)) {
                        PdfDictionary merged = item.getMerged(k);
                        PdfString da = merged.getAsString(PdfName.DA);
                        PdfDictionary dr = merged.getAsDict(PdfName.DR);
                        if (da != null) {
                            if (dr == null) {
                                dr = new PdfDictionary();
                                merged.put(PdfName.DR, dr);
                            }
                            Object[] dao = splitDAelements(da.toUnicodeString());
                            PdfAppearance cb = new PdfAppearance();
                            if (dao[0] != null) {
                                BaseFont bf = (BaseFont) value;
                                PdfName psn = PdfAppearance.stdFieldFontNames.get(bf.getPostscriptFontName());
                                if (psn == null) {
                                    psn = new PdfName(bf.getPostscriptFontName());
                                }
                                PdfDictionary fonts = dr.getAsDict(PdfName.FONT);
                                if (fonts == null) {
                                    fonts = new PdfDictionary();
                                    dr.put(PdfName.FONT, fonts);
                                }
                                PdfIndirectReference fref = (PdfIndirectReference) fonts.get(psn);
                                PdfDictionary top = this.reader.getCatalog().getAsDict(PdfName.ACROFORM);
                                markUsed(top);
                                dr = top.getAsDict(PdfName.DR);
                                if (dr == null) {
                                    dr = new PdfDictionary();
                                    top.put(PdfName.DR, dr);
                                }
                                markUsed(dr);
                                PdfDictionary fontsTop = dr.getAsDict(PdfName.FONT);
                                if (fontsTop == null) {
                                    fontsTop = new PdfDictionary();
                                    dr.put(PdfName.FONT, fontsTop);
                                }
                                markUsed(fontsTop);
                                PdfIndirectReference frefTop = (PdfIndirectReference) fontsTop.get(psn);
                                if (frefTop != null) {
                                    if (fref == null) {
                                        fonts.put(psn, frefTop);
                                    }
                                } else if (fref == null) {
                                    FontDetails fd;
                                    if (bf.getFontType() == 4) {
                                        fd = new FontDetails(null, ((DocumentFont) bf).getIndirectReference(), bf);
                                    } else {

                                        bf.setSubset(false);
                                        fd = this.writer.addSimple(bf);
                                        this.localFonts.put(psn.toString().substring(1), bf);
                                    }
                                    fontsTop.put(psn, fd.getIndirectReference());
                                    fonts.put(psn, fd.getIndirectReference());
                                }
                                ByteBuffer buf = cb.getInternalBuffer();
                                buf.append(psn.getBytes()).append(' ').append(((Float) dao[1]).floatValue()).append(" Tf ");
                                if (dao[2] != null) {
                                    cb.setColorFill((BaseColor) dao[2]);
                                }
                                PdfString s = new PdfString(cb.toString());
                                item.getMerged(k).put(PdfName.DA, s);
                                item.getWidget(k).put(PdfName.DA, s);
                                markUsed(item.getWidget(k));
                            }

                        }
                    }
                }
            } else if (name.equalsIgnoreCase("textcolor")) {
                for (int k = 0; k < item.size(); k++) {
                    if (hit.isHit(k)) {
                        PdfDictionary merged = item.getMerged(k);
                        PdfString da = merged.getAsString(PdfName.DA);
                        if (da != null) {
                            Object[] dao = splitDAelements(da.toUnicodeString());
                            PdfAppearance cb = new PdfAppearance();
                            if (dao[0] != null) {
                                ByteBuffer buf = cb.getInternalBuffer();
                                buf.append((new PdfName((String) dao[0])).getBytes()).append(' ').append(((Float) dao[1]).floatValue()).append(" Tf ");
                                cb.setColorFill((BaseColor) value);
                                PdfString s = new PdfString(cb.toString());
                                item.getMerged(k).put(PdfName.DA, s);
                                item.getWidget(k).put(PdfName.DA, s);
                                markUsed(item.getWidget(k));
                            }

                        }
                    }
                }
            } else if (name.equalsIgnoreCase("textsize")) {
                for (int k = 0; k < item.size(); k++) {
                    if (hit.isHit(k)) {
                        PdfDictionary merged = item.getMerged(k);
                        PdfString da = merged.getAsString(PdfName.DA);
                        if (da != null) {
                            Object[] dao = splitDAelements(da.toUnicodeString());
                            PdfAppearance cb = new PdfAppearance();
                            if (dao[0] != null) {
                                ByteBuffer buf = cb.getInternalBuffer();
                                buf.append((new PdfName((String) dao[0])).getBytes()).append(' ').append(((Float) value).floatValue()).append(" Tf ");
                                if (dao[2] != null) {
                                    cb.setColorFill((BaseColor) dao[2]);
                                }
                                PdfString s = new PdfString(cb.toString());
                                item.getMerged(k).put(PdfName.DA, s);
                                item.getWidget(k).put(PdfName.DA, s);
                                markUsed(item.getWidget(k));
                            }

                        }
                    }
                }
            } else if (name.equalsIgnoreCase("bgcolor") || name.equalsIgnoreCase("bordercolor")) {
                PdfName dname = name.equalsIgnoreCase("bgcolor") ? PdfName.BG : PdfName.BC;
                for (int k = 0; k < item.size(); k++) {
                    if (hit.isHit(k)) {
                        PdfDictionary merged = item.getMerged(k);
                        PdfDictionary mk = merged.getAsDict(PdfName.MK);
                        if (mk == null) {
                            if (value == null) {
                                return true;
                            }
                            mk = new PdfDictionary();
                            item.getMerged(k).put(PdfName.MK, mk);
                            item.getWidget(k).put(PdfName.MK, mk);
                            markUsed(item.getWidget(k));
                        } else {
                            markUsed(mk);
                        }
                        if (value == null) {
                            mk.remove(dname);
                        } else {
                            mk.put(dname, PdfFormField.getMKColor((BaseColor) value));
                        }
                    }
                }
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public boolean setFieldProperty(String field, String name, int value, int[] inst) {
        if (this.writer == null) {
            throw new RuntimeException(MessageLocalization.getComposedMessage("this.acrofields.instance.is.read.only", new Object[0]));
        }
        Item item = this.fields.get(field);
        if (item == null) {
            return false;
        }
        InstHit hit = new InstHit(inst);
        if (name.equalsIgnoreCase("flags")) {
            PdfNumber num = new PdfNumber(value);
            for (int k = 0; k < item.size(); k++) {
                if (hit.isHit(k)) {
                    item.getMerged(k).put(PdfName.F, num);
                    item.getWidget(k).put(PdfName.F, num);
                    markUsed(item.getWidget(k));
                }

            }
        } else if (name.equalsIgnoreCase("setflags")) {
            for (int k = 0; k < item.size(); k++) {
                if (hit.isHit(k)) {
                    PdfNumber num = item.getWidget(k).getAsNumber(PdfName.F);
                    int val = 0;
                    if (num != null) {
                        val = num.intValue();
                    }
                    num = new PdfNumber(val | value);
                    item.getMerged(k).put(PdfName.F, num);
                    item.getWidget(k).put(PdfName.F, num);
                    markUsed(item.getWidget(k));
                }

            }
        } else if (name.equalsIgnoreCase("clrflags")) {
            for (int k = 0; k < item.size(); k++) {
                if (hit.isHit(k)) {
                    PdfDictionary widget = item.getWidget(k);
                    PdfNumber num = widget.getAsNumber(PdfName.F);
                    int val = 0;
                    if (num != null) {
                        val = num.intValue();
                    }
                    num = new PdfNumber(val & (value ^ 0xFFFFFFFF));
                    item.getMerged(k).put(PdfName.F, num);
                    widget.put(PdfName.F, num);
                    markUsed(widget);
                }

            }
        } else if (name.equalsIgnoreCase("fflags")) {
            PdfNumber num = new PdfNumber(value);
            for (int k = 0; k < item.size(); k++) {
                if (hit.isHit(k)) {
                    item.getMerged(k).put(PdfName.FF, num);
                    item.getValue(k).put(PdfName.FF, num);
                    markUsed(item.getValue(k));
                }

            }
        } else if (name.equalsIgnoreCase("setfflags")) {
            for (int k = 0; k < item.size(); k++) {
                if (hit.isHit(k)) {
                    PdfDictionary valDict = item.getValue(k);
                    PdfNumber num = valDict.getAsNumber(PdfName.FF);
                    int val = 0;
                    if (num != null) {
                        val = num.intValue();
                    }
                    num = new PdfNumber(val | value);
                    item.getMerged(k).put(PdfName.FF, num);
                    valDict.put(PdfName.FF, num);
                    markUsed(valDict);
                }

            }
        } else if (name.equalsIgnoreCase("clrfflags")) {
            for (int k = 0; k < item.size(); k++) {
                if (hit.isHit(k)) {
                    PdfDictionary valDict = item.getValue(k);
                    PdfNumber num = valDict.getAsNumber(PdfName.FF);
                    int val = 0;
                    if (num != null) {
                        val = num.intValue();
                    }
                    num = new PdfNumber(val & (value ^ 0xFFFFFFFF));
                    item.getMerged(k).put(PdfName.FF, num);
                    valDict.put(PdfName.FF, num);
                    markUsed(valDict);
                }
            }
        } else {

            return false;
        }
        return true;
    }

    public void mergeXfaData(Node n) throws IOException, DocumentException {
        XfaForm.Xml2SomDatasets data = new XfaForm.Xml2SomDatasets(n);
        for (String string : data.getOrder()) {
            String name = string;
            String text = XfaForm.getNodeText(data.getName2Node().get(name));
            setField(name, text);
        }
    }

    public void setFields(FdfReader fdf) throws IOException, DocumentException {
        HashMap<String, PdfDictionary> fd = fdf.getFields();
        for (String f : fd.keySet()) {
            String v = fdf.getFieldValue(f);
            if (v != null) {
                setField(f, v);
            }
        }
    }

    public void setFields(XfdfReader xfdf) throws IOException, DocumentException {
        HashMap<String, String> fd = xfdf.getFields();
        for (String f : fd.keySet()) {
            String v = xfdf.getFieldValue(f);
            if (v != null) {
                setField(f, v);
            }
            List<String> l = xfdf.getListValues(f);
            if (l != null) {
                setListSelection(v, l.<String>toArray(new String[l.size()]));
            }
        }
    }

    public boolean regenerateField(String name) throws IOException, DocumentException {
        String value = getField(name);
        return setField(name, value, value);
    }

    public boolean setField(String name, String value) throws IOException, DocumentException {
        return setField(name, value, (String) null);
    }

    public boolean setField(String name, String value, boolean saveAppearance) throws IOException, DocumentException {
        return setField(name, value, null, saveAppearance);
    }

    public boolean setFieldRichValue(String name, String richValue) throws DocumentException, IOException {
        if (this.writer == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("this.acrofields.instance.is.read.only", new Object[0]));
        }

        Item item = getFieldItem(name);
        if (item == null) {
            return false;
        }

        if (getFieldType(name) != 4) {
            return false;
        }

        PdfDictionary merged = item.getMerged(0);
        PdfNumber ffNum = merged.getAsNumber(PdfName.FF);
        int flagVal = 0;
        if (ffNum != null) {
            flagVal = ffNum.intValue();
        }
        if ((flagVal & 0x2000000) == 0) {
            return false;
        }

        PdfString richString = new PdfString(richValue);
        item.writeToAll(PdfName.RV, richString, 5);

        InputStream is = new ByteArrayInputStream(richValue.getBytes());
        PdfString valueString = new PdfString(XmlToTxt.parse(is));
        item.writeToAll(PdfName.V, valueString, 5);
        return true;
    }

    public boolean setField(String name, String value, String display) throws IOException, DocumentException {
        return setField(name, value, display, false);
    }

    public boolean setField(String name, String value, String display, boolean saveAppearance) throws IOException, DocumentException {
        if (this.writer == null) {
            throw new DocumentException(MessageLocalization.getComposedMessage("this.acrofields.instance.is.read.only", new Object[0]));
        }
        if (this.xfa.isXfaPresent()) {
            name = this.xfa.findFieldName(name, this);
            if (name == null) {
                return false;
            }
            String shortName = XfaForm.Xml2Som.getShortName(name);
            Node xn = this.xfa.findDatasetsNode(shortName);
            if (xn == null) {
                xn = this.xfa.getDatasetsSom().insertNode(this.xfa.getDatasetsNode(), shortName);
            }
            this.xfa.setNodeText(xn, value);
        }
        Item item = this.fields.get(name);
        if (item == null) {
            return false;
        }
        PdfDictionary merged = item.getMerged(0);
        PdfName type = merged.getAsName(PdfName.FT);
        if (PdfName.TX.equals(type)) {
            PdfNumber maxLen = merged.getAsNumber(PdfName.MAXLEN);
            int len = 0;
            if (maxLen != null) {
                len = maxLen.intValue();
            }
            if (len > 0) {
                value = value.substring(0, Math.min(len, value.length()));
            }
        }
        if (display == null) {
            display = value;
        }
        if (PdfName.TX.equals(type) || PdfName.CH.equals(type)) {
            PdfString v = new PdfString(value, "UnicodeBig");
            for (int idx = 0; idx < item.size(); idx++) {
                PdfDictionary valueDic = item.getValue(idx);
                valueDic.put(PdfName.V, v);
                valueDic.remove(PdfName.I);
                markUsed(valueDic);
                merged = item.getMerged(idx);
                merged.remove(PdfName.I);
                merged.put(PdfName.V, v);
                PdfDictionary widget = item.getWidget(idx);
                if (this.generateAppearances) {
                    PdfAppearance app = getAppearance(merged, display, name);
                    if (PdfName.CH.equals(type)) {
                        PdfNumber n = new PdfNumber(this.topFirst);
                        widget.put(PdfName.TI, n);
                        merged.put(PdfName.TI, n);
                    }
                    PdfDictionary appDic = widget.getAsDict(PdfName.AP);
                    if (appDic == null) {
                        appDic = new PdfDictionary();
                        widget.put(PdfName.AP, appDic);
                        merged.put(PdfName.AP, appDic);
                    }
                    appDic.put(PdfName.N, app.getIndirectReference());
                    this.writer.releaseTemplate(app);
                } else {

                    widget.remove(PdfName.AP);
                    merged.remove(PdfName.AP);
                }
                markUsed(widget);
            }
            return true;
        }
        if (PdfName.BTN.equals(type)) {
            PdfName vt;
            PdfNumber ff = item.getMerged(0).getAsNumber(PdfName.FF);
            int flags = 0;
            if (ff != null) {
                flags = ff.intValue();
            }
            if ((flags & 0x10000) != 0) {
                Image img;

                try {
                    img = Image.getInstance(Base64.decode(value));
                } catch (Exception e) {
                    return false;
                }
                PushbuttonField pb = getNewPushbuttonFromField(name);
                pb.setImage(img);
                replacePushbuttonField(name, pb.getField());
                return true;
            }
            PdfName v = new PdfName(value);
            ArrayList<String> lopt = new ArrayList<String>();
            PdfArray opts = item.getValue(0).getAsArray(PdfName.OPT);
            if (opts != null) {
                for (int k = 0; k < opts.size(); k++) {
                    PdfString valStr = opts.getAsString(k);
                    if (valStr != null) {
                        lopt.add(valStr.toUnicodeString());
                    } else {
                        lopt.add(null);
                    }
                }
            }
            int vidx = lopt.indexOf(value);

            if (vidx >= 0) {
                vt = new PdfName(String.valueOf(vidx));
            } else {
                vt = v;
            }
            for (int idx = 0; idx < item.size(); idx++) {
                merged = item.getMerged(idx);
                PdfDictionary widget = item.getWidget(idx);
                PdfDictionary valDict = item.getValue(idx);
                markUsed(item.getValue(idx));
                valDict.put(PdfName.V, vt);
                merged.put(PdfName.V, vt);
                markUsed(widget);
                PdfDictionary appDic = widget.getAsDict(PdfName.AP);
                if (appDic == null) {
                    return false;
                }
                PdfDictionary normal = appDic.getAsDict(PdfName.N);
                if (isInAP(normal, vt) || normal == null) {
                    merged.put(PdfName.AS, vt);
                    widget.put(PdfName.AS, vt);
                } else {

                    merged.put(PdfName.AS, PdfName.Off);
                    widget.put(PdfName.AS, PdfName.Off);
                }
                if (this.generateAppearances && !saveAppearance) {
                    PdfAppearance app = getAppearance(merged, display, name);
                    if (normal != null) {
                        normal.put(merged.getAsName(PdfName.AS), app.getIndirectReference());
                    } else {
                        appDic.put(PdfName.N, app.getIndirectReference());
                    }
                    this.writer.releaseTemplate(app);
                }
            }
            return true;
        }
        return false;
    }

    public boolean setListSelection(String name, String[] value) throws IOException, DocumentException {
        Item item = getFieldItem(name);
        if (item == null) {
            return false;
        }
        PdfDictionary merged = item.getMerged(0);
        PdfName type = merged.getAsName(PdfName.FT);
        if (!PdfName.CH.equals(type)) {
            return false;
        }
        String[] options = getListOptionExport(name);
        PdfArray array = new PdfArray();
        for (String element : value) {
            for (int j = 0; j < options.length; j++) {
                if (options[j].equals(element)) {
                    array.add(new PdfNumber(j));
                    break;
                }
            }
        }
        item.writeToAll(PdfName.I, array, 5);

        PdfArray vals = new PdfArray();
        for (int i = 0; i < value.length; i++) {
            vals.add(new PdfString(value[i]));
        }
        item.writeToAll(PdfName.V, vals, 5);

        PdfAppearance app = getAppearance(merged, value, name);

        PdfDictionary apDic = new PdfDictionary();
        apDic.put(PdfName.N, app.getIndirectReference());
        item.writeToAll(PdfName.AP, apDic, 3);

        this.writer.releaseTemplate(app);

        item.markUsed(this, 6);
        return true;
    }

    boolean isInAP(PdfDictionary nDic, PdfName check) {
        return (nDic != null && nDic.get(check) != null);
    }

    public Map<String, Item> getFields() {
        return this.fields;
    }

    public Item getFieldItem(String name) {
        if (this.xfa.isXfaPresent()) {
            name = this.xfa.findFieldName(name, this);
            if (name == null) {
                return null;
            }
        }
        return this.fields.get(name);
    }

    public String getTranslatedFieldName(String name) {
        if (this.xfa.isXfaPresent()) {
            String namex = this.xfa.findFieldName(name, this);
            if (namex != null) {
                name = namex;
            }
        }
        return name;
    }

    public List<FieldPosition> getFieldPositions(String name) {
        Item item = getFieldItem(name);
        if (item == null) {
            return null;
        }
        ArrayList<FieldPosition> ret = new ArrayList<FieldPosition>();
        for (int k = 0; k < item.size(); k++) {
            try {
                PdfDictionary wd = item.getWidget(k);
                PdfArray rect = wd.getAsArray(PdfName.RECT);
                if (rect != null) {

                    Rectangle r = PdfReader.getNormalizedRectangle(rect);
                    int page = item.getPage(k).intValue();
                    int rotation = this.reader.getPageRotation(page);
                    FieldPosition fp = new FieldPosition();
                    fp.page = page;
                    if (rotation != 0) {
                        Rectangle pageSize = this.reader.getPageSize(page);
                        switch (rotation) {

                            case 270:
                                r = new Rectangle(pageSize.getTop() - r.getBottom(), r.getLeft(), pageSize.getTop() - r.getTop(), r.getRight());
                                break;

                            case 180:
                                r = new Rectangle(pageSize.getRight() - r.getLeft(), pageSize.getTop() - r.getBottom(), pageSize.getRight() - r.getRight(), pageSize.getTop() - r.getTop());
                                break;

                            case 90:
                                r = new Rectangle(r.getBottom(), pageSize.getRight() - r.getLeft(), r.getTop(), pageSize.getRight() - r.getRight());
                                break;
                        }
                        r.normalize();
                    }
                    fp.position = r;
                    ret.add(fp);
                }
            } catch (Exception exception) {
            }
        }

        return ret;
    }

    private int removeRefFromArray(PdfArray array, PdfObject refo) {
        if (refo == null || !refo.isIndirect()) {
            return array.size();
        }
        PdfIndirectReference ref = (PdfIndirectReference) refo;
        for (int j = 0; j < array.size(); j++) {
            PdfObject obj = array.getPdfObject(j);
            if (obj.isIndirect()) {
                if (((PdfIndirectReference) obj).getNumber() == ref.getNumber()) {
                    array.remove(j--);
                }
            }
        }
        return array.size();
    }

    public boolean removeFieldsFromPage(int page) {
        if (page < 1) {
            return false;
        }
        String[] names = new String[this.fields.size()];
        this.fields.keySet().toArray((Object[]) names);
        boolean found = false;
        for (int k = 0; k < names.length; k++) {
            boolean fr = removeField(names[k], page);
            found = (found || fr);
        }
        return found;
    }

    public boolean removeField(String name, int page) {
        Item item = getFieldItem(name);
        if (item == null) {
            return false;
        }
        PdfDictionary acroForm = (PdfDictionary) PdfReader.getPdfObject(this.reader.getCatalog().get(PdfName.ACROFORM), this.reader.getCatalog());

        if (acroForm == null) {
            return false;
        }
        PdfArray arrayf = acroForm.getAsArray(PdfName.FIELDS);
        if (arrayf == null) {
            return false;
        }
        for (int k = 0; k < item.size(); k++) {
            int pageV = item.getPage(k).intValue();
            if (page == -1 || page == pageV) {

                PdfIndirectReference ref = item.getWidgetRef(k);
                PdfDictionary wd = item.getWidget(k);
                PdfDictionary pageDic = this.reader.getPageN(pageV);
                PdfArray annots = (pageDic != null) ? pageDic.getAsArray(PdfName.ANNOTS) : null;
                if (annots != null) {
                    if (removeRefFromArray(annots, ref) == 0) {
                        pageDic.remove(PdfName.ANNOTS);
                        markUsed(pageDic);
                    } else {

                        markUsed(annots);
                    }
                }
                PdfReader.killIndirect(ref);
                PdfIndirectReference kid = ref;
                while ((ref = wd.getAsIndirectObject(PdfName.PARENT)) != null) {
                    wd = wd.getAsDict(PdfName.PARENT);
                    PdfArray kids = wd.getAsArray(PdfName.KIDS);
                    if (removeRefFromArray(kids, kid) != 0) {
                        break;
                    }
                    kid = ref;
                    PdfReader.killIndirect(ref);
                }
                if (ref == null) {
                    removeRefFromArray(arrayf, kid);
                    markUsed(arrayf);
                }
                if (page != -1) {
                    item.remove(k);
                    k--;
                }
            }
        }
        if (page == -1 || item.size() == 0) {
            this.fields.remove(name);
        }
        return true;
    }

    public boolean removeField(String name) {
        return removeField(name, -1);
    }

    public boolean isGenerateAppearances() {
        return this.generateAppearances;
    }

    public void setGenerateAppearances(boolean generateAppearances) {
        this.generateAppearances = generateAppearances;
        PdfDictionary top = this.reader.getCatalog().getAsDict(PdfName.ACROFORM);
        if (generateAppearances) {
            top.remove(PdfName.NEEDAPPEARANCES);
        } else {
            top.put(PdfName.NEEDAPPEARANCES, PdfBoolean.PDFTRUE);
        }
    }

    public static class Item {

        public static final int WRITE_MERGED = 1;

        public static final int WRITE_WIDGET = 2;

        public static final int WRITE_VALUE = 4;

        public void writeToAll(PdfName key, PdfObject value, int writeFlags) {
            PdfDictionary curDict = null;
            if ((writeFlags & 0x1) != 0) {
                for (int i = 0; i < this.merged.size(); i++) {
                    curDict = getMerged(i);
                    curDict.put(key, value);
                }
            }
            if ((writeFlags & 0x2) != 0) {
                for (int i = 0; i < this.widgets.size(); i++) {
                    curDict = getWidget(i);
                    curDict.put(key, value);
                }
            }
            if ((writeFlags & 0x4) != 0) {
                for (int i = 0; i < this.values.size(); i++) {
                    curDict = getValue(i);
                    curDict.put(key, value);
                }
            }
        }

        public void markUsed(AcroFields parentFields, int writeFlags) {
            if ((writeFlags & 0x4) != 0) {
                for (int i = 0; i < size(); i++) {
                    parentFields.markUsed(getValue(i));
                }
            }
            if ((writeFlags & 0x2) != 0) {
                for (int i = 0; i < size(); i++) {
                    parentFields.markUsed(getWidget(i));
                }
            }
        }

        protected ArrayList<PdfDictionary> values = new ArrayList<PdfDictionary>();

        protected ArrayList<PdfDictionary> widgets = new ArrayList<PdfDictionary>();

        protected ArrayList<PdfIndirectReference> widget_refs = new ArrayList<PdfIndirectReference>();

        protected ArrayList<PdfDictionary> merged = new ArrayList<PdfDictionary>();

        protected ArrayList<Integer> page = new ArrayList<Integer>();

        protected ArrayList<Integer> tabOrder = new ArrayList<Integer>();

        public int size() {
            return this.values.size();
        }

        void remove(int killIdx) {
            this.values.remove(killIdx);
            this.widgets.remove(killIdx);
            this.widget_refs.remove(killIdx);
            this.merged.remove(killIdx);
            this.page.remove(killIdx);
            this.tabOrder.remove(killIdx);
        }

        public PdfDictionary getValue(int idx) {
            return this.values.get(idx);
        }

        void addValue(PdfDictionary value) {
            this.values.add(value);
        }

        public PdfDictionary getWidget(int idx) {
            return this.widgets.get(idx);
        }

        void addWidget(PdfDictionary widget) {
            this.widgets.add(widget);
        }

        public PdfIndirectReference getWidgetRef(int idx) {
            return this.widget_refs.get(idx);
        }

        void addWidgetRef(PdfIndirectReference widgRef) {
            this.widget_refs.add(widgRef);
        }

        public PdfDictionary getMerged(int idx) {
            return this.merged.get(idx);
        }

        void addMerged(PdfDictionary mergeDict) {
            this.merged.add(mergeDict);
        }

        public Integer getPage(int idx) {
            return this.page.get(idx);
        }

        void addPage(int pg) {
            this.page.add(Integer.valueOf(pg));
        }

        void forcePage(int idx, int pg) {
            this.page.set(idx, Integer.valueOf(pg));
        }

        public Integer getTabOrder(int idx) {
            return this.tabOrder.get(idx);
        }

        void addTabOrder(int order) {
            this.tabOrder.add(Integer.valueOf(order));
        }
    }

    private static class InstHit {

        IntHashtable hits;

        public InstHit(int[] inst) {
            if (inst == null) {
                return;
            }
            this.hits = new IntHashtable();
            for (int k = 0; k < inst.length; k++) {
                this.hits.put(inst[k], 1);
            }
        }

        public boolean isHit(int n) {
            if (this.hits == null) {
                return true;
            }
            return this.hits.containsKey(n);
        }
    }

    public boolean clearSignatureField(String name) {
        this.sigNames = null;
        getSignatureNames();
        if (!this.sigNames.containsKey(name)) {
            return false;
        }
        Item sig = this.fields.get(name);
        sig.markUsed(this, 6);
        int n = sig.size();
        for (int k = 0; k < n; k++) {
            clearSigDic(sig.getMerged(k));
            clearSigDic(sig.getWidget(k));
            clearSigDic(sig.getValue(k));
        }
        return true;
    }

    private static void clearSigDic(PdfDictionary dic) {
        dic.remove(PdfName.AP);
        dic.remove(PdfName.AS);
        dic.remove(PdfName.V);
        dic.remove(PdfName.DV);
        dic.remove(PdfName.SV);
        dic.remove(PdfName.FF);
        dic.put(PdfName.F, new PdfNumber(4));
    }

    public ArrayList<String> getSignatureNames() {
        if (this.sigNames != null) {
            return new ArrayList<String>(this.orderedSignatureNames);
        }
        this.sigNames = new HashMap<String, int[]>();
        this.orderedSignatureNames = new ArrayList<String>();
        ArrayList<Object[]> sorter = new ArrayList();
        for (Map.Entry<String, Item> entry : this.fields.entrySet()) {
            Item item = entry.getValue();
            PdfDictionary merged = item.getMerged(0);
            if (!PdfName.SIG.equals(merged.get(PdfName.FT))) {
                continue;
            }
            PdfDictionary v = merged.getAsDict(PdfName.V);
            if (v == null) {
                continue;
            }
            PdfString contents = v.getAsString(PdfName.CONTENTS);
            if (contents == null) {
                continue;
            }
            PdfArray ro = v.getAsArray(PdfName.BYTERANGE);
            if (ro == null) {
                continue;
            }
            int rangeSize = ro.size();
            if (rangeSize < 2) {
                continue;
            }
            int length = ro.getAsNumber(rangeSize - 1).intValue() + ro.getAsNumber(rangeSize - 2).intValue();
            sorter.add(new Object[]{entry.getKey(), new int[]{length, 0}});
        }
        Collections.sort(sorter, new SorterComparator());
        if (!sorter.isEmpty()) {
            if (((int[]) ((Object[]) sorter.get(sorter.size() - 1))[1])[0] == this.reader.getFileLength()) {
                this.totalRevisions = sorter.size();
            } else {
                this.totalRevisions = sorter.size() + 1;
            }
            for (int k = 0; k < sorter.size(); k++) {
                Object[] objs = sorter.get(k);
                String name = (String) objs[0];
                int[] p = (int[]) objs[1];
                p[1] = k + 1;
                this.sigNames.put(name, p);
                this.orderedSignatureNames.add(name);
            }
        }
        return new ArrayList<String>(this.orderedSignatureNames);
    }

    public ArrayList<String> getBlankSignatureNames() {
        getSignatureNames();
        ArrayList<String> sigs = new ArrayList<String>();
        for (Map.Entry<String, Item> entry : this.fields.entrySet()) {
            Item item = entry.getValue();
            PdfDictionary merged = item.getMerged(0);
            if (!PdfName.SIG.equals(merged.getAsName(PdfName.FT))) {
                continue;
            }
            if (this.sigNames.containsKey(entry.getKey())) {
                continue;
            }
            sigs.add(entry.getKey());
        }
        return sigs;
    }

    public PdfDictionary getSignatureDictionary(String name) {
        getSignatureNames();
        name = getTranslatedFieldName(name);
        if (!this.sigNames.containsKey(name)) {
            return null;
        }
        Item item = this.fields.get(name);
        PdfDictionary merged = item.getMerged(0);
        return merged.getAsDict(PdfName.V);
    }

    public PdfIndirectReference getNormalAppearance(String name) {
        getSignatureNames();
        name = getTranslatedFieldName(name);
        Item item = this.fields.get(name);
        if (item == null) {
            return null;
        }
        PdfDictionary merged = item.getMerged(0);
        PdfDictionary ap = merged.getAsDict(PdfName.AP);
        if (ap == null) {
            return null;
        }
        PdfIndirectReference ref = ap.getAsIndirectObject(PdfName.N);
        if (ref == null) {
            return null;
        }
        return ref;
    }

    public boolean signatureCoversWholeDocument(String name) {
        getSignatureNames();
        name = getTranslatedFieldName(name);
        if (!this.sigNames.containsKey(name)) {
            return false;
        }
        return (((int[]) this.sigNames.get(name))[0] == this.reader.getFileLength());
    }

    public PdfPKCS7 verifySignature(String name) {
        return verifySignature(name, null);
    }

    public PdfPKCS7 verifySignature(String name, String provider) {
        PdfDictionary v = getSignatureDictionary(name);
        if (v == null) {
            return null;
        }
        try {
            PdfName sub = v.getAsName(PdfName.SUBFILTER);
            PdfString contents = v.getAsString(PdfName.CONTENTS);
            PdfPKCS7 pk = null;
            if (sub.equals(PdfName.ADBE_X509_RSA_SHA1)) {
                PdfString cert = v.getAsString(PdfName.CERT);
                if (cert == null) {
                    cert = v.getAsArray(PdfName.CERT).getAsString(0);
                }
                pk = new PdfPKCS7(contents.getOriginalBytes(), cert.getBytes(), provider);
            } else {

                pk = new PdfPKCS7(contents.getOriginalBytes(), sub, provider);
            }
            updateByteRange(pk, v);
            PdfString str = v.getAsString(PdfName.M);
            if (str != null) {
                pk.setSignDate(PdfDate.decode(str.toString()));
            }
            PdfObject obj = PdfReader.getPdfObject(v.get(PdfName.NAME));
            if (obj != null) {
                if (obj.isString()) {
                    pk.setSignName(((PdfString) obj).toUnicodeString());
                } else if (obj.isName()) {
                    pk.setSignName(PdfName.decodeName(obj.toString()));
                }
            }
            str = v.getAsString(PdfName.REASON);
            if (str != null) {
                pk.setReason(str.toUnicodeString());
            }
            str = v.getAsString(PdfName.LOCATION);
            if (str != null) {
                pk.setLocation(str.toUnicodeString());
            }
            return pk;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    private void updateByteRange(PdfPKCS7 pkcs7, PdfDictionary v) {
        RASInputStream rASInputStream = null;
        PdfArray b = v.getAsArray(PdfName.BYTERANGE);
        RandomAccessFileOrArray rf = this.reader.getSafeFile();
        InputStream rg = null;
        try {
            rASInputStream = new RASInputStream((new RandomAccessSourceFactory()).createRanged(rf.createSourceView(), b.asLongArray()));
            byte[] buf = new byte[8192];
            int rd;
            while ((rd = rASInputStream.read(buf, 0, buf.length)) > 0) {
                pkcs7.update(buf, 0, rd);
            }
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        } finally {
            try {
                if (rASInputStream != null) {
                    rASInputStream.close();
                }
            } catch (IOException e) {

                throw new ExceptionConverter(e);
            }
        }
    }

    private void markUsed(PdfObject obj) {
        if (!this.append) {
            return;
        }
        ((PdfStamperImp) this.writer).markUsed(obj);
    }

    public int getTotalRevisions() {
        getSignatureNames();
        return this.totalRevisions;
    }

    public int getRevision(String field) {
        getSignatureNames();
        field = getTranslatedFieldName(field);
        if (!this.sigNames.containsKey(field)) {
            return 0;
        }
        return ((int[]) this.sigNames.get(field))[1];
    }

    public InputStream extractRevision(String field) throws IOException {
        getSignatureNames();
        field = getTranslatedFieldName(field);
        if (!this.sigNames.containsKey(field)) {
            return null;
        }
        int length = ((int[]) this.sigNames.get(field))[0];
        RandomAccessFileOrArray raf = this.reader.getSafeFile();
        return (InputStream) new RASInputStream((RandomAccessSource) new WindowRandomAccessSource(raf.createSourceView(), 0L, length));
    }

    public Map<String, TextField> getFieldCache() {
        return this.fieldCache;
    }

    public void setFieldCache(Map<String, TextField> fieldCache) {
        this.fieldCache = fieldCache;
    }

    public void setExtraMargin(float extraMarginLeft, float extraMarginTop) {
        this.extraMarginLeft = extraMarginLeft;
        this.extraMarginTop = extraMarginTop;
    }

    public void addSubstitutionFont(BaseFont font) {
        if (this.substitutionFonts == null) {
            this.substitutionFonts = new ArrayList<BaseFont>();
        }
        this.substitutionFonts.add(font);
    }

    private static final HashMap<String, String[]> stdFieldFontNames = (HashMap) new HashMap<String, String>();

    private int totalRevisions;

    private Map<String, TextField> fieldCache;

    static {
        stdFieldFontNames.put("CoBO", new String[]{"Courier-BoldOblique"});
        stdFieldFontNames.put("CoBo", new String[]{"Courier-Bold"});
        stdFieldFontNames.put("CoOb", new String[]{"Courier-Oblique"});
        stdFieldFontNames.put("Cour", new String[]{"Courier"});
        stdFieldFontNames.put("HeBO", new String[]{"Helvetica-BoldOblique"});
        stdFieldFontNames.put("HeBo", new String[]{"Helvetica-Bold"});
        stdFieldFontNames.put("HeOb", new String[]{"Helvetica-Oblique"});
        stdFieldFontNames.put("Helv", new String[]{"Helvetica"});
        stdFieldFontNames.put("Symb", new String[]{"Symbol"});
        stdFieldFontNames.put("TiBI", new String[]{"Times-BoldItalic"});
        stdFieldFontNames.put("TiBo", new String[]{"Times-Bold"});
        stdFieldFontNames.put("TiIt", new String[]{"Times-Italic"});
        stdFieldFontNames.put("TiRo", new String[]{"Times-Roman"});
        stdFieldFontNames.put("ZaDb", new String[]{"ZapfDingbats"});
        stdFieldFontNames.put("HySm", new String[]{"HYSMyeongJo-Medium", "UniKS-UCS2-H"});
        stdFieldFontNames.put("HyGo", new String[]{"HYGoThic-Medium", "UniKS-UCS2-H"});
        stdFieldFontNames.put("KaGo", new String[]{"HeiseiKakuGo-W5", "UniKS-UCS2-H"});
        stdFieldFontNames.put("KaMi", new String[]{"HeiseiMin-W3", "UniJIS-UCS2-H"});
        stdFieldFontNames.put("MHei", new String[]{"MHei-Medium", "UniCNS-UCS2-H"});
        stdFieldFontNames.put("MSun", new String[]{"MSung-Light", "UniCNS-UCS2-H"});
        stdFieldFontNames.put("STSo", new String[]{"STSong-Light", "UniGB-UCS2-H"});
    }

    private static class SorterComparator implements Comparator<Object[]> {

        public int compare(Object[] o1, Object[] o2) {
            int n1 = ((int[]) o1[1])[0];
            int n2 = ((int[]) o2[1])[0];
            return n1 - n2;
        }

        private SorterComparator() {
        }
    }

    public ArrayList<BaseFont> getSubstitutionFonts() {
        return this.substitutionFonts;
    }

    public void setSubstitutionFonts(ArrayList<BaseFont> substitutionFonts) {
        this.substitutionFonts = substitutionFonts;
    }

    public XfaForm getXfa() {
        return this.xfa;
    }

    public void removeXfa() {
        PdfDictionary root = this.reader.getCatalog();
        PdfDictionary acroform = root.getAsDict(PdfName.ACROFORM);
        acroform.remove(PdfName.XFA);
        try {
            this.xfa = new XfaForm(this.reader);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    private static final PdfName[] buttonRemove = new PdfName[]{PdfName.MK, PdfName.F, PdfName.FF, PdfName.Q, PdfName.BS, PdfName.BORDER};

    public PushbuttonField getNewPushbuttonFromField(String field) {
        return getNewPushbuttonFromField(field, 0);
    }

    public PushbuttonField getNewPushbuttonFromField(String field, int order) {
        try {
            if (getFieldType(field) != 1) {
                return null;
            }
            Item item = getFieldItem(field);
            if (order >= item.size()) {
                return null;
            }
            List<FieldPosition> pos = getFieldPositions(field);
            Rectangle box = ((FieldPosition) pos.get(order)).position;
            PushbuttonField newButton = new PushbuttonField(this.writer, box, null);
            PdfDictionary dic = item.getMerged(order);
            decodeGenericDictionary(dic, newButton);
            PdfDictionary mk = dic.getAsDict(PdfName.MK);
            if (mk != null) {
                PdfString text = mk.getAsString(PdfName.CA);
                if (text != null) {
                    newButton.setText(text.toUnicodeString());
                }
                PdfNumber tp = mk.getAsNumber(PdfName.TP);
                if (tp != null) {
                    newButton.setLayout(tp.intValue() + 1);
                }
                PdfDictionary ifit = mk.getAsDict(PdfName.IF);
                if (ifit != null) {
                    PdfName sw = ifit.getAsName(PdfName.SW);
                    if (sw != null) {
                        int scale = 1;
                        if (sw.equals(PdfName.B)) {
                            scale = 3;
                        } else if (sw.equals(PdfName.S)) {
                            scale = 4;
                        } else if (sw.equals(PdfName.N)) {
                            scale = 2;
                        }
                        newButton.setScaleIcon(scale);
                    }
                    sw = ifit.getAsName(PdfName.S);
                    if (sw != null
                            && sw.equals(PdfName.A)) {
                        newButton.setProportionalIcon(false);
                    }
                    PdfArray aj = ifit.getAsArray(PdfName.A);
                    if (aj != null && aj.size() == 2) {
                        float left = aj.getAsNumber(0).floatValue();
                        float bottom = aj.getAsNumber(1).floatValue();
                        newButton.setIconHorizontalAdjustment(left);
                        newButton.setIconVerticalAdjustment(bottom);
                    }
                    PdfBoolean fb = ifit.getAsBoolean(PdfName.FB);
                    if (fb != null && fb.booleanValue()) {
                        newButton.setIconFitToBounds(true);
                    }
                }
                PdfObject i = mk.get(PdfName.I);
                if (i != null && i.isIndirect()) {
                    newButton.setIconReference((PRIndirectReference) i);
                }
            }
            return newButton;
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public boolean replacePushbuttonField(String field, PdfFormField button) {
        return replacePushbuttonField(field, button, 0);
    }

    public boolean replacePushbuttonField(String field, PdfFormField button, int order) {
        if (getFieldType(field) != 1) {
            return false;
        }
        Item item = getFieldItem(field);
        if (order >= item.size()) {
            return false;
        }
        PdfDictionary merged = item.getMerged(order);
        PdfDictionary values = item.getValue(order);
        PdfDictionary widgets = item.getWidget(order);
        for (int k = 0; k < buttonRemove.length; k++) {
            merged.remove(buttonRemove[k]);
            values.remove(buttonRemove[k]);
            widgets.remove(buttonRemove[k]);
        }
        for (PdfName element : button.getKeys()) {
            PdfName key = element;
            if (key.equals(PdfName.T)) {
                continue;
            }
            if (key.equals(PdfName.FF)) {
                values.put(key, button.get(key));
            } else {
                widgets.put(key, button.get(key));
            }
            merged.put(key, button.get(key));
            markUsed(values);
            markUsed(widgets);
        }
        return true;
    }

    public boolean doesSignatureFieldExist(String name) {
        return (getBlankSignatureNames().contains(name) || getSignatureNames().contains(name));
    }

    public static class FieldPosition {

        public int page;
        public Rectangle position;
    }
}
