package esign.text;

import java.util.ArrayList;
import java.util.List;

public class Meta
        implements Element {

    private final int type;
    private final StringBuffer content;
    public static final String UNKNOWN = "unknown";
    public static final String PRODUCER = "producer";
    public static final String CREATIONDATE = "creationdate";
    public static final String AUTHOR = "author";
    public static final String KEYWORDS = "keywords";
    public static final String SUBJECT = "subject";
    public static final String TITLE = "title";

    Meta(int type, String content) {
        this.type = type;
        this.content = new StringBuffer(content);
    }

    public Meta(String tag, String content) {
        this.type = getType(tag);
        this.content = new StringBuffer(content);
    }

    public boolean process(ElementListener listener) {
        try {
            return listener.add(this);
        } catch (DocumentException de) {
            return false;
        }
    }

    public int type() {
        return this.type;
    }

    public List<Chunk> getChunks() {
        return new ArrayList<Chunk>();
    }

    public boolean isContent() {
        return false;
    }

    public boolean isNestable() {
        return false;
    }

    public StringBuffer append(String string) {
        return this.content.append(string);
    }

    public String getContent() {
        return this.content.toString();
    }

    public String getName() {
        switch (this.type) {
            case 2:
                return "subject";
            case 3:
                return "keywords";
            case 4:
                return "author";
            case 1:
                return "title";
            case 5:
                return "producer";
            case 6:
                return "creationdate";
        }
        return "unknown";
    }

    public static int getType(String tag) {
        if ("subject".equals(tag)) {
            return 2;
        }
        if ("keywords".equals(tag)) {
            return 3;
        }
        if ("author".equals(tag)) {
            return 4;
        }
        if ("title".equals(tag)) {
            return 1;
        }
        if ("producer".equals(tag)) {
            return 5;
        }
        if ("creationdate".equals(tag)) {
            return 6;
        }
        return 0;
    }
}
