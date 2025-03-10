package esign.text.pdf.languages;

public abstract class IndicLigaturizer
        implements LanguageProcessor {

    public static final int MATRA_AA = 0;
    public static final int MATRA_I = 1;
    public static final int MATRA_E = 2;
    public static final int MATRA_AI = 3;
    public static final int MATRA_HLR = 4;
    public static final int MATRA_HLRR = 5;
    public static final int LETTER_A = 6;
    public static final int LETTER_AU = 7;
    public static final int LETTER_KA = 8;
    public static final int LETTER_HA = 9;
    public static final int HALANTA = 10;
    protected char[] langTable;

    public String process(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        StringBuilder res = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char letter = s.charAt(i);

            if (IsVyanjana(letter) || IsSwaraLetter(letter)) {
                res.append(letter);
            } else if (IsSwaraMatra(letter)) {
                int prevCharIndex = res.length() - 1;

                if (prevCharIndex >= 0) {

                    if (res.charAt(prevCharIndex) == this.langTable[10]) {
                        res.deleteCharAt(prevCharIndex);
                    }
                    res.append(letter);
                    int prevPrevCharIndex = res.length() - 2;

                    if (letter == this.langTable[1] && prevPrevCharIndex >= 0) {
                        swap(res, prevPrevCharIndex, res.length() - 1);
                    }
                } else {
                    res.append(letter);
                }
            } else {
                res.append(letter);
            }
        }

        return res.toString();
    }

    public boolean isRTL() {
        return false;
    }

    protected boolean IsSwaraLetter(char ch) {
        return (ch >= this.langTable[6] && ch <= this.langTable[7]);
    }

    protected boolean IsSwaraMatra(char ch) {
        return ((ch >= this.langTable[0] && ch <= this.langTable[3]) || ch == this.langTable[4] || ch == this.langTable[5]);
    }

    protected boolean IsVyanjana(char ch) {
        return (ch >= this.langTable[8] && ch <= this.langTable[9]);
    }

    private static void swap(StringBuilder s, int i, int j) {
        char temp = s.charAt(i);
        s.setCharAt(i, s.charAt(j));
        s.setCharAt(j, temp);
    }
}
