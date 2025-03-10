package esign.text.pdf;

import esign.text.Chunk;
import esign.text.Element;
import esign.text.Font;
import esign.text.Phrase;
import esign.text.Utilities;
import esign.text.error_messages.MessageLocalization;
import java.util.ArrayList;

public class FontSelector {

    protected ArrayList<Font> fonts = new ArrayList<Font>();
    protected Font currentFont = null;

    public void addFont(Font font) {
        if (font.getBaseFont() != null) {
            this.fonts.add(font);
            return;
        }
        BaseFont bf = font.getCalculatedBaseFont(true);
        Font f2 = new Font(bf, font.getSize(), font.getCalculatedStyle(), font.getColor());
        this.fonts.add(f2);
    }

    public Phrase process(String text) {
        if (this.fonts.size() == 0) {
            throw new IndexOutOfBoundsException(MessageLocalization.getComposedMessage("no.font.is.defined", new Object[0]));
        }
        char[] cc = text.toCharArray();
        int len = cc.length;
        StringBuffer sb = new StringBuffer();
        Phrase ret = new Phrase();
        this.currentFont = null;
        for (int k = 0; k < len; k++) {
            Chunk newChunk = processChar(cc, k, sb);
            if (newChunk != null) {
                ret.add((Element) newChunk);
            }
        }
        if (sb.length() > 0) {
            Chunk ck = new Chunk(sb.toString(), (this.currentFont != null) ? this.currentFont : this.fonts.get(0));
            ret.add((Element) ck);
        }
        return ret;
    }

    protected Chunk processChar(char[] cc, int k, StringBuffer sb) {
        Chunk newChunk = null;
        char c = cc[k];
        if (c == '\n' || c == '\r') {
            sb.append(c);
        } else {
            Font font = null;
            if (Utilities.isSurrogatePair(cc, k)) {
                int u = Utilities.convertToUtf32(cc, k);
                for (int f = 0; f < this.fonts.size(); f++) {
                    font = this.fonts.get(f);
                    if (font.getBaseFont().charExists(u) || Character.getType(u) == 16) {
                        if (this.currentFont != font) {
                            if (sb.length() > 0 && this.currentFont != null) {
                                newChunk = new Chunk(sb.toString(), this.currentFont);
                                sb.setLength(0);
                            }
                            this.currentFont = font;
                        }
                        sb.append(c);
                        sb.append(cc[++k]);
                        break;
                    }
                }
            } else {
                for (int f = 0; f < this.fonts.size(); f++) {
                    font = this.fonts.get(f);
                    if (font.getBaseFont().charExists(c) || Character.getType(c) == 16) {
                        if (this.currentFont != font) {
                            if (sb.length() > 0 && this.currentFont != null) {
                                newChunk = new Chunk(sb.toString(), this.currentFont);
                                sb.setLength(0);
                            }
                            this.currentFont = font;
                        }
                        sb.append(c);
                        break;
                    }
                }
            }
        }
        return newChunk;
    }
}
