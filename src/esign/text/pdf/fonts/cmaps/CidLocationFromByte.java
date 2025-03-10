package esign.text.pdf.fonts.cmaps;

import esign.text.io.RandomAccessSourceFactory;
import esign.text.pdf.PRTokeniser;
import esign.text.pdf.RandomAccessFileOrArray;
import java.io.IOException;

public class CidLocationFromByte
        implements CidLocation {

    private byte[] data;

    public CidLocationFromByte(byte[] data) {
        this.data = data;
    }

    public PRTokeniser getLocation(String location) throws IOException {
        return new PRTokeniser(new RandomAccessFileOrArray((new RandomAccessSourceFactory()).createSource(this.data)));
    }
}
