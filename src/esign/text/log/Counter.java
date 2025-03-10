package esign.text.log;

public interface Counter {
  Counter getCounter(Class<?> paramClass);
  
  void read(long paramLong);
  
  void written(long paramLong);
}

