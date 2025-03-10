package esign.text.pdf.parser.clipper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class ClipperBase
        implements Clipper {

    private static final long LOW_RANGE = 1073741823L;
    private static final long HI_RANGE = 4611686018427387903L;
    protected LocalMinima minimaList;
    protected LocalMinima currentLM;
    private final List<List<Edge>> edges;
    protected boolean useFullRange;
    protected boolean hasOpenPaths;
    protected final boolean preserveCollinear;

    protected class LocalMinima {

        long y;
        Edge leftBound;
        Edge rightBound;
        LocalMinima next;
    }

    protected class Scanbeam {

        long y;
        Scanbeam next;
    }

    private static void initEdge(Edge e, Edge eNext, Edge ePrev, Point.LongPoint pt) {
        e.next = eNext;
        e.prev = ePrev;
        e.setCurrent(new Point.LongPoint(pt));
        e.outIdx = -1;
    }

    private static void initEdge2(Edge e, Clipper.PolyType polyType) {
        if (e.getCurrent().getY() >= e.next.getCurrent().getY()) {
            e.setBot(new Point.LongPoint(e.getCurrent()));
            e.setTop(new Point.LongPoint(e.next.getCurrent()));
        } else {

            e.setTop(new Point.LongPoint(e.getCurrent()));
            e.setBot(new Point.LongPoint(e.next.getCurrent()));
        }
        e.updateDeltaX();
        e.polyTyp = polyType;
    }

    private static boolean rangeTest(Point.LongPoint Pt, boolean useFullRange) {
        if (useFullRange) {
            if (Pt.getX() > 4611686018427387903L || Pt.getY() > 4611686018427387903L || -Pt.getX() > 4611686018427387903L || -Pt.getY() > 4611686018427387903L) {
                throw new IllegalStateException("Coordinate outside allowed range");
            }
        } else if (Pt.getX() > 1073741823L || Pt.getY() > 1073741823L || -Pt.getX() > 1073741823L || -Pt.getY() > 1073741823L) {
            return rangeTest(Pt, true);
        }

        return useFullRange;
    }

    private static Edge removeEdge(Edge e) {
        e.prev.next = e.next;
        e.next.prev = e.prev;
        Edge result = e.next;
        e.prev = null;
        return result;
    }

    private static final Logger LOGGER = Logger.getLogger(Clipper.class.getName());

    protected ClipperBase(boolean preserveCollinear) {
        this.preserveCollinear = preserveCollinear;
        this.minimaList = null;
        this.currentLM = null;
        this.hasOpenPaths = false;
        this.edges = new ArrayList<List<Edge>>();
    }

    public boolean addPath(Path pg, Clipper.PolyType polyType, boolean Closed) {
        if (!Closed && polyType == Clipper.PolyType.CLIP) {
            throw new IllegalStateException("AddPath: Open paths must be subject.");
        }

        int highI = pg.size() - 1;
        if (Closed) {
            while (highI > 0 && pg.get(highI).equals(pg.get(0))) {
                highI--;
            }
        }
        while (highI > 0 && pg.get(highI).equals(pg.get(highI - 1))) {
            highI--;
        }
        if ((Closed && highI < 2) || (!Closed && highI < 1)) {
            return false;
        }

        List<Edge> edges = new ArrayList<Edge>(highI + 1);
        for (int i = 0; i <= highI; i++) {
            edges.add(new Edge());
        }

        boolean IsFlat = true;

        ((Edge) edges.get(1)).setCurrent(new Point.LongPoint(pg.get(1)));
        this.useFullRange = rangeTest(pg.get(0), this.useFullRange);
        this.useFullRange = rangeTest(pg.get(highI), this.useFullRange);
        initEdge(edges.get(0), edges.get(1), edges.get(highI), pg.get(0));
        initEdge(edges.get(highI), edges.get(0), edges.get(highI - 1), pg.get(highI));
        for (int j = highI - 1; j >= 1; j--) {
            this.useFullRange = rangeTest(pg.get(j), this.useFullRange);
            initEdge(edges.get(j), edges.get(j + 1), edges.get(j - 1), pg.get(j));
        }
        Edge eStart = edges.get(0);

        Edge e = eStart, eLoopStop = eStart;

        while (true) {
            if (e.getCurrent().equals(e.next.getCurrent()) && (Closed || !e.next.equals(eStart))) {
                if (e == e.next) {
                    break;
                }
                if (e == eStart) {
                    eStart = e.next;
                }
                e = removeEdge(e);
                eLoopStop = e;
                continue;
            }
            if (e.prev == e.next) {
                break;
            }
            if (Closed && Point.slopesEqual(e.prev.getCurrent(), e.getCurrent(), e.next.getCurrent(), this.useFullRange) && (!isPreserveCollinear() || !Point.isPt2BetweenPt1AndPt3(e.prev.getCurrent(), e.getCurrent(), e.next.getCurrent()))) {

                if (e == eStart) {
                    eStart = e.next;
                }
                e = removeEdge(e);
                e = e.prev;
                eLoopStop = e;
                continue;
            }
            e = e.next;
            if (e == eLoopStop || (!Closed && e.next == eStart)) {
                break;
            }
        }

        if ((!Closed && e == e.next) || (Closed && e.prev == e.next)) {
            return false;
        }

        if (!Closed) {
            this.hasOpenPaths = true;
            eStart.prev.outIdx = -2;
        }

        e = eStart;
        do {
            initEdge2(e, polyType);
            e = e.next;
            if (!IsFlat || e.getCurrent().getY() == eStart.getCurrent().getY()) {
                continue;
            }
            IsFlat = false;

        } while (e != eStart);

        if (IsFlat) {
            if (Closed) {
                return false;
            }
            e.prev.outIdx = -2;
            LocalMinima locMin = new LocalMinima();
            locMin.next = null;
            locMin.y = e.getBot().getY();
            locMin.leftBound = null;
            locMin.rightBound = e;
            locMin.rightBound.side = Edge.Side.RIGHT;
            locMin.rightBound.windDelta = 0;

            while (true) {
                if (e.getBot().getX() != e.prev.getTop().getX()) {
                    e.reverseHorizontal();
                }
                if (e.next.outIdx == -2) {
                    break;
                }
                e.nextInLML = e.next;
                e = e.next;
            }
            insertLocalMinima(locMin);
            this.edges.add(edges);
            return true;
        }

        this.edges.add(edges);

        Edge EMin = null;

        if (e.prev.getBot().equals(e.prev.getTop())) {
            e = e.next;
        }
        while (true) {
            boolean leftBoundIsForward;
            e = e.findNextLocMin();
            if (e == EMin) {
                break;
            }
            if (EMin == null) {
                EMin = e;
            }

            LocalMinima locMin = new LocalMinima();
            locMin.next = null;
            locMin.y = e.getBot().getY();
            if (e.deltaX < e.prev.deltaX) {
                locMin.leftBound = e.prev;
                locMin.rightBound = e;
                leftBoundIsForward = false;
            } else {

                locMin.leftBound = e;
                locMin.rightBound = e.prev;
                leftBoundIsForward = true;
            }
            locMin.leftBound.side = Edge.Side.LEFT;
            locMin.rightBound.side = Edge.Side.RIGHT;

            if (!Closed) {
                locMin.leftBound.windDelta = 0;
            } else if (locMin.leftBound.next == locMin.rightBound) {
                locMin.leftBound.windDelta = -1;
            } else {

                locMin.leftBound.windDelta = 1;
            }
            locMin.rightBound.windDelta = -locMin.leftBound.windDelta;

            e = processBound(locMin.leftBound, leftBoundIsForward);
            if (e.outIdx == -2) {
                e = processBound(e, leftBoundIsForward);
            }

            Edge E2 = processBound(locMin.rightBound, !leftBoundIsForward);
            if (E2.outIdx == -2) {
                E2 = processBound(E2, !leftBoundIsForward);
            }

            if (locMin.leftBound.outIdx == -2) {
                locMin.leftBound = null;
            } else if (locMin.rightBound.outIdx == -2) {
                locMin.rightBound = null;
            }
            insertLocalMinima(locMin);
            if (!leftBoundIsForward) {
                e = E2;
            }
        }
        return true;
    }

    public boolean addPaths(Paths ppg, Clipper.PolyType polyType, boolean closed) {
        boolean result = false;
        for (int i = 0; i < ppg.size(); i++) {
            if (addPath(ppg.get(i), polyType, closed)) {
                result = true;
            }
        }
        return result;
    }

    public void clear() {
        disposeLocalMinimaList();
        this.edges.clear();
        this.useFullRange = false;
        this.hasOpenPaths = false;
    }

    private void disposeLocalMinimaList() {
        while (this.minimaList != null) {
            LocalMinima tmpLm = this.minimaList.next;
            this.minimaList = null;
            this.minimaList = tmpLm;
        }
        this.currentLM = null;
    }

    private void insertLocalMinima(LocalMinima newLm) {
        if (this.minimaList == null) {
            this.minimaList = newLm;
        } else if (newLm.y >= this.minimaList.y) {
            newLm.next = this.minimaList;
            this.minimaList = newLm;
        } else {

            LocalMinima tmpLm = this.minimaList;
            while (tmpLm.next != null && newLm.y < tmpLm.next.y) {
                tmpLm = tmpLm.next;
            }
            newLm.next = tmpLm.next;
            tmpLm.next = newLm;
        }
    }

    public boolean isPreserveCollinear() {
        return this.preserveCollinear;
    }

    protected void popLocalMinima() {
        LOGGER.entering(ClipperBase.class.getName(), "popLocalMinima");
        if (this.currentLM == null) {
            return;
        }
        this.currentLM = this.currentLM.next;
    }

    private Edge processBound(Edge e, boolean LeftBoundIsForward) {
        Edge result = e;

        if (result.outIdx == -2) {

            e = result;
            if (LeftBoundIsForward) {
                while (e.getTop().getY() == e.next.getBot().getY()) {
                    e = e.next;
                }
                while (e != result && e.deltaX == -3.4E38D) {
                    e = e.prev;
                }
            } else {

                while (e.getTop().getY() == e.prev.getBot().getY()) {
                    e = e.prev;
                }
                while (e != result && e.deltaX == -3.4E38D) {
                    e = e.next;
                }
            }
            if (e == result) {
                if (LeftBoundIsForward) {
                    result = e.next;
                } else {

                    result = e.prev;
                }

            } else {

                if (LeftBoundIsForward) {
                    e = result.next;
                } else {

                    e = result.prev;
                }
                LocalMinima locMin = new LocalMinima();
                locMin.next = null;
                locMin.y = e.getBot().getY();
                locMin.leftBound = null;
                locMin.rightBound = e;
                e.windDelta = 0;
                result = processBound(e, LeftBoundIsForward);
                insertLocalMinima(locMin);
            }
            return result;
        }

        if (e.deltaX == -3.4E38D) {
            Edge edge;

            if (LeftBoundIsForward) {
                edge = e.prev;
            } else {

                edge = e.next;
            }
            if (edge.deltaX == -3.4E38D) {

                if (edge.getBot().getX() != e.getBot().getX() && edge.getTop().getX() != e.getBot().getX()) {
                    e.reverseHorizontal();
                }
            } else if (edge.getBot().getX() != e.getBot().getX()) {
                e.reverseHorizontal();
            }
        }
        Edge EStart = e;
        if (LeftBoundIsForward) {
            while (result.getTop().getY() == result.next.getBot().getY() && result.next.outIdx != -2) {
                result = result.next;
            }
            if (result.deltaX == -3.4E38D && result.next.outIdx != -2) {

                Edge Horz = result;
                while (Horz.prev.deltaX == -3.4E38D) {
                    Horz = Horz.prev;
                }
                if (Horz.prev.getTop().getX() > result.next.getTop().getX()) {
                    result = Horz.prev;
                }
            }
            while (e != result) {
                e.nextInLML = e.next;
                if (e.deltaX == -3.4E38D && e != EStart && e.getBot().getX() != e.prev.getTop().getX()) {
                    e.reverseHorizontal();
                }
                e = e.next;
            }
            if (e.deltaX == -3.4E38D && e != EStart && e.getBot().getX() != e.prev.getTop().getX()) {
                e.reverseHorizontal();
            }
            result = result.next;
        } else {

            while (result.getTop().getY() == result.prev.getBot().getY() && result.prev.outIdx != -2) {
                result = result.prev;
            }
            if (result.deltaX == -3.4E38D && result.prev.outIdx != -2) {
                Edge Horz = result;
                while (Horz.next.deltaX == -3.4E38D) {
                    Horz = Horz.next;
                }
                if (Horz.next.getTop().getX() == result.prev.getTop().getX() || Horz.next
                        .getTop().getX() > result.prev.getTop().getX()) {
                    result = Horz.next;
                }

            }
            while (e != result) {
                e.nextInLML = e.prev;
                if (e.deltaX == -3.4E38D && e != EStart && e.getBot().getX() != e.next.getTop().getX()) {
                    e.reverseHorizontal();
                }
                e = e.prev;
            }
            if (e.deltaX == -3.4E38D && e != EStart && e.getBot().getX() != e.next.getTop().getX()) {
                e.reverseHorizontal();
            }
            result = result.prev;
        }
        return result;
    }

    protected static Path.OutRec parseFirstLeft(Path.OutRec FirstLeft) {
        while (FirstLeft != null && FirstLeft.getPoints() == null) {
            FirstLeft = FirstLeft.firstLeft;
        }
        return FirstLeft;
    }

    protected void reset() {
        this.currentLM = this.minimaList;
        if (this.currentLM == null) {
            return;
        }

        LocalMinima lm = this.minimaList;
        while (lm != null) {
            Edge e = lm.leftBound;
            if (e != null) {
                e.setCurrent(new Point.LongPoint(e.getBot()));
                e.side = Edge.Side.LEFT;
                e.outIdx = -1;
            }
            e = lm.rightBound;
            if (e != null) {
                e.setCurrent(new Point.LongPoint(e.getBot()));
                e.side = Edge.Side.RIGHT;
                e.outIdx = -1;
            }
            lm = lm.next;
        }
    }
}
