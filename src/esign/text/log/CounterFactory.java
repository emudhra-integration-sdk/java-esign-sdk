package esign.text.log;

public class CounterFactory {

    private static CounterFactory myself = new CounterFactory();

    private Counter counter = new DefaultCounter();

    public static CounterFactory getInstance() {
        return myself;
    }

    public static Counter getCounter(Class<?> klass) {
        return myself.counter.getCounter(klass);
    }

    public Counter getCounter() {
        return this.counter;
    }

    public void setCounter(Counter counter) {
        this.counter = counter;
    }
}
