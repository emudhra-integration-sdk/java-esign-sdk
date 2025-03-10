package esign.text.pdf;

import esign.text.SplitCharacter;

public class DefaultSplitCharacter
        implements SplitCharacter {

    public static final SplitCharacter DEFAULT = new DefaultSplitCharacter();

    protected char[] characters;

    public DefaultSplitCharacter() {
    }

    public DefaultSplitCharacter(char character) {
        this(new char[]{character});
    }

    public DefaultSplitCharacter(char[] characters) {
        this.characters = characters;
    }

    public boolean isSplitCharacter(int start, int current, int end, char[] cc, PdfChunk[] ck) {
        char c = getCurrentCharacter(current, cc, ck);

        if (this.characters != null) {
            for (int i = 0; i < this.characters.length; i++) {
                if (c == this.characters[i]) {
                    return true;
                }
            }
            return false;
        }

        if (c <= ' ' || c == '-' || c == '‐') {
            return true;
        }
        if (c < ' ') {
            return false;
        }
        return ((c >= ' ' && c <= '​') || (c >= '⺀' && c < '힠') || (c >= '豈' && c < 'ﬀ') || (c >= '︰' && c < '﹐') || (c >= '｡' && c < 'ﾠ'));
    }

    protected char getCurrentCharacter(int current, char[] cc, PdfChunk[] ck) {
        if (ck == null) {
            return cc[current];
        }
        return (char) ck[Math.min(current, ck.length - 1)].getUnicodeEquivalent(cc[current]);
    }
}
