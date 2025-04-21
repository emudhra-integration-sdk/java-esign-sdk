package com.emudhra.esign;

import esign.text.Rectangle;
import esign.text.pdf.PdfReader;
import esign.text.pdf.parser.LocationTextExtractionStrategy;
import esign.text.pdf.parser.PdfTextExtractor;
import esign.text.pdf.parser.TextRenderInfo;
import esign.text.pdf.parser.Vector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
//import org.emcastle.util.encoders.Base64;

public class TextCoordinates extends LocationTextExtractionStrategy {

    // Added by Bavaji
    public List<Coord> Coords = new ArrayList<>();
    private String searchPattern;

    public String getSearchPattern() {
        return searchPattern;
    }

    public void setSearchPattern(String searchPattern) {
        this.searchPattern = searchPattern;
    }

    private StringBuilder chunkText = new StringBuilder();

    public StringBuilder getChunkText() {
        return chunkText;
    }

    public void setChunkText(StringBuilder chunkText) {
        this.chunkText = chunkText;
    }

    private List<TextRenderInfo> Text = new ArrayList<>();

    public List<TextRenderInfo> getText() {
        return Text;
    }

    public void setText(List<TextRenderInfo> text) {
        this.Text = text;
    }

    public TextCoordinates() {
        reset();
    }

    private void reset() {
        beginTextBlock();
    }

    @Override
    public void beginTextBlock() {
        chunkText = new StringBuilder();
        Text = new ArrayList<>();
    }

    @Override
    public void endTextBlock() {
        String line = chunkText.toString();
        Pattern pattern = Pattern.compile(searchPattern);
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            int startIdx = matcher.start();
            int endIdx = matcher.end() - 1;

            if (startIdx < Text.size() && endIdx < Text.size()) {
                TextRenderInfo startInfo = Text.get(startIdx);
                TextRenderInfo endInfo = Text.get(endIdx);

                Vector letterStart = startInfo.getBaseline().getStartPoint();
                Vector letterEnd = endInfo.getAscentLine().getEndPoint();

                Coords.add(new Coord(
                        letterStart.get(Vector.I1), letterStart.get(Vector.I2),
                        letterEnd.get(Vector.I1), letterEnd.get(Vector.I2)
                ));
            }
        }
    }

    @Override
    public void renderText(TextRenderInfo renderInfo) {
        Text.addAll(renderInfo.getCharacterRenderInfos());
        chunkText.append(renderInfo.getText());
    }

    public String getCoordinates(PdfReader pdfReader, String textToSearch, String offset, int height, int width, ContentSearch.Position position) throws IOException {
        StringBuilder coordinates = new StringBuilder();
        String[] offsets = offset.split("\\|");
        int offX = Integer.parseInt(offsets[0]);
        int offY = Integer.parseInt(offsets[1]);

        TextCoordinates pr = new TextCoordinates();
        pr.setSearchPattern(textToSearch);

        int numberOfPages = pdfReader.getNumberOfPages();

        for (int i = 1; i <= numberOfPages; i++) {
            pr.Coords.clear(); // Clear previous coordinates

            // Extract text from the current page
            PdfTextExtractor.getTextFromPage(pdfReader, i, pr);
            Rectangle pagedetails = pdfReader.getPageSize(i);
            float Pageheight = pagedetails.getHeight();
            if (!pr.Coords.isEmpty()) {
                long lastIndex = 1;
                for (Coord c : pr.Coords) {
                    String coordString = i + "-" + getCordFromPosition(c, position, offX, offY, height, width) + ";";
                    coordinates.append(coordString);
                    lastIndex++;
                }
            }
        }
        return coordinates.toString();
    }

//    public static void main(String[] args) {
//        String textToSearch = "User";
//        String position = "IBL"; 
//        String offset = "0|0";
//        int height = 60,  width= 120;
//        try {
//            String co =   getCoordinates("D:\\Test Pdf\\eSign-APIv3.3.pdf",  textToSearch,  offset,  height,  width,  ContentSearch.Position.IBL);
//            System.out.println(co);
//        } catch (IOException ex) {
//            Logger.getLogger(TextCoordinates.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    /**
     * @param pdfReader pdfreader objrct for text search
     * @return cordinates for searched text
     * @
     *
     */
    /*
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
                Pattern p = Pattern.compile(textToSearch);
                Matcher m = p.matcher(line);
                //
                while (m.find()) {
                    Vector letterStart = ((TextRenderInfo) t.text.get(m.start())).getBaseline().getStartPoint();
                    Vector letterEnd = ((TextRenderInfo) t.text.get(m.end() - 1)).getAscentLine().getEndPoint();
                    if (pdfReader.getPageRotation(i) == 90) {
                        Rectangle r1 = pdfReader.getPageSize(i);
                        coordinates.add(new Coord(letterStart.get(Vector.I2), r1.getWidth() - letterStart.get(Vector.I1), letterEnd.get(Vector.I2), r1.getWidth() - letterEnd.get(Vector.I1)));
                    } else if (pdfReader.getPageRotation(i) == 180) {
                        Rectangle r1 = pdfReader.getPageSize(i);
                        coordinates.add(new Coord(r1.getWidth() - letterStart.get(Vector.I1), r1.getHeight() - letterStart.get(Vector.I2), r1.getWidth() - letterEnd.get(Vector.I1), r1.getHeight() - letterEnd.get(Vector.I2)));
                    } else if (pdfReader.getPageRotation(i) == 270) {
                        Rectangle r1 = pdfReader.getPageSize(i);
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
    }*/
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

//class Program extends LocationTextExtractionStrategy {
//
//    protected StringBuffer chunkText = new StringBuffer();
//    protected List<TextRenderInfo> text = new ArrayList<>();
//
//    public void beginTextBlock() {
//    }
//
//    public void endTextBlock() {
//    }
//
//    public void renderText(TextRenderInfo renderInfo) {
//        this.text.addAll(renderInfo.getCharacterRenderInfos());
//        this.chunkText.append(renderInfo.getText());
//    }
//}
