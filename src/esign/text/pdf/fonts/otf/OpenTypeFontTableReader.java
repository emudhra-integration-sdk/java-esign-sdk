package esign.text.pdf.fonts.otf;

import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import esign.text.pdf.RandomAccessFileOrArray;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class OpenTypeFontTableReader {

    protected static final Logger LOG = LoggerFactory.getLogger(OpenTypeFontTableReader.class);

    protected final RandomAccessFileOrArray rf;

    protected final int tableLocation;

    private List<String> supportedLanguages;

    public OpenTypeFontTableReader(RandomAccessFileOrArray rf, int tableLocation) throws IOException {
        this.rf = rf;
        this.tableLocation = tableLocation;
    }

    public Language getSupportedLanguage() throws FontReadingException {
        Language[] allLangs = Language.values();

        for (String supportedLang : this.supportedLanguages) {
            for (Language lang : allLangs) {
                if (lang.isSupported(supportedLang)) {
                    return lang;
                }
            }
        }

        throw new FontReadingException("Unsupported languages " + this.supportedLanguages);
    }

    protected final void startReadingTable() throws FontReadingException {
        try {
            TableHeader header = readHeader();

            readScriptListTable(this.tableLocation + header.scriptListOffset);

            readFeatureListTable(this.tableLocation + header.featureListOffset);

            readLookupListTable(this.tableLocation + header.lookupListOffset);
        } catch (IOException e) {
            throw new FontReadingException("Error reading font file", e);
        }
    }

    protected abstract void readSubTable(int paramInt1, int paramInt2) throws IOException;

    private void readLookupListTable(int lookupListTableLocation) throws IOException {
        this.rf.seek(lookupListTableLocation);
        int lookupCount = this.rf.readShort();

        List<Integer> lookupTableOffsets = new ArrayList<Integer>();
        int i;
        for (i = 0; i < lookupCount; i++) {
            int lookupTableOffset = this.rf.readShort();
            lookupTableOffsets.add(Integer.valueOf(lookupTableOffset));
        }

        for (i = 0; i < lookupCount; i++) {

            int lookupTableOffset = ((Integer) lookupTableOffsets.get(i)).intValue();
            readLookupTable(lookupListTableLocation + lookupTableOffset);
        }
    }

    private void readLookupTable(int lookupTableLocation) throws IOException {
        this.rf.seek(lookupTableLocation);
        int lookupType = this.rf.readShort();

        this.rf.skipBytes(2);

        int subTableCount = this.rf.readShort();

        List<Integer> subTableOffsets = new ArrayList<Integer>();

        for (int i = 0; i < subTableCount; i++) {
            int subTableOffset = this.rf.readShort();
            subTableOffsets.add(Integer.valueOf(subTableOffset));
        }

        for (Iterator<Integer> iterator = subTableOffsets.iterator(); iterator.hasNext();) {
            int subTableOffset = ((Integer) iterator.next()).intValue();

            readSubTable(lookupType, lookupTableLocation + subTableOffset);
        }

    }

    protected final List<Integer> readCoverageFormat(int coverageLocation) throws IOException {
        List<Integer> glyphIds;
        this.rf.seek(coverageLocation);
        int coverageFormat = this.rf.readShort();

        if (coverageFormat == 1) {
            int glyphCount = this.rf.readShort();

            glyphIds = new ArrayList<Integer>(glyphCount);

            for (int i = 0; i < glyphCount; i++) {
                int coverageGlyphId = this.rf.readShort();
                glyphIds.add(Integer.valueOf(coverageGlyphId));
            }

        } else if (coverageFormat == 2) {

            int rangeCount = this.rf.readShort();

            glyphIds = new ArrayList<Integer>();

            for (int i = 0; i < rangeCount; i++) {
                readRangeRecord(glyphIds);
            }
        } else {

            throw new UnsupportedOperationException("Invalid coverage format: " + coverageFormat);
        }

        return Collections.unmodifiableList(glyphIds);
    }

    private void readRangeRecord(List<Integer> glyphIds) throws IOException {
        int startGlyphId = this.rf.readShort();
        int endGlyphId = this.rf.readShort();
        int startCoverageIndex = this.rf.readShort();

        for (int glyphId = startGlyphId; glyphId <= endGlyphId; glyphId++) {
            glyphIds.add(Integer.valueOf(glyphId));
        }
    }

    private void readScriptListTable(int scriptListTableLocationOffset) throws IOException {
        this.rf.seek(scriptListTableLocationOffset);

        int scriptCount = this.rf.readShort();

        Map<String, Integer> scriptRecords = new HashMap<String, Integer>(scriptCount);

        for (int i = 0; i < scriptCount; i++) {
            readScriptRecord(scriptListTableLocationOffset, scriptRecords);
        }

        List<String> supportedLanguages = new ArrayList<String>(scriptCount);

        for (String scriptName : scriptRecords.keySet()) {
            readScriptTable(((Integer) scriptRecords.get(scriptName)).intValue());
            supportedLanguages.add(scriptName);
        }

        this.supportedLanguages = Collections.unmodifiableList(supportedLanguages);
    }

    private void readScriptRecord(int scriptListTableLocationOffset, Map<String, Integer> scriptRecords) throws IOException {
        String scriptTag = this.rf.readString(4, "utf-8");

        int scriptOffset = this.rf.readShort();

        scriptRecords.put(scriptTag, Integer.valueOf(scriptListTableLocationOffset + scriptOffset));
    }

    private void readScriptTable(int scriptTableLocationOffset) throws IOException {
        this.rf.seek(scriptTableLocationOffset);
        int defaultLangSys = this.rf.readShort();
        int langSysCount = this.rf.readShort();

        if (langSysCount > 0) {
            Map<String, Integer> langSysRecords = new LinkedHashMap<String, Integer>(langSysCount);

            for (int i = 0; i < langSysCount; i++) {
                readLangSysRecord(langSysRecords);
            }

            for (String langSysTag : langSysRecords.keySet()) {
                readLangSysTable(scriptTableLocationOffset + ((Integer) langSysRecords
                        .get(langSysTag)).intValue());
            }
        }

        readLangSysTable(scriptTableLocationOffset + defaultLangSys);
    }

    private void readLangSysRecord(Map<String, Integer> langSysRecords) throws IOException {
        String langSysTag = this.rf.readString(4, "utf-8");
        int langSys = this.rf.readShort();
        langSysRecords.put(langSysTag, Integer.valueOf(langSys));
    }

    private void readLangSysTable(int langSysTableLocationOffset) throws IOException {
        this.rf.seek(langSysTableLocationOffset);
        int lookupOrderOffset = this.rf.readShort();
        LOG.debug("lookupOrderOffset=" + lookupOrderOffset);
        int reqFeatureIndex = this.rf.readShort();
        LOG.debug("reqFeatureIndex=" + reqFeatureIndex);
        int featureCount = this.rf.readShort();

        List<Short> featureListIndices = new ArrayList<Short>(featureCount);
        for (int i = 0; i < featureCount; i++) {
            featureListIndices.add(Short.valueOf(this.rf.readShort()));
        }

        LOG.debug("featureListIndices=" + featureListIndices);
    }

    private void readFeatureListTable(int featureListTableLocationOffset) throws IOException {
        this.rf.seek(featureListTableLocationOffset);
        int featureCount = this.rf.readShort();
        LOG.debug("featureCount=" + featureCount);

        Map<String, Short> featureRecords = new LinkedHashMap<String, Short>(featureCount);

        for (int i = 0; i < featureCount; i++) {
            featureRecords.put(this.rf.readString(4, "utf-8"), Short.valueOf(this.rf.readShort()));
        }

        for (String featureName : featureRecords.keySet()) {
            LOG.debug("*************featureName=" + featureName);
            readFeatureTable(featureListTableLocationOffset + ((Short) featureRecords
                    .get(featureName)).shortValue());
        }
    }

    private void readFeatureTable(int featureTableLocationOffset) throws IOException {
        this.rf.seek(featureTableLocationOffset);
        int featureParamsOffset = this.rf.readShort();
        LOG.debug("featureParamsOffset=" + featureParamsOffset);

        int lookupCount = this.rf.readShort();
        LOG.debug("lookupCount=" + lookupCount);

        List<Short> lookupListIndices = new ArrayList<Short>(lookupCount);
        for (int i = 0; i < lookupCount; i++) {
            lookupListIndices.add(Short.valueOf(this.rf.readShort()));
        }
    }

    private TableHeader readHeader() throws IOException {
        this.rf.seek(this.tableLocation);

        int version = this.rf.readInt();

        int scriptListOffset = this.rf.readUnsignedShort();
        int featureListOffset = this.rf.readUnsignedShort();
        int lookupListOffset = this.rf.readUnsignedShort();

        TableHeader header = new TableHeader(version, scriptListOffset, featureListOffset, lookupListOffset);

        return header;
    }
}
