package io.github.caioclavico.kafkametamorphosis;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fluent, type-safe builder for Kafka configuration maps consumed by
 * {@link KafkaProducerWrapper}, {@link KafkaConsumerWrapper} and
 * {@link KafkaAdminWrapper}.
 *
 * <p>Keys are kept in standard Kafka dot-notation (e.g. {@code bootstrap.servers}).
 * The underlying Clojure layer normalizes both kebab-case and dot-notation, so
 * this class always emits the canonical form to keep the API explicit.
 */
public final class KafkaConfig {

    private final Map<String, Object> entries = new LinkedHashMap<>();

    public static KafkaConfig create() {
        return new KafkaConfig();
    }

    public KafkaConfig bootstrapServers(String value) {
        return set("bootstrap.servers", value);
    }

    public KafkaConfig groupId(String value) {
        return set("group.id", value);
    }

    public KafkaConfig clientId(String value) {
        return set("client.id", value);
    }

    public KafkaConfig acks(String value) {
        return set("acks", value);
    }

    public KafkaConfig retries(int value) {
        return set("retries", value);
    }

    public KafkaConfig autoOffsetReset(String value) {
        return set("auto.offset.reset", value);
    }

    public KafkaConfig enableAutoCommit(boolean value) {
        return set("enable.auto.commit", value);
    }

    public KafkaConfig keySerializer(String fqn)       { return set("key.serializer", fqn); }
    public KafkaConfig valueSerializer(String fqn)     { return set("value.serializer", fqn); }
    public KafkaConfig keyDeserializer(String fqn)     { return set("key.deserializer", fqn); }
    public KafkaConfig valueDeserializer(String fqn)   { return set("value.deserializer", fqn); }

    /** Set any arbitrary Kafka property. */
    public KafkaConfig set(String key, Object value) {
        entries.put(key, value);
        return this;
    }

    public Map<String, Object> toMap() {
        return new HashMap<>(entries);
    }
}
