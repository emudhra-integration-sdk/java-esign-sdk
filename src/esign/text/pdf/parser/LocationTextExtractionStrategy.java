package esign.text.pdf.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocationTextExtractionStrategy
        implements TextExtractionStrategy {

    static boolean DUMP_STATE = false;
    private final List<TextChunk> locationalResult = new ArrayList<TextChunk>();

    private final TextChunkLocationStrategy tclStrat;

    public LocationTextExtractionStrategy() {
        this(new TextChunkLocationStrategy() {
            public LocationTextExtractionStrategy.TextChunkLocation createLocation(TextRenderInfo renderInfo, LineSegment baseline) {
                return new LocationTextExtractionStrategy.TextChunkLocationDefaultImp(baseline.getStartPoint(), baseline.getEndPoint(), renderInfo.getSingleSpaceWidth());
            }
        });
    }

    public LocationTextExtractionStrategy(TextChunkLocationStrategy strat) {
        this.tclStrat = strat;
    }

    public void beginTextBlock() {
    }

    public void endTextBlock() {
    }

    private boolean startsWithSpace(String str) {
        if (str.length() == 0) {
            return false;
        }
        return (str.charAt(0) == ' ');
    }

    private boolean endsWithSpace(String str) {
        if (str.length() == 0) {
            return false;
        }
        return (str.charAt(str.length() - 1) == ' ');
    }

    private List<TextChunk> filterTextChunks(List<TextChunk> textChunks, TextChunkFilter filter) {
        if (filter == null) {
            return textChunks;
        }
        List<TextChunk> filtered = new ArrayList<TextChunk>();
        for (TextChunk textChunk : textChunks) {
            if (filter.accept(textChunk)) {
                filtered.add(textChunk);
            }
        }
        return filtered;
    }

    protected boolean isChunkAtWordBoundary(TextChunk chunk, TextChunk previousChunk) {
        return chunk.getLocation().isAtWordBoundary(previousChunk.getLocation());
    }

    public String getResultantText(TextChunkFilter chunkFilter) {
        if (DUMP_STATE) {
            dumpState();
        }

        List<TextChunk> filteredTextChunks = filterTextChunks(this.locationalResult, chunkFilter);
        Collections.sort(filteredTextChunks);

        StringBuilder sb = new StringBuilder();
        TextChunk lastChunk = null;
        for (TextChunk chunk : filteredTextChunks) {

            if (lastChunk == null) {
                sb.append(chunk.text);
            } else if (chunk.sameLine(lastChunk)) {

                if (isChunkAtWordBoundary(chunk, lastChunk) && !startsWithSpace(chunk.text) && !endsWithSpace(lastChunk.text)) {
                    sb.append(' ');
                }
                sb.append(chunk.text);
            } else {
                sb.append('\n');
                sb.append(chunk.text);
            }

            lastChunk = chunk;
        }

        return sb.toString();
    }

    public String getResultantText() {
        return getResultantText(null);
    }

    private void dumpState() {
        for (TextChunk location : this.locationalResult) {
            location.printDiagnostics();

            System.out.println();
        }
    }

    public void renderText(TextRenderInfo renderInfo) {
        LineSegment segment = renderInfo.getBaseline();
        if (renderInfo.getRise() != 0.0F) {
            Matrix riseOffsetTransform = new Matrix(0.0F, -renderInfo.getRise());
            segment = segment.transformBy(riseOffsetTransform);
        }
        TextChunk tc = new TextChunk(renderInfo.getText(), this.tclStrat.createLocation(renderInfo, segment));
        this.locationalResult.add(tc);
    }

    public static interface TextChunkLocationStrategy {

        LocationTextExtractionStrategy.TextChunkLocation createLocation(TextRenderInfo param1TextRenderInfo, LineSegment param1LineSegment);
    }

    public static interface TextChunkLocation
            extends Comparable<TextChunkLocation> {

        float distParallelEnd();

        float distParallelStart();

        int distPerpendicular();

        float getCharSpaceWidth();

        Vector getEndLocation();

        Vector getStartLocation();

        int orientationMagnitude();

        boolean sameLine(TextChunkLocation param1TextChunkLocation);

        float distanceFromEndOf(TextChunkLocation param1TextChunkLocation);

        boolean isAtWordBoundary(TextChunkLocation param1TextChunkLocation);
    }

    public static class TextChunkLocationDefaultImp
            implements TextChunkLocation {

        private final Vector startLocation;
        private final Vector endLocation;
        private final Vector orientationVector;
        private final int orientationMagnitude;
        private final int distPerpendicular;
        private final float distParallelStart;
        private final float distParallelEnd;
        private final float charSpaceWidth;

        public TextChunkLocationDefaultImp(Vector startLocation, Vector endLocation, float charSpaceWidth) {
            this.startLocation = startLocation;
            this.endLocation = endLocation;
            this.charSpaceWidth = charSpaceWidth;

            Vector oVector = endLocation.subtract(startLocation);
            if (oVector.length() == 0.0F) {
                oVector = new Vector(1.0F, 0.0F, 0.0F);
            }
            this.orientationVector = oVector.normalize();
            this.orientationMagnitude = (int) (Math.atan2(this.orientationVector.get(1), this.orientationVector.get(0)) * 1000.0D);

            Vector origin = new Vector(0.0F, 0.0F, 1.0F);
            this.distPerpendicular = (int) startLocation.subtract(origin).cross(this.orientationVector).get(2);

            this.distParallelStart = this.orientationVector.dot(startLocation);
            this.distParallelEnd = this.orientationVector.dot(endLocation);
        }

        public int orientationMagnitude() {
            return this.orientationMagnitude;
        }

        public int distPerpendicular() {
            return this.distPerpendicular;
        }

        public float distParallelStart() {
            return this.distParallelStart;
        }

        public float distParallelEnd() {
            return this.distParallelEnd;
        }

        public Vector getStartLocation() {
            return this.startLocation;
        }

        public Vector getEndLocation() {
            return this.endLocation;
        }

        public float getCharSpaceWidth() {
            return this.charSpaceWidth;
        }

        public boolean sameLine(LocationTextExtractionStrategy.TextChunkLocation as) {
            return (orientationMagnitude() == as.orientationMagnitude() && distPerpendicular() == as.distPerpendicular());
        }

        public float distanceFromEndOf(LocationTextExtractionStrategy.TextChunkLocation other) {
            float distance = distParallelStart() - other.distParallelEnd();
            return distance;
        }

        public boolean isAtWordBoundary(LocationTextExtractionStrategy.TextChunkLocation previous) {
            if (getCharSpaceWidth() < 0.1F) {
                return false;
            }
            float dist = distanceFromEndOf(previous);

            return (dist < -getCharSpaceWidth() || dist > getCharSpaceWidth() / 2.0F);
        }

        public int compareTo(LocationTextExtractionStrategy.TextChunkLocation other) {
            if (this == other) {
                return 0;
            }

            int rslt = LocationTextExtractionStrategy.compareInts(orientationMagnitude(), other.orientationMagnitude());
            if (rslt != 0) {
                return rslt;
            }

            rslt = LocationTextExtractionStrategy.compareInts(distPerpendicular(), other.distPerpendicular());
            if (rslt != 0) {
                return rslt;
            }

            return Float.compare(distParallelStart(), other.distParallelStart());
        }
    }

    public static class TextChunk
            implements Comparable<TextChunk> {

        private final String text;
        private final LocationTextExtractionStrategy.TextChunkLocation location;

        public TextChunk(String string, Vector startLocation, Vector endLocation, float charSpaceWidth) {
            this(string, new LocationTextExtractionStrategy.TextChunkLocationDefaultImp(startLocation, endLocation, charSpaceWidth));
        }

        public TextChunk(String string, LocationTextExtractionStrategy.TextChunkLocation loc) {
            this.text = string;
            this.location = loc;
        }

        public String getText() {
            return this.text;
        }

        public LocationTextExtractionStrategy.TextChunkLocation getLocation() {
            return this.location;
        }

        public Vector getStartLocation() {
            return this.location.getStartLocation();
        }

        public Vector getEndLocation() {
            return this.location.getEndLocation();
        }

        public float getCharSpaceWidth() {
            return this.location.getCharSpaceWidth();
        }

        public float distanceFromEndOf(TextChunk other) {
            return this.location.distanceFromEndOf(other.location);
        }

        private void printDiagnostics() {
            System.out.println("Text (@" + this.location.getStartLocation() + " -> " + this.location.getEndLocation() + "): " + this.text);
            System.out.println("orientationMagnitude: " + this.location.orientationMagnitude());
            System.out.println("distPerpendicular: " + this.location.distPerpendicular());
            System.out.println("distParallel: " + this.location.distParallelStart());
        }

        public int compareTo(TextChunk rhs) {
            return this.location.compareTo(rhs.location);
        }

        private boolean sameLine(TextChunk lastChunk) {
            return getLocation().sameLine(lastChunk.getLocation());
        }
    }

    private static int compareInts(int int1, int int2) {
        return (int1 == int2) ? 0 : ((int1 < int2) ? -1 : 1);
    }

    public void renderImage(ImageRenderInfo renderInfo) {
    }

    public static interface TextChunkFilter {

        boolean accept(LocationTextExtractionStrategy.TextChunk param1TextChunk);
    }
}
