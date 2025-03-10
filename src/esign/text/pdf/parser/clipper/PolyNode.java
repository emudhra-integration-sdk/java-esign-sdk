package esign.text.pdf.parser.clipper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PolyNode {

    private PolyNode parent;

    enum NodeType {
        ANY, OPEN, CLOSED;
    }

    private final Path polygon = new Path();
    private int index;
    private Clipper.JoinType joinType;
    private Clipper.EndType endType;
    protected final List<PolyNode> childs = new ArrayList<PolyNode>();
    private boolean isOpen;

    public void addChild(PolyNode child) {
        int cnt = this.childs.size();
        this.childs.add(child);
        child.parent = this;
        child.index = cnt;
    }

    public int getChildCount() {
        return this.childs.size();
    }

    public List<PolyNode> getChilds() {
        return Collections.unmodifiableList(this.childs);
    }

    public List<Point.LongPoint> getContour() {
        return this.polygon;
    }

    public Clipper.EndType getEndType() {
        return this.endType;
    }

    public Clipper.JoinType getJoinType() {
        return this.joinType;
    }

    public PolyNode getNext() {
        if (!this.childs.isEmpty()) {
            return this.childs.get(0);
        }

        return getNextSiblingUp();
    }

    private PolyNode getNextSiblingUp() {
        if (this.parent == null) {
            return null;
        }
        if (this.index == this.parent.childs.size() - 1) {
            return this.parent.getNextSiblingUp();
        }

        return this.parent.childs.get(this.index + 1);
    }

    public PolyNode getParent() {
        return this.parent;
    }

    public Path getPolygon() {
        return this.polygon;
    }

    public boolean isHole() {
        return isHoleNode();
    }

    private boolean isHoleNode() {
        boolean result = true;
        PolyNode node = this.parent;
        while (node != null) {
            result = !result;
            node = node.parent;
        }
        return result;
    }

    public boolean isOpen() {
        return this.isOpen;
    }

    public void setEndType(Clipper.EndType value) {
        this.endType = value;
    }

    public void setJoinType(Clipper.JoinType value) {
        this.joinType = value;
    }

    public void setOpen(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public void setParent(PolyNode n) {
        this.parent = n;
    }
}
