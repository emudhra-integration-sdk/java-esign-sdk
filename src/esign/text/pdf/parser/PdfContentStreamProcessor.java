package esign.text.pdf.parser;

import esign.text.BaseColor;
import esign.text.ExceptionConverter;
import esign.text.error_messages.MessageLocalization;
import esign.text.io.RandomAccessSourceFactory;
import esign.text.pdf.CMYKColor;
import esign.text.pdf.CMapAwareDocumentFont;
import esign.text.pdf.GrayColor;
import esign.text.pdf.PRIndirectReference;
import esign.text.pdf.PRTokeniser;
import esign.text.pdf.PdfArray;
import esign.text.pdf.PdfContentParser;
import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfIndirectReference;
import esign.text.pdf.PdfLiteral;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfNumber;
import esign.text.pdf.PdfObject;
import esign.text.pdf.PdfStream;
import esign.text.pdf.PdfString;
import esign.text.pdf.RandomAccessFileOrArray;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class PdfContentStreamProcessor {

    public static final String DEFAULTOPERATOR = "DefaultOperator";
    private final Map<String, ContentOperator> operators;
    private ResourceDictionary resources;
    private final Stack<GraphicsState> gsStack = new Stack<GraphicsState>();

    private Matrix textMatrix;

    private Matrix textLineMatrix;

    private final RenderListener renderListener;

    private final Map<PdfName, XObjectDoHandler> xobjectDoHandlers;

    private final Map<Integer, CMapAwareDocumentFont> cachedFonts = new HashMap<Integer, CMapAwareDocumentFont>();

    private final Stack<MarkedContentInfo> markedContentStack = new Stack<MarkedContentInfo>();

    public PdfContentStreamProcessor(RenderListener renderListener) {
        this.renderListener = renderListener;
        this.operators = new HashMap<String, ContentOperator>();
        populateOperators();
        this.xobjectDoHandlers = new HashMap<PdfName, XObjectDoHandler>();
        populateXObjectDoHandlers();
        reset();
    }

    private void populateXObjectDoHandlers() {
        registerXObjectDoHandler(PdfName.DEFAULT, new IgnoreXObjectDoHandler());
        registerXObjectDoHandler(PdfName.FORM, new FormXObjectDoHandler());
        registerXObjectDoHandler(PdfName.IMAGE, new ImageXObjectDoHandler());
    }

    public XObjectDoHandler registerXObjectDoHandler(PdfName xobjectSubType, XObjectDoHandler handler) {
        return this.xobjectDoHandlers.put(xobjectSubType, handler);
    }

    private CMapAwareDocumentFont getFont(PRIndirectReference ind) {
        Integer n = Integer.valueOf(ind.getNumber());
        CMapAwareDocumentFont font = this.cachedFonts.get(n);
        if (font == null) {
            font = new CMapAwareDocumentFont(ind);
            this.cachedFonts.put(n, font);
        }
        return font;
    }

    private CMapAwareDocumentFont getFont(PdfDictionary fontResource) {
        return new CMapAwareDocumentFont(fontResource);
    }

    private void populateOperators() {
        registerContentOperator("DefaultOperator", new IgnoreOperatorContentOperator());

        registerContentOperator("q", new PushGraphicsState());
        registerContentOperator("Q", new PopGraphicsState());
        registerContentOperator("g", new SetGrayFill());
        registerContentOperator("G", new SetGrayStroke());
        registerContentOperator("rg", new SetRGBFill());
        registerContentOperator("RG", new SetRGBStroke());
        registerContentOperator("k", new SetCMYKFill());
        registerContentOperator("K", new SetCMYKStroke());
        registerContentOperator("cs", new SetColorSpaceFill());
        registerContentOperator("CS", new SetColorSpaceStroke());
        registerContentOperator("sc", new SetColorFill());
        registerContentOperator("SC", new SetColorStroke());
        registerContentOperator("scn", new SetColorFill());
        registerContentOperator("SCN", new SetColorStroke());
        registerContentOperator("cm", new ModifyCurrentTransformationMatrix());
        registerContentOperator("gs", new ProcessGraphicsStateResource());

        SetTextCharacterSpacing tcOperator = new SetTextCharacterSpacing();
        registerContentOperator("Tc", tcOperator);
        SetTextWordSpacing twOperator = new SetTextWordSpacing();
        registerContentOperator("Tw", twOperator);
        registerContentOperator("Tz", new SetTextHorizontalScaling());
        SetTextLeading tlOperator = new SetTextLeading();
        registerContentOperator("TL", tlOperator);
        registerContentOperator("Tf", new SetTextFont());
        registerContentOperator("Tr", new SetTextRenderMode());
        registerContentOperator("Ts", new SetTextRise());

        registerContentOperator("BT", new BeginText());
        registerContentOperator("ET", new EndText());
        registerContentOperator("BMC", new BeginMarkedContent());
        registerContentOperator("BDC", new BeginMarkedContentDictionary());
        registerContentOperator("EMC", new EndMarkedContent());

        TextMoveStartNextLine tdOperator = new TextMoveStartNextLine();
        registerContentOperator("Td", tdOperator);
        registerContentOperator("TD", new TextMoveStartNextLineWithLeading(tdOperator, tlOperator));
        registerContentOperator("Tm", new TextSetTextMatrix());
        TextMoveNextLine tstarOperator = new TextMoveNextLine(tdOperator);
        registerContentOperator("T*", tstarOperator);

        ShowText tjOperator = new ShowText();
        registerContentOperator("Tj", tjOperator);
        MoveNextLineAndShowText tickOperator = new MoveNextLineAndShowText(tstarOperator, tjOperator);
        registerContentOperator("'", tickOperator);
        registerContentOperator("\"", new MoveNextLineAndShowTextWithSpacing(twOperator, tcOperator, tickOperator));
        registerContentOperator("TJ", new ShowTextArray());

        registerContentOperator("Do", new Do());

        registerContentOperator("w", new SetLineWidth());
        registerContentOperator("J", new SetLineCap());
        registerContentOperator("j", new SetLineJoin());
        registerContentOperator("M", new SetMiterLimit());
        registerContentOperator("d", new SetLineDashPattern());

        if (this.renderListener instanceof ExtRenderListener) {
            int fillStroke = 3;
            registerContentOperator("m", new MoveTo());
            registerContentOperator("l", new LineTo());
            registerContentOperator("c", new Curve());
            registerContentOperator("v", new CurveFirstPointDuplicated());
            registerContentOperator("y", new CurveFourhPointDuplicated());
            registerContentOperator("h", new CloseSubpath());
            registerContentOperator("re", new Rectangle());
            registerContentOperator("S", new PaintPath(1, -1, false));
            registerContentOperator("s", new PaintPath(1, -1, true));
            registerContentOperator("f", new PaintPath(2, 1, false));
            registerContentOperator("F", new PaintPath(2, 1, false));
            registerContentOperator("f*", new PaintPath(2, 2, false));
            registerContentOperator("B", new PaintPath(fillStroke, 1, false));
            registerContentOperator("B*", new PaintPath(fillStroke, 2, false));
            registerContentOperator("b", new PaintPath(fillStroke, 1, true));
            registerContentOperator("b*", new PaintPath(fillStroke, 2, true));
            registerContentOperator("n", new PaintPath(0, -1, false));
            registerContentOperator("W", new ClipPath(1));
            registerContentOperator("W*", new ClipPath(2));
        }
    }

    public ContentOperator registerContentOperator(String operatorString, ContentOperator operator) {
        return this.operators.put(operatorString, operator);
    }

    public Collection<String> getRegisteredOperatorStrings() {
        return new ArrayList<String>(this.operators.keySet());
    }

    public void reset() {
        this.gsStack.removeAllElements();
        this.gsStack.add(new GraphicsState());
        this.textMatrix = null;
        this.textLineMatrix = null;
        this.resources = new ResourceDictionary();
    }

    public GraphicsState gs() {
        return this.gsStack.peek();
    }

    private void invokeOperator(PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
        ContentOperator op = this.operators.get(operator.toString());
        if (op == null) {
            op = this.operators.get("DefaultOperator");
        }
        op.invoke(this, operator, operands);
    }

    private void beginMarkedContent(PdfName tag, PdfDictionary dict) {
        this.markedContentStack.push(new MarkedContentInfo(tag, dict));
    }

    private void endMarkedContent() {
        this.markedContentStack.pop();
    }

    private void beginText() {
        this.renderListener.beginTextBlock();
    }

    private void endText() {
        this.renderListener.endTextBlock();
    }

    private void displayPdfString(PdfString string) {
        TextRenderInfo renderInfo = new TextRenderInfo(string, gs(), this.textMatrix, this.markedContentStack);

        this.renderListener.renderText(renderInfo);

        this.textMatrix = (new Matrix(renderInfo.getUnscaledWidth(), 0.0F)).multiply(this.textMatrix);
    }

    private void displayXObject(PdfName xobjectName) throws IOException {
        PdfDictionary xobjects = this.resources.getAsDict(PdfName.XOBJECT);
        PdfObject xobject = xobjects.getDirectObject(xobjectName);
        PdfStream xobjectStream = (PdfStream) xobject;

        PdfName subType = xobjectStream.getAsName(PdfName.SUBTYPE);
        if (xobject.isStream()) {
            XObjectDoHandler handler = this.xobjectDoHandlers.get(subType);
            if (handler == null) {
                handler = this.xobjectDoHandlers.get(PdfName.DEFAULT);
            }
            handler.handleXObject(this, xobjectStream, xobjects.getAsIndirectObject(xobjectName));
        } else {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("XObject.1.is.not.a.stream", new Object[]{xobjectName}));
        }
    }

    private void paintPath(int operation, int rule, boolean close) {
        if (close) {
            modifyPath(6, null);
        }

        PathPaintingRenderInfo renderInfo = new PathPaintingRenderInfo(operation, rule, gs());
        ((ExtRenderListener) this.renderListener).renderPath(renderInfo);
    }

    private void modifyPath(int operation, List<Float> segmentData) {
        PathConstructionRenderInfo renderInfo = new PathConstructionRenderInfo(operation, segmentData, gs().getCtm());
        ((ExtRenderListener) this.renderListener).modifyPath(renderInfo);
    }

    private void clipPath(int rule) {
        ((ExtRenderListener) this.renderListener).clipPath(rule);
    }

    private void applyTextAdjust(float tj) {
        float adjustBy = -tj / 1000.0F * (gs()).fontSize * (gs()).horizontalScaling;

        this.textMatrix = (new Matrix(adjustBy, 0.0F)).multiply(this.textMatrix);
    }

    public void processContent(byte[] contentBytes, PdfDictionary resources) {
        this.resources.push(resources);
        try {
            PRTokeniser tokeniser = new PRTokeniser(new RandomAccessFileOrArray((new RandomAccessSourceFactory()).createSource(contentBytes)));
            PdfContentParser ps = new PdfContentParser(tokeniser);
            ArrayList<PdfObject> operands = new ArrayList<PdfObject>();
            while (ps.parse(operands).size() > 0) {
                PdfLiteral operator = (PdfLiteral) operands.get(operands.size() - 1);
                if ("BI".equals(operator.toString())) {

                    PdfDictionary colorSpaceDic = (resources != null) ? resources.getAsDict(PdfName.COLORSPACE) : null;
                    handleInlineImage(InlineImageUtils.parseInlineImage(ps, colorSpaceDic), colorSpaceDic);
                    continue;
                }
                invokeOperator(operator, operands);

            }

        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
        this.resources.pop();
    }

    protected void handleInlineImage(InlineImageInfo info, PdfDictionary colorSpaceDic) {
        ImageRenderInfo renderInfo = ImageRenderInfo.createForEmbeddedImage(gs(), info, colorSpaceDic);
        this.renderListener.renderImage(renderInfo);
    }

    public RenderListener getRenderListener() {
        return this.renderListener;
    }

    private static class ResourceDictionary
            extends PdfDictionary {

        private final List<PdfDictionary> resourcesStack = new ArrayList<PdfDictionary>();

        public void push(PdfDictionary resources) {
            this.resourcesStack.add(resources);
        }

        public void pop() {
            this.resourcesStack.remove(this.resourcesStack.size() - 1);
        }

        public PdfObject getDirectObject(PdfName key) {
            for (int i = this.resourcesStack.size() - 1; i >= 0; i--) {
                PdfDictionary subResource = this.resourcesStack.get(i);
                if (subResource != null) {
                    PdfObject obj = subResource.getDirectObject(key);
                    if (obj != null) {
                        return obj;
                    }
                }
            }
            return super.getDirectObject(key);
        }
    }

    private static class IgnoreOperatorContentOperator
            implements ContentOperator {

        private IgnoreOperatorContentOperator() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
        }
    }

    private static class ShowTextArray
            implements ContentOperator {

        private ShowTextArray() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            PdfArray array = (PdfArray) operands.get(0);
            float tj = 0.0F;
            for (Iterator<PdfObject> i = array.listIterator(); i.hasNext();) {
                PdfObject entryObj = i.next();
                if (entryObj instanceof PdfString) {
                    processor.displayPdfString((PdfString) entryObj);
                    tj = 0.0F;
                    continue;
                }
                tj = ((PdfNumber) entryObj).floatValue();
                processor.applyTextAdjust(tj);
            }
        }
    }

    private static class MoveNextLineAndShowTextWithSpacing
            implements ContentOperator {

        private final PdfContentStreamProcessor.SetTextWordSpacing setTextWordSpacing;

        private final PdfContentStreamProcessor.SetTextCharacterSpacing setTextCharacterSpacing;

        private final PdfContentStreamProcessor.MoveNextLineAndShowText moveNextLineAndShowText;

        public MoveNextLineAndShowTextWithSpacing(PdfContentStreamProcessor.SetTextWordSpacing setTextWordSpacing, PdfContentStreamProcessor.SetTextCharacterSpacing setTextCharacterSpacing, PdfContentStreamProcessor.MoveNextLineAndShowText moveNextLineAndShowText) {
            this.setTextWordSpacing = setTextWordSpacing;
            this.setTextCharacterSpacing = setTextCharacterSpacing;
            this.moveNextLineAndShowText = moveNextLineAndShowText;
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            PdfNumber aw = (PdfNumber) operands.get(0);
            PdfNumber ac = (PdfNumber) operands.get(1);
            PdfString string = (PdfString) operands.get(2);

            ArrayList<PdfObject> twOperands = new ArrayList<PdfObject>(1);
            twOperands.add(0, aw);
            this.setTextWordSpacing.invoke(processor, null, twOperands);

            ArrayList<PdfObject> tcOperands = new ArrayList<PdfObject>(1);
            tcOperands.add(0, ac);
            this.setTextCharacterSpacing.invoke(processor, null, tcOperands);

            ArrayList<PdfObject> tickOperands = new ArrayList<PdfObject>(1);
            tickOperands.add(0, string);
            this.moveNextLineAndShowText.invoke(processor, null, tickOperands);
        }
    }

    private static class MoveNextLineAndShowText
            implements ContentOperator {

        private final PdfContentStreamProcessor.TextMoveNextLine textMoveNextLine;
        private final PdfContentStreamProcessor.ShowText showText;

        public MoveNextLineAndShowText(PdfContentStreamProcessor.TextMoveNextLine textMoveNextLine, PdfContentStreamProcessor.ShowText showText) {
            this.textMoveNextLine = textMoveNextLine;
            this.showText = showText;
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            this.textMoveNextLine.invoke(processor, null, new ArrayList<PdfObject>(0));
            this.showText.invoke(processor, null, operands);
        }
    }

    private static class ShowText
            implements ContentOperator {

        private ShowText() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            PdfString string = (PdfString) operands.get(0);

            processor.displayPdfString(string);
        }
    }

    private static class TextMoveNextLine
            implements ContentOperator {

        private final PdfContentStreamProcessor.TextMoveStartNextLine moveStartNextLine;

        public TextMoveNextLine(PdfContentStreamProcessor.TextMoveStartNextLine moveStartNextLine) {
            this.moveStartNextLine = moveStartNextLine;
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            ArrayList<PdfObject> tdoperands = new ArrayList<PdfObject>(2);
            tdoperands.add(0, new PdfNumber(0));
            tdoperands.add(1, new PdfNumber(-(processor.gs()).leading));
            this.moveStartNextLine.invoke(processor, null, tdoperands);
        }
    }

    private static class TextSetTextMatrix
            implements ContentOperator {

        private TextSetTextMatrix() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            float a = ((PdfNumber) operands.get(0)).floatValue();
            float b = ((PdfNumber) operands.get(1)).floatValue();
            float c = ((PdfNumber) operands.get(2)).floatValue();
            float d = ((PdfNumber) operands.get(3)).floatValue();
            float e = ((PdfNumber) operands.get(4)).floatValue();
            float f = ((PdfNumber) operands.get(5)).floatValue();

            processor.textLineMatrix = new Matrix(a, b, c, d, e, f);
            processor.textMatrix = processor.textLineMatrix;
        }
    }

    private static class TextMoveStartNextLineWithLeading
            implements ContentOperator {

        private final PdfContentStreamProcessor.TextMoveStartNextLine moveStartNextLine;
        private final PdfContentStreamProcessor.SetTextLeading setTextLeading;

        public TextMoveStartNextLineWithLeading(PdfContentStreamProcessor.TextMoveStartNextLine moveStartNextLine, PdfContentStreamProcessor.SetTextLeading setTextLeading) {
            this.moveStartNextLine = moveStartNextLine;
            this.setTextLeading = setTextLeading;
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            float ty = ((PdfNumber) operands.get(1)).floatValue();

            ArrayList<PdfObject> tlOperands = new ArrayList<PdfObject>(1);
            tlOperands.add(0, new PdfNumber(-ty));
            this.setTextLeading.invoke(processor, null, tlOperands);
            this.moveStartNextLine.invoke(processor, null, operands);
        }
    }

    private static class TextMoveStartNextLine
            implements ContentOperator {

        private TextMoveStartNextLine() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            float tx = ((PdfNumber) operands.get(0)).floatValue();
            float ty = ((PdfNumber) operands.get(1)).floatValue();

            Matrix translationMatrix = new Matrix(tx, ty);
            processor.textMatrix = translationMatrix.multiply(processor.textLineMatrix);
            processor.textLineMatrix = processor.textMatrix;
        }
    }

    private static class SetTextFont implements ContentOperator {

        private SetTextFont() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            CMapAwareDocumentFont font;
            PdfName fontResourceName = (PdfName) operands.get(0);
            float size = ((PdfNumber) operands.get(1)).floatValue();

            PdfDictionary fontsDictionary = processor.resources.getAsDict(PdfName.FONT);

            PdfObject fontObject = fontsDictionary.get(fontResourceName);
            if (fontObject instanceof PdfDictionary) {
                font = processor.getFont((PdfDictionary) fontObject);
            } else {
                font = processor.getFont((PRIndirectReference) fontObject);
            }
            (processor.gs()).font = font;
            (processor.gs()).fontSize = size;
        }
    }

    private static class SetTextRenderMode
            implements ContentOperator {

        private SetTextRenderMode() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            PdfNumber render = (PdfNumber) operands.get(0);
            (processor.gs()).renderMode = render.intValue();
        }
    }

    private static class SetTextRise
            implements ContentOperator {

        private SetTextRise() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            PdfNumber rise = (PdfNumber) operands.get(0);
            (processor.gs()).rise = rise.floatValue();
        }
    }

    private static class SetTextLeading
            implements ContentOperator {

        private SetTextLeading() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            PdfNumber leading = (PdfNumber) operands.get(0);
            (processor.gs()).leading = leading.floatValue();
        }
    }

    private static class SetTextHorizontalScaling
            implements ContentOperator {

        private SetTextHorizontalScaling() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            PdfNumber scale = (PdfNumber) operands.get(0);
            (processor.gs()).horizontalScaling = scale.floatValue() / 100.0F;
        }
    }

    private static class SetTextCharacterSpacing
            implements ContentOperator {

        private SetTextCharacterSpacing() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            PdfNumber charSpace = (PdfNumber) operands.get(0);
            (processor.gs()).characterSpacing = charSpace.floatValue();
        }
    }

    private static class SetTextWordSpacing
            implements ContentOperator {

        private SetTextWordSpacing() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            PdfNumber wordSpace = (PdfNumber) operands.get(0);
            (processor.gs()).wordSpacing = wordSpace.floatValue();
        }
    }

    private static class ProcessGraphicsStateResource
            implements ContentOperator {

        private ProcessGraphicsStateResource() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            PdfName dictionaryName = (PdfName) operands.get(0);
            PdfDictionary extGState = processor.resources.getAsDict(PdfName.EXTGSTATE);
            if (extGState == null) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("resources.do.not.contain.extgstate.entry.unable.to.process.operator.1", new Object[]{operator}));
            }
            PdfDictionary gsDic = extGState.getAsDict(dictionaryName);
            if (gsDic == null) {
                throw new IllegalArgumentException(MessageLocalization.getComposedMessage("1.is.an.unknown.graphics.state.dictionary", new Object[]{dictionaryName}));
            }

            PdfArray fontParameter = gsDic.getAsArray(PdfName.FONT);
            if (fontParameter != null) {
                CMapAwareDocumentFont font = processor.getFont((PRIndirectReference) fontParameter.getPdfObject(0));
                float size = fontParameter.getAsNumber(1).floatValue();

                (processor.gs()).font = font;
                (processor.gs()).fontSize = size;
            }
        }
    }

    private static class PushGraphicsState
            implements ContentOperator {

        private PushGraphicsState() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            GraphicsState gs = processor.gsStack.peek();
            GraphicsState copy = new GraphicsState(gs);
            processor.gsStack.push(copy);
        }
    }

    private static class ModifyCurrentTransformationMatrix
            implements ContentOperator {

        private ModifyCurrentTransformationMatrix() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            float a = ((PdfNumber) operands.get(0)).floatValue();
            float b = ((PdfNumber) operands.get(1)).floatValue();
            float c = ((PdfNumber) operands.get(2)).floatValue();
            float d = ((PdfNumber) operands.get(3)).floatValue();
            float e = ((PdfNumber) operands.get(4)).floatValue();
            float f = ((PdfNumber) operands.get(5)).floatValue();
            Matrix matrix = new Matrix(a, b, c, d, e, f);
            GraphicsState gs = processor.gsStack.peek();
            gs.ctm = matrix.multiply(gs.ctm);
        }
    }

    private static BaseColor getColor(PdfName colorSpace, List<PdfObject> operands) {
        if (PdfName.DEVICEGRAY.equals(colorSpace)) {
            return getColor(1, operands);
        }
        if (PdfName.DEVICERGB.equals(colorSpace)) {
            return getColor(3, operands);
        }
        if (PdfName.DEVICECMYK.equals(colorSpace)) {
            return getColor(4, operands);
        }
        return null;
    }

    private static BaseColor getColor(int nOperands, List<PdfObject> operands) {
        float[] c = new float[nOperands];
        for (int i = 0; i < nOperands; i++) {
            c[i] = ((PdfNumber) operands.get(i)).floatValue();

            if (c[i] > 1.0F) {
                c[i] = 1.0F;
            } else if (c[i] < 0.0F) {
                c[i] = 0.0F;
            }
        }
        switch (nOperands) {
            case 1:
                return (BaseColor) new GrayColor(c[0]);
            case 3:
                return new BaseColor(c[0], c[1], c[2]);
            case 4:
                return (BaseColor) new CMYKColor(c[0], c[1], c[2], c[3]);
        }
        return null;
    }

    private static class SetGrayFill
            implements ContentOperator {

        private SetGrayFill() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            (processor.gs()).fillColor = PdfContentStreamProcessor.getColor(1, operands);
        }
    }

    private static class SetGrayStroke
            implements ContentOperator {

        private SetGrayStroke() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            (processor.gs()).strokeColor = PdfContentStreamProcessor.getColor(1, operands);
        }
    }

    private static class SetRGBFill
            implements ContentOperator {

        private SetRGBFill() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            (processor.gs()).fillColor = PdfContentStreamProcessor.getColor(3, operands);
        }
    }

    private static class SetRGBStroke
            implements ContentOperator {

        private SetRGBStroke() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            (processor.gs()).strokeColor = PdfContentStreamProcessor.getColor(3, operands);
        }
    }

    private static class SetCMYKFill
            implements ContentOperator {

        private SetCMYKFill() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            (processor.gs()).fillColor = PdfContentStreamProcessor.getColor(4, operands);
        }
    }

    private static class SetCMYKStroke
            implements ContentOperator {

        private SetCMYKStroke() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            (processor.gs()).strokeColor = PdfContentStreamProcessor.getColor(4, operands);
        }
    }

    private static class SetColorSpaceFill
            implements ContentOperator {

        private SetColorSpaceFill() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            (processor.gs()).colorSpaceFill = (PdfName) operands.get(0);
        }
    }

    private static class SetColorSpaceStroke
            implements ContentOperator {

        private SetColorSpaceStroke() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            (processor.gs()).colorSpaceStroke = (PdfName) operands.get(0);
        }
    }

    private static class SetColorFill
            implements ContentOperator {

        private SetColorFill() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            (processor.gs()).fillColor = PdfContentStreamProcessor.getColor((processor.gs()).colorSpaceFill, operands);
        }
    }

    private static class SetColorStroke
            implements ContentOperator {

        private SetColorStroke() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            (processor.gs()).strokeColor = PdfContentStreamProcessor.getColor((processor.gs()).colorSpaceStroke, operands);
        }
    }

    private static class PopGraphicsState
            implements ContentOperator {

        private PopGraphicsState() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            processor.gsStack.pop();
        }
    }

    private static class BeginText
            implements ContentOperator {

        private BeginText() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            processor.textMatrix = new Matrix();
            processor.textLineMatrix = processor.textMatrix;
            processor.beginText();
        }
    }

    private static class EndText
            implements ContentOperator {

        private EndText() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) {
            processor.textMatrix = null;
            processor.textLineMatrix = null;
            processor.endText();
        }
    }

    private static class BeginMarkedContent
            implements ContentOperator {

        private BeginMarkedContent() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            processor.beginMarkedContent((PdfName) operands.get(0), new PdfDictionary());
        }
    }

    private static class BeginMarkedContentDictionary
            implements ContentOperator {

        private BeginMarkedContentDictionary() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            PdfObject properties = operands.get(1);

            processor.beginMarkedContent((PdfName) operands.get(0), getPropertiesDictionary(properties, processor.resources));
        }

        private PdfDictionary getPropertiesDictionary(PdfObject operand1, PdfContentStreamProcessor.ResourceDictionary resources) {
            if (operand1.isDictionary()) {
                return (PdfDictionary) operand1;
            }
            PdfName dictionaryName = (PdfName) operand1;
            return resources.getAsDict(dictionaryName);
        }
    }

    private static class EndMarkedContent
            implements ContentOperator {

        private EndMarkedContent() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            processor.endMarkedContent();
        }
    }

    private static class Do
            implements ContentOperator {

        private Do() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws IOException {
            PdfName xobjectName = (PdfName) operands.get(0);
            processor.displayXObject(xobjectName);
        }
    }

    private static class SetLineWidth
            implements ContentOperator {

        private SetLineWidth() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral oper, ArrayList<PdfObject> operands) {
            float lineWidth = ((PdfNumber) operands.get(0)).floatValue();
            processor.gs().setLineWidth(lineWidth);
        }
    }

    private class SetLineCap
            implements ContentOperator {

        private SetLineCap() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral oper, ArrayList<PdfObject> operands) {
            int lineCap = ((PdfNumber) operands.get(0)).intValue();
            processor.gs().setLineCapStyle(lineCap);
        }
    }

    private class SetLineJoin
            implements ContentOperator {

        private SetLineJoin() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral oper, ArrayList<PdfObject> operands) {
            int lineJoin = ((PdfNumber) operands.get(0)).intValue();
            processor.gs().setLineJoinStyle(lineJoin);
        }
    }

    private class SetMiterLimit
            implements ContentOperator {

        private SetMiterLimit() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral oper, ArrayList<PdfObject> operands) {
            float miterLimit = ((PdfNumber) operands.get(0)).floatValue();
            processor.gs().setMiterLimit(miterLimit);
        }
    }

    private class SetLineDashPattern
            implements ContentOperator {

        private SetLineDashPattern() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral oper, ArrayList<PdfObject> operands) {
            LineDashPattern pattern = new LineDashPattern((PdfArray) operands.get(0), ((PdfNumber) operands.get(1)).floatValue());
            processor.gs().setLineDashPattern(pattern);
        }
    }

    private static class MoveTo
            implements ContentOperator {

        private MoveTo() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            float x = ((PdfNumber) operands.get(0)).floatValue();
            float y = ((PdfNumber) operands.get(1)).floatValue();
            processor.modifyPath(1, Arrays.asList(new Float[]{Float.valueOf(x), Float.valueOf(y)}));
        }
    }

    private static class LineTo
            implements ContentOperator {

        private LineTo() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            float x = ((PdfNumber) operands.get(0)).floatValue();
            float y = ((PdfNumber) operands.get(1)).floatValue();
            processor.modifyPath(2, Arrays.asList(new Float[]{Float.valueOf(x), Float.valueOf(y)}));
        }
    }

    private static class Curve
            implements ContentOperator {

        private Curve() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            float x1 = ((PdfNumber) operands.get(0)).floatValue();
            float y1 = ((PdfNumber) operands.get(1)).floatValue();
            float x2 = ((PdfNumber) operands.get(2)).floatValue();
            float y2 = ((PdfNumber) operands.get(3)).floatValue();
            float x3 = ((PdfNumber) operands.get(4)).floatValue();
            float y3 = ((PdfNumber) operands.get(5)).floatValue();
            processor.modifyPath(3, Arrays.asList(new Float[]{Float.valueOf(x1), Float.valueOf(y1), Float.valueOf(x2), Float.valueOf(y2), Float.valueOf(x3), Float.valueOf(y3)}));
        }
    }

    private static class CurveFirstPointDuplicated
            implements ContentOperator {

        private CurveFirstPointDuplicated() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            float x2 = ((PdfNumber) operands.get(0)).floatValue();
            float y2 = ((PdfNumber) operands.get(1)).floatValue();
            float x3 = ((PdfNumber) operands.get(2)).floatValue();
            float y3 = ((PdfNumber) operands.get(3)).floatValue();
            processor.modifyPath(4, Arrays.asList(new Float[]{Float.valueOf(x2), Float.valueOf(y2), Float.valueOf(x3), Float.valueOf(y3)}));
        }
    }

    private static class CurveFourhPointDuplicated
            implements ContentOperator {

        private CurveFourhPointDuplicated() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            float x1 = ((PdfNumber) operands.get(0)).floatValue();
            float y1 = ((PdfNumber) operands.get(1)).floatValue();
            float x3 = ((PdfNumber) operands.get(2)).floatValue();
            float y3 = ((PdfNumber) operands.get(3)).floatValue();
            processor.modifyPath(5, Arrays.asList(new Float[]{Float.valueOf(x1), Float.valueOf(y1), Float.valueOf(x3), Float.valueOf(y3)}));
        }
    }

    private static class CloseSubpath
            implements ContentOperator {

        private CloseSubpath() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            processor.modifyPath(6, null);
        }
    }

    private static class Rectangle
            implements ContentOperator {

        private Rectangle() {
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            float x = ((PdfNumber) operands.get(0)).floatValue();
            float y = ((PdfNumber) operands.get(1)).floatValue();
            float w = ((PdfNumber) operands.get(2)).floatValue();
            float h = ((PdfNumber) operands.get(3)).floatValue();
            processor.modifyPath(7, Arrays.asList(new Float[]{Float.valueOf(x), Float.valueOf(y), Float.valueOf(w), Float.valueOf(h)}));
        }
    }

    private static class PaintPath
            implements ContentOperator {

        private int operation;

        private int rule;

        private boolean close;

        public PaintPath(int operation, int rule, boolean close) {
            this.operation = operation;
            this.rule = rule;
            this.close = close;
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            processor.paintPath(this.operation, this.rule, this.close);
        }
    }

    private static class ClipPath
            implements ContentOperator {

        private int rule;

        public ClipPath(int rule) {
            this.rule = rule;
        }

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            processor.clipPath(this.rule);
        }
    }

    private static class EndPath
            implements ContentOperator {

        public void invoke(PdfContentStreamProcessor processor, PdfLiteral operator, ArrayList<PdfObject> operands) throws Exception {
            processor.paintPath(0, -1, false);
        }
    }

    private static class FormXObjectDoHandler
            implements XObjectDoHandler {

        private FormXObjectDoHandler() {
        }

        public void handleXObject(PdfContentStreamProcessor processor, PdfStream stream, PdfIndirectReference ref) {
            byte[] contentBytes;
            PdfDictionary resources = stream.getAsDict(PdfName.RESOURCES);

            try {
                contentBytes = ContentByteUtils.getContentBytesFromContentObject((PdfObject) stream);
            } catch (IOException e1) {
                throw new ExceptionConverter(e1);
            }
            PdfArray matrix = stream.getAsArray(PdfName.MATRIX);

            (new PdfContentStreamProcessor.PushGraphicsState()).invoke(processor, null, null);

            if (matrix != null) {
                float a = matrix.getAsNumber(0).floatValue();
                float b = matrix.getAsNumber(1).floatValue();
                float c = matrix.getAsNumber(2).floatValue();
                float d = matrix.getAsNumber(3).floatValue();
                float e = matrix.getAsNumber(4).floatValue();
                float f = matrix.getAsNumber(5).floatValue();
                Matrix formMatrix = new Matrix(a, b, c, d, e, f);

                (processor.gs()).ctm = formMatrix.multiply((processor.gs()).ctm);
            }

            processor.processContent(contentBytes, resources);

            (new PdfContentStreamProcessor.PopGraphicsState()).invoke(processor, null, null);
        }
    }

    private static class ImageXObjectDoHandler
            implements XObjectDoHandler {

        private ImageXObjectDoHandler() {
        }

        public void handleXObject(PdfContentStreamProcessor processor, PdfStream xobjectStream, PdfIndirectReference ref) {
            PdfDictionary colorSpaceDic = processor.resources.getAsDict(PdfName.COLORSPACE);
            ImageRenderInfo renderInfo = ImageRenderInfo.createForXObject(processor.gs(), ref, colorSpaceDic);
            processor.renderListener.renderImage(renderInfo);
        }
    }

    private static class IgnoreXObjectDoHandler implements XObjectDoHandler {

        private IgnoreXObjectDoHandler() {
        }

        public void handleXObject(PdfContentStreamProcessor processor, PdfStream xobjectStream, PdfIndirectReference ref) {
        }
    }
}
