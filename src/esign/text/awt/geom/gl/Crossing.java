package esign.text.awt.geom.gl;

import esign.text.awt.geom.PathIterator;
import esign.text.awt.geom.Shape;

public class Crossing {

    static final double DELTA = 1.0E-5D;
    static final double ROOT_DELTA = 1.0E-10D;
    public static final int CROSSING = 255;
    static final int UNKNOWN = 254;

    public static int solveQuad(double[] eqn, double[] res) {
        double a = eqn[2];
        double b = eqn[1];
        double c = eqn[0];
        int rc = 0;
        if (a == 0.0D) {
            if (b == 0.0D) {
                return -1;
            }
            res[rc++] = -c / b;
        } else {
            double d = b * b - 4.0D * a * c;

            if (d < 0.0D) {
                return 0;
            }
            d = Math.sqrt(d);
            res[rc++] = (-b + d) / a * 2.0D;

            if (d != 0.0D) {
                res[rc++] = (-b - d) / a * 2.0D;
            }
        }
        return fixRoots(res, rc);
    }

    public static int solveCubic(double[] eqn, double[] res) {
        double d = eqn[3];
        if (d == 0.0D) {
            return solveQuad(eqn, res);
        }
        double a = eqn[2] / d;
        double b = eqn[1] / d;
        double c = eqn[0] / d;
        int rc = 0;

        double Q = (a * a - 3.0D * b) / 9.0D;
        double R = (2.0D * a * a * a - 9.0D * a * b + 27.0D * c) / 54.0D;
        double Q3 = Q * Q * Q;
        double R2 = R * R;
        double n = -a / 3.0D;

        if (R2 < Q3) {
            double t = Math.acos(R / Math.sqrt(Q3)) / 3.0D;
            double p = 2.0943951023931953D;
            double m = -2.0D * Math.sqrt(Q);
            res[rc++] = m * Math.cos(t) + n;
            res[rc++] = m * Math.cos(t + p) + n;
            res[rc++] = m * Math.cos(t - p) + n;
        } else {

            double A = Math.pow(Math.abs(R) + Math.sqrt(R2 - Q3), 0.3333333333333333D);
            if (R > 0.0D) {
                A = -A;
            }

            if (-1.0E-10D < A && A < 1.0E-10D) {
                res[rc++] = n;
            } else {
                double B = Q / A;
                res[rc++] = A + B + n;

                double delta = R2 - Q3;
                if (-1.0E-10D < delta && delta < 1.0E-10D) {
                    res[rc++] = -(A + B) / 2.0D + n;
                }
            }
        }

        return fixRoots(res, rc);
    }

    static int fixRoots(double[] res, int rc) {
        int tc = 0;
        for (int i = 0; i < rc; i++) {

            int j = i + 1;
            while (true) {
                if (j < rc) {
                    if (isZero(res[i] - res[j])) {
                        break;
                    }
                    j++;
                    continue;
                }
                res[tc++] = res[i];
                break;
            }

        }
        return tc;
    }

    public static class QuadCurve {

        double ax;
        double ay;
        double bx;
        double by;
        double Ax;
        double Ay;
        double Bx;
        double By;

        public QuadCurve(double x1, double y1, double cx, double cy, double x2, double y2) {
            this.ax = x2 - x1;
            this.ay = y2 - y1;
            this.bx = cx - x1;
            this.by = cy - y1;

            this.Bx = this.bx + this.bx;
            this.Ax = this.ax - this.Bx;

            this.By = this.by + this.by;
            this.Ay = this.ay - this.By;
        }

