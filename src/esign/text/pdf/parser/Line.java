package esign.text.pdf.parser;

import esign.text.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class Line
        implements Shape {

    private final Point2D p1;
    private final Point2D p2;

    public Line() {
        this(0.0F, 0.0F, 0.0F, 0.0F);
    }

    public Line(float x1, float y1, float x2, float y2) {
        this.p1 = (Point2D) new Point2D.Float(x1, y1);
        this.p2 = (Point2D) new Point2D.Float(x2, y2);
    }

    public Line(Point2D p1, Point2D p2) {
        this((float) p1.getX(), (float) p1.getY(), (float) p2.getX(), (float) p2.getY());
    }

    public List<Point2D> getBasePoints() {
        List<Point2D> basePoints = new ArrayList<Point2D>(2);
        basePoints.add(this.p1);
        basePoints.add(this.p2);

        return basePoints;
    }
}
