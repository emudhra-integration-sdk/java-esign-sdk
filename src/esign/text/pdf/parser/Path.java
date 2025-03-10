package esign.text.pdf.parser;

import esign.text.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Path {

    private static final String START_PATH_ERR_MSG = "Path shall start with \"re\" or \"m\" operator";
    private List<Subpath> subpaths = new ArrayList<Subpath>();

    private Point2D currentPoint;

    public Path() {
    }

    public Path(List<? extends Subpath> subpaths) {
        addSubpaths(subpaths);
    }

    public List<Subpath> getSubpaths() {
        return this.subpaths;
    }

    public void addSubpath(Subpath subpath) {
        this.subpaths.add(subpath);
        this.currentPoint = subpath.getLastPoint();
    }

    public void addSubpaths(List<? extends Subpath> subpaths) {
        if (subpaths.size() > 0) {
            this.subpaths.addAll(subpaths);
            this.currentPoint = ((Subpath) this.subpaths.get(subpaths.size() - 1)).getLastPoint();
        }
    }

    public Point2D getCurrentPoint() {
        return this.currentPoint;
    }

    public void moveTo(float x, float y) {
        this.currentPoint = (Point2D) new Point2D.Float(x, y);
        Subpath lastSubpath = getLastSubpath();

        if (lastSubpath != null && lastSubpath.isSinglePointOpen()) {
            lastSubpath.setStartPoint(this.currentPoint);
        } else {
            this.subpaths.add(new Subpath(this.currentPoint));
        }
    }

    public void lineTo(float x, float y) {
        if (this.currentPoint == null) {
            throw new RuntimeException("Path shall start with \"re\" or \"m\" operator");
        }

        Point2D.Float float_ = new Point2D.Float(x, y);
        getLastSubpath().addSegment(new Line(this.currentPoint, (Point2D) float_));
        this.currentPoint = (Point2D) float_;
    }

    public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3) {
        if (this.currentPoint == null) {
            throw new RuntimeException("Path shall start with \"re\" or \"m\" operator");
        }

        Point2D.Float float_1 = new Point2D.Float(x1, y1);
        Point2D.Float float_2 = new Point2D.Float(x2, y2);
        Point2D.Float float_3 = new Point2D.Float(x3, y3);

        List<Point2D> controlPoints = new ArrayList<Point2D>(Arrays.asList(new Point2D[]{this.currentPoint, (Point2D) float_1, (Point2D) float_2, (Point2D) float_3}));
        getLastSubpath().addSegment(new BezierCurve(controlPoints));

        this.currentPoint = (Point2D) float_3;
    }

    public void curveTo(float x2, float y2, float x3, float y3) {
        if (this.currentPoint == null) {
            throw new RuntimeException("Path shall start with \"re\" or \"m\" operator");
        }

        curveTo((float) this.currentPoint.getX(), (float) this.currentPoint.getY(), x2, y2, x3, y3);
    }

    public void curveFromTo(float x1, float y1, float x3, float y3) {
        if (this.currentPoint == null) {
            throw new RuntimeException("Path shall start with \"re\" or \"m\" operator");
        }

        curveTo(x1, y1, x3, y3, x3, y3);
    }

    public void rectangle(float x, float y, float w, float h) {
        moveTo(x, y);
        lineTo(x + w, y);
        lineTo(x + w, y + h);
        lineTo(x, y + h);
        closeSubpath();
    }

    public void closeSubpath() {
        Subpath lastSubpath = getLastSubpath();
        lastSubpath.setClosed(true);

        Point2D startPoint = lastSubpath.getStartPoint();
        moveTo((float) startPoint.getX(), (float) startPoint.getY());
    }

    public void closeAllSubpaths() {
        for (Subpath subpath : this.subpaths) {
            subpath.setClosed(true);
        }
    }

    public List<Integer> replaceCloseWithLine() {
        List<Integer> modifiedSubpathsIndices = new ArrayList<Integer>();
        int i = 0;

        for (Subpath subpath : this.subpaths) {
            if (subpath.isClosed()) {
                subpath.setClosed(false);
                subpath.addSegment(new Line(subpath.getLastPoint(), subpath.getStartPoint()));
                modifiedSubpathsIndices.add(Integer.valueOf(i));
            }

            i++;
        }

        return modifiedSubpathsIndices;
    }

    public boolean isEmpty() {
        return (this.subpaths.size() == 0);
    }

    private Subpath getLastSubpath() {
        return (this.subpaths.size() > 0) ? this.subpaths.get(this.subpaths.size() - 1) : null;
    }
}
