package io.github.caioclavico.kafkametamorphosis;

/**
 * Functional handler invoked by {@link KafkaConsumerWrapper#consume(long, MessageHandler)}
 * for every record polled from Kafka. Implement as a lambda for an elegant call site:
 *
 * <pre>
 *   consumer.consume(1000L, record -&gt; {
 *       System.out.println(record.value());
 *   });
 * </pre>
 */
@FunctionalInterface
public interface MessageHandler {
    void onMessage(KafkaRecord record);
}
