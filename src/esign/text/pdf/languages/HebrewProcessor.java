package esign.text.pdf.languages;

import esign.text.pdf.BidiLine;

public class HebrewProcessor
        implements LanguageProcessor {

    protected int runDirection = 3;

    public HebrewProcessor() {
    }

    public HebrewProcessor(int runDirection) {
        this.runDirection = runDirection;
    }

    public String process(String s) {
        return BidiLine.processLTR(s, this.runDirection, 0);
    }

    public boolean isRTL() {
        return true;
    }
}
