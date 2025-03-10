package esign.text.pdf.parser.clipper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class Path
        extends ArrayList<Point.LongPoint> {

    private static final long serialVersionUID = -7120161578077546673L;

    static class Join {

        Path.OutPt outPt1;
        Path.OutPt outPt2;
        private Point.LongPoint offPt;

        public Point.LongPoint getOffPt() {
            return this.offPt;
        }

        public void setOffPt(Point.LongPoint offPt) {
            this.offPt = offPt;
        }
    }

    static class OutPt {

        int idx;
        protected Point.LongPoint pt;

        public static Path.OutRec getLowerMostRec(Path.OutRec outRec1, Path.OutRec outRec2) {
            if (outRec1.bottomPt == null) {
                outRec1.bottomPt = outRec1.pts.getBottomPt();
            }
            if (outRec2.bottomPt == null) {
                outRec2.bottomPt = outRec2.pts.getBottomPt();
            }
            OutPt bPt1 = outRec1.bottomPt;
            OutPt bPt2 = outRec2.bottomPt;
            if (bPt1.getPt().getY() > bPt2.getPt().getY()) {
                return outRec1;
            }
            if (bPt1.getPt().getY() < bPt2.getPt().getY()) {
                return outRec2;
            }
            if (bPt1.getPt().getX() < bPt2.getPt().getX()) {
                return outRec1;
            }
            if (bPt1.getPt().getX() > bPt2.getPt().getX()) {
                return outRec2;
            }
            if (bPt1.next == bPt1) {
                return outRec2;
            }
            if (bPt2.next == bPt2) {
                return outRec1;
            }
            if (isFirstBottomPt(bPt1, bPt2)) {
                return outRec1;
            }

            return outRec2;
        }
        OutPt next;
        OutPt prev;

        private static boolean isFirstBottomPt(OutPt btmPt1, OutPt btmPt2) {
            OutPt p = btmPt1.prev;
            while (p.getPt().equals(btmPt1.getPt()) && !p.equals(btmPt1)) {
                p = p.prev;
            }
            double dx1p = Math.abs(Point.LongPoint.getDeltaX(btmPt1.getPt(), p.getPt()));
            p = btmPt1.next;
            while (p.getPt().equals(btmPt1.getPt()) && !p.equals(btmPt1)) {
                p = p.next;
            }
            double dx1n = Math.abs(Point.LongPoint.getDeltaX(btmPt1.getPt(), p.getPt()));

            p = btmPt2.prev;
            while (p.getPt().equals(btmPt2.getPt()) && !p.equals(btmPt2)) {
                p = p.prev;
            }
            double dx2p = Math.abs(Point.LongPoint.getDeltaX(btmPt2.getPt(), p.getPt()));
            p = btmPt2.next;
            while (p.getPt().equals(btmPt2.getPt()) && p.equals(btmPt2)) {
                p = p.next;
            }
            double dx2n = Math.abs(Point.LongPoint.getDeltaX(btmPt2.getPt(), p.getPt()));
            return ((dx1p >= dx2p && dx1p >= dx2n) || (dx1n >= dx2p && dx1n >= dx2n));
        }

        public OutPt duplicate(boolean InsertAfter) {
            OutPt result = new OutPt();
            result.setPt(new Point.LongPoint(getPt()));
            result.idx = this.idx;
            if (InsertAfter) {
                result.next = this.next;
                result.prev = this;
                this.next.prev = result;
                this.next = result;
            } else {

                result.prev = this.prev;
                result.next = this;
                this.prev.next = result;
                this.prev = result;
            }
            return result;
        }

        OutPt getBottomPt() {
            OutPt dups = null;
            OutPt p = this.next;
            OutPt pp = this;
            while (p != pp) {
                if (p.getPt().getY() > pp.getPt().getY()) {
                    pp = p;
                    dups = null;
                } else if (p.getPt().getY() == pp.getPt().getY() && p.getPt().getX() <= pp.getPt().getX()) {
                    if (p.getPt().getX() < pp.getPt().getX()) {
                        dups = null;
                        pp = p;

                    } else if (p.next != pp && p.prev != pp) {
                        dups = p;
                    }
                }

                p = p.next;
            }
            if (dups != null) {
                while (dups != p) {
                    if (!isFirstBottomPt(p, dups)) {
                        pp = dups;
                    }
                    dups = dups.next;
                    while (!dups.getPt().equals(pp.getPt())) {
                        dups = dups.next;
                    }
                }
            }
            return pp;
        }

        public int getPointCount() {
            int result = 0;
            OutPt p = this;
            do {
                result++;
                p = p.next;
            } while (p != this && p != null);
            return result;
        }

        public Point.LongPoint getPt() {
            return this.pt;
        }

        public void reversePolyPtLinks() {
            OutPt pp1 = this;
            do {
                OutPt pp2 = pp1.next;
                pp1.next = pp1.prev;
                pp1.prev = pp2;
                pp1 = pp2;
            } while (pp1 != this);
        }

        public void setPt(Point.LongPoint pt) {
            this.pt = pt;
        }
    }

    protected static class Maxima {

        protected long X;
        protected Maxima Next;
        protected Maxima Prev;
    }

    static class OutRec {

        int Idx;
        boolean isHole;
        boolean isOpen;
        OutRec firstLeft;
        protected Path.OutPt pts;
        Path.OutPt bottomPt;
        PolyNode polyNode;

        public double area() {
            Path.OutPt op = this.pts;
            if (op == null) {
                return 0.0D;
            }
            double a = 0.0D;
            while (true) {
                a += (op.prev.getPt().getX() + op.getPt().getX()) * (op.prev.getPt().getY() - op.getPt().getY());
                op = op.next;

                if (op == this.pts) {
                    return a * 0.5D;
                }
            }
        }

        public void fixHoleLinkage() {
            if (this.firstLeft == null || (this.isHole != this.firstLeft.isHole && this.firstLeft.pts != null)) {
                return;
            }

            OutRec orfl = this.firstLeft;
            while (orfl != null && (orfl.isHole == this.isHole || orfl.pts == null)) {
                orfl = orfl.firstLeft;
            }
            this.firstLeft = orfl;
        }

        public Path.OutPt getPoints() {
            return this.pts;
        }

        public void setPoints(Path.OutPt pts) {
            this.pts = pts;
        }
    }

    private static OutPt excludeOp(OutPt op) {
        OutPt result = op.prev;
        result.next = op.next;
        op.next.prev = result;
        result.idx = 0;
        return result;
    }

    public Path() {
    }

    public Path(Point.LongPoint[] points) {
        this();
        for (Point.LongPoint point : points) {
            add(point);
        }
    }

    public Path(int cnt) {
        super(cnt);
    }

    public Path(Collection<? extends Point.LongPoint> c) {
        super(c);
    }

    public double area() {
        int cnt = size();
        if (cnt < 3) {
            return 0.0D;
        }
        double a = 0.0D;
        for (int i = 0, j = cnt - 1; i < cnt; i++) {
            a += (get(j).getX() + get(i).getX()) * (get(j).getY() - get(i).getY());
            j = i;
        }
        return -a * 0.5D;
    }

    public Path cleanPolygon() {
        return cleanPolygon(1.415D);
    }

    public Path cleanPolygon(double distance) {
        int cnt = size();

        if (cnt == 0) {
            return new Path();
        }

        OutPt[] outPts = new OutPt[cnt];
        int i;
        for (i = 0; i < cnt; i++) {
            outPts[i] = new OutPt();
        }

        for (i = 0; i < cnt; i++) {
            (outPts[i]).pt = get(i);
            (outPts[i]).next = outPts[(i + 1) % cnt];
            (outPts[i]).next.prev = outPts[i];
            (outPts[i]).idx = 0;
        }

        double distSqrd = distance * distance;
        OutPt op = outPts[0];
        while (op.idx == 0 && op.next != op.prev) {
            if (Point.arePointsClose(op.pt, op.prev.pt, distSqrd)) {
                op = excludeOp(op);
                cnt--;
                continue;
            }
            if (Point.arePointsClose(op.prev.pt, op.next.pt, distSqrd)) {
                excludeOp(op.next);
                op = excludeOp(op);
                cnt -= 2;
                continue;
            }
            if (Point.slopesNearCollinear(op.prev.pt, op.pt, op.next.pt, distSqrd)) {
                op = excludeOp(op);
                cnt--;
                continue;
            }
            op.idx = 1;
            op = op.next;
        }

        if (cnt < 3) {
            cnt = 0;
        }
        Path result = new Path(cnt);
        for (int j = 0; j < cnt; j++) {
            result.add(op.pt);
            op = op.next;
        }
        outPts = null;
        return result;
    }

    public int isPointInPolygon(Point.LongPoint pt) {
        int result = 0;
        int cnt = size();
        if (cnt < 3) {
            return 0;
        }
        Point.LongPoint ip = get(0);
        for (int i = 1; i <= cnt; i++) {
            Point.LongPoint ipNext = (i == cnt) ? get(0) : get(i);
            if (ipNext.getY() == pt.getY()) {
                if (ipNext.getX() != pt.getX()) {
                    if (ip.getY() == pt.getY()) {
                        if (((ipNext.getX() > pt.getX()) ? true : false) == ((ip.getX() < pt.getX()) ? true : false)) {
                            return -1;
                        }
                    }
                } else {
                    return -1;
                }

            }
            if (((ip.getY() < pt.getY()) ? true : false) != ((ipNext.getY() < pt.getY()) ? true : false)) {
                if (ip.getX() >= pt.getX()) {
                    if (ipNext.getX() > pt.getX()) {
                        result = 1 - result;
                    } else {

                        double d = (ip.getX() - pt.getX()) * (ipNext.getY() - pt.getY()) - (ipNext.getX() - pt.getX()) * (ip.getY() - pt.getY());
                        if (d == 0.0D) {
                            return -1;
                        }
                        if (((d > 0.0D) ? true : false) == ((ipNext.getY() > ip.getY()) ? true : false)) {
                            result = 1 - result;
                        }
                    }

                } else if (ipNext.getX() > pt.getX()) {

                    double d = (ip.getX() - pt.getX()) * (ipNext.getY() - pt.getY()) - (ipNext.getX() - pt.getX()) * (ip.getY() - pt.getY());
                    if (d == 0.0D) {
                        return -1;
                    }
                    if (((d > 0.0D) ? true : false) == ((ipNext.getY() > ip.getY()) ? true : false)) {
                        result = 1 - result;
                    }
                }
            }

            ip = ipNext;
        }
        return result;
    }

    public boolean orientation() {
        return (area() >= 0.0D);
    }

    public void reverse() {
        Collections.reverse(this);
    }

    public Path TranslatePath(Point.LongPoint delta) {
        Path outPath = new Path(size());
        for (int i = 0; i < size(); i++) {
            outPath.add(new Point.LongPoint(get(i).getX() + delta.getX(), get(i).getY() + delta.getY()));
        }
        return outPath;
    }
}
