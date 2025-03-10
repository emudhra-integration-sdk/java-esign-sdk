package esign.text.xml.simpleparser.handler;

import esign.text.xml.simpleparser.NewLineHandler;
import java.util.HashSet;
import java.util.Set;

public class HTMLNewLineHandler
        implements NewLineHandler {

    private final Set<String> newLineTags = new HashSet<String>();

    public HTMLNewLineHandler() {
        this.newLineTags.add("p");
        this.newLineTags.add("blockquote");
        this.newLineTags.add("br");
    }

    public boolean isNewLineTag(String tag) {
        return this.newLineTags.contains(tag);
    }
}
