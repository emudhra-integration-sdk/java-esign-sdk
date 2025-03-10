package esign.text.pdf.parser;

import esign.text.pdf.PdfDictionary;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PdfReaderContentParser {

    private final PdfReader reader;

    public PdfReaderContentParser(PdfReader reader) {
        this.reader = reader;
    }

    public <E extends RenderListener> E processContent(int pageNumber, E renderListener, Map<String, ContentOperator> additionalContentOperators) throws IOException {
        PdfDictionary pageDic = this.reader.getPageN(pageNumber);
        PdfDictionary resourcesDic = pageDic.getAsDict(PdfName.RESOURCES);

        PdfContentStreamProcessor processor = new PdfContentStreamProcessor((RenderListener) renderListener);
        for (Map.Entry<String, ContentOperator> entry : additionalContentOperators.entrySet()) {
            processor.registerContentOperator(entry.getKey(), entry.getValue());
        }
        processor.processContent(ContentByteUtils.getContentBytesForPage(this.reader, pageNumber), resourcesDic);
        return renderListener;
    }

    public <E extends RenderListener> E processContent(int pageNumber, E renderListener) throws IOException {
        return processContent(pageNumber, renderListener, new HashMap<String, ContentOperator>());
    }
}
