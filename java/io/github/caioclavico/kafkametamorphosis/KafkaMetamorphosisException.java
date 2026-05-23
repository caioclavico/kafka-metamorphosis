package io.github.caioclavico.kafkametamorphosis;

/**
 * Unchecked exception thrown by the Java-friendly facade of
 * kafka-metamorphosis. Wraps any underlying Kafka or Clojure runtime
 * failure so that Java callers only ever see one exception type.
 */
public class KafkaMetamorphosisException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public KafkaMetamorphosisException(String message) {
        super(message);
    }

    public KafkaMetamorphosisException(String message, Throwable cause) {
        super(message, cause);
    }
}
