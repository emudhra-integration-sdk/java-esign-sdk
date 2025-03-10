package esign.text.pdf.codec.wmf;

import esign.text.BaseColor;
import esign.text.DocumentException;
import esign.text.Image;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.BaseFont;
import esign.text.pdf.PdfContentByte;
import esign.text.pdf.codec.BmpImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class MetaDo {

    public static final int META_SETBKCOLOR = 513;
    public static final int META_SETBKMODE = 258;
    public static final int META_SETMAPMODE = 259;
    public static final int META_SETROP2 = 260;
    public static final int META_SETRELABS = 261;
    public static final int META_SETPOLYFILLMODE = 262;
    public static final int META_SETSTRETCHBLTMODE = 263;
    public static final int META_SETTEXTCHAREXTRA = 264;
    public static final int META_SETTEXTCOLOR = 521;
    public static final int META_SETTEXTJUSTIFICATION = 522;
    public static final int META_SETWINDOWORG = 523;
    public static final int META_SETWINDOWEXT = 524;
    public static final int META_SETVIEWPORTORG = 525;
    public static final int META_SETVIEWPORTEXT = 526;
    public static final int META_OFFSETWINDOWORG = 527;
    public static final int META_SCALEWINDOWEXT = 1040;
    public static final int META_OFFSETVIEWPORTORG = 529;
    public static final int META_SCALEVIEWPORTEXT = 1042;
    public static final int META_LINETO = 531;
    public static final int META_MOVETO = 532;
    public static final int META_EXCLUDECLIPRECT = 1045;
    public static final int META_INTERSECTCLIPRECT = 1046;
    public static final int META_ARC = 2071;
    public static final int META_ELLIPSE = 1048;
    public static final int META_FLOODFILL = 1049;
    public static final int META_PIE = 2074;
    public static final int META_RECTANGLE = 1051;
    public static final int META_ROUNDRECT = 1564;
    public static final int META_PATBLT = 1565;
    public static final int META_SAVEDC = 30;
    public static final int META_SETPIXEL = 1055;
    public static final int META_OFFSETCLIPRGN = 544;
    public static final int META_TEXTOUT = 1313;
    public static final int META_BITBLT = 2338;
    public static final int META_STRETCHBLT = 2851;
    public static final int META_POLYGON = 804;
    public static final int META_POLYLINE = 805;
    public static final int META_ESCAPE = 1574;
    public static final int META_RESTOREDC = 295;
    public static final int META_FILLREGION = 552;
    public static final int META_FRAMEREGION = 1065;
    public static final int META_INVERTREGION = 298;
    public static final int META_PAINTREGION = 299;
    public static final int META_SELECTCLIPREGION = 300;
    public static final int META_SELECTOBJECT = 301;
    public static final int META_SETTEXTALIGN = 302;
    public static final int META_CHORD = 2096;
    public static final int META_SETMAPPERFLAGS = 561;
    public static final int META_EXTTEXTOUT = 2610;
    public static final int META_SETDIBTODEV = 3379;
    public static final int META_SELECTPALETTE = 564;
    public static final int META_REALIZEPALETTE = 53;
    public static final int META_ANIMATEPALETTE = 1078;
    public static final int META_SETPALENTRIES = 55;
    public static final int META_POLYPOLYGON = 1336;
    public static final int META_RESIZEPALETTE = 313;
    public static final int META_DIBBITBLT = 2368;
    public static final int META_DIBSTRETCHBLT = 2881;
    public static final int META_DIBCREATEPATTERNBRUSH = 322;
    public static final int META_STRETCHDIB = 3907;
    public static final int META_EXTFLOODFILL = 1352;
    public static final int META_DELETEOBJECT = 496;
    public static final int META_CREATEPALETTE = 247;
    public static final int META_CREATEPATTERNBRUSH = 505;
    public static final int META_CREATEPENINDIRECT = 762;
    public static final int META_CREATEFONTINDIRECT = 763;
    public static final int META_CREATEBRUSHINDIRECT = 764;
    public static final int META_CREATEREGION = 1791;
    public PdfContentByte cb;
    public InputMeta in;
    int left;
    int top;
    int right;
    int bottom;
    int inch;
    MetaState state = new MetaState();

    public MetaDo(InputStream in, PdfContentByte cb) {
        this.cb = cb;
        this.in = new InputMeta(in);
    }

    public void readAll() throws IOException, DocumentException {
        if (this.in.readInt() != -1698247209) {
            throw new DocumentException(MessageLocalization.getComposedMessage("not.a.placeable.windows.metafile", new Object[0]));
        }
        this.in.readWord();
        this.left = this.in.readShort();
        this.top = this.in.readShort();
        this.right = this.in.readShort();
        this.bottom = this.in.readShort();
        this.inch = this.in.readWord();
        this.state.setScalingX((this.right - this.left) / this.inch * 72.0F);
        this.state.setScalingY((this.bottom - this.top) / this.inch * 72.0F);
        this.state.setOffsetWx(this.left);
        this.state.setOffsetWy(this.top);
        this.state.setExtentWx(this.right - this.left);
        this.state.setExtentWy(this.bottom - this.top);
        this.in.readInt();
        this.in.readWord();
        this.in.skip(18);

        this.cb.setLineCap(1);
        this.cb.setLineJoin(1);
        while (true) {
            MetaPen pen;
            MetaBrush brush;
            MetaFont font;
            int idx, m, len, numPoly, i;
            float yend, f1, h, b;
            int y, count;
            BaseColor color;
            int rop;
            Point p;
            int i2, sx, lens[], i1;
            float xend, f2, w, r;
            int x;
            byte[] text;
            int n, srcHeight;
            Point point1;
            int i7, sy, i6, j, i5;
            float ystart, f4, f3, t;
            int i4, k, i3, srcWidth, i9, i8;
            float xstart, f6, f5, l;
            int flag;
            String s;
            int ySrc;
            float f8, f7;
            int x1, i10, xSrc;
            float f10, f9;
            int y1, i11;
            float destHeight, f11;
            int x2;
            float destWidth, f12;
            int y2;
            float yDest, f13;
            double cx;
            byte[] arrayOfByte1;
            float xDest, cy;
            int i12;
            byte[] arrayOfByte2;
            float f14;
            double arc1, d1;
            String str1;
            int i13;
            float arc2;
            double d3, d2;
            ArrayList<double[]> ar;
            double d4, pt[];
            int i14;
            ArrayList<double[]> arrayList1;
            double[] arrayOfDouble1;
            int i15, lenMarker = this.in.getLength();
            int tsize = this.in.readInt();
            if (tsize < 3) {
                break;
            }
            int function = this.in.readWord();
            switch (function) {

                case 247:
                case 322:
                case 1791:
                    this.state.addMetaObject(new MetaObject());
                    break;

                case 762:
                    pen = new MetaPen();
                    pen.init(this.in);
                    this.state.addMetaObject(pen);
                    break;

                case 764:
                    brush = new MetaBrush();
                    brush.init(this.in);
                    this.state.addMetaObject(brush);
                    break;

                case 763:
                    font = new MetaFont();
                    font.init(this.in);
                    this.state.addMetaObject(font);
                    break;

                case 301:
                    idx = this.in.readWord();
                    this.state.selectMetaObject(idx, this.cb);
                    break;

                case 496:
                    idx = this.in.readWord();
                    this.state.deleteMetaObject(idx);
                    break;

                case 30:
                    this.state.saveState(this.cb);
                    break;

                case 295:
                    idx = this.in.readShort();
                    this.state.restoreState(idx, this.cb);
                    break;

                case 523:
                    this.state.setOffsetWy(this.in.readShort());
                    this.state.setOffsetWx(this.in.readShort());
                    break;
                case 524:
                    this.state.setExtentWy(this.in.readShort());
                    this.state.setExtentWx(this.in.readShort());
                    break;

                case 532:
                    m = this.in.readShort();
                    p = new Point(this.in.readShort(), m);
                    this.state.setCurrentPoint(p);
                    break;

                case 531:
                    m = this.in.readShort();
                    i2 = this.in.readShort();
                    point1 = this.state.getCurrentPoint();
                    this.cb.moveTo(this.state.transformX(point1.x), this.state.transformY(point1.y));
                    this.cb.lineTo(this.state.transformX(i2), this.state.transformY(m));
                    this.cb.stroke();
                    this.state.setCurrentPoint(new Point(i2, m));
                    break;

                case 805:
                    this.state.setLineJoinPolygon(this.cb);
                    len = this.in.readWord();
                    i2 = this.in.readShort();
                    i7 = this.in.readShort();
                    this.cb.moveTo(this.state.transformX(i2), this.state.transformY(i7));
                    for (i9 = 1; i9 < len; i9++) {
                        i2 = this.in.readShort();
                        i7 = this.in.readShort();
                        this.cb.lineTo(this.state.transformX(i2), this.state.transformY(i7));
                    }
                    this.cb.stroke();
                    break;

                case 804:
                    if (isNullStrokeFill(false)) {
                        break;
                    }
                    len = this.in.readWord();
                    sx = this.in.readShort();
                    sy = this.in.readShort();
                    this.cb.moveTo(this.state.transformX(sx), this.state.transformY(sy));
                    for (i9 = 1; i9 < len; i9++) {
                        int i16 = this.in.readShort();
                        int i17 = this.in.readShort();
                        this.cb.lineTo(this.state.transformX(i16), this.state.transformY(i17));
                    }
                    this.cb.lineTo(this.state.transformX(sx), this.state.transformY(sy));
                    strokeAndFill();
                    break;

                case 1336:
                    if (isNullStrokeFill(false)) {
                        break;
                    }
                    numPoly = this.in.readWord();
                    lens = new int[numPoly];
                    for (i6 = 0; i6 < lens.length; i6++) {
                        lens[i6] = this.in.readWord();
                    }
                    for (j = 0; j < lens.length; j++) {
                        int i16 = lens[j];
                        int i17 = this.in.readShort();
                        int i18 = this.in.readShort();
                        this.cb.moveTo(this.state.transformX(i17), this.state.transformY(i18));
                        for (int i19 = 1; i19 < i16; i19++) {
                            int i20 = this.in.readShort();
                            int i21 = this.in.readShort();
                            this.cb.lineTo(this.state.transformX(i20), this.state.transformY(i21));
                        }
                        this.cb.lineTo(this.state.transformX(i17), this.state.transformY(i18));
                    }
                    strokeAndFill();
                    break;

                case 1048:
                    if (isNullStrokeFill(this.state.getLineNeutral())) {
                        break;
                    }
                    i = this.in.readShort();
                    i1 = this.in.readShort();
                    i5 = this.in.readShort();
                    i8 = this.in.readShort();
                    this.cb.arc(this.state.transformX(i8), this.state.transformY(i), this.state.transformX(i1), this.state.transformY(i5), 0.0F, 360.0F);
                    strokeAndFill();
                    break;

                case 2071:
                    if (isNullStrokeFill(this.state.getLineNeutral())) {
                        break;
                    }
                    yend = this.state.transformY(this.in.readShort());
                    xend = this.state.transformX(this.in.readShort());
                    ystart = this.state.transformY(this.in.readShort());
                    xstart = this.state.transformX(this.in.readShort());
                    f8 = this.state.transformY(this.in.readShort());
                    f10 = this.state.transformX(this.in.readShort());
                    f11 = this.state.transformY(this.in.readShort());
                    f12 = this.state.transformX(this.in.readShort());
                    f13 = (f10 + f12) / 2.0F;
                    cy = (f11 + f8) / 2.0F;
                    f14 = getArc(f13, cy, xstart, ystart);
                    arc2 = getArc(f13, cy, xend, yend);
                    arc2 -= f14;
                    if (arc2 <= 0.0F) {
                        arc2 += 360.0F;
                    }
                    this.cb.arc(f12, f8, f10, f11, f14, arc2);
                    this.cb.stroke();
                    break;

                case 2074:
                    if (isNullStrokeFill(this.state.getLineNeutral())) {
                        break;
                    }
                    yend = this.state.transformY(this.in.readShort());
                    xend = this.state.transformX(this.in.readShort());
                    ystart = this.state.transformY(this.in.readShort());
                    xstart = this.state.transformX(this.in.readShort());
                    f8 = this.state.transformY(this.in.readShort());
                    f10 = this.state.transformX(this.in.readShort());
                    f11 = this.state.transformY(this.in.readShort());
                    f12 = this.state.transformX(this.in.readShort());
                    f13 = (f10 + f12) / 2.0F;
                    cy = (f11 + f8) / 2.0F;
                    arc1 = getArc(f13, cy, xstart, ystart);
                    d3 = getArc(f13, cy, xend, yend);
                    d3 -= arc1;
                    if (d3 <= 0.0D) {
                        d3 += 360.0D;
                    }
                    ar = PdfContentByte.bezierArc(f12, f8, f10, f11, arc1, d3);
                    if (ar.isEmpty()) {
                        break;
                    }
                    pt = ar.get(0);
                    this.cb.moveTo(f13, cy);
                    this.cb.lineTo(pt[0], pt[1]);
                    for (i14 = 0; i14 < ar.size(); i14++) {
                        pt = ar.get(i14);
                        this.cb.curveTo(pt[2], pt[3], pt[4], pt[5], pt[6], pt[7]);
                    }
                    this.cb.lineTo(f13, cy);
                    strokeAndFill();
                    break;

                case 2096:
                    if (isNullStrokeFill(this.state.getLineNeutral())) {
                        break;
                    }
                    yend = this.state.transformY(this.in.readShort());
                    xend = this.state.transformX(this.in.readShort());
                    ystart = this.state.transformY(this.in.readShort());
                    xstart = this.state.transformX(this.in.readShort());
                    f8 = this.state.transformY(this.in.readShort());
                    f10 = this.state.transformX(this.in.readShort());
                    f11 = this.state.transformY(this.in.readShort());
                    f12 = this.state.transformX(this.in.readShort());
                    cx = ((f10 + f12) / 2.0F);
                    d1 = ((f11 + f8) / 2.0F);
                    d2 = getArc(cx, d1, xstart, ystart);
                    d4 = getArc(cx, d1, xend, yend);
                    d4 -= d2;
                    if (d4 <= 0.0D) {
                        d4 += 360.0D;
                    }
                    arrayList1 = PdfContentByte.bezierArc(f12, f8, f10, f11, d2, d4);
                    if (arrayList1.isEmpty()) {
                        break;
                    }
                    arrayOfDouble1 = arrayList1.get(0);
                    cx = arrayOfDouble1[0];
                    d1 = arrayOfDouble1[1];
                    this.cb.moveTo(cx, d1);
                    for (i15 = 0; i15 < arrayList1.size(); i15++) {
                        arrayOfDouble1 = arrayList1.get(i15);
                        this.cb.curveTo(arrayOfDouble1[2], arrayOfDouble1[3], arrayOfDouble1[4], arrayOfDouble1[5], arrayOfDouble1[6], arrayOfDouble1[7]);
                    }
                    this.cb.lineTo(cx, d1);
                    strokeAndFill();
                    break;

                case 1051:
                    if (isNullStrokeFill(true)) {
                        break;
                    }
                    f1 = this.state.transformY(this.in.readShort());
                    f2 = this.state.transformX(this.in.readShort());
                    f4 = this.state.transformY(this.in.readShort());
                    f6 = this.state.transformX(this.in.readShort());
                    this.cb.rectangle(f6, f1, f2 - f6, f4 - f1);
                    strokeAndFill();
                    break;

                case 1564:
                    if (isNullStrokeFill(true)) {
                        break;
                    }
                    h = this.state.transformY(0) - this.state.transformY(this.in.readShort());
                    w = this.state.transformX(this.in.readShort()) - this.state.transformX(0);
                    f3 = this.state.transformY(this.in.readShort());
                    f5 = this.state.transformX(this.in.readShort());
                    f7 = this.state.transformY(this.in.readShort());
                    f9 = this.state.transformX(this.in.readShort());
                    this.cb.roundRectangle(f9, f3, f5 - f9, f7 - f3, (h + w) / 4.0F);
                    strokeAndFill();
                    break;

                case 1046:
                    b = this.state.transformY(this.in.readShort());
                    r = this.state.transformX(this.in.readShort());
                    t = this.state.transformY(this.in.readShort());
                    l = this.state.transformX(this.in.readShort());
                    this.cb.rectangle(l, b, r - l, t - b);
                    this.cb.eoClip();
                    this.cb.newPath();
                    break;

                case 2610:
                    y = this.in.readShort();
                    x = this.in.readShort();
                    i4 = this.in.readWord();
                    flag = this.in.readWord();
                    x1 = 0;
                    y1 = 0;
                    x2 = 0;
                    y2 = 0;
                    if ((flag & 0x6) != 0) {
                        x1 = this.in.readShort();
                        y1 = this.in.readShort();
                        x2 = this.in.readShort();
                        y2 = this.in.readShort();
                    }
                    arrayOfByte1 = new byte[i4];

                    for (i12 = 0; i12 < i4; i12++) {
                        byte c = (byte) this.in.readByte();
                        if (c == 0) {
                            break;
                        }
                        arrayOfByte1[i12] = c;
                    }

                    try {
                        str1 = new String(arrayOfByte1, 0, i12, "Cp1252");
                    } catch (UnsupportedEncodingException e) {
                        str1 = new String(arrayOfByte1, 0, i12);
                    }
                    outputText(x, y, flag, x1, y1, x2, y2, str1);
                    break;

                case 1313:
                    count = this.in.readWord();
                    text = new byte[count];

                    for (k = 0; k < count; k++) {
                        byte c = (byte) this.in.readByte();
                        if (c == 0) {
                            break;
                        }
                        text[k] = c;
                    }

                    try {
                        s = new String(text, 0, k, "Cp1252");
                    } catch (UnsupportedEncodingException e) {
                        s = new String(text, 0, k);
                    }
                    count = count + 1 & 0xFFFE;
                    this.in.skip(count - k);
                    i10 = this.in.readShort();
                    i11 = this.in.readShort();
                    outputText(i11, i10, 0, 0, 0, 0, 0, s);
                    break;

                case 513:
                    this.state.setCurrentBackgroundColor(this.in.readColor());
                    break;
                case 521:
                    this.state.setCurrentTextColor(this.in.readColor());
                    break;
                case 302:
                    this.state.setTextAlign(this.in.readWord());
                    break;
                case 258:
                    this.state.setBackgroundMode(this.in.readWord());
                    break;
                case 262:
                    this.state.setPolyFillMode(this.in.readWord());
                    break;

                case 1055:
                    color = this.in.readColor();
                    n = this.in.readShort();
                    i3 = this.in.readShort();
                    this.cb.saveState();
                    this.cb.setColorFill(color);
                    this.cb.rectangle(this.state.transformX(i3), this.state.transformY(n), 0.2F, 0.2F);
                    this.cb.fill();
                    this.cb.restoreState();
                    break;

                case 2881:
                case 3907:
                    rop = this.in.readInt();
                    if (function == 3907) {
                        this.in.readWord();
                    }
                    srcHeight = this.in.readShort();
                    srcWidth = this.in.readShort();
                    ySrc = this.in.readShort();
                    xSrc = this.in.readShort();
                    destHeight = this.state.transformY(this.in.readShort()) - this.state.transformY(0);
                    destWidth = this.state.transformX(this.in.readShort()) - this.state.transformX(0);
                    yDest = this.state.transformY(this.in.readShort());
                    xDest = this.state.transformX(this.in.readShort());
                    arrayOfByte2 = new byte[tsize * 2 - this.in.getLength() - lenMarker];
                    for (i13 = 0; i13 < arrayOfByte2.length; i13++) {
                        arrayOfByte2[i13] = (byte) this.in.readByte();
                    }
                    try {
                        ByteArrayInputStream inb = new ByteArrayInputStream(arrayOfByte2);
                        Image bmp = BmpImage.getImage(inb, true, arrayOfByte2.length);
                        this.cb.saveState();
                        this.cb.rectangle(xDest, yDest, destWidth, destHeight);
                        this.cb.clip();
                        this.cb.newPath();
                        bmp.scaleAbsolute(destWidth * bmp.getWidth() / srcWidth, -destHeight * bmp.getHeight() / srcHeight);
                        bmp.setAbsolutePosition(xDest - destWidth * xSrc / srcWidth, yDest + destHeight * ySrc / srcHeight - bmp.getScaledHeight());
                        this.cb.addImage(bmp);
                        this.cb.restoreState();
                    } catch (Exception exception) {
                    }
                    break;
            }

            this.in.skip(tsize * 2 - this.in.getLength() - lenMarker);
        }
        this.state.cleanup(this.cb);
    }

    public void outputText(int x, int y, int flag, int x1, int y1, int x2, int y2, String text) {
        MetaFont font = this.state.getCurrentFont();
        float refX = this.state.transformX(x);
        float refY = this.state.transformY(y);
        float angle = this.state.transformAngle(font.getAngle());
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);
        float fontSize = font.getFontSize(this.state);
        BaseFont bf = font.getFont();
        int align = this.state.getTextAlign();
        float textWidth = bf.getWidthPoint(text, fontSize);
        float tx = 0.0F;
        float ty = 0.0F;
        float descender = bf.getFontDescriptor(3, fontSize);
        float ury = bf.getFontDescriptor(8, fontSize);
        this.cb.saveState();
        this.cb.concatCTM(cos, sin, -sin, cos, refX, refY);
        if ((align & 0x6) == 6) {
            tx = -textWidth / 2.0F;
        } else if ((align & 0x2) == 2) {
            tx = -textWidth;
        }
        if ((align & 0x18) == 24) {
            ty = 0.0F;
        } else if ((align & 0x8) == 8) {
            ty = -descender;
        } else {
            ty = -ury;
        }
        if (this.state.getBackgroundMode() == 2) {
            BaseColor baseColor = this.state.getCurrentBackgroundColor();
            this.cb.setColorFill(baseColor);
            this.cb.rectangle(tx, ty + descender, textWidth, ury - descender);
            this.cb.fill();
        }
        BaseColor textColor = this.state.getCurrentTextColor();
        this.cb.setColorFill(textColor);
        this.cb.beginText();
        this.cb.setFontAndSize(bf, fontSize);
        this.cb.setTextMatrix(tx, ty);
        this.cb.showText(text);
        this.cb.endText();
        if (font.isUnderline()) {
            this.cb.rectangle(tx, ty - fontSize / 4.0F, textWidth, fontSize / 15.0F);
            this.cb.fill();
        }
        if (font.isStrikeout()) {
            this.cb.rectangle(tx, ty + fontSize / 3.0F, textWidth, fontSize / 15.0F);
            this.cb.fill();
        }
        this.cb.restoreState();
    }

    public boolean isNullStrokeFill(boolean isRectangle) {
        MetaPen pen = this.state.getCurrentPen();
        MetaBrush brush = this.state.getCurrentBrush();
        boolean noPen = (pen.getStyle() == 5);
        int style = brush.getStyle();
        boolean isBrush = (style == 0 || (style == 2 && this.state.getBackgroundMode() == 2));
        boolean result = (noPen && !isBrush);
        if (!noPen) {
            if (isRectangle) {
                this.state.setLineJoinRectangle(this.cb);
            } else {
                this.state.setLineJoinPolygon(this.cb);
            }
        }
        return result;
    }

    public void strokeAndFill() {
        MetaPen pen = this.state.getCurrentPen();
        MetaBrush brush = this.state.getCurrentBrush();
        int penStyle = pen.getStyle();
        int brushStyle = brush.getStyle();
        if (penStyle == 5) {
            this.cb.closePath();
            if (this.state.getPolyFillMode() == 1) {
                this.cb.eoFill();
            } else {

                this.cb.fill();
            }
        } else {

            boolean isBrush = (brushStyle == 0 || (brushStyle == 2 && this.state.getBackgroundMode() == 2));
            if (isBrush) {
                if (this.state.getPolyFillMode() == 1) {
                    this.cb.closePathEoFillStroke();
                } else {
                    this.cb.closePathFillStroke();
                }
            } else {
                this.cb.closePathStroke();
            }
        }
    }

    static float getArc(float xCenter, float yCenter, float xDot, float yDot) {
        return (float) getArc(xCenter, yCenter, xDot, yDot);
    }

    static double getArc(double xCenter, double yCenter, double xDot, double yDot) {
        double s = Math.atan2(yDot - yCenter, xDot - xCenter);
        if (s < 0.0D) {
            s += 6.283185307179586D;
        }
        return (float) (s / Math.PI * 180.0D);
    }

    public static byte[] wrapBMP(Image image) throws IOException {
        if (image.getOriginalType() != 4) {
            throw new IOException(MessageLocalization.getComposedMessage("only.bmp.can.be.wrapped.in.wmf", new Object[0]));
        }
        byte[] data = null;
        if (image.getOriginalData() == null) {
            InputStream imgIn = image.getUrl().openStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int b = 0;
            while ((b = imgIn.read()) != -1) {
                out.write(b);
            }
            imgIn.close();
            data = out.toByteArray();
        } else {

            data = image.getOriginalData();
        }
        int sizeBmpWords = data.length - 14 + 1 >>> 1;
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        writeWord(os, 1);
        writeWord(os, 9);
        writeWord(os, 768);
        writeDWord(os, 36 + sizeBmpWords + 3);
        writeWord(os, 1);
        writeDWord(os, 14 + sizeBmpWords);
        writeWord(os, 0);

        writeDWord(os, 4);
        writeWord(os, 259);
        writeWord(os, 8);

        writeDWord(os, 5);
        writeWord(os, 523);
        writeWord(os, 0);
        writeWord(os, 0);

        writeDWord(os, 5);
        writeWord(os, 524);
        writeWord(os, (int) image.getHeight());
        writeWord(os, (int) image.getWidth());

        writeDWord(os, 13 + sizeBmpWords);
        writeWord(os, 2881);
        writeDWord(os, 13369376);
        writeWord(os, (int) image.getHeight());
        writeWord(os, (int) image.getWidth());
        writeWord(os, 0);
        writeWord(os, 0);
        writeWord(os, (int) image.getHeight());
        writeWord(os, (int) image.getWidth());
        writeWord(os, 0);
        writeWord(os, 0);
        os.write(data, 14, data.length - 14);
        if ((data.length & 0x1) == 1) {
            os.write(0);
        }

        writeDWord(os, 3);
        writeWord(os, 0);
        os.close();
        return os.toByteArray();
    }

    public static void writeWord(OutputStream os, int v) throws IOException {
        os.write(v & 0xFF);
        os.write(v >>> 8 & 0xFF);
    }

    public static void writeDWord(OutputStream os, int v) throws IOException {
        writeWord(os, v & 0xFFFF);
        writeWord(os, v >>> 16 & 0xFFFF);
    }
}
