package esign.text.pdf.fonts.cmaps;

import esign.text.pdf.PRTokeniser;
import java.io.IOException;

public interface CidLocation {
  PRTokeniser getLocation(String paramString) throws IOException;
}


