package io.github.caioclavico.kafkametamorphosis;

import java.util.Collections;
import java.util.Map;

/**
 * Immutable, Java-friendly view of a Kafka record returned by the
 * {@link KafkaConsumerWrapper}. Independent of any Clojure type.
 */
public final class KafkaRecord {

    private final String topic;
    private final int partition;
    private final long offset;
    private final long timestamp;
    private final String key;
    private final String value;
    private final Map<String, byte[]> headers;

    public KafkaRecord(String topic,
                       int partition,
                       long offset,
                       long timestamp,
                       String key,
                       String value,
                       Map<String, byte[]> headers) {
        this.topic = topic;
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
        this.key = key;
        this.value = value;
        this.headers = headers == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(headers);
    }

    public String topic()                  { return topic; }
    public int partition()                 { return partition; }
    public long offset()                   { return offset; }
    public long timestamp()                { return timestamp; }
    public String key()                    { return key; }
    public String value()                  { return value; }
    public Map<String, byte[]> headers()   { return headers; }

    @Override
    public String toString() {
        return "KafkaRecord{topic=" + topic
                + ", partition=" + partition
                + ", offset=" + offset
                + ", key=" + key
                + ", value=" + value + '}';
    }
}
