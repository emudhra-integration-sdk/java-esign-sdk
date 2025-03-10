package esign.text.pdf.languages;

public interface LanguageProcessor {
  String process(String paramString);
  
  boolean isRTL();
}

