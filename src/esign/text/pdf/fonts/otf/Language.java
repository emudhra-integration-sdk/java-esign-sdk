package esign.text.pdf.fonts.otf;

import java.util.Arrays;
import java.util.List;

public enum Language {
    BENGALI(new String[]{"beng", "bng2"});

    private final List<String> codes;

    Language(String... codes) {
        this.codes = Arrays.asList(codes);
    }

    public boolean isSupported(String languageCode) {
        return this.codes.contains(languageCode);
    }
}
