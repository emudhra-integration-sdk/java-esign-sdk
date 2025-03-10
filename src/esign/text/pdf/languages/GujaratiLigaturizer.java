package esign.text.pdf.languages;

public class GujaratiLigaturizer
        extends IndicLigaturizer {

    public static final char GUJR_MATRA_AA = 'ા';
    public static final char GUJR_MATRA_I = 'િ';
    public static final char GUJR_MATRA_E = 'ે';
    public static final char GUJR_MATRA_AI = 'ૈ';
    public static final char GUJR_MATRA_HLR = 'ૢ';
    public static final char GUJR_MATRA_HLRR = 'ૣ';
    public static final char GUJR_LETTER_A = 'અ';
    public static final char GUJR_LETTER_AU = 'ઔ';
    public static final char GUJR_LETTER_KA = 'ક';
    public static final char GUJR_LETTER_HA = 'હ';
    public static final char GUJR_HALANTA = '્';

    public GujaratiLigaturizer() {
        this.langTable = new char[11];
        this.langTable[0] = 'ા';
        this.langTable[1] = 'િ';
        this.langTable[2] = 'ે';
        this.langTable[3] = 'ૈ';
        this.langTable[4] = 'ૢ';
        this.langTable[5] = 'ૣ';
        this.langTable[6] = 'અ';
        this.langTable[7] = 'ઔ';
        this.langTable[8] = 'ક';
        this.langTable[9] = 'હ';
        this.langTable[10] = '્';
    }
}
