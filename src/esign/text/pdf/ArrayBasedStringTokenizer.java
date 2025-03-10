package esign.text.pdf;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArrayBasedStringTokenizer {

    private final Pattern regex;

    public ArrayBasedStringTokenizer(String[] tokens) {
        this.regex = Pattern.compile(getRegexFromTokens(tokens));
    }

    public String[] tokenize(String text) {
        List<String> tokens = new ArrayList<String>();

        Matcher matcher = this.regex.matcher(text);

        int endIndexOfpreviousMatch = 0;

        while (matcher.find()) {

            int startIndexOfMatch = matcher.start();

            String previousToken = text.substring(endIndexOfpreviousMatch, startIndexOfMatch);

            if (previousToken.length() > 0) {
                tokens.add(previousToken);
            }

            String currentMatch = matcher.group();

            tokens.add(currentMatch);

            endIndexOfpreviousMatch = matcher.end();
        }

        String tail = text.substring(endIndexOfpreviousMatch, text.length());

        if (tail.length() > 0) {
            tokens.add(tail);
        }

        return tokens.<String>toArray(new String[0]);
    }

    private String getRegexFromTokens(String[] tokens) {
        StringBuilder regexBuilder = new StringBuilder(100);

        for (String token : tokens) {
            regexBuilder.append("(").append(token).append(")|");
        }

        regexBuilder.setLength(regexBuilder.length() - 1);

        String regex = regexBuilder.toString();

        return regex;
    }
}