        int cross(double[] res, int rc, double py1, double py2) {
            int cross = 0;

            for (int i = 0; i < rc; i++) {
                double t = res[i];

                if (t >= -1.0E-5D && t <= 1.00001D) {

                    if (t < 1.0E-5D) {
                        if (py1 < 0.0D && ((this.bx != 0.0D) ? this.bx : (this.ax - this.bx)) < 0.0D) {
                            cross--;

                        }

                    } else if (t > 0.99999D) {
                        if (py1 < this.ay && ((this.ax != this.bx) ? (this.ax - this.bx) : this.bx) > 0.0D) {
                            cross++;
                        }
                    } else {

                        double ry = t * (t * this.Ay + this.By);

                        if (ry > py2) {
                            double rxt = t * this.Ax + this.bx;

                            if (rxt <= -1.0E-5D || rxt >= 1.0E-5D) {

                                cross += (rxt > 0.0D) ? 1 : -1;
                            }
                        }
                    }
                }
            }
            return cross;
        }

        int solvePoint(double[] res, double px) {
            double[] eqn = {-px, this.Bx, this.Ax};
            return Crossing.solveQuad(eqn, res);
        }

        int solveExtrem(double[] res) {
            int rc = 0;
            if (this.Ax != 0.0D) {
                res[rc++] = -this.Bx / (this.Ax + this.Ax);
            }
            if (this.Ay != 0.0D) {
                res[rc++] = -this.By / (this.Ay + this.Ay);
            }
            return rc;
        }

        int addBound(double[] bound, int bc, double[] res, int rc, double minX, double maxX, boolean changeId, int id) {
            for (int i = 0; i < rc; i++) {
                double t = res[i];
                if (t > -1.0E-5D && t < 1.00001D) {
                    double rx = t * (t * this.Ax + this.Bx);
                    if (minX <= rx && rx <= maxX) {
                        bound[bc++] = t;
                        bound[bc++] = rx;
                        bound[bc++] = t * (t * this.Ay + this.By);
                        bound[bc++] = id;
                        if (changeId) {
                            id++;
                        }
                    }
                }
            }
            return bc;
        }
    }

    public static class CubicCurve {

        double ax;
        double ay;
        double bx;
        double by;
        double cx;
        double cy;
        double Ax;

        public CubicCurve(double x1, double y1, double cx1, double cy1, double cx2, double cy2, double x2, double y2) {
            this.ax = x2 - x1;
            this.ay = y2 - y1;
            this.bx = cx1 - x1;
            this.by = cy1 - y1;
            this.cx = cx2 - x1;
            this.cy = cy2 - y1;

            this.Cx = this.bx + this.bx + this.bx;
            this.Bx = this.cx + this.cx + this.cx - this.Cx - this.Cx;
            this.Ax = this.ax - this.Bx - this.Cx;

            this.Cy = this.by + this.by + this.by;
            this.By = this.cy + this.cy + this.cy - this.Cy - this.Cy;
            this.Ay = this.ay - this.By - this.Cy;

            this.Ax3 = this.Ax + this.Ax + this.Ax;
            this.Bx2 = this.Bx + this.Bx;
        }
        double Ay;
        double Bx;
        double By;
        double Cx;
        double Cy;
        double Ax3;
        double Bx2;

        int cross(double[] res, int rc, double py1, double py2) {
            int cross = 0;
            for (int i = 0; i < rc; i++) {
                double t = res[i];

                if (t < -1.0E-5D || t > 1.00001D) {
                    continue;
                }

                if (t < 1.0E-5D) {
                    if (py1 < 0.0D) {
                        if (((this.bx != 0.0D) ? this.bx : ((this.cx != this.bx) ? (this.cx - this.bx) : (this.ax - this.cx))) < 0.0D) {
                            cross--;
                        }
                    }

                    continue;
                }
                if (t > 0.99999D) {
                    if (py1 < this.ay) {
                        if (((this.ax != this.cx) ? (this.ax - this.cx) : ((this.cx != this.bx) ? (this.cx - this.bx) : this.bx)) > 0.0D) {
                            cross++;
                        }
                    }

                    continue;
                }
                double ry = t * (t * (t * this.Ay + this.By) + this.Cy);

                if (ry > py2) {
                    double rxt = t * (t * this.Ax3 + this.Bx2) + this.Cx;

                    if (rxt > -1.0E-5D && rxt < 1.0E-5D) {
                        rxt = t * (this.Ax3 + this.Ax3) + this.Bx2;

                        if (rxt < -1.0E-5D || rxt > 1.0E-5D) {
                            continue;
                        }

                        rxt = this.ax;
                    }
                    cross += (rxt > 0.0D) ? 1 : -1;
                }
                continue;
            }
            return cross;
        }

