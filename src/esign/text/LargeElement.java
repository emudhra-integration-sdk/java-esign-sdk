package esign.text;

public interface LargeElement extends Element {

    void setComplete(boolean paramBoolean);

    boolean isComplete();

    void flushContent();
}
