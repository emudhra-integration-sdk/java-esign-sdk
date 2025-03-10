package esign.text;

import esign.text.log.Level;
import esign.text.log.Logger;
import esign.text.log.LoggerFactory;
import esign.text.pdf.BaseFont;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

public class FontFactoryImp
        implements FontProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FontFactoryImp.class);

    private final Hashtable<String, String> trueTypeFonts = new Hashtable<String, String>();

    private static String[] TTFamilyOrder = new String[]{"3", "1", "1033", "3", "0", "1033", "1", "0", "0", "0", "3", "0"};

    private final Hashtable<String, ArrayList<String>> fontFamilies = new Hashtable<String, ArrayList<String>>();

    public String defaultEncoding = "Cp1252";

    public boolean defaultEmbedding = false;

    public FontFactoryImp() {
        this.trueTypeFonts.put("Courier".toLowerCase(), "Courier");
        this.trueTypeFonts.put("Courier-Bold".toLowerCase(), "Courier-Bold");
        this.trueTypeFonts.put("Courier-Oblique".toLowerCase(), "Courier-Oblique");
        this.trueTypeFonts.put("Courier-BoldOblique".toLowerCase(), "Courier-BoldOblique");
        this.trueTypeFonts.put("Helvetica".toLowerCase(), "Helvetica");
        this.trueTypeFonts.put("Helvetica-Bold".toLowerCase(), "Helvetica-Bold");
        this.trueTypeFonts.put("Helvetica-Oblique".toLowerCase(), "Helvetica-Oblique");
        this.trueTypeFonts.put("Helvetica-BoldOblique".toLowerCase(), "Helvetica-BoldOblique");
        this.trueTypeFonts.put("Symbol".toLowerCase(), "Symbol");
        this.trueTypeFonts.put("Times-Roman".toLowerCase(), "Times-Roman");
        this.trueTypeFonts.put("Times-Bold".toLowerCase(), "Times-Bold");
        this.trueTypeFonts.put("Times-Italic".toLowerCase(), "Times-Italic");
        this.trueTypeFonts.put("Times-BoldItalic".toLowerCase(), "Times-BoldItalic");
        this.trueTypeFonts.put("ZapfDingbats".toLowerCase(), "ZapfDingbats");

        ArrayList<String> tmp = new ArrayList<String>();
        tmp.add("Courier");
        tmp.add("Courier-Bold");
        tmp.add("Courier-Oblique");
        tmp.add("Courier-BoldOblique");
        this.fontFamilies.put("Courier".toLowerCase(), tmp);
        tmp = new ArrayList<String>();
        tmp.add("Helvetica");
        tmp.add("Helvetica-Bold");
        tmp.add("Helvetica-Oblique");
        tmp.add("Helvetica-BoldOblique");
        this.fontFamilies.put("Helvetica".toLowerCase(), tmp);
        tmp = new ArrayList<String>();
        tmp.add("Symbol");
        this.fontFamilies.put("Symbol".toLowerCase(), tmp);
        tmp = new ArrayList<String>();
        tmp.add("Times-Roman");
        tmp.add("Times-Bold");
        tmp.add("Times-Italic");
        tmp.add("Times-BoldItalic");
        this.fontFamilies.put("Times".toLowerCase(), tmp);
        this.fontFamilies.put("Times-Roman".toLowerCase(), tmp);
        tmp = new ArrayList<String>();
        tmp.add("ZapfDingbats");
        this.fontFamilies.put("ZapfDingbats".toLowerCase(), tmp);
    }

    public Font getFont(String fontname, String encoding, boolean embedded, float size, int style, BaseColor color) {
        return getFont(fontname, encoding, embedded, size, style, color, true);
    }

    public Font getFont(String fontname, String encoding, boolean embedded, float size, int style, BaseColor color, boolean cached) {
        if (fontname == null) {
            return new Font(Font.FontFamily.UNDEFINED, size, style, color);
        }
        String lowercasefontname = fontname.toLowerCase();
        ArrayList<String> tmp = this.fontFamilies.get(lowercasefontname);
        if (tmp != null) {
            synchronized (tmp) {

                int s = (style == -1) ? 0 : style;
                int fs = 0;
                boolean found = false;
                for (String f : tmp) {
                    String lcf = f.toLowerCase();
                    fs = 0;
                    if (lcf.indexOf("bold") != -1) {
                        fs |= 0x1;
                    }
                    if (lcf.indexOf("italic") != -1 || lcf.indexOf("oblique") != -1) {
                        fs |= 0x2;
                    }
                    if ((s & 0x3) == fs) {
                        fontname = f;
                        found = true;
                        break;
                    }
                }
                if (style != -1 && found) {
                    style &= fs ^ 0xFFFFFFFF;
                }
            }
        }
        BaseFont basefont = null;
        try {
            basefont = getBaseFont(fontname, encoding, embedded, cached);
            if (basefont == null) {
                return new Font(Font.FontFamily.UNDEFINED, size, style, color);
            }
        } catch (DocumentException de) {

            throw new ExceptionConverter(de);
        } catch (IOException ioe) {

            return new Font(Font.FontFamily.UNDEFINED, size, style, color);
        } catch (NullPointerException npe) {

            return new Font(Font.FontFamily.UNDEFINED, size, style, color);
        }
        return new Font(basefont, size, style, color);
    }

    protected BaseFont getBaseFont(String fontname, String encoding, boolean embedded, boolean cached) throws IOException, DocumentException {
        BaseFont basefont = null;

        try {
            basefont = BaseFont.createFont(fontname, encoding, embedded, cached, null, null, true);
        } catch (DocumentException documentException) {
        }

        if (basefont == null) {

            fontname = this.trueTypeFonts.get(fontname.toLowerCase());

            if (fontname != null) {
                basefont = BaseFont.createFont(fontname, encoding, embedded, cached, null, null);
            }
        }
        return basefont;
    }

    public Font getFont(String fontname, String encoding, boolean embedded, float size, int style) {
        return getFont(fontname, encoding, embedded, size, style, null);
    }

    public Font getFont(String fontname, String encoding, boolean embedded, float size) {
        return getFont(fontname, encoding, embedded, size, -1, null);
    }

    public Font getFont(String fontname, String encoding, boolean embedded) {
        return getFont(fontname, encoding, embedded, -1.0F, -1, null);
    }

    public Font getFont(String fontname, String encoding, float size, int style, BaseColor color) {
        return getFont(fontname, encoding, this.defaultEmbedding, size, style, color);
    }

    public Font getFont(String fontname, String encoding, float size, int style) {
        return getFont(fontname, encoding, this.defaultEmbedding, size, style, null);
    }

    public Font getFont(String fontname, String encoding, float size) {
        return getFont(fontname, encoding, this.defaultEmbedding, size, -1, null);
    }

    public Font getFont(String fontname, float size, BaseColor color) {
        return getFont(fontname, this.defaultEncoding, this.defaultEmbedding, size, -1, color);
    }

    public Font getFont(String fontname, String encoding) {
        return getFont(fontname, encoding, this.defaultEmbedding, -1.0F, -1, null);
    }

    public Font getFont(String fontname, float size, int style, BaseColor color) {
        return getFont(fontname, this.defaultEncoding, this.defaultEmbedding, size, style, color);
    }

    public Font getFont(String fontname, float size, int style) {
        return getFont(fontname, this.defaultEncoding, this.defaultEmbedding, size, style, null);
    }

    public Font getFont(String fontname, float size) {
        return getFont(fontname, this.defaultEncoding, this.defaultEmbedding, size, -1, null);
    }

    public Font getFont(String fontname) {
        return getFont(fontname, this.defaultEncoding, this.defaultEmbedding, -1.0F, -1, null);
    }

    public void registerFamily(String familyName, String fullName, String path) {
        ArrayList<String> tmp;
        if (path != null) {
            this.trueTypeFonts.put(fullName, path);
        }
        synchronized (this.fontFamilies) {
            tmp = this.fontFamilies.get(familyName);
            if (tmp == null) {
                tmp = new ArrayList<String>();
                this.fontFamilies.put(familyName, tmp);
            }
        }
        synchronized (tmp) {
            if (!tmp.contains(fullName)) {
                int fullNameLength = fullName.length();
                boolean inserted = false;
                for (int j = 0; j < tmp.size(); j++) {
                    if (((String) tmp.get(j)).length() >= fullNameLength) {
                        tmp.add(j, fullName);
                        inserted = true;
                        break;
                    }
                }
                if (!inserted) {
                    tmp.add(fullName);
                    String newFullName = fullName.toLowerCase();
                    if (newFullName.endsWith("regular")) {

                        newFullName = newFullName.substring(0, newFullName.length() - 7).trim();

                        tmp.add(0, fullName.substring(0, newFullName.length()));
                    }
                }
            }
        }
    }

    public void register(String path) {
        register(path, null);
    }

    public void register(String path, String alias) {
        try {
            if (path.toLowerCase().endsWith(".ttf") || path.toLowerCase().endsWith(".otf") || path.toLowerCase().indexOf(".ttc,") > 0) {
                Object[] allNames = BaseFont.getAllFontNames(path, "Cp1252", null);
                this.trueTypeFonts.put(((String) allNames[0]).toLowerCase(), path);
                if (alias != null) {
                    String lcAlias = alias.toLowerCase();
                    this.trueTypeFonts.put(lcAlias, path);
                    if (lcAlias.endsWith("regular")) {
                        saveCopyOfRegularFont(lcAlias, path);
                    }
                }

                String[][] names = (String[][]) allNames[2];
                for (String[] name : names) {
                    String lcName = name[3].toLowerCase();
                    this.trueTypeFonts.put(lcName, path);
                    if (lcName.endsWith("regular")) {
                        saveCopyOfRegularFont(lcName, path);
                    }
                }
                String fullName = null;
                String familyName = null;
                names = (String[][]) allNames[1];
                for (int k = 0; k < TTFamilyOrder.length; k += 3) {
                    for (String[] name : names) {
                        if (TTFamilyOrder[k].equals(name[0]) && TTFamilyOrder[k + 1].equals(name[1]) && TTFamilyOrder[k + 2].equals(name[2])) {
                            familyName = name[3].toLowerCase();
                            k = TTFamilyOrder.length;
                            break;
                        }
                    }
                }
                if (familyName != null) {
                    String lastName = "";
                    names = (String[][]) allNames[2];
                    for (String[] name : names) {
                        for (int i = 0; i < TTFamilyOrder.length; i += 3) {
                            if (TTFamilyOrder[i].equals(name[0]) && TTFamilyOrder[i + 1].equals(name[1]) && TTFamilyOrder[i + 2].equals(name[2])) {
                                fullName = name[3];
                                if (!fullName.equals(lastName)) {

                                    lastName = fullName;
                                    registerFamily(familyName, fullName, null);
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (path.toLowerCase().endsWith(".ttc")) {
                if (alias != null) {
                    LOGGER.error("You can't define an alias for a true type collection.");
                }
                String[] names = BaseFont.enumerateTTCNames(path);
                for (int i = 0; i < names.length; i++) {
                    register(path + "," + i);
                }
            } else if (path.toLowerCase().endsWith(".afm") || path.toLowerCase().endsWith(".pfm")) {
                BaseFont bf = BaseFont.createFont(path, "Cp1252", false);
                String fullName = bf.getFullFontName()[0][3].toLowerCase();
                String familyName = bf.getFamilyFontName()[0][3].toLowerCase();
                String psName = bf.getPostscriptFontName().toLowerCase();
                registerFamily(familyName, fullName, null);
                this.trueTypeFonts.put(psName, path);
                this.trueTypeFonts.put(fullName, path);
            }
            if (LOGGER.isLogging(Level.TRACE)) {
                LOGGER.trace(String.format("Registered %s", new Object[]{path}));
            }
        } catch (DocumentException de) {

            throw new ExceptionConverter(de);
        } catch (IOException ioe) {
            throw new ExceptionConverter(ioe);
        }
    }

    protected boolean saveCopyOfRegularFont(String regularFontName, String path) {
        String alias = regularFontName.substring(0, regularFontName.length() - 7).trim();
        if (!this.trueTypeFonts.containsKey(alias)) {
            this.trueTypeFonts.put(alias, path);
            return true;
        }
        return false;
    }

    public int registerDirectory(String dir) {
        return registerDirectory(dir, false);
    }

    public int registerDirectory(String dir, boolean scanSubdirectories) {
        if (LOGGER.isLogging(Level.DEBUG)) {
            LOGGER.debug(String.format("Registering directory %s, looking for fonts", new Object[]{dir}));
        }
        int count = 0;
        try {
            File file = new File(dir);
            if (!file.exists() || !file.isDirectory()) {
                return 0;
            }
            String[] files = file.list();
            if (files == null) {
                return 0;
            }
            for (int k = 0; k < files.length; k++) {
                try {
                    file = new File(dir, files[k]);
                    if (file.isDirectory()) {
                        if (scanSubdirectories) {
                            count += registerDirectory(file.getAbsolutePath(), true);
                        }
                    } else {
                        String name = file.getPath();
                        String suffix = (name.length() < 4) ? null : name.substring(name.length() - 4).toLowerCase();
                        if (".afm".equals(suffix) || ".pfm".equals(suffix)) {

                            File pfb = new File(name.substring(0, name.length() - 4) + ".pfb");
                            if (pfb.exists()) {
                                register(name, null);
                                count++;
                            }
                        } else if (".ttf".equals(suffix) || ".otf".equals(suffix) || ".ttc".equals(suffix)) {
                            register(name, null);
                            count++;
                        }

                    }
                } catch (Exception exception) {
                }

            }

        } catch (Exception exception) {
        }

        return count;
    }

    public int registerDirectories() {
        int count = 0;
        String windir = System.getenv("windir");
        String fileseparator = System.getProperty("file.separator");
        if (windir != null && fileseparator != null) {
            count += registerDirectory(windir + fileseparator + "fonts");
        }
        count += registerDirectory("/usr/share/X11/fonts", true);
        count += registerDirectory("/usr/X/lib/X11/fonts", true);
        count += registerDirectory("/usr/openwin/lib/X11/fonts", true);
        count += registerDirectory("/usr/share/fonts", true);
        count += registerDirectory("/usr/X11R6/lib/X11/fonts", true);
        count += registerDirectory("/Library/Fonts");
        count += registerDirectory("/System/Library/Fonts");
        return count;
    }

    public Set<String> getRegisteredFonts() {
        return this.trueTypeFonts.keySet();
    }

    public Set<String> getRegisteredFamilies() {
        return this.fontFamilies.keySet();
    }

    public boolean isRegistered(String fontname) {
        return this.trueTypeFonts.containsKey(fontname.toLowerCase());
    }
}