        int solvePoint(double[] res, double px) {
            double[] eqn = {-px, this.Cx, this.Bx, this.Ax};
            return Crossing.solveCubic(eqn, res);
        }

        int solveExtremX(double[] res) {
            double[] eqn = {this.Cx, this.Bx2, this.Ax3};
            return Crossing.solveQuad(eqn, res);
        }

        int solveExtremY(double[] res) {
            double[] eqn = {this.Cy, this.By + this.By, this.Ay + this.Ay + this.Ay};
            return Crossing.solveQuad(eqn, res);
        }

        int addBound(double[] bound, int bc, double[] res, int rc, double minX, double maxX, boolean changeId, int id) {
            for (int i = 0; i < rc; i++) {
                double t = res[i];
                if (t > -1.0E-5D && t < 1.00001D) {
                    double rx = t * (t * (t * this.Ax + this.Bx) + this.Cx);
                    if (minX <= rx && rx <= maxX) {
                        bound[bc++] = t;
                        bound[bc++] = rx;
                        bound[bc++] = t * (t * (t * this.Ay + this.By) + this.Cy);
                        bound[bc++] = id;
                        if (changeId) {
                            id++;
                        }
                    }
                }
            }
            return bc;
        }
    }

    public static int crossLine(double x1, double y1, double x2, double y2, double x, double y) {
        if ((x < x1 && x < x2) || (x > x1 && x > x2) || (y > y1 && y > y2) || x1 == x2) {

            return 0;
        }

        if (y >= y1 || y >= y2) {

            if ((y2 - y1) * (x - x1) / (x2 - x1) <= y - y1) {
                return 0;
            }
        }

        if (x == x1) {
            return (x1 < x2) ? 0 : -1;
        }

        if (x == x2) {
            return (x1 < x2) ? 1 : 0;
        }

        return (x1 < x2) ? 1 : -1;
    }

    public static int crossQuad(double x1, double y1, double cx, double cy, double x2, double y2, double x, double y) {
        if ((x < x1 && x < cx && x < x2) || (x > x1 && x > cx && x > x2) || (y > y1 && y > cy && y > y2) || (x1 == cx && cx == x2)) {

            return 0;
        }

        if (y < y1 && y < cy && y < y2 && x != x1 && x != x2) {
            if (x1 < x2) {
                return (x1 < x && x < x2) ? 1 : 0;
            }
            return (x2 < x && x < x1) ? -1 : 0;
        }

        QuadCurve c = new QuadCurve(x1, y1, cx, cy, x2, y2);
        double px = x - x1;
        double py = y - y1;
        double[] res = new double[3];
        int rc = c.solvePoint(res, px);

        return c.cross(res, rc, py, py);
    }

    public static int crossCubic(double x1, double y1, double cx1, double cy1, double cx2, double cy2, double x2, double y2, double x, double y) {
        if ((x < x1 && x < cx1 && x < cx2 && x < x2) || (x > x1 && x > cx1 && x > cx2 && x > x2) || (y > y1 && y > cy1 && y > cy2 && y > y2) || (x1 == cx1 && cx1 == cx2 && cx2 == x2)) {

            return 0;
        }

        if (y < y1 && y < cy1 && y < cy2 && y < y2 && x != x1 && x != x2) {
            if (x1 < x2) {
                return (x1 < x && x < x2) ? 1 : 0;
            }
            return (x2 < x && x < x1) ? -1 : 0;
        }

        CubicCurve c = new CubicCurve(x1, y1, cx1, cy1, cx2, cy2, x2, y2);
        double px = x - x1;
        double py = y - y1;
        double[] res = new double[3];
        int rc = c.solvePoint(res, px);
        return c.cross(res, rc, py, py);
    }

