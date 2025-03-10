package esign.text.pdf;

public class Glyph {

    public final int code;
    public final int width;
    public final String chars;

    public Glyph(int code, int width, String chars) {
        this.code = code;
        this.width = width;
        this.chars = chars;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + ((this.chars == null) ? 0 : this.chars.hashCode());
        result = 31 * result + this.code;
        result = 31 * result + this.width;
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Glyph other = (Glyph) obj;
        if (this.chars == null) {
            if (other.chars != null) {
                return false;
            }
        } else if (!this.chars.equals(other.chars)) {
            return false;
        }
        if (this.code != other.code) {
            return false;
        }
        if (this.width != other.width) {
            return false;
        }
        return true;
    }

    public String toString() {
        return Glyph.class.getSimpleName() + " [id=" + this.code + ", width=" + this.width + ", chars=" + this.chars + "]";
    }
}
