package esign.text.pdf.parser.clipper;

import java.util.ArrayList;

public class Paths
        extends ArrayList<Path> {

    private static final long serialVersionUID = 1910552127810480852L;

    public static Paths closedPathsFromPolyTree(PolyTree polytree) {
        Paths result = new Paths();

        result.addPolyNode(polytree, PolyNode.NodeType.CLOSED);
        return result;
    }

    public static Paths makePolyTreeToPaths(PolyTree polytree) {
        Paths result = new Paths();

        result.addPolyNode(polytree, PolyNode.NodeType.ANY);
        return result;
    }

    public static Paths openPathsFromPolyTree(PolyTree polytree) {
        Paths result = new Paths();

        for (PolyNode c : polytree.getChilds()) {
            if (c.isOpen()) {
                result.add(c.getPolygon());
            }
        }
        return result;
    }

    public Paths() {
    }

    public Paths(int initialCapacity) {
        super(initialCapacity);
    }

    public void addPolyNode(PolyNode polynode, PolyNode.NodeType nt) {
        boolean match = true;
        switch (nt) {
            case OPEN:
                return;
            case CLOSED:
                match = !polynode.isOpen();
                break;
        }

        if (polynode.getPolygon().size() > 0 && match) {
            add(polynode.getPolygon());
        }
        for (PolyNode pn : polynode.getChilds()) {
            addPolyNode(pn, nt);
        }
    }

    public Paths cleanPolygons() {
        return cleanPolygons(1.415D);
    }

    public Paths cleanPolygons(double distance) {
        Paths result = new Paths(size());
        for (int i = 0; i < size(); i++) {
            result.add(get(i).cleanPolygon(distance));
        }
        return result;
    }

    public LongRect getBounds() {
        int i = 0;
        int cnt = size();
        LongRect result = new LongRect();
        while (i < cnt && get(i).isEmpty()) {
            i++;
        }
        if (i == cnt) {
            return result;
        }

        result.left = get(i).get(0).getX();
        result.right = result.left;
        result.top = get(i).get(0).getY();
        result.bottom = result.top;
        for (; i < cnt; i++) {
            for (int j = 0; j < get(i).size(); j++) {
                if (get(i).get(j).getX() < result.left) {
                    result.left = get(i).get(j).getX();
                } else if (get(i).get(j).getX() > result.right) {
                    result.right = get(i).get(j).getX();
                }
                if (get(i).get(j).getY() < result.top) {
                    result.top = get(i).get(j).getY();
                } else if (get(i).get(j).getY() > result.bottom) {
                    result.bottom = get(i).get(j).getY();
                }
            }
        }
        return result;
    }

    public void reversePaths() {
        for (Path poly : this) {
            poly.reverse();
        }
    }
}