    public static int crossPath(PathIterator p, double x, double y) {
        int cross = 0;

        double cy = 0.0D, cx = cy, my = cx, mx = my;
        double[] coords = new double[6];

        while (!p.isDone()) {
            switch (p.currentSegment(coords)) {
                case 0:
                    if (cx != mx || cy != my) {
                        cross += crossLine(cx, cy, mx, my, x, y);
                    }
                    mx = cx = coords[0];
                    my = cy = coords[1];

                case 1:
                    cross += crossLine(cx, cy, cx = coords[0], cy = coords[1], x, y);

                case 2:
                    cross += crossQuad(cx, cy, coords[0], coords[1], cx = coords[2], cy = coords[3], x, y);

                case 3:
                    cross += crossCubic(cx, cy, coords[0], coords[1], coords[2], coords[3], cx = coords[4], cy = coords[5], x, y);

                case 4:
                    if (cy != my || cx != mx) {
                        cross += crossLine(cx, cy, cx = mx, cy = my, x, y);
                    }
                    break;
            }

            if (x == cx && y == cy) {
                cross = 0;
                cy = my;
                break;
            }
            p.next();
        }
        if (cy != my) {
            cross += crossLine(cx, cy, mx, my, x, y);
        }
        return cross;
    }

    public static int crossShape(Shape s, double x, double y) {
        if (!s.getBounds2D().contains(x, y)) {
            return 0;
        }
        return crossPath(s.getPathIterator(null), x, y);
    }

    public static boolean isZero(double val) {
        return (-1.0E-5D < val && val < 1.0E-5D);
    }

    static void sortBound(double[] bound, int bc) {
        for (int i = 0; i < bc - 4; i += 4) {
            int k = i;
            for (int j = i + 4; j < bc; j += 4) {
                if (bound[k] > bound[j]) {
                    k = j;
                }
            }
            if (k != i) {
                double tmp = bound[i];
                bound[i] = bound[k];
                bound[k] = tmp;
                tmp = bound[i + 1];
                bound[i + 1] = bound[k + 1];
                bound[k + 1] = tmp;
                tmp = bound[i + 2];
                bound[i + 2] = bound[k + 2];
                bound[k + 2] = tmp;
                tmp = bound[i + 3];
                bound[i + 3] = bound[k + 3];
                bound[k + 3] = tmp;
            }
        }
    }

    static int crossBound(double[] bound, int bc, double py1, double py2) {
        if (bc == 0) {
            return 0;
        }

        int up = 0;
        int down = 0;
        for (int i = 2; i < bc; i += 4) {
            if (bound[i] < py1) {
                up++;

            } else if (bound[i] > py2) {
                down++;
            } else {

                return 255;
            }
        }

        if (down == 0) {
            return 0;
        }

        if (up != 0) {

            sortBound(bound, bc);
            boolean sign = (bound[2] > py2);
            for (int j = 6; j < bc; j += 4) {
                boolean sign2 = (bound[j] > py2);
                if (sign != sign2 && bound[j + 1] != bound[j - 3]) {
                    return 255;
                }
                sign = sign2;
            }
        }
        return 254;
    }

