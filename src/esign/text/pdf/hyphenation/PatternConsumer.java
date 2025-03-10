package esign.text.pdf.hyphenation;

import java.util.ArrayList;

public interface PatternConsumer {
  void addClass(String paramString);
  
  void addException(String paramString, ArrayList<Object> paramArrayList);
  
  void addPattern(String paramString1, String paramString2);
}

