package esign.text.pdf.parser.clipper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ClipperOffset {

    private Paths destPolys;
    private Path srcPoly;
    private Path destPoly;
    private final List<Point.DoublePoint> normals;
    private double delta;
    private double inA;
    private double sin;
    private double cos;
    private double miterLim;
    private double stepsPerRad;
    private Point.LongPoint lowest;
    private final PolyNode polyNodes;
    private final double arcTolerance;
    private final double miterLimit;
    private static final double TWO_PI = 6.283185307179586D;
    private static final double DEFAULT_ARC_TOLERANCE = 0.25D;
    private static final double TOLERANCE = 1.0E-20D;

    private static boolean nearZero(double val) {
        return (val > -1.0E-20D && val < 1.0E-20D);
    }

    public ClipperOffset() {
        this(2.0D, 0.25D);
    }

    public ClipperOffset(double miterLimit) {
        this(miterLimit, 0.25D);
    }

    public ClipperOffset(double miterLimit, double arcTolerance) {
        this.miterLimit = miterLimit;
        this.arcTolerance = arcTolerance;
        this.lowest = new Point.LongPoint();
        this.lowest.setX(Long.valueOf(-1L));
        this.polyNodes = new PolyNode();
        this.normals = new ArrayList<Point.DoublePoint>();
    }

    public void addPath(Path path, Clipper.JoinType joinType, Clipper.EndType endType) {
        int highI = path.size() - 1;
        if (highI < 0) {
            return;
        }
        PolyNode newNode = new PolyNode();
        newNode.setJoinType(joinType);
        newNode.setEndType(endType);

        if (endType == Clipper.EndType.CLOSED_LINE || endType == Clipper.EndType.CLOSED_POLYGON) {
            while (highI > 0 && path.get(0) == path.get(highI)) {
                highI--;
            }
        }

        newNode.getPolygon().add(path.get(0));
        int j = 0, k = 0;
        for (int i = 1; i <= highI; i++) {
            if (newNode.getPolygon().get(j) != path.get(i)) {
                j++;
                newNode.getPolygon().add(path.get(i));
                if (path.get(i).getY() > newNode.getPolygon().get(k).getY() || (path.get(i).getY() == newNode.getPolygon().get(k).getY() && path
                        .get(i).getX() < newNode.getPolygon().get(k).getX())) {
                    k = j;
                }
            }
        }
        if (endType == Clipper.EndType.CLOSED_POLYGON && j < 2) {
            return;
        }

        this.polyNodes.addChild(newNode);

        if (endType != Clipper.EndType.CLOSED_POLYGON) {
            return;
        }
        if (this.lowest.getX() < 0L) {
            this.lowest = new Point.LongPoint((this.polyNodes.getChildCount() - 1), k);
        } else {

            Point.LongPoint ip = ((PolyNode) this.polyNodes.getChilds().get((int) this.lowest.getX())).getPolygon().get((int) this.lowest.getY());
            if (newNode.getPolygon().get(k).getY() > ip.getY() || (newNode.getPolygon().get(k).getY() == ip.getY() && newNode
                    .getPolygon().get(k).getX() < ip.getX())) {
                this.lowest = new Point.LongPoint((this.polyNodes.getChildCount() - 1), k);
            }
        }
    }

    public void addPaths(Paths paths, Clipper.JoinType joinType, Clipper.EndType endType) {
        for (Path p : paths) {
            addPath(p, joinType, endType);
        }
    }

    public void clear() {
        this.polyNodes.getChilds().clear();
        this.lowest.setX(Long.valueOf(-1L));
    }

    private void doMiter(int j, int k, double r) {
        double q = this.delta / r;
        this.destPoly.add(new Point.LongPoint(Math.round(this.srcPoly.get(j).getX() + (((Point.DoublePoint) this.normals.get(k)).getX() + ((Point.DoublePoint) this.normals.get(j)).getX()) * q),
                Math.round(this.srcPoly.get(j).getY() + (((Point.DoublePoint) this.normals.get(k)).getY() + ((Point.DoublePoint) this.normals.get(j)).getY()) * q)));
    }

    private void doOffset(double delta) {
        double y;
        this.destPolys = new Paths();
        this.delta = delta;

        if (nearZero(delta)) {
            for (int j = 0; j < this.polyNodes.getChildCount(); j++) {
                PolyNode node = this.polyNodes.getChilds().get(j);
                if (node.getEndType() == Clipper.EndType.CLOSED_POLYGON) {
                    this.destPolys.add(node.getPolygon());
                }
            }

            return;
        }

        if (this.miterLimit > 2.0D) {
            this.miterLim = 2.0D / this.miterLimit * this.miterLimit;
        } else {

            this.miterLim = 0.5D;
        }

        if (this.arcTolerance <= 0.0D) {
            y = 0.25D;
        } else if (this.arcTolerance > Math.abs(delta) * 0.25D) {
            y = Math.abs(delta) * 0.25D;
        } else {

            y = this.arcTolerance;
        }

        double steps = Math.PI / Math.acos(1.0D - y / Math.abs(delta));
        this.sin = Math.sin(6.283185307179586D / steps);
        this.cos = Math.cos(6.283185307179586D / steps);
        this.stepsPerRad = steps / 6.283185307179586D;
        if (delta < 0.0D) {
            this.sin = -this.sin;
        }

        for (int i = 0; i < this.polyNodes.getChildCount(); i++) {
            PolyNode node = this.polyNodes.getChilds().get(i);
            this.srcPoly = node.getPolygon();

            int len = this.srcPoly.size();

            if (len != 0 && (delta > 0.0D || (len >= 3 && node.getEndType() == Clipper.EndType.CLOSED_POLYGON))) {

                this.destPoly = new Path();

                if (len == 1) {
                    if (node.getJoinType() == Clipper.JoinType.ROUND) {
                        double X = 1.0D, Y = 0.0D;
                        for (int j = 1; j <= steps; j++) {
                            this.destPoly.add(new Point.LongPoint(Math.round(this.srcPoly.get(0).getX() + X * delta), Math.round(this.srcPoly.get(0).getY() + Y * delta)));

                            double X2 = X;
                            X = X * this.cos - this.sin * Y;
                            Y = X2 * this.sin + Y * this.cos;
                        }
                    } else {

                        double X = -1.0D, Y = -1.0D;
                        for (int j = 0; j < 4; j++) {
                            this.destPoly.add(new Point.LongPoint(Math.round(this.srcPoly.get(0).getX() + X * delta), Math.round(this.srcPoly.get(0).getY() + Y * delta)));

                            if (X < 0.0D) {
                                X = 1.0D;
                            } else if (Y < 0.0D) {
                                Y = 1.0D;
                            } else {

                                X = -1.0D;
                            }
                        }
                    }
                    this.destPolys.add(this.destPoly);

                } else {

                    this.normals.clear();
                    for (int j = 0; j < len - 1; j++) {
                        this.normals.add(Point.getUnitNormal(this.srcPoly.get(j), this.srcPoly.get(j + 1)));
                    }
                    if (node.getEndType() == Clipper.EndType.CLOSED_LINE || node.getEndType() == Clipper.EndType.CLOSED_POLYGON) {
                        this.normals.add(Point.getUnitNormal(this.srcPoly.get(len - 1), this.srcPoly.get(0)));
                    } else {

                        this.normals.add(new Point.DoublePoint(this.normals.get(len - 2)));
                    }

                    if (node.getEndType() == Clipper.EndType.CLOSED_POLYGON) {
                        int[] k = {len - 1};
                        for (int m = 0; m < len; m++) {
                            offsetPoint(m, k, node.getJoinType());
                        }
                        this.destPolys.add(this.destPoly);
                    } else if (node.getEndType() == Clipper.EndType.CLOSED_LINE) {
                        int[] k = {len - 1};
                        for (int m = 0; m < len; m++) {
                            offsetPoint(m, k, node.getJoinType());
                        }
                        this.destPolys.add(this.destPoly);
                        this.destPoly = new Path();

                        Point.DoublePoint n = this.normals.get(len - 1);
                        int i1;
                        for (i1 = len - 1; i1 > 0; i1--) {
                            this.normals.set(i1, new Point.DoublePoint(-((Point.DoublePoint) this.normals.get(i1 - 1)).getX(), -((Point.DoublePoint) this.normals.get(i1 - 1)).getY()));
                        }
                        this.normals.set(0, new Point.DoublePoint(-n.getX(), -n.getY(), 0.0D));
                        k[0] = 0;
                        for (i1 = len - 1; i1 >= 0; i1--) {
                            offsetPoint(i1, k, node.getJoinType());
                        }
                        this.destPolys.add(this.destPoly);
                    } else {

                        int[] k = new int[1];
                        for (int m = 1; m < len - 1; m++) {
                            offsetPoint(m, k, node.getJoinType());
                        }

                        if (node.getEndType() == Clipper.EndType.OPEN_BUTT) {
                            int i1 = len - 1;
                            Point.LongPoint pt1 = new Point.LongPoint(Math.round(this.srcPoly.get(i1).getX() + ((Point.DoublePoint) this.normals.get(i1)).getX() * delta), Math.round(this.srcPoly.get(i1)
                                    .getY() + ((Point.DoublePoint) this.normals.get(i1)).getY() * delta), 0L);
                            this.destPoly.add(pt1);
                            pt1 = new Point.LongPoint(Math.round(this.srcPoly.get(i1).getX() - ((Point.DoublePoint) this.normals.get(i1)).getX() * delta), Math.round(this.srcPoly.get(i1)
                                    .getY() - ((Point.DoublePoint) this.normals.get(i1)).getY() * delta), 0L);
                            this.destPoly.add(pt1);
                        } else {

                            int i1 = len - 1;
                            k[0] = len - 2;
                            this.inA = 0.0D;
                            this.normals.set(i1, new Point.DoublePoint(-((Point.DoublePoint) this.normals.get(i1)).getX(), -((Point.DoublePoint) this.normals.get(i1)).getY()));
                            if (node.getEndType() == Clipper.EndType.OPEN_SQUARE) {
                                doSquare(i1, k[0], true);
                            } else {

                                doRound(i1, k[0]);
                            }
                        }

                        int n;
                        for (n = len - 1; n > 0; n--) {
                            this.normals.set(n, new Point.DoublePoint(-((Point.DoublePoint) this.normals.get(n - 1)).getX(), -((Point.DoublePoint) this.normals.get(n - 1)).getY()));
                        }

                        this.normals.set(0, new Point.DoublePoint(-((Point.DoublePoint) this.normals.get(1)).getX(), -((Point.DoublePoint) this.normals.get(1)).getY()));

                        k[0] = len - 1;
                        for (n = k[0] - 1; n > 0; n--) {
                            offsetPoint(n, k, node.getJoinType());
                        }

                        if (node.getEndType() == Clipper.EndType.OPEN_BUTT) {
                            Point.LongPoint pt1 = new Point.LongPoint(Math.round(this.srcPoly.get(0).getX() - ((Point.DoublePoint) this.normals.get(0)).getX() * delta), Math.round(this.srcPoly.get(0)
                                    .getY() - ((Point.DoublePoint) this.normals.get(0)).getY() * delta));
                            this.destPoly.add(pt1);
                            pt1 = new Point.LongPoint(Math.round(this.srcPoly.get(0).getX() + ((Point.DoublePoint) this.normals.get(0)).getX() * delta), Math.round(this.srcPoly.get(0)
                                    .getY() + ((Point.DoublePoint) this.normals.get(0)).getY() * delta));
                            this.destPoly.add(pt1);
                        } else {

                            k[0] = 1;
                            this.inA = 0.0D;
                            if (node.getEndType() == Clipper.EndType.OPEN_SQUARE) {
                                doSquare(0, 1, true);
                            } else {

                                doRound(0, 1);
                            }
                        }
                        this.destPolys.add(this.destPoly);
                    }
                }
            }
        }
    }

    private void doRound(int j, int k) {
        double a = Math.atan2(this.inA, ((Point.DoublePoint) this.normals.get(k)).getX() * ((Point.DoublePoint) this.normals.get(j)).getX() + ((Point.DoublePoint) this.normals.get(k)).getY() * ((Point.DoublePoint) this.normals.get(j)).getY());
        int steps = Math.max((int) Math.round(this.stepsPerRad * Math.abs(a)), 1);

        double X = ((Point.DoublePoint) this.normals.get(k)).getX(), Y = ((Point.DoublePoint) this.normals.get(k)).getY();
        for (int i = 0; i < steps; i++) {
            this.destPoly.add(new Point.LongPoint(Math.round(this.srcPoly.get(j).getX() + X * this.delta), Math.round(this.srcPoly.get(j).getY() + Y * this.delta)));
            double X2 = X;
            X = X * this.cos - this.sin * Y;
            Y = X2 * this.sin + Y * this.cos;
        }
        this.destPoly.add(new Point.LongPoint(Math.round(this.srcPoly.get(j).getX() + ((Point.DoublePoint) this.normals.get(j)).getX() * this.delta), Math.round(this.srcPoly.get(j).getY() + ((Point.DoublePoint) this.normals
                .get(j)).getY() * this.delta)));
    }

    private void doSquare(int j, int k, boolean addExtra) {
        double nkx = ((Point.DoublePoint) this.normals.get(k)).getX();
        double nky = ((Point.DoublePoint) this.normals.get(k)).getY();
        double njx = ((Point.DoublePoint) this.normals.get(j)).getX();
        double njy = ((Point.DoublePoint) this.normals.get(j)).getY();
        double sjx = this.srcPoly.get(j).getX();
        double sjy = this.srcPoly.get(j).getY();
        double dx = Math.tan(Math.atan2(this.inA, nkx * njx + nky * njy) / 4.0D);
        this.destPoly.add(new Point.LongPoint(Math.round(sjx + this.delta * (nkx - (addExtra ? (nky * dx) : 0.0D))), Math.round(sjy + this.delta * (nky + (addExtra ? (nkx * dx) : 0.0D))), 0L));
        this.destPoly.add(new Point.LongPoint(Math.round(sjx + this.delta * (njx + (addExtra ? (njy * dx) : 0.0D))), Math.round(sjy + this.delta * (njy - (addExtra ? (njx * dx) : 0.0D))), 0L));
    }

    public void execute(Paths solution, double delta) {
        solution.clear();
        fixOrientations();
        doOffset(delta);

        DefaultClipper clpr = new DefaultClipper(1);
        clpr.addPaths(this.destPolys, Clipper.PolyType.SUBJECT, true);
        if (delta > 0.0D) {
            clpr.execute(Clipper.ClipType.UNION, solution, Clipper.PolyFillType.POSITIVE, Clipper.PolyFillType.POSITIVE);
        } else {

            LongRect r = this.destPolys.getBounds();
            Path outer = new Path(4);

            outer.add(new Point.LongPoint(r.left - 10L, r.bottom + 10L, 0L));
            outer.add(new Point.LongPoint(r.right + 10L, r.bottom + 10L, 0L));
            outer.add(new Point.LongPoint(r.right + 10L, r.top - 10L, 0L));
            outer.add(new Point.LongPoint(r.left - 10L, r.top - 10L, 0L));

            clpr.addPath(outer, Clipper.PolyType.SUBJECT, true);

            clpr.execute(Clipper.ClipType.UNION, solution, Clipper.PolyFillType.NEGATIVE, Clipper.PolyFillType.NEGATIVE);
            if (solution.size() > 0) {
                solution.remove(0);
            }
        }
    }

    public void execute(PolyTree solution, double delta) {
        solution.Clear();
        fixOrientations();
        doOffset(delta);

        DefaultClipper clpr = new DefaultClipper(1);
        clpr.addPaths(this.destPolys, Clipper.PolyType.SUBJECT, true);
        if (delta > 0.0D) {
            clpr.execute(Clipper.ClipType.UNION, solution, Clipper.PolyFillType.POSITIVE, Clipper.PolyFillType.POSITIVE);
        } else {

            LongRect r = this.destPolys.getBounds();
            Path outer = new Path(4);

            outer.add(new Point.LongPoint(r.left - 10L, r.bottom + 10L, 0L));
            outer.add(new Point.LongPoint(r.right + 10L, r.bottom + 10L, 0L));
            outer.add(new Point.LongPoint(r.right + 10L, r.top - 10L, 0L));
            outer.add(new Point.LongPoint(r.left - 10L, r.top - 10L, 0L));

            clpr.addPath(outer, Clipper.PolyType.SUBJECT, true);

            clpr.execute(Clipper.ClipType.UNION, solution, Clipper.PolyFillType.NEGATIVE, Clipper.PolyFillType.NEGATIVE);

            if (solution.getChildCount() == 1 && ((PolyNode) solution.getChilds().get(0)).getChildCount() > 0) {
                PolyNode outerNode = solution.getChilds().get(0);
                solution.getChilds().set(0, outerNode.getChilds().get(0));
                ((PolyNode) solution.getChilds().get(0)).setParent(solution);
                for (int i = 1; i < outerNode.getChildCount(); i++) {
                    solution.addChild(outerNode.getChilds().get(i));
                }
            } else {

                solution.Clear();
            }
        }
    }

    private void fixOrientations() {
        if (this.lowest.getX() >= 0L && !((PolyNode) this.polyNodes.childs.get((int) this.lowest.getX())).getPolygon().orientation()) {
            for (int i = 0; i < this.polyNodes.getChildCount(); i++) {
                PolyNode node = this.polyNodes.childs.get(i);
                if (node.getEndType() == Clipper.EndType.CLOSED_POLYGON || (node.getEndType() == Clipper.EndType.CLOSED_LINE && node.getPolygon().orientation())) {
                    Collections.reverse(node.getPolygon());
                }
            }

        } else {

            for (int i = 0; i < this.polyNodes.getChildCount(); i++) {
                PolyNode node = this.polyNodes.childs.get(i);
                if (node.getEndType() == Clipper.EndType.CLOSED_LINE && !node.getPolygon().orientation()) {
                    Collections.reverse(node.getPolygon());
                }
            }
        }
    }

    private void offsetPoint(int j, int[] kV, Clipper.JoinType jointype) {
        int k = kV[0];
        double nkx = ((Point.DoublePoint) this.normals.get(k)).getX();
        double nky = ((Point.DoublePoint) this.normals.get(k)).getY();
        double njy = ((Point.DoublePoint) this.normals.get(j)).getY();
        double njx = ((Point.DoublePoint) this.normals.get(j)).getX();
        long sjx = this.srcPoly.get(j).getX();
        long sjy = this.srcPoly.get(j).getY();
        this.inA = nkx * njy - njx * nky;

        if (Math.abs(this.inA * this.delta) < 1.0D) {

            double cosA = nkx * njx + njy * nky;
            if (cosA > 0.0D) {

                this.destPoly.add(new Point.LongPoint(Math.round(sjx + nkx * this.delta), Math.round(sjy + nky * this.delta), 0L));

                return;
            }
        } else if (this.inA > 1.0D) {
            this.inA = 1.0D;
        } else if (this.inA < -1.0D) {
            this.inA = -1.0D;
        }

        if (this.inA * this.delta < 0.0D) {
            this.destPoly.add(new Point.LongPoint(Math.round(sjx + nkx * this.delta), Math.round(sjy + nky * this.delta)));
            this.destPoly.add(this.srcPoly.get(j));
            this.destPoly.add(new Point.LongPoint(Math.round(sjx + njx * this.delta), Math.round(sjy + njy * this.delta)));
        } else {
            double r;
            switch (jointype) {
                case MITER:
                    r = 1.0D + njx * nkx + njy * nky;
                    if (r >= this.miterLim) {
                        doMiter(j, k, r);
                        break;
                    }
                    doSquare(j, k, false);
                    break;

                case BEVEL:
                    doSquare(j, k, false);
                    break;
                case ROUND:
                    doRound(j, k);
                    break;
            }
        }
        kV[0] = j;
    }
}
