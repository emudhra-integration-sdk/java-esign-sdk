package esign.text;

import java.util.ArrayList;

public class Chapter
        extends Section {

    private static final long serialVersionUID = 1791000695779357361L;

    public Chapter(int number) {
        super(null, 1);
        this.numbers = new ArrayList<Integer>();
        this.numbers.add(Integer.valueOf(number));
        this.triggerNewPage = true;
    }

    public Chapter(Paragraph title, int number) {
        super(title, 1);
        this.numbers = new ArrayList<Integer>();
        this.numbers.add(Integer.valueOf(number));
        this.triggerNewPage = true;
    }

    public Chapter(String title, int number) {
        this(new Paragraph(title), number);
    }

    public int type() {
        return 16;
    }

    public boolean isNestable() {
        return false;
    }
}
