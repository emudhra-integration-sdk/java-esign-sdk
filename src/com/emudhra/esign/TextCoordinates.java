package com.emudhra.esign;

import esign.text.pdf.PdfReader;
import esign.text.pdf.parser.LocationTextExtractionStrategy;
import esign.text.pdf.parser.PdfTextExtractor;
import esign.text.pdf.parser.TextExtractionStrategy;
import esign.text.pdf.parser.TextRenderInfo;
import esign.text.pdf.parser.Vector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextCoordinates extends LocationTextExtractionStrategy {

    /**
     * @param pdfReader pdfreader objrct for text search
     * @return cordinates for searched text
     * @
     *
     */
    public static String getCoordinates(PdfReader pdfReader, String textToSearch, String offset, int height, int width, ContentSearch.Position position) throws IOException {
        StringJoiner coords = new StringJoiner(";");
        int offX = Integer.parseInt(offset.split("\\|")[0]);
        int offY = Integer.parseInt(offset.split("\\|")[1]);

        try {
//            pr = new PdfReader(pdfPath);
            List<Coord> coordinates = new ArrayList<>();
            //get number of pages
            int pageNums = pdfReader.getNumberOfPages();
            for (int i = 1; i <= pageNums; i++) {
                Program t = new Program();
                //extracting  text in the page
                PdfTextExtractor.getTextFromPage(pdfReader, i, (TextExtractionStrategy) t);
                coordinates.clear();
                String line = t.chunkText.toString();
                // Normalize: escape regex special chars, then replace whitespace with flexible \s+ matcher
                String regexPattern = Pattern.quote(textToSearch).replaceAll("\\s+", "\\\\E\\\\s+\\\\Q");
                Pattern p = Pattern.compile(regexPattern);
                Matcher m = p.matcher(line);
                //
                while (m.find()) {
                    int startIdx = t.getTextListIndex(m.start());
                    int endIdx = t.getTextListIndex(m.end() - 1);
                    Vector letterStart = ((TextRenderInfo) t.text.get(startIdx)).getBaseline().getStartPoint();
                    Vector letterEnd = ((TextRenderInfo) t.text.get(endIdx)).getAscentLine().getEndPoint();
                    if (pdfReader.getPageRotation(i) == 90) {
                        esign.text.Rectangle r1 = pdfReader.getPageSize(i);
                        coordinates.add(new Coord(letterStart.get(Vector.I2), r1.getWidth() - letterStart.get(Vector.I1), letterEnd.get(Vector.I2), r1.getWidth() - letterEnd.get(Vector.I1)));
                    } else if (pdfReader.getPageRotation(i) == 180) {
                        esign.text.Rectangle r1 = pdfReader.getPageSize(i);
                        coordinates.add(new Coord(r1.getWidth() - letterStart.get(Vector.I1), r1.getHeight() - letterStart.get(Vector.I2), r1.getWidth() - letterEnd.get(Vector.I1), r1.getHeight() - letterEnd.get(Vector.I2)));
                    } else if (pdfReader.getPageRotation(i) == 270) {
                        esign.text.Rectangle r1 = pdfReader.getPageSize(i);
                        coordinates.add(new Coord(r1.getHeight() - letterStart.get(Vector.I2), r1.getWidth() - (r1.getWidth() - letterStart.get(Vector.I1)), r1.getHeight() - letterEnd.get(Vector.I2), r1.getWidth() - (r1.getWidth() - letterEnd.get(Vector.I1))));
                    } else {
                        coordinates.add(new Coord(letterStart.get(Vector.I1), letterStart.get(Vector.I2), letterEnd.get(Vector.I1), letterEnd.get(Vector.I2)));
                    }
                }
                if (coordinates.size() > 0) {
                    for (Coord c : coordinates) {

                        coords.add(i + "-" + getCordFromPosition(c, position, offX, offY, height, width));
                    }
                }
            }
        } catch (IOException ex) {
            throw ex;
        }

        return coords.toString();
    }

    private static String getCordFromPosition(Coord c, ContentSearch.Position position, int offX, int offY, int height, int width) {

        switch (position) {
            case OTL: {
                return Math.round(c.X1 + offX - width) + "," + Math.round(c.Y2 + offY) + "," + Math.round(c.X1 + offX) + "," + Math.round(c.Y2 + offY + height);
            }
            case OTM: {
                return Math.round(c.X1 + offX + (c.X2 - c.X1 - width) / 2) + "," + Math.round(c.Y2 + offY) + "," + Math.round(c.X1 + offX + (c.X2 - c.X1 + width) / 2) + "," + Math.round(c.Y2 + offY + height);
            }
            case OTR: {
                return Math.round(c.X2 + offX) + "," + Math.round(c.Y2 + offY) + "," + Math.round(c.X2 + offX + width) + "," + Math.round(c.Y2 + offY + height);
            }
            case OBL: {
                return Math.round(c.X1 + offX - width) + "," + Math.round(c.Y1 + offY - height) + "," + Math.round(c.X1 + offX) + "," + Math.round(c.Y1 + offY);
            }
            case OBM: {
                return Math.round(c.X1 + offX + (c.X2 - c.X1 - width) / 2) + "," + Math.round(c.Y1 + offY - height) + "," + Math.round(c.X1 + offX + (c.X2 - c.X1 + width) / 2) + "," + Math.round(c.Y1 + offY);
            }
            case OBR: {
                return Math.round(c.X2 + offX) + "," + Math.round(c.Y1 + offY - height) + "," + Math.round(c.X2 + offX + width) + "," + Math.round(c.Y1 + offY);
            }
            case ITL: {
                return Math.round(c.X1 + offX) + "," + Math.round(c.Y2 + offY - height) + "," + Math.round(c.X1 + offX + width) + "," + Math.round(c.Y2 + offY);
            }
            case ITM: {
                return Math.round(c.X1 + offX + (c.X2 - c.X1 - width) / 2) + "," + Math.round(c.Y2 + offY - height) + "," + Math.round(c.X1 + offX + (c.X2 - c.X1 + width) / 2) + "," + Math.round(c.Y2 + offY);
            }
            case ITR: {
                return Math.round(c.X2 + offX - width) + "," + Math.round(c.Y2 + offY - height) + "," + Math.round(c.X2 + offX) + "," + Math.round(c.Y2 + offY);
            }
            case IML: {
                return Math.round(c.X1 + offX) + "," + Math.round(c.Y1 + offY + (c.Y2 - c.Y1 - height) / 2) + "," + Math.round(c.X1 + offX + width) + "," + Math.round(c.Y1 + offY + (c.Y2 - c.Y1 + height) / 2);
            }
            case IMC: {
                return Math.round(c.X1 + offX + (c.X2 - c.X1 - width) / 2) + "," + Math.round(c.Y1 + offY + (c.Y2 - c.Y1 - height) / 2) + "," + Math.round(c.X1 + offX + (c.X2 - c.X1 + width) / 2) + "," + Math.round(c.Y1 + offY + (c.Y2 - c.Y1 + height) / 2);
            }
            case IMR: {
                return Math.round(c.X2 + offX - width) + "," + Math.round(c.Y1 + offY + (c.Y2 - c.Y1 - height) / 2) + "," + Math.round(c.X2 + offX) + "," + Math.round(c.Y1 + offY + (c.Y2 - c.Y1 + height) / 2);
            }
            case IBL: {
                return Math.round(c.X1 + offX) + "," + Math.round(c.Y1 + offY) + "," + Math.round(c.X1 + offX + width) + "," + Math.round(c.Y1 + offY + height);
            }
            case IBM: {
                return Math.round(c.X1 + offX + (c.X2 - c.X1 - width) / 2) + "," + Math.round(c.Y1 + offY) + "," + Math.round(c.X1 + offX + (c.X2 - c.X1 + width) / 2) + "," + Math.round(c.Y1 + offY + height);
            }
            case IBR: {
                return Math.round(c.X2 + offX - width) + "," + Math.round(c.Y1 + offY) + "," + Math.round(c.X2 + offX) + "," + Math.round(c.Y1 + offY + height);
            }
        }
        return "";
    }

}

