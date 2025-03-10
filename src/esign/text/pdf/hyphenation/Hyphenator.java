package esign.text.pdf.hyphenation;

import esign.text.io.StreamUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;

public class Hyphenator {

    private static Hashtable<String, HyphenationTree> hyphenTrees = new Hashtable<String, HyphenationTree>();

    private HyphenationTree hyphenTree = null;
    private int remainCharCount = 2;
    private int pushCharCount = 2;

    private static final String defaultHyphLocation = "com/itextpdf/text/pdf/hyphenation/hyph/";

    private static String hyphenDir = "";

    public Hyphenator(String lang, String country, int leftMin, int rightMin) {
        this.hyphenTree = getHyphenationTree(lang, country);
        this.remainCharCount = leftMin;
        this.pushCharCount = rightMin;
    }

    public static HyphenationTree getHyphenationTree(String lang, String country) {
        String key = lang;

        if (country != null && !country.equals("none")) {
            key = key + "_" + country;
        }

        if (hyphenTrees.containsKey(key)) {
            return hyphenTrees.get(key);
        }
        if (hyphenTrees.containsKey(lang)) {
            return hyphenTrees.get(lang);
        }

        HyphenationTree hTree = getResourceHyphenationTree(key);
        if (hTree == null) {
            hTree = getFileHyphenationTree(key);
        }
        if (hTree != null) {
            hyphenTrees.put(key, hTree);
        }
        return hTree;
    }

    public static HyphenationTree getResourceHyphenationTree(String key) {
        try {
            InputStream stream = StreamUtil.getResourceStream("com/itextpdf/text/pdf/hyphenation/hyph/" + key + ".xml");
            if (stream == null && key.length() > 2) {
                stream = StreamUtil.getResourceStream("com/itextpdf/text/pdf/hyphenation/hyph/" + key.substring(0, 2) + ".xml");
            }
            if (stream == null) {
                return null;
            }
            HyphenationTree hTree = new HyphenationTree();
            hTree.loadSimplePatterns(stream);
            return hTree;
        } catch (Exception e) {
            return null;
        }
    }

    public static HyphenationTree getFileHyphenationTree(String key) {
        try {
            if (hyphenDir == null) {
                return null;
            }
            InputStream stream = null;
            File hyphenFile = new File(hyphenDir, key + ".xml");
            if (hyphenFile.canRead()) {
                stream = new FileInputStream(hyphenFile);
            }
            if (stream == null && key.length() > 2) {
                hyphenFile = new File(hyphenDir, key.substring(0, 2) + ".xml");
                if (hyphenFile.canRead()) {
                    stream = new FileInputStream(hyphenFile);
                }
            }
            if (stream == null) {
                return null;
            }
            HyphenationTree hTree = new HyphenationTree();
            hTree.loadSimplePatterns(stream);
            return hTree;
        } catch (Exception e) {
            return null;
        }
    }

    public static Hyphenation hyphenate(String lang, String country, String word, int leftMin, int rightMin) {
        HyphenationTree hTree = getHyphenationTree(lang, country);
        if (hTree == null) {

            return null;
        }
        return hTree.hyphenate(word, leftMin, rightMin);
    }

    public static Hyphenation hyphenate(String lang, String country, char[] word, int offset, int len, int leftMin, int rightMin) {
        HyphenationTree hTree = getHyphenationTree(lang, country);
        if (hTree == null) {

            return null;
        }
        return hTree.hyphenate(word, offset, len, leftMin, rightMin);
    }

    public void setMinRemainCharCount(int min) {
        this.remainCharCount = min;
    }

    public void setMinPushCharCount(int min) {
        this.pushCharCount = min;
    }

    public void setLanguage(String lang, String country) {
        this.hyphenTree = getHyphenationTree(lang, country);
    }

    public Hyphenation hyphenate(char[] word, int offset, int len) {
        if (this.hyphenTree == null) {
            return null;
        }
        return this.hyphenTree.hyphenate(word, offset, len, this.remainCharCount, this.pushCharCount);
    }

    public Hyphenation hyphenate(String word) {
        if (this.hyphenTree == null) {
            return null;
        }
        return this.hyphenTree.hyphenate(word, this.remainCharCount, this.pushCharCount);
    }

    public static String getHyphenDir() {
        return hyphenDir;
    }

    public static void setHyphenDir(String _hyphenDir) {
        hyphenDir = _hyphenDir;
    }
}
