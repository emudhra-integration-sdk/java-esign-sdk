package esign.text.pdf.languages;

import java.util.Comparator;

public class IndicCompositeCharacterComparator
        implements Comparator<String> {

    public int compare(String o1, String o2) {
        if (o2.length() > o1.length()) {
            return 1;
        }
        if (o1.length() > o2.length()) {
            return -1;
        }
        return o1.compareTo(o2);
    }
}
