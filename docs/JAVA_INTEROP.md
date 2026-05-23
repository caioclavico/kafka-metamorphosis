# Java Interop Guide — kafka-metamorphosis

This document explains how the Clojure library **kafka-metamorphosis** is
exposed as a **Java-friendly JAR** that any Java / Spring Boot project can
consume with zero knowledge of Clojure.

> The Java-facing API lives under
> `io.github.caioclavico.kafkametamorphosis.*` and is produced by Clojure
> `gen-class` namespaces, plus a handful of plain Java POJOs / interfaces.
> No `clojure.java.api.Clojure` and no `IFn.invoke` ever appear in user
> code.

---

## 1. Project structure

```
kafka-metamorphosis/
├── project.clj
├── java/                                          # Plain Java sources (compiled FIRST)
│   └── io/github/caioclavico/kafkametamorphosis/
│       ├── KafkaConfig.java                       # Fluent config builder
│       ├── KafkaRecord.java                       # Immutable record POJO
│       ├── MessageHandler.java                    # @FunctionalInterface
│       └── KafkaMetamorphosisException.java       # RuntimeException
└── src/kafka_metamorphosis/
    ├── core.clj                                   # Existing Clojure API
    ├── producer.clj
    ├── consumer.clj
    ├── admin.clj
    └── java/                                      # Java-facing bridges (AOT)
        ├── interop.clj                            # Internal helpers
        ├── kafka_producer_wrapper.clj             # → KafkaProducerWrapper
        ├── kafka_consumer_wrapper.clj             # → KafkaConsumerWrapper
        └── kafka_admin_wrapper.clj                # → KafkaAdminWrapper
```

**Build order (Leiningen default `:prep-tasks ["javac" "compile"]`):**

1. `javac` compiles the POJOs / interfaces under `java/`.
2. Clojure AOT (`:aot [...]`) compiles the `gen-class` namespaces, which
   safely reference those Java types.
3. `lein jar` packages everything into a single JAR.

There is **no circular dependency**: Java code never references a
gen-class output, only the Clojure side does — and that runs after javac.

---

## 2. The Java surface

```java
// Quick-start
try (KafkaProducerWrapper producer = new KafkaProducerWrapper()) {
    producer.publish("orders.new", "123", "{\"ticker\":\"PETR4\"}");
}

// Explicit config
Map<String, Object> cfg = KafkaConfig.create()
        .bootstrapServers("broker:9092")
        .acks("all")
        .retries(5)
        .keySerializer("org.apache.kafka.common.serialization.StringSerializer")
        .valueSerializer("org.apache.kafka.common.serialization.StringSerializer")
        .toMap();

try (KafkaProducerWrapper producer = new KafkaProducerWrapper(cfg)) {
    producer.publishJson("orders.new", "abc", "{\"ticker\":\"VALE3\"}");
}

// Consumer with lambda handler
try (KafkaConsumerWrapper consumer = new KafkaConsumerWrapper("orders-service")) {
    consumer.subscribe(List.of("orders.new"));
    consumer.consume(1000L, record ->
        System.out.println(record.topic() + " => " + record.value()));
}

// Admin
try (KafkaAdminWrapper admin = new KafkaAdminWrapper()) {
    admin.createTopicIfNotExists("orders.new", 3, 1);
}
```

All wrappers implement `AutoCloseable`, so `try-with-resources` is the
canonical lifecycle pattern.

---

## 3. Why `gen-class` instead of `clojure.java.api.Clojure`

| Concern | `Clojure.var(...).invoke(...)` | `gen-class` (this lib) |
|---|---|---|
| Java IDE support | Untyped `Object` everywhere | Real classes, real methods, real javadoc |
| Reflection | Heavy at call site | Zero at call site (real bytecode) |
| Exception surface | `clojure.lang.ExceptionInfo` leaks | Only `KafkaMetamorphosisException` |
| Spring DI | Awkward (manual wiring) | `@Bean KafkaProducerWrapper` works out of the box |

`gen-class` emits real `.class` files with stable signatures during AOT.
From Java's perspective, `KafkaProducerWrapper` *is* a normal class.

---

