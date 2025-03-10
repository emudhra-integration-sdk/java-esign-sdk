package esign.text.pdf.fonts.cmaps;

import esign.text.error_messages.MessageLocalization;
import esign.text.io.RandomAccessSourceFactory;
import esign.text.io.StreamUtil;
import esign.text.pdf.PRTokeniser;
import esign.text.pdf.RandomAccessFileOrArray;
import java.io.IOException;
import java.io.InputStream;

public class CidResource
        implements CidLocation {

    public PRTokeniser getLocation(String location) throws IOException {
        String fullName = "esign/text/pdf/fonts/cmaps/" + location;
        InputStream inp = StreamUtil.getResourceStream(fullName);
        if (inp == null) {
            throw new IOException(MessageLocalization.getComposedMessage("the.cmap.1.was.not.found", new Object[]{fullName}));
        }
        return new PRTokeniser(new RandomAccessFileOrArray((new RandomAccessSourceFactory()).createSource(inp)));
    }
}
