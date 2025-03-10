package esign.text.pdf.parser;

import esign.text.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Subpath {

    private Point2D startPoint;
    private List<Shape> segments = new ArrayList<Shape>();

    private boolean closed;

    public Subpath() {
    }

    public Subpath(Subpath subpath) {
        this.startPoint = subpath.startPoint;
        this.segments.addAll(subpath.getSegments());
        this.closed = subpath.closed;
    }

    public Subpath(Point2D startPoint) {
        this((float) startPoint.getX(), (float) startPoint.getY());
    }

    public Subpath(float startPointX, float startPointY) {
        this.startPoint = (Point2D) new Point2D.Float(startPointX, startPointY);
    }

    public void setStartPoint(Point2D startPoint) {
        setStartPoint((float) startPoint.getX(), (float) startPoint.getY());
    }

    public void setStartPoint(float x, float y) {
        this.startPoint = (Point2D) new Point2D.Float(x, y);
    }

    public Point2D getStartPoint() {
        return this.startPoint;
    }

    public Point2D getLastPoint() {
        Point2D lastPoint = this.startPoint;

        if (this.segments.size() > 0 && !this.closed) {
            Shape shape = this.segments.get(this.segments.size() - 1);
            lastPoint = shape.getBasePoints().get(shape.getBasePoints().size() - 1);
        }

        return lastPoint;
    }

    public void addSegment(Shape segment) {
        if (this.closed) {
            return;
        }

        if (isSinglePointOpen()) {
            this.startPoint = segment.getBasePoints().get(0);
        }

        this.segments.add(segment);
    }

    public List<Shape> getSegments() {
        return this.segments;
    }

    public boolean isEmpty() {
        return (this.startPoint == null);
    }

    public boolean isSinglePointOpen() {
        return (this.segments.size() == 0 && !this.closed);
    }

    public boolean isSinglePointClosed() {
        return (this.segments.size() == 0 && this.closed);
    }

    public boolean isClosed() {
        return this.closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public boolean isDegenerate() {
        if (this.segments.size() > 0 && this.closed) {
            return false;
        }

        for (Shape segment : this.segments) {
            Set<Point2D> points = new HashSet<Point2D>(segment.getBasePoints());

            if (points.size() != 1) {
                return false;
            }
        }

        return (this.segments.size() > 0 || this.closed);
    }

    public List<Point2D> getPiecewiseLinearApproximation() {
        List<Point2D> result = new ArrayList<Point2D>();

        if (this.segments.size() == 0) {
            return result;
        }

        if (this.segments.get(0) instanceof BezierCurve) {
            result.addAll(((BezierCurve) this.segments.get(0)).getPiecewiseLinearApproximation());
        } else {
            result.addAll(((Shape) this.segments.get(0)).getBasePoints());
        }

        for (int i = 1; i < this.segments.size(); i++) {
            List<Point2D> segApprox;

            if (this.segments.get(i) instanceof BezierCurve) {
                segApprox = ((BezierCurve) this.segments.get(i)).getPiecewiseLinearApproximation();
                segApprox = segApprox.subList(1, segApprox.size());
            } else {
                segApprox = ((Shape) this.segments.get(i)).getBasePoints();
                segApprox = segApprox.subList(1, segApprox.size());
            }

            result.addAll(segApprox);
        }

        return result;
    }
}