## 4. Organization of namespaces

* **Public Clojure core** — `kafka-metamorphosis.{core,producer,consumer,admin,serializers,schema}`
  unchanged; the existing API stays idiomatic for Clojure consumers.
* **Java bridge layer** — `kafka-metamorphosis.java.*`. Only this layer
  uses `gen-class` and is allowed to depend on Java types from `java/`.
* **Java public types** — `io.github.caioclavico.kafkametamorphosis.*`.
  Only POJOs / interfaces / exceptions live here.

This keeps the Clojure core decoupled from the JVM facade: you can evolve
the Clojure API freely as long as the bridge layer absorbs the changes.

---

## 5. Avoiding reflection

* Every bridge namespace declares `(set! *warn-on-reflection* true)`.
* `gen-class` declares typed `:methods` signatures, so the call site
  bytecode uses `INVOKEVIRTUAL`/`INVOKESTATIC` directly — no
  `java.lang.reflect.Method` involved.
* Internally, type hints (`^String`, `^Map`, `^java.util.List`) are used
  on every Java-typed parameter.
* Uberjar profile sets `-Dclojure.compiler.direct-linking=true` which
  removes the `Var` indirection from inter-namespace Clojure calls.

Build with `lein check` to confirm zero reflection warnings.

---

## 6. JVM interop pitfalls handled for you

| Pitfall | Solution applied |
|---|---|
| Clojure `nil` collections | `interop/java-map->config` tolerates `null` |
| Mutable Kafka properties | Bridges always copy into immutable Clojure maps |
| Headers as `Iterable` | Materialized into a `HashMap<String, byte[]>` |
| Clojure keywords leaking | Records are returned as `KafkaRecord` POJOs |
| Lazy seqs across the boundary | `poll()` returns an eagerly-realized `ArrayList` |
| `ClassNotFoundException` for `clojure.core` | AOT places `clojure/core__init.class` inside the JAR |

---

## 7. Exception handling

Every bridge method is wrapped with `interop/with-kafka-ex`, which:

1. Lets `KafkaMetamorphosisException` pass through.
2. Wraps any other `Throwable` (Kafka client errors, Clojure runtime
   errors, `IllegalArgumentException`, etc.) into
   `KafkaMetamorphosisException` with a useful message and the original
   cause attached.

So in Java you only need to catch one type:

```java
try {
    producer.publish("orders.new", "k", "v");
} catch (KafkaMetamorphosisException ex) {
    log.error("Kafka publish failed", ex);   // ex.getCause() is the original
}
```

---

## 8. Logging

The Clojure side uses `clojure.tools.logging`, which is a façade over
SLF4J. **You add no logging dependency in the library**: when your Spring
Boot app pulls in Logback (or Log4j2) via `spring-boot-starter`, the
library automatically logs through it. Logger names look like
`kafka_metamorphosis.java.interop` — configurable in
`application.yml`:

```yaml
logging:
  level:
    kafka_metamorphosis: INFO
```

---

## 9. Reusable Kafka configuration

`KafkaConfig` provides typed fluent setters for the most common
properties and an escape hatch (`set(String, Object)`) for the rest.
Internally, the bridge accepts:

* `new KafkaProducerWrapper()` → `localhost:9092` defaults
* `new KafkaProducerWrapper("broker:9092")` → broker override only
* `new KafkaProducerWrapper(Map<String,Object>)` → full control
  (kebab-case or dot-notation keys both accepted)

You can centralize configuration in a Spring `@ConfigurationProperties`
bean and feed it to the wrapper via the `Map` constructor.

---

## 10. Packaging the JAR

```bash
lein clean
lein check       # must be reflection-clean
lein test
lein jar         # → target/kafka-metamorphosis-X.Y.Z.jar
lein install     # install to local ~/.m2 for downstream testing
```

The resulting JAR contains:

* Your Clojure sources and AOT-compiled `.class` files
* `clojure/core__init.class` and friends (mandatory for `gen-class`)
* The plain `.class` files compiled from `java/`
* A `pom.xml` with `org.clojure/clojure` and `kafka-clients` as
  transitive dependencies

