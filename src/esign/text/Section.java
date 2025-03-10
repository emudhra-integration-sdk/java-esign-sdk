package esign.text;

import esign.text.api.Indentable;
import esign.text.error_messages.MessageLocalization;
import esign.text.pdf.PdfName;
import esign.text.pdf.PdfObject;
import esign.text.pdf.interfaces.IAccessibleElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class Section
        extends ArrayList<Element>
        implements TextElementArray, LargeElement, Indentable, IAccessibleElement {

    public static final int NUMBERSTYLE_DOTTED = 0;
    public static final int NUMBERSTYLE_DOTTED_WITHOUT_FINAL_DOT = 1;
    private static final long serialVersionUID = 3324172577544748043L;
    protected Paragraph title;
    protected String bookmarkTitle;
    protected int numberDepth;
    protected int numberStyle = 0;

    protected float indentationLeft;

    protected float indentationRight;

    protected float indentation;

    protected boolean bookmarkOpen = true;

    protected boolean triggerNewPage = false;

    protected int subsections = 0;

    protected ArrayList<Integer> numbers = null;

    protected boolean complete = true;

    protected boolean addedCompletely = false;

    protected boolean notAddedYet = true;

    protected Section() {
        this.title = new Paragraph();
        this.numberDepth = 1;
        this.title.setRole(new PdfName("H" + this.numberDepth));
    }

    protected Section(Paragraph title, int numberDepth) {
        this.numberDepth = numberDepth;
        this.title = title;
        if (title != null) {
            title.setRole(new PdfName("H" + numberDepth));
        }
    }

    public boolean process(ElementListener listener) {
        try {
            for (Object element2 : this) {
                Element element = (Element) element2;
                listener.add(element);
            }
            return true;
        } catch (DocumentException de) {
            return false;
        }
    }

    public int type() {
        return 13;
    }

    public boolean isChapter() {
        return (type() == 16);
    }

    public boolean isSection() {
        return (type() == 13);
    }

    public List<Chunk> getChunks() {
        List<Chunk> tmp = new ArrayList<Chunk>();
        for (Object element : this) {
            tmp.addAll(((Element) element).getChunks());
        }
        return tmp;
    }

    public boolean isContent() {
        return true;
    }

    public boolean isNestable() {
        return false;
    }

    public void add(int index, Element element) {
        if (isAddedCompletely()) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("this.largeelement.has.already.been.added.to.the.document", new Object[0]));
        }
        try {
            if (element.isNestable()) {
                super.add(index, element);
            } else {

                throw new ClassCastException(MessageLocalization.getComposedMessage("you.can.t.add.a.1.to.a.section", new Object[]{element.getClass().getName()}));
            }

        } catch (ClassCastException cce) {
            throw new ClassCastException(MessageLocalization.getComposedMessage("insertion.of.illegal.element.1", new Object[]{cce.getMessage()}));
        }
    }

    public boolean add(Element element) {
        if (isAddedCompletely()) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("this.largeelement.has.already.been.added.to.the.document", new Object[0]));
        }
        try {
            if (element.type() == 13) {
                Section section = (Section) element;
                section.setNumbers(++this.subsections, this.numbers);
                return super.add(section);
            }
            if (element instanceof MarkedSection && ((MarkedObject) element).element.type() == 13) {
                MarkedSection mo = (MarkedSection) element;
                Section section = (Section) mo.element;
                section.setNumbers(++this.subsections, this.numbers);
                return super.add(mo);
            }
            if (element.isNestable()) {
                return super.add(element);
            }

            throw new ClassCastException(MessageLocalization.getComposedMessage("you.can.t.add.a.1.to.a.section", new Object[]{element.getClass().getName()}));

        } catch (ClassCastException cce) {
            throw new ClassCastException(MessageLocalization.getComposedMessage("insertion.of.illegal.element.1", new Object[]{cce.getMessage()}));
        }
    }

    public boolean addAll(Collection<? extends Element> collection) {
        if (collection.size() == 0) {
            return false;
        }
        for (Element element : collection) {
            add(element);
        }
        return true;
    }

    public Section addSection(float indentation, Paragraph title, int numberDepth) {
        if (isAddedCompletely()) {
            throw new IllegalStateException(MessageLocalization.getComposedMessage("this.largeelement.has.already.been.added.to.the.document", new Object[0]));
        }
        Section section = new Section(title, numberDepth);
        section.setIndentation(indentation);
        add(section);
        return section;
    }

    public Section addSection(float indentation, Paragraph title) {
        return addSection(indentation, title, this.numberDepth + 1);
    }

    public Section addSection(Paragraph title, int numberDepth) {
        return addSection(0.0F, title, numberDepth);
    }

    protected MarkedSection addMarkedSection() {
        MarkedSection section = new MarkedSection(new Section(null, this.numberDepth + 1));
        add(section);
        return section;
    }

    public Section addSection(Paragraph title) {
        return addSection(0.0F, title, this.numberDepth + 1);
    }

    public Section addSection(float indentation, String title, int numberDepth) {
        return addSection(indentation, new Paragraph(title), numberDepth);
    }

    public Section addSection(String title, int numberDepth) {
        return addSection(new Paragraph(title), numberDepth);
    }

    public Section addSection(float indentation, String title) {
        return addSection(indentation, new Paragraph(title));
    }

    public Section addSection(String title) {
        return addSection(new Paragraph(title));
    }

    public void setTitle(Paragraph title) {
        this.title = title;
    }

    public Paragraph getTitle() {
        return constructTitle(this.title, this.numbers, this.numberDepth, this.numberStyle);
    }

    public static Paragraph constructTitle(Paragraph title, ArrayList<Integer> numbers, int numberDepth, int numberStyle) {
        if (title == null) {
            return null;
        }

        int depth = Math.min(numbers.size(), numberDepth);
        if (depth < 1) {
            return title;
        }
        StringBuffer buf = new StringBuffer(" ");
        for (int i = 0; i < depth; i++) {
            buf.insert(0, ".");
            buf.insert(0, ((Integer) numbers.get(i)).intValue());
        }
        if (numberStyle == 1) {
            buf.deleteCharAt(buf.length() - 2);
        }
        Paragraph result = new Paragraph(title);

        result.add(0, new Chunk(buf.toString(), title.getFont()));
        return result;
    }

    public void setNumberDepth(int numberDepth) {
        this.numberDepth = numberDepth;
    }

    public int getNumberDepth() {
        return this.numberDepth;
    }

    public void setNumberStyle(int numberStyle) {
        this.numberStyle = numberStyle;
    }

    public int getNumberStyle() {
        return this.numberStyle;
    }

    public void setIndentationLeft(float indentation) {
        this.indentationLeft = indentation;
    }

    public float getIndentationLeft() {
        return this.indentationLeft;
    }

    public void setIndentationRight(float indentation) {
        this.indentationRight = indentation;
    }

    public float getIndentationRight() {
        return this.indentationRight;
    }

    public void setIndentation(float indentation) {
        this.indentation = indentation;
    }

    public float getIndentation() {
        return this.indentation;
    }

    public void setBookmarkOpen(boolean bookmarkOpen) {
        this.bookmarkOpen = bookmarkOpen;
    }

    public boolean isBookmarkOpen() {
        return this.bookmarkOpen;
    }

    public void setTriggerNewPage(boolean triggerNewPage) {
        this.triggerNewPage = triggerNewPage;
    }

    public boolean isTriggerNewPage() {
        return (this.triggerNewPage && this.notAddedYet);
    }

    public void setBookmarkTitle(String bookmarkTitle) {
        this.bookmarkTitle = bookmarkTitle;
    }

    public Paragraph getBookmarkTitle() {
        if (this.bookmarkTitle == null) {
            return getTitle();
        }
        return new Paragraph(this.bookmarkTitle);
    }

    public void setChapterNumber(int number) {
        this.numbers.set(this.numbers.size() - 1, Integer.valueOf(number));

        for (Iterator<Element> i = iterator(); i.hasNext();) {
            Object s = i.next();
            if (s instanceof Section) {
                ((Section) s).setChapterNumber(number);
            }
        }
    }

    public int getDepth() {
        return this.numbers.size();
    }

    private void setNumbers(int number, ArrayList<Integer> numbers) {
        this.numbers = new ArrayList<Integer>();
        this.numbers.add(Integer.valueOf(number));
        this.numbers.addAll(numbers);
    }

    public boolean isNotAddedYet() {
        return this.notAddedYet;
    }

    public void setNotAddedYet(boolean notAddedYet) {
        this.notAddedYet = notAddedYet;
    }

    protected boolean isAddedCompletely() {
        return this.addedCompletely;
    }

    protected void setAddedCompletely(boolean addedCompletely) {
        this.addedCompletely = addedCompletely;
    }

    public void flushContent() {
        setNotAddedYet(false);
        this.title = null;

        for (Iterator<Element> i = iterator(); i.hasNext();) {
            Element element = i.next();
            if (element instanceof Section) {
                Section s = (Section) element;
                if (!s.isComplete() && size() == 1) {
                    s.flushContent();

                    return;
                }
                s.setAddedCompletely(true);
            }

            i.remove();
        }
    }

    public boolean isComplete() {
        return this.complete;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public void newPage() {
        add(Chunk.NEXTPAGE);
    }

    public PdfObject getAccessibleAttribute(PdfName key) {
        return this.title.getAccessibleAttribute(key);
    }

    public void setAccessibleAttribute(PdfName key, PdfObject value) {
        this.title.setAccessibleAttribute(key, value);
    }

    public HashMap<PdfName, PdfObject> getAccessibleAttributes() {
        return this.title.getAccessibleAttributes();
    }

    public PdfName getRole() {
        return this.title.getRole();
    }

    public void setRole(PdfName role) {
        this.title.setRole(role);
    }

    public AccessibleElementId getId() {
        return this.title.getId();
    }

    public void setId(AccessibleElementId id) {
        this.title.setId(id);
    }

    public boolean isInline() {
        return false;
    }
}
