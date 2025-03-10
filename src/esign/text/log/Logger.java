package esign.text.log;

public interface Logger {

    Logger getLogger(Class<?> paramClass);

    Logger getLogger(String paramString);

    boolean isLogging(Level paramLevel);

    void warn(String paramString);

    void trace(String paramString);

    void debug(String paramString);

    void info(String paramString);

    void error(String paramString);

    void error(String paramString, Exception paramException);
}
