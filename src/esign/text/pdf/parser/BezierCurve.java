package esign.text.pdf.parser;

import esign.text.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class BezierCurve
        implements Shape {

    public static double curveCollinearityEpsilon = 1.0E-30D;

    public static double distanceToleranceSquare = 0.025D;

    public static double distanceToleranceManhattan = 0.4D;

    private final List<Point2D> controlPoints;

    public BezierCurve(List<Point2D> controlPoints) {
        this.controlPoints = new ArrayList<Point2D>(controlPoints);
    }

    public List<Point2D> getBasePoints() {
        return this.controlPoints;
    }

    public List<Point2D> getPiecewiseLinearApproximation() {
        List<Point2D> points = new ArrayList<Point2D>();
        points.add(this.controlPoints.get(0));

        recursiveApproximation(((Point2D) this.controlPoints.get(0)).getX(), ((Point2D) this.controlPoints.get(0)).getY(), ((Point2D) this.controlPoints
                .get(1)).getX(), ((Point2D) this.controlPoints.get(1)).getY(), ((Point2D) this.controlPoints
                .get(2)).getX(), ((Point2D) this.controlPoints.get(2)).getY(), ((Point2D) this.controlPoints
                .get(3)).getX(), ((Point2D) this.controlPoints.get(3)).getY(), points);

        points.add(this.controlPoints.get(this.controlPoints.size() - 1));
        return points;
    }

    private void recursiveApproximation(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4, List<Point2D> points) {
        double x12 = (x1 + x2) / 2.0D;
        double y12 = (y1 + y2) / 2.0D;
        double x23 = (x2 + x3) / 2.0D;
        double y23 = (y2 + y3) / 2.0D;
        double x34 = (x3 + x4) / 2.0D;
        double y34 = (y3 + y4) / 2.0D;
        double x123 = (x12 + x23) / 2.0D;
        double y123 = (y12 + y23) / 2.0D;
        double x234 = (x23 + x34) / 2.0D;
        double y234 = (y23 + y34) / 2.0D;
        double x1234 = (x123 + x234) / 2.0D;
        double y1234 = (y123 + y234) / 2.0D;

        double dx = x4 - x1;
        double dy = y4 - y1;

        double d2 = Math.abs((x2 - x4) * dy - (y2 - y4) * dx);

        double d3 = Math.abs((x3 - x4) * dy - (y3 - y4) * dx);

        if (d2 > curveCollinearityEpsilon || d3 > curveCollinearityEpsilon) {

            if ((d2 + d3) * (d2 + d3) <= distanceToleranceSquare * (dx * dx + dy * dy)) {
                points.add(new Point2D.Double(x1234, y1234));

                return;
            }
        } else if (Math.abs(x1 + x3 - x2 - x2) + Math.abs(y1 + y3 - y2 - y2)
                + Math.abs(x2 + x4 - x3 - x3) + Math.abs(y2 + y4 - y3 - y3) <= distanceToleranceManhattan) {
            points.add(new Point2D.Double(x1234, y1234));

            return;
        }

        recursiveApproximation(x1, y1, x12, y12, x123, y123, x1234, y1234, points);
        recursiveApproximation(x1234, y1234, x234, y234, x34, y34, x4, y4, points);
    }
}
