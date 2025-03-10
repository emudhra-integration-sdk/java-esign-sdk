package esign.text.error_messages;

import esign.text.io.StreamUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;

public final class MessageLocalization {

    private static HashMap<String, String> defaultLanguage = new HashMap<String, String>();

    private static HashMap<String, String> currentLanguage;

    private static final String BASE_PATH = "com/itextpdf/text/l10n/error/";

    static {
        try {
            defaultLanguage = getLanguageMessages("en", null);
        } catch (Exception exception) {
        }

        if (defaultLanguage == null) {
            defaultLanguage = new HashMap<String, String>();
        }
    }

    public static String getMessage(String key) {
        return getMessage(key, true);
    }

    public static String getMessage(String key, boolean useDefaultLanguageIfMessageNotFound) {
        HashMap<String, String> cl = currentLanguage;

        if (cl != null) {
            String val = cl.get(key);
            if (val != null) {
                return val;
            }
        }
        if (useDefaultLanguageIfMessageNotFound) {
            cl = defaultLanguage;
            String val = cl.get(key);
            if (val != null) {
                return val;
            }
        }
        return "No message found for " + key;
    }

    public static String getComposedMessage(String key, int p1) {
        return getComposedMessage(key, new Object[]{String.valueOf(p1), null, null, null});
    }

    public static String getComposedMessage(String key, Object... param) {
        String msg = getMessage(key);
        if (null != param) {
            int i = 1;
            for (Object o : param) {
                if (null != o) {
                    msg = msg.replace("{" + i + "}", o.toString());
                }
                i++;
            }
        }
        return msg;
    }

    public static boolean setLanguage(String language, String country) throws IOException {
        HashMap<String, String> lang = getLanguageMessages(language, country);
        if (lang == null) {
            return false;
        }
        currentLanguage = lang;
        return true;
    }

    public static void setMessages(Reader r) throws IOException {
        currentLanguage = readLanguageStream(r);
    }

    private static HashMap<String, String> getLanguageMessages(String language, String country) throws IOException {
        if (language == null) {
            throw new IllegalArgumentException("The language cannot be null.");
        }
        InputStream is = null;
        String file = language + ".lng";
        try {
            if (country != null) {
                file = language + "_" + country + ".lng";
            } else {
                file = language + ".lng";
            }
            is = StreamUtil.getResourceStream("com/itextpdf/text/l10n/error/" + file, (new MessageLocalization()).getClass().getClassLoader());
            if (is != null) {
                return readLanguageStream(is);
            }
            if (country == null) {
                return null;
            }

            is = StreamUtil.getResourceStream("com/itextpdf/text/l10n/error/" + file, (new MessageLocalization()).getClass().getClassLoader());
            if (is != null) {
                return readLanguageStream(is);
            }
            return null;
        } finally {

            try {
                if (null != is) {
                    is.close();
                }
            } catch (Exception exception) {
            }
        }
    }

    private static HashMap<String, String> readLanguageStream(InputStream is) throws IOException {
        return readLanguageStream(new InputStreamReader(is, "UTF-8"));
    }

    private static HashMap<String, String> readLanguageStream(Reader r) throws IOException {
        HashMap<String, String> lang = new HashMap<String, String>();
        BufferedReader br = new BufferedReader(r);
        String line;
        while ((line = br.readLine()) != null) {
            int idxeq = line.indexOf('=');
            if (idxeq < 0) {
                continue;
            }
            String key = line.substring(0, idxeq).trim();
            if (key.startsWith("#")) {
                continue;
            }
            lang.put(key, line.substring(idxeq + 1));
        }
        return lang;
    }
}
