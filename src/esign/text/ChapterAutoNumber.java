package esign.text;

import esign.text.error_messages.MessageLocalization;

public class ChapterAutoNumber
        extends Chapter {

    private static final long serialVersionUID = -9217457637987854167L;
    protected boolean numberSet = false;

    public ChapterAutoNumber(Paragraph para) {
        super(para, 0);
    }

    public ChapterAutoNumber(String title) {
        super(title, 0);
    }

    public Section addSection(String title) {
        if (isAddedCompletely()) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("this.largeelement.has.already.been.added.to.the.document", new Object[0]));
        }
        return addSection(title, 2);
    }

    public Section addSection(Paragraph title) {
        if (isAddedCompletely()) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("this.largeelement.has.already.been.added.to.the.document", new Object[0]));
        }
        return addSection(title, 2);
    }

    public int setAutomaticNumber(int number) {
        if (!this.numberSet) {
            number++;
            setChapterNumber(number);
            this.numberSet = true;
        }
        return number;
    }
}
