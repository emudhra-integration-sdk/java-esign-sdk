package esign.text.pdf;

public interface HyphenationEvent {

    String getHyphenSymbol();

    String getHyphenatedWordPre(String paramString, BaseFont paramBaseFont, float paramFloat1, float paramFloat2);

    String getHyphenatedWordPost();
}