class Coord {

    float X1;
    float Y1;
    float X2;
    float Y2;

    public Coord(float X1, float Y1, float X2, float Y2) {
        this.X1 = X1;
        this.Y1 = Y1;
        this.X2 = X2;
        this.Y2 = Y2;
    }
}

class Program extends LocationTextExtractionStrategy {

    protected StringBuffer chunkText = new StringBuffer();
    protected List<TextRenderInfo> text = new ArrayList<>();
    private Float lastY = null;
    private List<Integer> insertedSpacePositions = new ArrayList<>();

    public void beginTextBlock() {
    }

    public void endTextBlock() {
    }

    public void renderText(TextRenderInfo renderInfo) {
        List<TextRenderInfo> charInfos = renderInfo.getCharacterRenderInfos();

        // Detect line break: if Y coordinate changed, insert space
        if (!charInfos.isEmpty()) {
            Vector start = charInfos.get(0).getBaseline().getStartPoint();
            float currentY = start.get(Vector.I2);

            if (lastY != null && Math.abs(currentY - lastY) > 2) {
                insertedSpacePositions.add(this.chunkText.length());
                this.chunkText.append(" ");
            }
            lastY = currentY;
        }

        this.text.addAll(charInfos);
        this.chunkText.append(renderInfo.getText());
    }

    // Convert chunkText position to text list index by subtracting inserted spaces before that position
    public int getTextListIndex(int chunkTextPosition) {
        int adjustment = 0;
        for (int spacePos : insertedSpacePositions) {
            if (spacePos < chunkTextPosition) {
                adjustment++;
            }
        }
        return chunkTextPosition - adjustment;
    }
}
