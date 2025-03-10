package esign.text.pdf.fonts.otf;

import esign.text.pdf.RandomAccessFileOrArray;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class GlyphPositioningTableReader
        extends OpenTypeFontTableReader {

    public GlyphPositioningTableReader(RandomAccessFileOrArray rf, int gposTableLocation) throws IOException {
        super(rf, gposTableLocation);
    }

    public void read() throws FontReadingException {
        startReadingTable();
    }

    protected void readSubTable(int lookupType, int subTableLocation) throws IOException {
        if (lookupType == 1) {
            readLookUpType_1(subTableLocation);
        } else if (lookupType == 4) {
            readLookUpType_4(subTableLocation);
        } else if (lookupType == 8) {
            readLookUpType_8(subTableLocation);
        } else {
            System.err.println("The lookupType " + lookupType + " is not yet supported by " + GlyphPositioningTableReader.class.getSimpleName());
        }
    }

    private void readLookUpType_1(int lookupTableLocation) throws IOException {
        this.rf.seek(lookupTableLocation);
        int posFormat = this.rf.readShort();

        if (posFormat == 1) {
            LOG.debug("Reading `Look Up Type 1, Format 1` ....");
            int coverageOffset = this.rf.readShort();
            int valueFormat = this.rf.readShort();

            if ((valueFormat & 0x1) == 1) {
                int xPlacement = this.rf.readShort();
                LOG.debug("xPlacement=" + xPlacement);
            }

            if ((valueFormat & 0x2) == 2) {
                int yPlacement = this.rf.readShort();
                LOG.debug("yPlacement=" + yPlacement);
            }

            List<Integer> glyphCodes = readCoverageFormat(lookupTableLocation + coverageOffset);

            LOG.debug("glyphCodes=" + glyphCodes);
        } else {
            System.err.println("The PosFormat " + posFormat + " for `LookupType 1` is not yet supported by " + GlyphPositioningTableReader.class.getSimpleName());
        }
    }

    private void readLookUpType_4(int lookupTableLocation) throws IOException {
        this.rf.seek(lookupTableLocation);

        int posFormat = this.rf.readShort();

        if (posFormat == 1) {

            LOG.debug("Reading `Look Up Type 4, Format 1` ....");

            int markCoverageOffset = this.rf.readShort();
            int baseCoverageOffset = this.rf.readShort();
            int classCount = this.rf.readShort();
            int markArrayOffset = this.rf.readShort();
            int baseArrayOffset = this.rf.readShort();

            List<Integer> markCoverages = readCoverageFormat(lookupTableLocation + markCoverageOffset);
            LOG.debug("markCoverages=" + markCoverages);

            List<Integer> baseCoverages = readCoverageFormat(lookupTableLocation + baseCoverageOffset);
            LOG.debug("baseCoverages=" + baseCoverages);

            readMarkArrayTable(lookupTableLocation + markArrayOffset);

            readBaseArrayTable(lookupTableLocation + baseArrayOffset, classCount);
        } else {
            System.err.println("The posFormat " + posFormat + " is not supported by " + GlyphPositioningTableReader.class.getSimpleName());
        }
    }

    private void readLookUpType_8(int lookupTableLocation) throws IOException {
        this.rf.seek(lookupTableLocation);

        int posFormat = this.rf.readShort();

        if (posFormat == 3) {
            LOG.debug("Reading `Look Up Type 8, Format 3` ....");
            readChainingContextPositioningFormat_3(lookupTableLocation);
        } else {
            System.err.println("The posFormat " + posFormat + " for `Look Up Type 8` is not supported by " + GlyphPositioningTableReader.class.getSimpleName());
        }
    }

    private void readChainingContextPositioningFormat_3(int lookupTableLocation) throws IOException {
        int backtrackGlyphCount = this.rf.readShort();
        LOG.debug("backtrackGlyphCount=" + backtrackGlyphCount);
        List<Integer> backtrackGlyphOffsets = new ArrayList<Integer>(backtrackGlyphCount);

        for (int i = 0; i < backtrackGlyphCount; i++) {
            int backtrackGlyphOffset = this.rf.readShort();
            backtrackGlyphOffsets.add(Integer.valueOf(backtrackGlyphOffset));
        }

        int inputGlyphCount = this.rf.readShort();
        LOG.debug("inputGlyphCount=" + inputGlyphCount);
        List<Integer> inputGlyphOffsets = new ArrayList<Integer>(inputGlyphCount);

        for (int j = 0; j < inputGlyphCount; j++) {
            int inputGlyphOffset = this.rf.readShort();
            inputGlyphOffsets.add(Integer.valueOf(inputGlyphOffset));
        }

        int lookaheadGlyphCount = this.rf.readShort();
        LOG.debug("lookaheadGlyphCount=" + lookaheadGlyphCount);
        List<Integer> lookaheadGlyphOffsets = new ArrayList<Integer>(lookaheadGlyphCount);

        for (int k = 0; k < lookaheadGlyphCount; k++) {
            int lookaheadGlyphOffset = this.rf.readShort();
            lookaheadGlyphOffsets.add(Integer.valueOf(lookaheadGlyphOffset));
        }

        int posCount = this.rf.readShort();
        LOG.debug("posCount=" + posCount);

        List<PosLookupRecord> posLookupRecords = new ArrayList<PosLookupRecord>(posCount);

        for (int m = 0; m < posCount; m++) {
            int sequenceIndex = this.rf.readShort();
            int lookupListIndex = this.rf.readShort();
            LOG.debug("sequenceIndex=" + sequenceIndex + ", lookupListIndex=" + lookupListIndex);
            posLookupRecords.add(new PosLookupRecord(sequenceIndex, lookupListIndex));
        }
        Iterator<Integer> iterator;
        for (iterator = backtrackGlyphOffsets.iterator(); iterator.hasNext();) {
            int backtrackGlyphOffset = ((Integer) iterator.next()).intValue();
            List<Integer> backtrackGlyphs = readCoverageFormat(lookupTableLocation + backtrackGlyphOffset);
            LOG.debug("backtrackGlyphs=" + backtrackGlyphs);
        }

        for (iterator = inputGlyphOffsets.iterator(); iterator.hasNext();) {
            int inputGlyphOffset = ((Integer) iterator.next()).intValue();
            List<Integer> inputGlyphs = readCoverageFormat(lookupTableLocation + inputGlyphOffset);
            LOG.debug("inputGlyphs=" + inputGlyphs);
        }

        for (iterator = lookaheadGlyphOffsets.iterator(); iterator.hasNext();) {
            int lookaheadGlyphOffset = ((Integer) iterator.next()).intValue();
            List<Integer> lookaheadGlyphs = readCoverageFormat(lookupTableLocation + lookaheadGlyphOffset);
            LOG.debug("lookaheadGlyphs=" + lookaheadGlyphs);
        }

    }

    private void readMarkArrayTable(int markArrayLocation) throws IOException {
        this.rf.seek(markArrayLocation);
        int markCount = this.rf.readShort();
        List<MarkRecord> markRecords = new ArrayList<MarkRecord>();

        for (int i = 0; i < markCount; i++) {
            markRecords.add(readMarkRecord());
        }

        for (MarkRecord markRecord : markRecords) {
            readAnchorTable(markArrayLocation + markRecord.markAnchorOffset);
        }
    }

    private MarkRecord readMarkRecord() throws IOException {
        int markClass = this.rf.readShort();
        int markAnchorOffset = this.rf.readShort();
        return new MarkRecord(markClass, markAnchorOffset);
    }

    private void readAnchorTable(int anchorTableLocation) throws IOException {
        this.rf.seek(anchorTableLocation);
        int anchorFormat = this.rf.readShort();

        if (anchorFormat != 1) {
            System.err.println("The extra features of the AnchorFormat " + anchorFormat + " will not be used");
        }

        int x = this.rf.readShort();
        int y = this.rf.readShort();
    }

    private void readBaseArrayTable(int baseArrayTableLocation, int classCount) throws IOException {
        this.rf.seek(baseArrayTableLocation);
        int baseCount = this.rf.readShort();
        Set<Integer> baseAnchors = new HashSet<Integer>();

        for (int i = 0; i < baseCount; i++) {

            for (int k = 0; k < classCount; k++) {
                int baseAnchor = this.rf.readShort();
                baseAnchors.add(Integer.valueOf(baseAnchor));
            }
        }

        for (Iterator<Integer> iterator = baseAnchors.iterator(); iterator.hasNext();) {
            int baseAnchor = ((Integer) iterator.next()).intValue();
            readAnchorTable(baseArrayTableLocation + baseAnchor);
        }

    }

    static class MarkRecord {

        final int markClass;
        final int markAnchorOffset;

        public MarkRecord(int markClass, int markAnchorOffset) {
            this.markClass = markClass;
            this.markAnchorOffset = markAnchorOffset;
        }
    }

    static class PosLookupRecord {

        final int sequenceIndex;
        final int lookupListIndex;

        public PosLookupRecord(int sequenceIndex, int lookupListIndex) {
            this.sequenceIndex = sequenceIndex;
            this.lookupListIndex = lookupListIndex;
        }
    }
}
