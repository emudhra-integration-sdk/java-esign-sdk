package esign.text.pdf.parser;

import esign.text.pdf.PdfReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class PdfTextExtractor {

    public static String getTextFromPage(PdfReader reader, int pageNumber, TextExtractionStrategy strategy, Map<String, ContentOperator> additionalContentOperators) throws IOException {
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        return ((TextExtractionStrategy) parser.<TextExtractionStrategy>processContent(pageNumber, strategy, additionalContentOperators)).getResultantText();
    }

    public static String getTextFromPage(PdfReader reader, int pageNumber, TextExtractionStrategy strategy) throws IOException {
        return getTextFromPage(reader, pageNumber, strategy, new HashMap<String, ContentOperator>());
    }

    public static String getTextFromPage(PdfReader reader, int pageNumber) throws IOException {
        return getTextFromPage(reader, pageNumber, new LocationTextExtractionStrategy());
    }
}
