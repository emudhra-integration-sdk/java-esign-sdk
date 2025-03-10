package esign.text.xml.simpleparser.handler;

import esign.text.xml.simpleparser.NewLineHandler;

public class NeverNewLineHandler
        implements NewLineHandler {

    public boolean isNewLineTag(String tag) {
        return false;
    }
}
