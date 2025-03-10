package esign.text.log;

public class SysoLogger
        implements Logger {

    private String name;
    private final int shorten;

    public SysoLogger() {
        this(1);
    }

    public SysoLogger(int packageReduce) {
        this.shorten = packageReduce;
    }

    protected SysoLogger(String klass, int shorten) {
        this.shorten = shorten;
        this.name = klass;
    }

    public Logger getLogger(Class<?> klass) {
        return new SysoLogger(klass.getName(), this.shorten);
    }

    public Logger getLogger(String name) {
        return new SysoLogger("[itext]", 0);
    }

    public boolean isLogging(Level level) {
        return true;
    }

    public void warn(String message) {
        System.out.println(String.format("%s WARN  %s", new Object[]{shorten(this.name), message}));
    }

    private String shorten(String className) {
        if (this.shorten != 0) {
            StringBuilder target = new StringBuilder();
            String name = className;
            int fromIndex = className.indexOf('.');
            while (fromIndex != -1) {
                int parseTo = (fromIndex < this.shorten) ? fromIndex : this.shorten;
                target.append(name.substring(0, parseTo));
                target.append('.');
                name = name.substring(fromIndex + 1);
                fromIndex = name.indexOf('.');
            }
            target.append(className.substring(className.lastIndexOf('.') + 1));
            return target.toString();
        }
        return className;
    }

    public void trace(String message) {
        System.out.println(String.format("%s TRACE %s", new Object[]{shorten(this.name), message}));
    }

    public void debug(String message) {
        System.out.println(String.format("%s DEBUG %s", new Object[]{shorten(this.name), message}));
    }

    public void info(String message) {
        System.out.println(String.format("%s INFO  %s", new Object[]{shorten(this.name), message}));
    }

    public void error(String message) {
        System.out.println(String.format("%s ERROR %s", new Object[]{this.name, message}));
    }

    public void error(String message, Exception e) {
        System.out.println(String.format("%s ERROR %s", new Object[]{this.name, message}));
        e.printStackTrace(System.out);
    }
}
