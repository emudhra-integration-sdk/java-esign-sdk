package esign.text;

public interface DocListener extends ElementListener {

    void open();

    void close();

    boolean newPage();

    boolean setPageSize(Rectangle paramRectangle);

    boolean setMargins(float paramFloat1, float paramFloat2, float paramFloat3, float paramFloat4);

    boolean setMarginMirroring(boolean paramBoolean);

    boolean setMarginMirroringTopBottom(boolean paramBoolean);

    void setPageCount(int paramInt);

    void resetPageCount();
}
