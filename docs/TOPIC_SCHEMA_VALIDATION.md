# Topic-Scoped Schema Validation - v0.4.0

## 🎯 New Features Implemented

### **1. Topic-Scoped Schemas**

You can now define topic-specific schemas using the `:topic-name/schema-id` convention:

```clojure
;; Schema for 'users' topic
(schema/defschema :users/default {:user-id int? :name string?})
(schema/defschema :users/profile {:user-id int? :name string? :bio string?})

;; Direct topic schema
(schema/defschema :orders {:order-id string? :total double?})
```

### **2. Automatic Topic Validation**

```clojure
;; Auto-detect schema based on topic name
(schema/validate-message-for-topic message "users")  ; uses :users/default
(schema/explain-validation-for-topic message "users") ; detailed explanation
```

### **3. Producer and Consumer with Automatic Validation**

Both producer and consumer now support automatic validation through the `:schemas` configuration:

```clojure
;; Producer with auto-validation
(def producer (producer/create {:bootstrap-servers "localhost:9092"
                               :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                               :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                               :schemas true}))  ; ← ENABLES AUTOMATIC VALIDATION!

;; Consumer with auto-validation
(def consumer (consumer/create {:bootstrap-servers "localhost:9092"
                               :group-id "my-group"
                               :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                               :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                               :auto-offset-reset "earliest"
                               :schemas true}))  ; ← ENABLES AUTOMATIC VALIDATION!

;; Normal usage - validation happens automatically
(producer/send! producer "users" "key" {:user-id 123 :name "John"})
(consumer/subscribe! consumer ["users"])
(let [records (consumer/poll! consumer 1000)]
  (doseq [record records]
    (println "Consumed:" (:value record))))  ; Warnings shown for invalid messages

(producer/close! producer)
(consumer/close! consumer)
```

### **4. Total Configuration Flexibility**

```clojure
;; Auto-detect schema based on topic (STRICT - requires schema to exist)
{:schemas true}

;; Specific schema for all topics
{:schemas :my-schema}

;; Per-topic mapping
{:schemas {"users" :users/default 
          "orders" :orders/default}}
```

**Important**: When using `{:schemas true}`, validation is **strict** - if no schema is found for a topic, messages will be rejected with a clear warning.

## 🚀 **How to Use**

### **Basic Setup**

```clojure
(require '[kafka-metamorphosis.producer :as producer])
(require '[kafka-metamorphosis.consumer :as consumer])
(require '[kafka-metamorphosis.schema :as schema])

;; 1. Define schemas by topic
(schema/defschema :users/default 
  {:user-id int?
   :name string?
   :email string?})

(schema/defschema :orders/default
  {:order-id string?
   :user-id int?
   :total double?
   :status (schema/one-of :pending :confirmed :shipped)})
```

### **Producer with Auto-Validation**

```clojure
;; 2. Create producer with automatic validation
(def producer (producer/create {:bootstrap-servers "localhost:9092"
                               :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                               :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                               :schemas true}))

;; 3. Send messages - shows warnings for invalid data and blocks sending!
(producer/send! producer "users" "user-123" 
               {:user-id 123 :name "John" :email "john@test.com"})

;; Invalid message - shows warning and returns nil (doesn't send)
(let [result (producer/send! producer "users" "invalid" {:user-id "not-a-number"})]
  (if result
    (println "Message sent successfully")
    (println "Message was rejected due to validation failure")))
;; ⚠️ Schema validation failed for topic 'users'
;; Message will NOT be sent to Kafka.
```

### **Consumer with Auto-Validation**

```clojure
;; 4. Create consumer with automatic validation
(def consumer (consumer/create {:bootstrap-servers "localhost:9092"
                               :group-id "my-consumer-group"
                               :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                               :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                               :auto-offset-reset "earliest"
                               :schemas true}))

;; 5. Consume messages - shows warnings for invalid data and filters them out!
(consumer/subscribe! consumer ["users"])
(let [records (consumer/poll! consumer 5000)]
  (doseq [record records]
    (println "Consumed:" (:value record))))
;; ⚠️ Schema validation failed for consumed message from topic 'users'
;; Message will NOT be processed.
;; Only valid messages will be returned in the poll! result

(consumer/close! consumer)
(producer/close! producer)
```

