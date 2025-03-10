package esign.text.pdf.languages;

public class DevanagariLigaturizer
        extends IndicLigaturizer {

    public static final char DEVA_MATRA_AA = 'ा';
    public static final char DEVA_MATRA_I = 'ि';
    public static final char DEVA_MATRA_E = 'े';
    public static final char DEVA_MATRA_AI = 'ै';
    public static final char DEVA_MATRA_HLR = 'ॢ';
    public static final char DEVA_MATRA_HLRR = 'ॣ';
    public static final char DEVA_LETTER_A = 'अ';
    public static final char DEVA_LETTER_AU = 'औ';
    public static final char DEVA_LETTER_KA = 'क';
    public static final char DEVA_LETTER_HA = 'ह';
    public static final char DEVA_HALANTA = '्';

    public DevanagariLigaturizer() {
        this.langTable = new char[11];
        this.langTable[0] = 'ा';
        this.langTable[1] = 'ि';
        this.langTable[2] = 'े';
        this.langTable[3] = 'ै';
        this.langTable[4] = 'ॢ';
        this.langTable[5] = 'ॣ';
        this.langTable[6] = 'अ';
        this.langTable[7] = 'औ';
        this.langTable[8] = 'क';
        this.langTable[9] = 'ह';
        this.langTable[10] = '्';
    }
}

