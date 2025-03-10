package esign.text.pdf.parser;

import esign.text.pdf.PdfDictionary;

public class InlineImageInfo {

    private final byte[] samples;
    private final PdfDictionary imageDictionary;

    public InlineImageInfo(byte[] samples, PdfDictionary imageDictionary) {
        this.samples = samples;
        this.imageDictionary = imageDictionary;
    }

    public PdfDictionary getImageDictionary() {
        return this.imageDictionary;
    }

    public byte[] getSamples() {
        return this.samples;
    }
}