### **Schema Discovery**

```clojure
;; List all schemas
(schema/list-schemas)
;; => (:users/default :orders/default :notifications ...)

;; Schemas for specific topic
(schema/list-schemas-for-topic "users")
;; => (:users/default :users/profile)

;; Get schema for topic
(schema/get-schema-for-topic "users")  ; searches :users/default or :users
```

## 🔧 **API Reference**

### **New Schema Functions**

- `(get-schema-for-topic topic-name)` - Get schema for topic
- `(list-schemas-for-topic topic-name)` - List topic's schemas
- `(validate-message-for-topic message topic)` - Validate against topic schema
- `(explain-validation-for-topic message topic)` - Detailed explanation

### **Updated Producer and Consumer**

- `(producer/create config)` - Supports `:schemas` in configuration
- `(producer/send! producer topic key value)` - Automatic validation with rejection of invalid messages
- `(producer/send-async! producer topic key value callback)` - Automatic validation with rejection of invalid messages
- `(consumer/create config)` - Supports `:schemas` in configuration  
- `(consumer/poll! consumer timeout)` - Automatic validation with filtering of invalid messages
- All functions compatible with both raw Kafka objects and enhanced objects

### **Supported Schema Configurations**

1. **`{:schemas true}`** - Auto-detect based on topic name (strict - requires schema)
2. **`{:schemas :schema-id}`** - Specific schema for all topics
3. **`{:schemas {"topic1" :schema1 "topic2" :schema2}}`** - Per-topic mapping (strict - requires mapping)

### **Strict Validation Behavior**

When schema validation is enabled, the system enforces strict requirements:

#### **Auto-Validation Mode (`{:schemas true}`)**
- **Schema MUST exist** for the topic (`:topic/default` or `:topic`)
- **Message is rejected** if no schema found for the topic
- **Message is rejected** if schema validation fails

#### **Per-Topic Mapping Mode (`{:schemas {...}}`)**
- **Topic MUST have a mapping** in the configuration map
- **Message is rejected** if topic not found in mapping
- **Message is rejected** if schema validation fails

#### **Example of Rejection Behavior**

```clojure
;; Schema mapping with only specific topics
(def producer (producer/create {:schemas {"users" :users/default
                                         "orders" :orders/default}}))

;; ✅ Will succeed - topic has mapping and valid data
(producer/send! producer "users" {:user-id 123 :name "John"})

;; ❌ Will be rejected - topic not in mapping
(producer/send! producer "products" {:id 1 :name "Widget"})
;; ⚠️ No schema mapping found for topic 'products'
;; Available mappings: (users orders)
;; Message will NOT be sent to Kafka.

;; ❌ Will be rejected - topic has mapping but invalid data
(producer/send! producer "users" {:user-id "invalid"})
;; ⚠️ Schema validation failed for topic 'users'
;; Message will NOT be sent to Kafka.
```

## 🧪 **Complete Examples**

See practical examples in:
- `src/kafka_metamorphosis/exemples/topic_schema_examples.clj`

Run in REPL:
```clojure
(require '[kafka-metamorphosis.exemples.topic-schema-examples :as examples])
(examples/run-all-examples)
```

## ✅ **Test Status**

- **46 tests executed**
- **232 assertions**
- **0 failures, 0 errors**
- **Backward compatibility maintained**

## 🎉 **Benefits**

1. **Total Transparency**: Just add `:schemas true` and validation happens automatically
2. **Intuitive Convention**: `:topic-name/default` or `:topic-name` 
3. **Flexibility**: Auto-detection, global schemas or per-topic mapping
4. **Backward Compatibility**: Existing code continues working unchanged
5. **Separate Functions**: Maintains preferred `(producer/send!)` and `(consumer/poll!)` style
6. **Message Filtering**: Invalid messages are rejected/filtered rather than processed
7. **Error Handling**: Clear warnings with detailed explanations for validation failures

Schema validation is now completely transparent, automatic, and protective! 🚀
