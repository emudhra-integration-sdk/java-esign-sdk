package esign.text.html.simpleparser;

import esign.text.DocumentException;
import java.io.IOException;
import java.util.Map;

@Deprecated
public interface HTMLTagProcessor {

    void startElement(HTMLWorker paramHTMLWorker, String paramString, Map<String, String> paramMap) throws DocumentException, IOException;

    void endElement(HTMLWorker paramHTMLWorker, String paramString) throws DocumentException;
}