> **Note:** consumers will pull in `org.clojure/clojure` as a runtime
> dependency automatically. This is **mandatory** — you can not strip
> Clojure from a `gen-class`-based JAR.

---

## 11. Publishing to Clojars

The existing `project.clj` already configures the Clojars repository.
For a Java-consumer-friendly release:

```bash
export CLOJARS_USERNAME=caioclavico
export CLOJARS_PASSWORD='<token>'
lein deploy clojars
```

The Maven coordinates that Java consumers will use:

```
groupId:    org.clojars.caioclavico
artifactId: kafka-metamorphosis
version:    0.4.3   (whatever is in project.clj)
```

For wider discoverability you can also mirror to **Maven Central** via
Sonatype OSSRH; the existing `:scm` and `:pom-addition` blocks already
contain the metadata Central requires.

---

## 12. Consuming via Maven / Gradle

**Maven** (`pom.xml`):

```xml
<repositories>
  <repository>
    <id>clojars</id>
    <url>https://repo.clojars.org/</url>
  </repository>
</repositories>

<dependency>
  <groupId>org.clojars.caioclavico</groupId>
  <artifactId>kafka-metamorphosis</artifactId>
  <version>0.4.3</version>
</dependency>
```

**Gradle** (`build.gradle.kts`):

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://repo.clojars.org/") }
}

dependencies {
    implementation("org.clojars.caioclavico:kafka-metamorphosis:0.4.3")
}
```

Spring Boot will resolve `kafka-clients` transitively. If you need a
specific Kafka client version, add an explicit `<dependency>` and Maven
will pick yours over the transitive one.

---

## 13. Spring Boot integration (no controllers, no REST)

The wrappers are plain JVM classes — declare them as beans:

```java
@Configuration
public class KafkaBeans {

    @Bean(destroyMethod = "close")
    KafkaProducerWrapper kafkaProducer(KafkaProps props) {
        return new KafkaProducerWrapper(
            KafkaConfig.create()
                .bootstrapServers(props.getBootstrapServers())
                .acks("all")
                .keySerializer("org.apache.kafka.common.serialization.StringSerializer")
                .valueSerializer("org.apache.kafka.common.serialization.StringSerializer")
                .toMap());
    }
}

@Service
@RequiredArgsConstructor
public class OrderPublisher {
    private final KafkaProducerWrapper producer;

    public void publish(Order order) {
        producer.publishJson("orders.new", order.id(), toJson(order));
    }
}
```

That's it — no controllers, no REST, no standalone app, just a
dependency.

---

## 14. Suggested architecture & evolution

| Concern | Where it lives | How to extend |
|---|---|---|
| **Performance** | `direct-linking=true` in uberjar; eager `ArrayList` from `poll()`; no reflection | Cache producer/consumer beans; reuse `KafkaProducerWrapper` across threads (the underlying `KafkaProducer` is thread-safe) |
| **Low coupling** | Clojure core ≠ Java facade ≠ POJOs | Add new methods only in bridge namespaces; never touch core for Java-specific concerns |
| **Maintainability** | One bridge per Kafka client type | Mirror the Clojure namespace surface 1-to-1 |
| **Elegant API** | `AutoCloseable`, fluent `KafkaConfig`, functional `MessageHandler` | Add typed builders for headers, transactions, etc. |
| **Future: async send** | Add `CompletableFuture<Void> publishAsync(...)` to producer wrapper, delegating to `producer/send-async!` | The bridge already supports it via callback fn |
| **Future: retry / DLQ** | New namespace `kafka-metamorphosis.java.retry-policy` + class `RetryPolicy` (POJO) accepted by `publish(..., RetryPolicy)` | Pure Clojure implementation behind the bridge |
| **Future: Avro / Schema Registry** | Already supported in Clojure via `:schemas`; expose through `KafkaConfig.schemaRegistryUrl(...)` and a typed `Schema` enum on the Java side | Add `publishAvro(String topic, String key, GenericRecord value)` to the wrapper |

The golden rule: **all evolution happens in the bridge layer**. The
Clojure core remains pristine and idiomatic for Clojure users; the Java
facade absorbs all interop concerns.
