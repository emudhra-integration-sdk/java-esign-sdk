package esign.text.pdf.languages;

import esign.text.pdf.BidiLine;
import esign.text.pdf.BidiOrder;
import java.util.HashMap;

public class ArabicLigaturizer
        implements LanguageProcessor {

    private static final HashMap<Character, char[]> maptable = (HashMap) new HashMap<Character, char[]>();
    private static final char ALEF = 'ا';
    private static final char ALEFHAMZA = 'أ';
    private static final char ALEFHAMZABELOW = 'إ';
    private static final char ALEFMADDA = 'آ';
    private static final char LAM = 'ل';
    private static final char HAMZA = 'ء';
    private static final HashMap<Character, Character> reverseLigatureMapTable = new HashMap<Character, Character>();
    private static final char TATWEEL = 'ـ';
    private static final char ZWJ = '‍';
    private static final char HAMZAABOVE = 'ٔ';
    private static final char HAMZABELOW = 'ٕ';
    private static final char WAWHAMZA = 'ؤ';
    private static final char YEHHAMZA = 'ئ';

    static boolean isVowel(char s) {
        return ((s >= 'ً' && s <= 'ٕ') || s == 'ٰ');
    }
    private static final char WAW = 'و';
    private static final char ALEFMAKSURA = 'ى';
    private static final char YEH = 'ي';
    private static final char FARSIYEH = 'ی';
    private static final char SHADDA = 'ّ';
    private static final char KASRA = 'ِ';

    static char charshape(char s, int which) {
        if (s >= 'ء' && s <= 'ۓ') {
            char[] c = maptable.get(Character.valueOf(s));
            if (c != null) {
                return c[which + 1];
            }
        } else if (s >= 'ﻵ' && s <= 'ﻻ') {
            return (char) (s + which);
        }
        return s;
    }
    private static final char FATHA = 'َ';
    private static final char DAMMA = 'ُ';
    private static final char MADDA = 'ٓ';
    private static final char LAM_ALEF = 'ﻻ';
    private static final char LAM_ALEFHAMZA = 'ﻷ';
    private static final char LAM_ALEFHAMZABELOW = 'ﻹ';
    private static final char LAM_ALEFMADDA = 'ﻵ';

    static int shapecount(char s) {
        if (s >= 'ء' && s <= 'ۓ' && !isVowel(s)) {
            char[] c = maptable.get(Character.valueOf(s));
            if (c != null) {
                return c.length - 1;
            }
        } else if (s == '‍') {
            return 4;
        }
        return 1;
    }

    static int ligature(char newchar, charstruct oldchar) {
        int retval = 0;

        if (oldchar.basechar == '\000') {
            return 0;
        }
        if (isVowel(newchar)) {
            retval = 1;
            if (oldchar.vowel != '\000' && newchar != 'ّ') {
                retval = 2;
            }
            switch (newchar) {
                case 'ّ':
                    if (oldchar.mark1 == '\000') {
                        oldchar.mark1 = 'ّ';
                        break;
                    }
                    return 0;

                case 'ٕ':
                    switch (oldchar.basechar) {
                        case 'ا':
                            oldchar.basechar = 'إ';
                            retval = 2;
                            break;
                        case 'ﻻ':
                            oldchar.basechar = 'ﻹ';
                            retval = 2;
                            break;
                    }
                    oldchar.mark1 = 'ٕ';
                    break;

                case 'ٔ':
                    switch (oldchar.basechar) {
                        case 'ا':
                            oldchar.basechar = 'أ';
                            retval = 2;
                            break;
                        case 'ﻻ':
                            oldchar.basechar = 'ﻷ';
                            retval = 2;
                            break;
                        case 'و':
                            oldchar.basechar = 'ؤ';
                            retval = 2;
                            break;
                        case 'ى':
                        case 'ي':
                        case 'ی':
                            oldchar.basechar = 'ئ';
                            retval = 2;
                            break;
                    }
                    oldchar.mark1 = 'ٔ';
                    break;

                case 'ٓ':
                    switch (oldchar.basechar) {
                        case 'ا':
                            oldchar.basechar = 'آ';
                            retval = 2;
                            break;
                    }
                    break;
                default:
                    oldchar.vowel = newchar;
                    break;
            }
            if (retval == 1) {
                oldchar.lignum++;
            }
            return retval;
        }
        if (oldchar.vowel != '\000') {
            return 0;
        }

        switch (oldchar.basechar) {
            case 'ل':
                switch (newchar) {
                    case 'ا':
                        oldchar.basechar = 'ﻻ';
                        oldchar.numshapes = 2;
                        retval = 3;
                        break;
                    case 'أ':
                        oldchar.basechar = 'ﻷ';
                        oldchar.numshapes = 2;
                        retval = 3;
                        break;
                    case 'إ':
                        oldchar.basechar = 'ﻹ';
                        oldchar.numshapes = 2;
                        retval = 3;
                        break;
                    case 'آ':
                        oldchar.basechar = 'ﻵ';
                        oldchar.numshapes = 2;
                        retval = 3;
                        break;
                }
                break;
            case '\000':
                oldchar.basechar = newchar;
                oldchar.numshapes = shapecount(newchar);
                retval = 1;
                break;
        }
        return retval;
    }

    static void copycstostring(StringBuffer string, charstruct s, int level) {
        if (s.basechar == '\000') {
            return;
        }
        string.append(s.basechar);
        s.lignum--;
        if (s.mark1 != '\000') {
            if ((level & 0x1) == 0) {
                string.append(s.mark1);
                s.lignum--;
            } else {

                s.lignum--;
            }
        }
        if (s.vowel != '\000') {
            if ((level & 0x1) == 0) {
                string.append(s.vowel);
                s.lignum--;
            } else {

                s.lignum--;
            }
        }
    }

    static void doublelig(StringBuffer string, int level) {
        int len = string.length(), olen = len;
        int j = 0, si = 1;

        while (si < olen) {
            char lapresult = Character.MIN_VALUE;
            if ((level & 0x4) != 0) {
                switch (string.charAt(j)) {
                    case 'ّ':
                        switch (string.charAt(si)) {
                            case 'ِ':
                                lapresult = 'ﱢ';
                                break;
                            case 'َ':
                                lapresult = 'ﱠ';
                                break;
                            case 'ُ':
                                lapresult = 'ﱡ';
                                break;
                            case 'ٌ':
                                lapresult = 'ﱞ';
                                break;
                            case 'ٍ':
                                lapresult = 'ﱟ';
                                break;
                        }
                        break;
                    case 'ِ':
                        if (string.charAt(si) == 'ّ') {
                            lapresult = 'ﱢ';
                        }
                        break;
                    case 'َ':
                        if (string.charAt(si) == 'ّ') {
                            lapresult = 'ﱠ';
                        }
                        break;
                    case 'ُ':
                        if (string.charAt(si) == 'ّ') {
                            lapresult = 'ﱡ';
                        }
                        break;
                }
            }
            if ((level & 0x8) != 0) {
                switch (string.charAt(j)) {
                    case 'ﻟ':
                        switch (string.charAt(si)) {
                            case 'ﺞ':
                                lapresult = 'ﰿ';
                                break;
                            case 'ﺠ':
                                lapresult = 'ﳉ';
                                break;
                            case 'ﺢ':
                                lapresult = 'ﱀ';
                                break;
                            case 'ﺤ':
                                lapresult = 'ﳊ';
                                break;
                            case 'ﺦ':
                                lapresult = 'ﱁ';
                                break;
                            case 'ﺨ':
                                lapresult = 'ﳋ';
                                break;
                            case 'ﻢ':
                                lapresult = 'ﱂ';
                                break;
                            case 'ﻤ':
                                lapresult = 'ﳌ';
                                break;
                        }
                        break;
                    case 'ﺗ':
                        switch (string.charAt(si)) {
                            case 'ﺠ':
                                lapresult = 'ﲡ';
                                break;
                            case 'ﺤ':
                                lapresult = 'ﲢ';
                                break;
                            case 'ﺨ':
                                lapresult = 'ﲣ';
                                break;
                        }
                        break;
                    case 'ﺑ':
                        switch (string.charAt(si)) {
                            case 'ﺠ':
                                lapresult = 'ﲜ';
                                break;
                            case 'ﺤ':
                                lapresult = 'ﲝ';
                                break;
                            case 'ﺨ':
                                lapresult = 'ﲞ';
                                break;
                        }
                        break;
                    case 'ﻧ':
                        switch (string.charAt(si)) {
                            case 'ﺠ':
                                lapresult = 'ﳒ';
                                break;
                            case 'ﺤ':
                                lapresult = 'ﳓ';
                                break;
                            case 'ﺨ':
                                lapresult = 'ﳔ';
                                break;
                        }

                        break;
                    case 'ﻨ':
                        switch (string.charAt(si)) {
                            case 'ﺮ':
                                lapresult = 'ﲊ';
                                break;
                            case 'ﺰ':
                                lapresult = 'ﲋ';
                                break;
                        }
                        break;
                    case 'ﻣ':
                        switch (string.charAt(si)) {
                            case 'ﺠ':
                                lapresult = 'ﳎ';
                                break;
                            case 'ﺤ':
                                lapresult = 'ﳏ';
                                break;
                            case 'ﺨ':
                                lapresult = 'ﳐ';
                                break;
                            case 'ﻤ':
                                lapresult = 'ﳑ';
                                break;
                        }

                        break;
                    case 'ﻓ':
                        switch (string.charAt(si)) {
                            case 'ﻲ':
                                lapresult = 'ﰲ';
                                break;
                        }

                        break;
                }

            }
            if (lapresult != '\000') {
                string.setCharAt(j, lapresult);
                len--;
                si++;

                continue;
            }
            j++;
            string.setCharAt(j, string.charAt(si));
            si++;
        }

        string.setLength(len);
    }

    static boolean connects_to_left(charstruct a) {
        return (a.numshapes > 2);
    }

    static void shape(char[] text, StringBuffer string, int level) {
        int which, p = 0;
        charstruct oldchar = new charstruct();
        charstruct curchar = new charstruct();
        while (p < text.length) {
            char nextletter = text[p++];

            int join = ligature(nextletter, curchar);
            if (join == 0) {
                int nc = shapecount(nextletter);

                if (nc == 1) {
                    which = 0;
                } else {

                    which = 2;
                }
                if (connects_to_left(oldchar)) {
                    which++;
                }

                which %= curchar.numshapes;
                curchar.basechar = charshape(curchar.basechar, which);

                copycstostring(string, oldchar, level);
                oldchar = curchar;

                curchar = new charstruct();
                curchar.basechar = nextletter;
                curchar.numshapes = nc;
                curchar.lignum++;
                continue;
            }
            if (join == 1);
        }

        if (connects_to_left(oldchar)) {
            which = 1;
        } else {
            which = 0;
        }
        which %= curchar.numshapes;
        curchar.basechar = charshape(curchar.basechar, which);

        copycstostring(string, oldchar, level);
        copycstostring(string, curchar, level);
    }

    public static int arabic_shape(char[] src, int srcoffset, int srclength, char[] dest, int destoffset, int destlength, int level) {
        char[] str = new char[srclength];
        for (int k = srclength + srcoffset - 1; k >= srcoffset; k--) {
            str[k - srcoffset] = src[k];
        }
        StringBuffer string = new StringBuffer(srclength);
        shape(str, string, level);
        if ((level & 0xC) != 0) {
            doublelig(string, level);
        }
        System.arraycopy(string.toString().toCharArray(), 0, dest, destoffset, string.length());
        return string.length();
    }

    public static void processNumbers(char[] text, int offset, int length, int options) {
        int limit = offset + length;
        if ((options & 0xE0) != 0) {
            int digitDelta;
            char digitTop;
            int i, j, k;
            char digitBase = '0';
            switch (options & 0x100) {
                case 0:
                    digitBase = '٠';
                    break;

                case 256:
                    digitBase = '۰';
                    break;
            }

            switch (options & 0xE0) {
                case 32:
                    digitDelta = digitBase - 48;
                    for (i = offset; i < limit; i++) {
                        char ch = text[i];
                        if (ch <= '9' && ch >= '0') {
                            text[i] = (char) (text[i] + digitDelta);
                        }
                    }
                    break;

                case 64:
                    digitTop = (char) (digitBase + 9);
                    j = 48 - digitBase;
                    for (k = offset; k < limit; k++) {
                        char ch = text[k];
                        if (ch <= digitTop && ch >= digitBase) {
                            text[k] = (char) (text[k] + j);
                        }
                    }
                    break;

                case 96:
                    shapeToArabicDigitsWithContext(text, 0, length, digitBase, false);
                    break;

                case 128:
                    shapeToArabicDigitsWithContext(text, 0, length, digitBase, true);
                    break;
            }
        }
    }

    static void shapeToArabicDigitsWithContext(char[] dest, int start, int length, char digitBase, boolean lastStrongWasAL) {
        digitBase = (char) (digitBase - 48);

        int limit = start + length;
        for (int i = start; i < limit; i++) {
            char ch = dest[i];
            switch (BidiOrder.getDirection(ch)) {
                case 0:
                case 3:
                    lastStrongWasAL = false;
                    break;
                case 4:
                    lastStrongWasAL = true;
                    break;
                case 8:
                    if (lastStrongWasAL && ch <= '9') {
                        dest[i] = (char) (ch + digitBase);
                    }
                    break;
            }
        }
    }

    public static Character getReverseMapping(char c) {
        return reverseLigatureMapTable.get(Character.valueOf(c));
    }

    private static final char[][] chartable = new char[][]{{'ء', 'ﺀ'}, {'آ', 'ﺁ', 'ﺂ'}, {'أ', 'ﺃ', 'ﺄ'}, {'ؤ', 'ﺅ', 'ﺆ'}, {'إ', 'ﺇ', 'ﺈ'}, {'ئ', 'ﺉ', 'ﺊ', 'ﺋ', 'ﺌ'}, {'ا', 'ﺍ', 'ﺎ'}, {'ب', 'ﺏ', 'ﺐ', 'ﺑ', 'ﺒ'}, {'ة', 'ﺓ', 'ﺔ'}, {'ت', 'ﺕ', 'ﺖ', 'ﺗ', 'ﺘ'}, {'ث', 'ﺙ', 'ﺚ', 'ﺛ', 'ﺜ'}, {'ج', 'ﺝ', 'ﺞ', 'ﺟ', 'ﺠ'}, {'ح', 'ﺡ', 'ﺢ', 'ﺣ', 'ﺤ'}, {'خ', 'ﺥ', 'ﺦ', 'ﺧ', 'ﺨ'}, {'د', 'ﺩ', 'ﺪ'}, {'ذ', 'ﺫ', 'ﺬ'}, {'ر', 'ﺭ', 'ﺮ'}, {'ز', 'ﺯ', 'ﺰ'}, {'س', 'ﺱ', 'ﺲ', 'ﺳ', 'ﺴ'}, {'ش', 'ﺵ', 'ﺶ', 'ﺷ', 'ﺸ'}, {'ص', 'ﺹ', 'ﺺ', 'ﺻ', 'ﺼ'}, {'ض', 'ﺽ', 'ﺾ', 'ﺿ', 'ﻀ'}, {'ط', 'ﻁ', 'ﻂ', 'ﻃ', 'ﻄ'}, {'ظ', 'ﻅ', 'ﻆ', 'ﻇ', 'ﻈ'}, {'ع', 'ﻉ', 'ﻊ', 'ﻋ', 'ﻌ'}, {'غ', 'ﻍ', 'ﻎ', 'ﻏ', 'ﻐ'}, {'ـ', 'ـ', 'ـ', 'ـ', 'ـ'}, {'ف', 'ﻑ', 'ﻒ', 'ﻓ', 'ﻔ'}, {'ق', 'ﻕ', 'ﻖ', 'ﻗ', 'ﻘ'}, {'ك', 'ﻙ', 'ﻚ', 'ﻛ', 'ﻜ'}, {'ل', 'ﻝ', 'ﻞ', 'ﻟ', 'ﻠ'}, {'م', 'ﻡ', 'ﻢ', 'ﻣ', 'ﻤ'}, {'ن', 'ﻥ', 'ﻦ', 'ﻧ', 'ﻨ'}, {'ه', 'ﻩ', 'ﻪ', 'ﻫ', 'ﻬ'}, {'و', 'ﻭ', 'ﻮ'}, {'ى', 'ﻯ', 'ﻰ', 'ﯨ', 'ﯩ'}, {'ي', 'ﻱ', 'ﻲ', 'ﻳ', 'ﻴ'}, {'ٱ', 'ﭐ', 'ﭑ'}, {'ٹ', 'ﭦ', 'ﭧ', 'ﭨ', 'ﭩ'}, {'ٺ', 'ﭞ', 'ﭟ', 'ﭠ', 'ﭡ'}, {'ٻ', 'ﭒ', 'ﭓ', 'ﭔ', 'ﭕ'}, {'پ', 'ﭖ', 'ﭗ', 'ﭘ', 'ﭙ'}, {'ٿ', 'ﭢ', 'ﭣ', 'ﭤ', 'ﭥ'}, {'ڀ', 'ﭚ', 'ﭛ', 'ﭜ', 'ﭝ'}, {'ڃ', 'ﭶ', 'ﭷ', 'ﭸ', 'ﭹ'}, {'ڄ', 'ﭲ', 'ﭳ', 'ﭴ', 'ﭵ'}, {'چ', 'ﭺ', 'ﭻ', 'ﭼ', 'ﭽ'}, {'ڇ', 'ﭾ', 'ﭿ', 'ﮀ', 'ﮁ'}, {'ڈ', 'ﮈ', 'ﮉ'}, {'ڌ', 'ﮄ', 'ﮅ'}, {'ڍ', 'ﮂ', 'ﮃ'}, {'ڎ', 'ﮆ', 'ﮇ'}, {'ڑ', 'ﮌ', 'ﮍ'}, {'ژ', 'ﮊ', 'ﮋ'}, {'ڤ', 'ﭪ', 'ﭫ', 'ﭬ', 'ﭭ'}, {'ڦ', 'ﭮ', 'ﭯ', 'ﭰ', 'ﭱ'}, {'ک', 'ﮎ', 'ﮏ', 'ﮐ', 'ﮑ'}, {'ڭ', 'ﯓ', 'ﯔ', 'ﯕ', 'ﯖ'}, {'گ', 'ﮒ', 'ﮓ', 'ﮔ', 'ﮕ'}, {'ڱ', 'ﮚ', 'ﮛ', 'ﮜ', 'ﮝ'}, {'ڳ', 'ﮖ', 'ﮗ', 'ﮘ', 'ﮙ'}, {'ں', 'ﮞ', 'ﮟ'}, {'ڻ', 'ﮠ', 'ﮡ', 'ﮢ', 'ﮣ'}, {'ھ', 'ﮪ', 'ﮫ', 'ﮬ', 'ﮭ'}, {'ۀ', 'ﮤ', 'ﮥ'}, {'ہ', 'ﮦ', 'ﮧ', 'ﮨ', 'ﮩ'}, {'ۅ', 'ﯠ', 'ﯡ'}, {'ۆ', 'ﯙ', 'ﯚ'}, {'ۇ', 'ﯗ', 'ﯘ'}, {'ۈ', 'ﯛ', 'ﯜ'}, {'ۉ', 'ﯢ', 'ﯣ'}, {'ۋ', 'ﯞ', 'ﯟ'}, {'ی', 'ﯼ', 'ﯽ', 'ﯾ', 'ﯿ'}, {'ې', 'ﯤ', 'ﯥ', 'ﯦ', 'ﯧ'}, {'ے', 'ﮮ', 'ﮯ'}, {'ۓ', 'ﮰ', 'ﮱ'}};

    public static final int ar_nothing = 0;

    public static final int ar_novowel = 1;

    public static final int ar_composedtashkeel = 4;

    public static final int ar_lig = 8;

    public static final int DIGITS_EN2AN = 32;

    public static final int DIGITS_AN2EN = 64;

    public static final int DIGITS_EN2AN_INIT_LR = 96;

    public static final int DIGITS_EN2AN_INIT_AL = 128;

    private static final int DIGITS_RESERVED = 160;

    public static final int DIGITS_MASK = 224;

    public static final int DIGIT_TYPE_AN = 0;

    public static final int DIGIT_TYPE_AN_EXTENDED = 256;

    public static final int DIGIT_TYPE_MASK = 256;

    static class charstruct {

        char basechar;

        char mark1;

        char vowel;

        int lignum;

        int numshapes = 1;
    }

    protected int options = 0;
    protected int runDirection = 3;

    public ArabicLigaturizer() {
    }

    public ArabicLigaturizer(int runDirection, int options) {
        this.runDirection = runDirection;
        this.options = options;
    }

    public String process(String s) {
        return BidiLine.processLTR(s, this.runDirection, this.options);
    }

    public boolean isRTL() {
        return true;
    }

    static {
        for (char[] c : chartable) {
            maptable.put(Character.valueOf(c[0]), c);
            switch (c.length) {

                case 5:
                    reverseLigatureMapTable.put(Character.valueOf(c[4]), Character.valueOf(c[3]));
                case 3:
                    reverseLigatureMapTable.put(Character.valueOf(c[2]), Character.valueOf(c[1]));
                    reverseLigatureMapTable.put(Character.valueOf(c[1]), Character.valueOf(c[0]));
                    break;
            }
            if (c[0] == 'ط' || c[0] == 'ظ') {
                reverseLigatureMapTable.put(Character.valueOf(c[4]), Character.valueOf(c[1]));
                reverseLigatureMapTable.put(Character.valueOf(c[3]), Character.valueOf(c[1]));
            }
        }
    }
}
