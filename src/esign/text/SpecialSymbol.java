package esign.text;

public class SpecialSymbol {

    public static int index(String string) {
        int length = string.length();
        for (int i = 0; i < length; i++) {
            if (getCorrespondingSymbol(string.charAt(i)) != ' ') {
                return i;
            }
        }
        return -1;
    }

    public static Chunk get(char c, Font font) {
        char greek = getCorrespondingSymbol(c);
        if (greek == ' ') {
            return new Chunk(String.valueOf(c), font);
        }
        Font symbol = new Font(Font.FontFamily.SYMBOL, font.getSize(), font.getStyle(), font.getColor());
        String s = String.valueOf(greek);
        return new Chunk(s, symbol);
    }

    public static char getCorrespondingSymbol(char c) {
        switch (c) {
            case 'Α':
                return 'A';
            case 'Β':
                return 'B';
            case 'Γ':
                return 'G';
            case 'Δ':
                return 'D';
            case 'Ε':
                return 'E';
            case 'Ζ':
                return 'Z';
            case 'Η':
                return 'H';
            case 'Θ':
                return 'Q';
            case 'Ι':
                return 'I';
            case 'Κ':
                return 'K';
            case 'Λ':
                return 'L';
            case 'Μ':
                return 'M';
            case 'Ν':
                return 'N';
            case 'Ξ':
                return 'X';
            case 'Ο':
                return 'O';
            case 'Π':
                return 'P';
            case 'Ρ':
                return 'R';
            case 'Σ':
                return 'S';
            case 'Τ':
                return 'T';
            case 'Υ':
                return 'U';
            case 'Φ':
                return 'F';
            case 'Χ':
                return 'C';
            case 'Ψ':
                return 'Y';
            case 'Ω':
                return 'W';
            case 'α':
                return 'a';
            case 'β':
                return 'b';
            case 'γ':
                return 'g';
            case 'δ':
                return 'd';
            case 'ε':
                return 'e';
            case 'ζ':
                return 'z';
            case 'η':
                return 'h';
            case 'θ':
                return 'q';
            case 'ι':
                return 'i';
            case 'κ':
                return 'k';
            case 'λ':
                return 'l';
            case 'μ':
                return 'm';
            case 'ν':
                return 'n';
            case 'ξ':
                return 'x';
            case 'ο':
                return 'o';
            case 'π':
                return 'p';
            case 'ρ':
                return 'r';
            case 'ς':
                return 'V';
            case 'σ':
                return 's';
            case 'τ':
                return 't';
            case 'υ':
                return 'u';
            case 'φ':
                return 'f';
            case 'χ':
                return 'c';
            case 'ψ':
                return 'y';
            case 'ω':
                return 'w';
        }
        return ' ';
    }
}
