package esign.text.log;

public class SysoCounter
        implements Counter {

    protected String name;

    public SysoCounter() {
        this.name = "iText";
    }

    protected SysoCounter(Class<?> klass) {
        this.name = klass.getName();
    }

    public Counter getCounter(Class<?> klass) {
        return new SysoCounter(klass);
    }

    public void read(long l) {
        System.out.println(String.format("[%s] %s bytes read", new Object[]{this.name, Long.valueOf(l)}));
    }

    public void written(long l) {
        System.out.println(String.format("[%s] %s bytes written", new Object[]{this.name, Long.valueOf(l)}));
    }
}
