package esign.text.awt;

import esign.text.ExceptionConverter;
import esign.text.pdf.BaseFont;
import java.awt.Font;
import java.io.File;
import java.util.HashMap;

public class DefaultFontMapper
        implements FontMapper {

    public static class BaseFontParameters {

        public String fontName;
        public String encoding;
        public boolean embedded;
        public boolean cached;
        public byte[] ttfAfm;
        public byte[] pfb;

        public BaseFontParameters(String fontName) {
            this.fontName = fontName;
            this.encoding = "Cp1252";
            this.embedded = true;
            this.cached = true;
        }
    }

    private HashMap<String, String> aliases = new HashMap<String, String>();

    private HashMap<String, BaseFontParameters> mapper = new HashMap<String, BaseFontParameters>();

    public BaseFont awtToPdf(Font font) {
        try {
            BaseFontParameters p = getBaseFontParameters(font.getFontName());
            if (p != null) {
                return BaseFont.createFont(p.fontName, p.encoding, p.embedded, p.cached, p.ttfAfm, p.pfb);
            }
            String fontKey = null;
            String logicalName = font.getName();

            if (logicalName.equalsIgnoreCase("DialogInput") || logicalName.equalsIgnoreCase("Monospaced") || logicalName.equalsIgnoreCase("Courier")) {

                if (font.isItalic()) {
                    if (font.isBold()) {
                        fontKey = "Courier-BoldOblique";
                    } else {

                        fontKey = "Courier-Oblique";
                    }

                } else if (font.isBold()) {
                    fontKey = "Courier-Bold";
                } else {

                    fontKey = "Courier";
                }

            } else if (logicalName.equalsIgnoreCase("Serif") || logicalName.equalsIgnoreCase("TimesRoman")) {

                if (font.isItalic()) {
                    if (font.isBold()) {
                        fontKey = "Times-BoldItalic";
                    } else {

                        fontKey = "Times-Italic";
                    }

                } else if (font.isBold()) {
                    fontKey = "Times-Bold";
                } else {

                    fontKey = "Times-Roman";

                }

            } else if (font.isItalic()) {
                if (font.isBold()) {
                    fontKey = "Helvetica-BoldOblique";
                } else {

                    fontKey = "Helvetica-Oblique";
                }

            } else if (font.isBold()) {
                fontKey = "Helvetica-Bold";
            } else {
                fontKey = "Helvetica";
            }

            return BaseFont.createFont(fontKey, "Cp1252", false);
        } catch (Exception e) {
            throw new ExceptionConverter(e);
        }
    }

    public Font pdfToAwt(BaseFont font, int size) {
        String[][] names = font.getFullFontName();
        if (names.length == 1) {
            return new Font(names[0][3], 0, size);
        }
        String name10 = null;
        String name3x = null;
        for (int k = 0; k < names.length; k++) {
            String[] name = names[k];
            if (name[0].equals("1") && name[1].equals("0")) {
                name10 = name[3];
            } else if (name[2].equals("1033")) {
                name3x = name[3];
                break;
            }
        }
        String finalName = name3x;
        if (finalName == null) {
            finalName = name10;
        }
        if (finalName == null) {
            finalName = names[0][3];
        }
        return new Font(finalName, 0, size);
    }

    public void putName(String awtName, BaseFontParameters parameters) {
        this.mapper.put(awtName, parameters);
    }

    public void putAlias(String alias, String awtName) {
        this.aliases.put(alias, awtName);
    }

    public BaseFontParameters getBaseFontParameters(String name) {
        String alias = this.aliases.get(name);
        if (alias == null) {
            return this.mapper.get(name);
        }
        BaseFontParameters p = this.mapper.get(alias);
        if (p == null) {
            return this.mapper.get(name);
        }
        return p;
    }

    public void insertNames(Object[] allNames, String path) {
        String[][] names = (String[][]) allNames[2];
        String main = null;
        for (int k = 0; k < names.length; k++) {
            String[] name = names[k];
            if (name[2].equals("1033")) {
                main = name[3];
                break;
            }
        }
        if (main == null) {
            main = names[0][3];
        }
        BaseFontParameters p = new BaseFontParameters(path);
        this.mapper.put(main, p);
        for (int i = 0; i < names.length; i++) {
            this.aliases.put(names[i][3], main);
        }
        this.aliases.put((String) allNames[0], main);
    }

    public int insertFile(File file) {
        String name = file.getPath().toLowerCase();
        try {
            if (name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".afm")) {
                Object[] allNames = BaseFont.getAllFontNames(file.getPath(), "Cp1252", null);
                insertNames(allNames, file.getPath());
                return 1;
            }
            if (name.endsWith(".ttc")) {
                String[] ttcs = BaseFont.enumerateTTCNames(file.getPath());
                for (int j = 0; j < ttcs.length; j++) {
                    String nt = file.getPath() + "," + j;
                    Object[] allNames = BaseFont.getAllFontNames(nt, "Cp1252", null);
                    insertNames(allNames, nt);
                }
                return 1;
            }
        } catch (Exception exception) {
        }

        return 0;
    }

    public int insertDirectory(String dir) {
        File file = new File(dir);
        if (!file.exists() || !file.isDirectory()) {
            return 0;
        }
        File[] files = file.listFiles();
        if (files == null) {
            return 0;
        }
        int count = 0;
        for (int k = 0; k < files.length; k++) {
            count += insertFile(files[k]);
        }
        return count;
    }

    public HashMap<String, BaseFontParameters> getMapper() {
        return this.mapper;
    }

    public HashMap<String, String> getAliases() {
        return this.aliases;
    }
}
