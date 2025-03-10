package esign.text.xml.simpleparser;

import java.util.Map;

public interface SimpleXMLDocHandler {
  void startElement(String paramString, Map<String, String> paramMap);
  
  void endElement(String paramString);
  
  void startDocument();
  
  void endDocument();
  
  void text(String paramString);
}


