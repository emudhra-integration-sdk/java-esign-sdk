package esign.text.log;

public class NoOpCounter
        implements Counter {

    public Counter getCounter(Class<?> klass) {
        return this;
    }

    public void read(long l) {
    }

    public void written(long l) {
    }
}