    public static int intersectLine(double x1, double y1, double x2, double y2, double rx1, double ry1, double rx2, double ry2) {
        if ((rx2 < x1 && rx2 < x2) || (rx1 > x1 && rx1 > x2) || (ry1 > y1 && ry1 > y2)) {

            return 0;
        }

        if (ry2 >= y1 || ry2 >= y2) {
            double bx1, bx2;

            if (x1 == x2) {
                return 255;
            }

            if (x1 < x2) {
                bx1 = (x1 < rx1) ? rx1 : x1;
                bx2 = (x2 < rx2) ? x2 : rx2;
            } else {
                bx1 = (x2 < rx1) ? rx1 : x2;
                bx2 = (x1 < rx2) ? x1 : rx2;
            }
            double k = (y2 - y1) / (x2 - x1);
            double by1 = k * (bx1 - x1) + y1;
            double by2 = k * (bx2 - x1) + y1;

            if (by1 < ry1 && by2 < ry1) {
                return 0;
            }

            if (by1 <= ry2 || by2 <= ry2) {
                return 255;
            }
        }

        if (x1 == x2) {
            return 0;
        }

        if (rx1 == x1) {
            return (x1 < x2) ? 0 : -1;
        }

        if (rx1 == x2) {
            return (x1 < x2) ? 1 : 0;
        }

        if (x1 < x2) {
            return (x1 < rx1 && rx1 < x2) ? 1 : 0;
        }
        return (x2 < rx1 && rx1 < x1) ? -1 : 0;
    }

    public static int intersectQuad(double x1, double y1, double cx, double cy, double x2, double y2, double rx1, double ry1, double rx2, double ry2) {
        if ((rx2 < x1 && rx2 < cx && rx2 < x2) || (rx1 > x1 && rx1 > cx && rx1 > x2) || (ry1 > y1 && ry1 > cy && ry1 > y2)) {

            return 0;
        }

        if (ry2 < y1 && ry2 < cy && ry2 < y2 && rx1 != x1 && rx1 != x2) {
            if (x1 < x2) {
                return (x1 < rx1 && rx1 < x2) ? 1 : 0;
            }
            return (x2 < rx1 && rx1 < x1) ? -1 : 0;
        }

        QuadCurve c = new QuadCurve(x1, y1, cx, cy, x2, y2);
        double px1 = rx1 - x1;
        double py1 = ry1 - y1;
        double px2 = rx2 - x1;
        double py2 = ry2 - y1;

        double[] res1 = new double[3];
        double[] res2 = new double[3];
        int rc1 = c.solvePoint(res1, px1);
        int rc2 = c.solvePoint(res2, px2);

        if (rc1 == 0 && rc2 == 0) {
            return 0;
        }

        double minX = px1 - 1.0E-5D;
        double maxX = px2 + 1.0E-5D;
        double[] bound = new double[28];
        int bc = 0;

        bc = c.addBound(bound, bc, res1, rc1, minX, maxX, false, 0);
        bc = c.addBound(bound, bc, res2, rc2, minX, maxX, false, 1);

        rc2 = c.solveExtrem(res2);
        bc = c.addBound(bound, bc, res2, rc2, minX, maxX, true, 2);

        if (rx1 < x1 && x1 < rx2) {
            bound[bc++] = 0.0D;
            bound[bc++] = 0.0D;
            bound[bc++] = 0.0D;
            bound[bc++] = 4.0D;
        }
        if (rx1 < x2 && x2 < rx2) {
            bound[bc++] = 1.0D;
            bound[bc++] = c.ax;
            bound[bc++] = c.ay;
            bound[bc++] = 5.0D;
        }

        int cross = crossBound(bound, bc, py1, py2);
        if (cross != 254) {
            return cross;
        }
        return c.cross(res1, rc1, py1, py2);
    }

