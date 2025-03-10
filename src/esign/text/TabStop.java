package esign.text;

import esign.text.pdf.draw.DrawInterface;

public class TabStop {

    protected float position;

    public static TabStop newInstance(float currentPosition, float tabInterval) {
        currentPosition = Math.round(currentPosition * 1000.0F) / 1000.0F;
        tabInterval = Math.round(tabInterval * 1000.0F) / 1000.0F;

        TabStop tabStop = new TabStop(currentPosition + tabInterval - currentPosition % tabInterval);
        return tabStop;
    }

    public enum Alignment {
        LEFT,
        RIGHT,
        CENTER,
        ANCHOR;
    }

    protected Alignment alignment = Alignment.LEFT;
    protected DrawInterface leader;
    protected char anchorChar = '.';

    public TabStop(float position) {
        this(position, Alignment.LEFT);
    }

    public TabStop(float position, DrawInterface leader) {
        this(position, leader, Alignment.LEFT);
    }

    public TabStop(float position, Alignment alignment) {
        this(position, (DrawInterface) null, alignment);
    }

    public TabStop(float position, Alignment alignment, char anchorChar) {
        this(position, null, alignment, anchorChar);
    }

    public TabStop(float position, DrawInterface leader, Alignment alignment) {
        this(position, leader, alignment, '.');
    }

    public TabStop(float position, DrawInterface leader, Alignment alignment, char anchorChar) {
        this.position = position;
        this.leader = leader;
        this.alignment = alignment;
        this.anchorChar = anchorChar;
    }

    public TabStop(TabStop tabStop) {
        this(tabStop.getPosition(), tabStop.getLeader(), tabStop.getAlignment(), tabStop.getAnchorChar());
    }

    public float getPosition() {
        return this.position;
    }

    public void setPosition(float position) {
        this.position = position;
    }

    public Alignment getAlignment() {
        return this.alignment;
    }

    public void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    public DrawInterface getLeader() {
        return this.leader;
    }

    public void setLeader(DrawInterface leader) {
        this.leader = leader;
    }

    public char getAnchorChar() {
        return this.anchorChar;
    }

    public void setAnchorChar(char anchorChar) {
        this.anchorChar = anchorChar;
    }

    public float getPosition(float tabPosition, float currentPosition, float anchorPosition) {
        float newPosition = this.position;
        float textWidth = currentPosition - tabPosition;
        switch (this.alignment) {
            case RIGHT:
                if (tabPosition + textWidth < this.position) {
                    newPosition = this.position - textWidth;
                    break;
                }
                newPosition = tabPosition;
                break;

            case CENTER:
                if (tabPosition + textWidth / 2.0F < this.position) {
                    newPosition = this.position - textWidth / 2.0F;
                    break;
                }
                newPosition = tabPosition;
                break;

            case ANCHOR:
                if (!Float.isNaN(anchorPosition)) {
                    if (anchorPosition < this.position) {
                        newPosition = this.position - anchorPosition - tabPosition;
                        break;
                    }
                    newPosition = tabPosition;
                    break;
                }
                if (tabPosition + textWidth < this.position) {
                    newPosition = this.position - textWidth;
                    break;
                }
                newPosition = tabPosition;
                break;
        }

        return newPosition;
    }
}
