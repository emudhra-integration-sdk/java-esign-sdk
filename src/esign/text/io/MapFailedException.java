package esign.text.io;

import java.io.IOException;

public class MapFailedException
        extends IOException {

    public MapFailedException(IOException e) {
        super(e.getMessage());
        initCause(e);
    }
}
