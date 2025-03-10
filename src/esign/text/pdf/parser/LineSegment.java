package esign.text.pdf.parser;

import esign.text.awt.geom.Rectangle2D;

public class LineSegment {

    private final Vector startPoint;
    private final Vector endPoint;

    public LineSegment(Vector startPoint, Vector endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    public Vector getStartPoint() {
        return this.startPoint;
    }

    public Vector getEndPoint() {
        return this.endPoint;
    }

    public float getLength() {
        return this.endPoint.subtract(this.startPoint).length();
    }

    public Rectangle2D.Float getBoundingRectange() {
        float x1 = getStartPoint().get(0);
        float y1 = getStartPoint().get(1);
        float x2 = getEndPoint().get(0);
        float y2 = getEndPoint().get(1);
        return new Rectangle2D.Float(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1), Math.abs(y2 - y1));
    }

    public LineSegment transformBy(Matrix m) {
        Vector newStart = this.startPoint.cross(m);
        Vector newEnd = this.endPoint.cross(m);
        return new LineSegment(newStart, newEnd);
    }
}
