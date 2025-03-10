package esign.text.pdf.parser;

import esign.text.awt.geom.Point2D;
import java.util.List;

public interface Shape {
  List<Point2D> getBasePoints();
}
