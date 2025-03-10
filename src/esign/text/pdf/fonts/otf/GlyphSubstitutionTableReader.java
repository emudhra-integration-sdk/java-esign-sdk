package esign.text.pdf.fonts.otf;

import esign.text.pdf.Glyph;
import esign.text.pdf.RandomAccessFileOrArray;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GlyphSubstitutionTableReader
        extends OpenTypeFontTableReader {

    private final int[] glyphWidthsByIndex;
    private final Map<Integer, Character> glyphToCharacterMap;
    private Map<Integer, List<Integer>> rawLigatureSubstitutionMap;

    public GlyphSubstitutionTableReader(RandomAccessFileOrArray rf, int gsubTableLocation, Map<Integer, Character> glyphToCharacterMap, int[] glyphWidthsByIndex) throws IOException {
        super(rf, gsubTableLocation);
        this.glyphWidthsByIndex = glyphWidthsByIndex;
        this.glyphToCharacterMap = glyphToCharacterMap;
    }

    public void read() throws FontReadingException {
        this.rawLigatureSubstitutionMap = new LinkedHashMap<Integer, List<Integer>>();
        startReadingTable();
    }

    public Map<String, Glyph> getGlyphSubstitutionMap() throws FontReadingException {
        Map<String, Glyph> glyphSubstitutionMap = new LinkedHashMap<String, Glyph>();

        for (Integer glyphIdToReplace : this.rawLigatureSubstitutionMap.keySet()) {
            List<Integer> constituentGlyphs = this.rawLigatureSubstitutionMap.get(glyphIdToReplace);
            StringBuilder chars = new StringBuilder(constituentGlyphs.size());

            for (Integer constituentGlyphId : constituentGlyphs) {
                chars.append(getTextFromGlyph(constituentGlyphId.intValue(), this.glyphToCharacterMap));
            }

            Glyph glyph = new Glyph(glyphIdToReplace.intValue(), this.glyphWidthsByIndex[glyphIdToReplace.intValue()], chars.toString());

            glyphSubstitutionMap.put(glyph.chars, glyph);
        }

        return Collections.unmodifiableMap(glyphSubstitutionMap);
    }

    private String getTextFromGlyph(int glyphId, Map<Integer, Character> glyphToCharacterMap) throws FontReadingException {
        StringBuilder chars = new StringBuilder(1);

        Character c = glyphToCharacterMap.get(Integer.valueOf(glyphId));

        if (c == null) {

            List<Integer> constituentGlyphs = this.rawLigatureSubstitutionMap.get(Integer.valueOf(glyphId));

            if (constituentGlyphs == null || constituentGlyphs.isEmpty()) {
                throw new FontReadingException("No corresponding character or simple glyphs found for GlyphID=" + glyphId);
            }

            for (Iterator<Integer> iterator = constituentGlyphs.iterator(); iterator.hasNext();) {
                int constituentGlyphId = ((Integer) iterator.next()).intValue();
                chars.append(getTextFromGlyph(constituentGlyphId, glyphToCharacterMap));
            }

        } else {

            chars.append(c.charValue());
        }

        return chars.toString();
    }

    protected void readSubTable(int lookupType, int subTableLocation) throws IOException {
        if (lookupType == 1) {
            readSingleSubstitutionSubtable(subTableLocation);
        } else if (lookupType == 4) {
            readLigatureSubstitutionSubtable(subTableLocation);
        } else {
            System.err.println("LookupType " + lookupType + " is not yet handled for " + GlyphSubstitutionTableReader.class.getSimpleName());
        }
    }

    private void readSingleSubstitutionSubtable(int subTableLocation) throws IOException {
        this.rf.seek(subTableLocation);

        int substFormat = this.rf.readShort();
        LOG.debug("substFormat=" + substFormat);

        if (substFormat == 1) {
            int coverage = this.rf.readShort();
            LOG.debug("coverage=" + coverage);

            int deltaGlyphID = this.rf.readShort();
            LOG.debug("deltaGlyphID=" + deltaGlyphID);

            List<Integer> coverageGlyphIds = readCoverageFormat(subTableLocation + coverage);

            for (Iterator<Integer> iterator = coverageGlyphIds.iterator(); iterator.hasNext();) {
                int coverageGlyphId = ((Integer) iterator.next()).intValue();
                int substituteGlyphId = coverageGlyphId + deltaGlyphID;
                this.rawLigatureSubstitutionMap.put(Integer.valueOf(substituteGlyphId), Arrays.asList(new Integer[]{Integer.valueOf(coverageGlyphId)}));
            }

        } else if (substFormat == 2) {
            int coverage = this.rf.readShort();
            LOG.debug("coverage=" + coverage);
            int glyphCount = this.rf.readUnsignedShort();
            int[] substitute = new int[glyphCount];
            for (int k = 0; k < glyphCount; k++) {
                substitute[k] = this.rf.readUnsignedShort();
            }
            List<Integer> coverageGlyphIds = readCoverageFormat(subTableLocation + coverage);
            for (int i = 0; i < glyphCount; i++) {
                this.rawLigatureSubstitutionMap.put(Integer.valueOf(substitute[i]), Arrays.asList(new Integer[]{coverageGlyphIds.get(i)}));
            }
        } else {

            throw new IllegalArgumentException("Bad substFormat: " + substFormat);
        }
    }

    private void readLigatureSubstitutionSubtable(int ligatureSubstitutionSubtableLocation) throws IOException {
        this.rf.seek(ligatureSubstitutionSubtableLocation);
        int substFormat = this.rf.readShort();
        LOG.debug("substFormat=" + substFormat);

        if (substFormat != 1) {
            throw new IllegalArgumentException("The expected SubstFormat is 1");
        }

        int coverage = this.rf.readShort();
        LOG.debug("coverage=" + coverage);

        int ligSetCount = this.rf.readShort();

        List<Integer> ligatureOffsets = new ArrayList<Integer>(ligSetCount);

        for (int i = 0; i < ligSetCount; i++) {
            int ligatureOffset = this.rf.readShort();
            ligatureOffsets.add(Integer.valueOf(ligatureOffset));
        }

        List<Integer> coverageGlyphIds = readCoverageFormat(ligatureSubstitutionSubtableLocation + coverage);

        if (ligSetCount != coverageGlyphIds.size()) {
            throw new IllegalArgumentException("According to the OpenTypeFont specifications, the coverage count should be equal to the no. of LigatureSetTables");
        }

        for (int j = 0; j < ligSetCount; j++) {

            int coverageGlyphId = ((Integer) coverageGlyphIds.get(j)).intValue();
            int ligatureOffset = ((Integer) ligatureOffsets.get(j)).intValue();
            LOG.debug("ligatureOffset=" + ligatureOffset);
            readLigatureSetTable(ligatureSubstitutionSubtableLocation + ligatureOffset, coverageGlyphId);
        }
    }

    private void readLigatureSetTable(int ligatureSetTableLocation, int coverageGlyphId) throws IOException {
        this.rf.seek(ligatureSetTableLocation);
        int ligatureCount = this.rf.readShort();
        LOG.debug("ligatureCount=" + ligatureCount);

        List<Integer> ligatureOffsets = new ArrayList<Integer>(ligatureCount);

        for (int i = 0; i < ligatureCount; i++) {
            int ligatureOffset = this.rf.readShort();
            ligatureOffsets.add(Integer.valueOf(ligatureOffset));
        }

        for (Iterator<Integer> iterator = ligatureOffsets.iterator(); iterator.hasNext();) {
            int ligatureOffset = ((Integer) iterator.next()).intValue();
            readLigatureTable(ligatureSetTableLocation + ligatureOffset, coverageGlyphId);
        }

    }

    private void readLigatureTable(int ligatureTableLocation, int coverageGlyphId) throws IOException {
        this.rf.seek(ligatureTableLocation);
        int ligGlyph = this.rf.readShort();
        LOG.debug("ligGlyph=" + ligGlyph);

        int compCount = this.rf.readShort();

        List<Integer> glyphIdList = new ArrayList<Integer>();

        glyphIdList.add(Integer.valueOf(coverageGlyphId));

        for (int i = 0; i < compCount - 1; i++) {
            int glyphId = this.rf.readShort();
            glyphIdList.add(Integer.valueOf(glyphId));
        }

        LOG.debug("glyphIdList=" + glyphIdList);

        List<Integer> previousValue = this.rawLigatureSubstitutionMap.put(Integer.valueOf(ligGlyph), glyphIdList);

        if (previousValue != null) {
            LOG.warn("!!!!!!!!!!glyphId=" + ligGlyph + ",\npreviousValue=" + previousValue + ",\ncurrentVal=" + glyphIdList);
        }
    }
}