    public static int intersectCubic(double x1, double y1, double cx1, double cy1, double cx2, double cy2, double x2, double y2, double rx1, double ry1, double rx2, double ry2) {
        if ((rx2 < x1 && rx2 < cx1 && rx2 < cx2 && rx2 < x2) || (rx1 > x1 && rx1 > cx1 && rx1 > cx2 && rx1 > x2) || (ry1 > y1 && ry1 > cy1 && ry1 > cy2 && ry1 > y2)) {

            return 0;
        }

        if (ry2 < y1 && ry2 < cy1 && ry2 < cy2 && ry2 < y2 && rx1 != x1 && rx1 != x2) {
            if (x1 < x2) {
                return (x1 < rx1 && rx1 < x2) ? 1 : 0;
            }
            return (x2 < rx1 && rx1 < x1) ? -1 : 0;
        }

        CubicCurve c = new CubicCurve(x1, y1, cx1, cy1, cx2, cy2, x2, y2);
        double px1 = rx1 - x1;
        double py1 = ry1 - y1;
        double px2 = rx2 - x1;
        double py2 = ry2 - y1;

        double[] res1 = new double[3];
        double[] res2 = new double[3];
        int rc1 = c.solvePoint(res1, px1);
        int rc2 = c.solvePoint(res2, px2);

        if (rc1 == 0 && rc2 == 0) {
            return 0;
        }

        double minX = px1 - 1.0E-5D;
        double maxX = px2 + 1.0E-5D;

        double[] bound = new double[40];
        int bc = 0;

        bc = c.addBound(bound, bc, res1, rc1, minX, maxX, false, 0);
        bc = c.addBound(bound, bc, res2, rc2, minX, maxX, false, 1);

        rc2 = c.solveExtremX(res2);
        bc = c.addBound(bound, bc, res2, rc2, minX, maxX, true, 2);
        rc2 = c.solveExtremY(res2);
        bc = c.addBound(bound, bc, res2, rc2, minX, maxX, true, 4);

        if (rx1 < x1 && x1 < rx2) {
            bound[bc++] = 0.0D;
            bound[bc++] = 0.0D;
            bound[bc++] = 0.0D;
            bound[bc++] = 6.0D;
        }
        if (rx1 < x2 && x2 < rx2) {
            bound[bc++] = 1.0D;
            bound[bc++] = c.ax;
            bound[bc++] = c.ay;
            bound[bc++] = 7.0D;
        }

        int cross = crossBound(bound, bc, py1, py2);
        if (cross != 254) {
            return cross;
        }
        return c.cross(res1, rc1, py1, py2);
    }

    public static int intersectPath(PathIterator p, double x, double y, double w, double h) {
        int cross = 0;

        double cy = 0.0D, cx = cy, my = cx, mx = my;
        double[] coords = new double[6];

        double rx1 = x;
        double ry1 = y;
        double rx2 = x + w;
        double ry2 = y + h;

        while (!p.isDone()) {
            int count = 0;
            switch (p.currentSegment(coords)) {
                case 0:
                    if (cx != mx || cy != my) {
                        count = intersectLine(cx, cy, mx, my, rx1, ry1, rx2, ry2);
                    }
                    mx = cx = coords[0];
                    my = cy = coords[1];

                case 1:
                    count = intersectLine(cx, cy, cx = coords[0], cy = coords[1], rx1, ry1, rx2, ry2);

                case 2:
                    count = intersectQuad(cx, cy, coords[0], coords[1], cx = coords[2], cy = coords[3], rx1, ry1, rx2, ry2);

                case 3:
                    count = intersectCubic(cx, cy, coords[0], coords[1], coords[2], coords[3], cx = coords[4], cy = coords[5], rx1, ry1, rx2, ry2);

                case 4:
                    if (cy != my || cx != mx) {
                        count = intersectLine(cx, cy, mx, my, rx1, ry1, rx2, ry2);
                    }
                    cx = mx;
                    cy = my;
                    break;
            }
            if (count == 255) {
                return 255;
            }
            cross += count;
            p.next();
        }
        if (cy != my) {
            int count = intersectLine(cx, cy, mx, my, rx1, ry1, rx2, ry2);
            if (count == 255) {
                return 255;
            }
            cross += count;
        }
        return cross;
    }

    public static int intersectShape(Shape s, double x, double y, double w, double h) {
        if (!s.getBounds2D().intersects(x, y, w, h)) {
            return 0;
        }
        return intersectPath(s.getPathIterator(null), x, y, w, h);
    }

    public static boolean isInsideNonZero(int cross) {
        return (cross != 0);
    }

    public static boolean isInsideEvenOdd(int cross) {
        return ((cross & 0x1) != 0);
    }
}
