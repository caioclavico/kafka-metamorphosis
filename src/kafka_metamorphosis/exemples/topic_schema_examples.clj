(ns kafka-metamorphosis.exemples.topic-schema-examples
  "Examples of topic-scoped schema validation"
  (:require [kafka-metamorphosis.producer :as producer]
            [kafka-metamorphosis.consumer :as consumer]
            [kafka-metamorphosis.schema :as schema]))

;; =============================================================================
;; Topic-Scoped Schema Examples
;; =============================================================================

(defn setup-topic-schemas
  "Setup schemas scoped to specific topics"
  []
  
  ;; Schema for 'users' topic
  (schema/defschema :users/default
    {:user-id int?
     :name string?
     :email string?
     :created-at string?})
  
  ;; Schema for 'orders' topic
  (schema/defschema :orders/default
    {:order-id string?
     :user-id int?
     :total double?
     :status (schema/one-of :pending :confirmed :shipped :delivered)})
  
  ;; Direct topic schema
  (schema/defschema :notifications
    {:notification-id string?
     :user-id int?
     :message string?
     :sent-at string?}))

(defn example-auto-validation-producer
  "Example of producer with automatic schema validation"
  []
  (setup-topic-schemas)
  
  (println "=== Producer with Auto Schema Validation ===")
  
  ;; Create producer with automatic schema validation
  (let [producer-config {:bootstrap-servers "localhost:9092"
                        :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :schemas true}  ; Enable auto-validation!
        
        producer (producer/create producer-config)]
    
    (try
      ;; Send valid user message (will validate against :users/default)
      (let [user-data {:user-id 123
                      :name "John Doe"
                      :email "john@example.com"
                      :created-at "2024-09-01T10:00:00Z"}
            result (producer/send! producer "users" "user-123" user-data)]
        (if result
          (println "✓ Valid user message sent successfully")
          (println "✗ User message was rejected")))
      
      ;; Send valid order message (will validate against :orders/default)
      (let [order-data {:order-id "ORD-2024-001"
                       :user-id 123
                       :total 99.99
                       :status :pending}
            result (producer/send! producer "orders" "order-456" order-data)]
        (if result
          (println "✓ Valid order message sent successfully")
          (println "✗ Order message was rejected")))
      
      ;; Try to send invalid message (should fail validation)
      (let [invalid-data {:user-id "not-a-number"  ; Invalid type
                         :name "Invalid User"}
            result (producer/send! producer "users" "user-invalid" invalid-data)]
        (if result
          (println "✗ This should not happen - invalid message was sent")
          (println "✓ Correctly rejected invalid message")))
      
      ;; Flush and close
      (producer/flush! producer)
      (producer/close! producer)
      
      (catch Exception e
        (println "Producer error:" (.getMessage e))
        (producer/close! producer)))))

(defn example-specific-schema-mapping
  "Example using per-topic schema mapping"
  []
  (setup-topic-schemas)
  
  ;; Define additional schemas
  (schema/defschema :user-profile
    {:user-id int?
     :name string?
     :email string?
     :profile {:bio string?
              :avatar string?}})
  
  (println "\n=== Per-Topic Schema Mapping ===")
  
  ;; Producer with specific schema mapping
  (let [producer-config {:bootstrap-servers "localhost:9092"
                        :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :schemas {"users" :user-profile
                                 "orders" :orders/default
                                 "notifications" :notifications}}
        
        producer (producer/create producer-config)]
    
    (try
      ;; Send message with profile (validates against :user-profile)
      (producer/send! producer "users" "user-789"
                     {:user-id 789
                      :name "Jane Smith"
                      :email "jane@example.com"
                      :profile {:bio "Software Engineer"
                               :avatar "jane.jpg"}})
      (println "✓ User profile message sent with custom schema")
      
      (producer/flush! producer)
      (producer/close! producer)
      
      (catch Exception e
        (println "Producer error:" (.getMessage e))
        (producer/close! producer)))))

(defn example-global-schema
  "Example using a specific schema for all topics"
  []
  (setup-topic-schemas)
  
  (println "\n=== Global Schema for All Topics ===")
  
  ;; Producer that validates all messages against the same schema
  (let [producer-config {:bootstrap-servers "localhost:9092"
                        :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :schemas :users/default}  ; Use this schema for ALL topics
        
        producer (producer/create producer-config)]
    
    (try
      ;; Send user data to different topics (all validated against :users/default)
      (producer/send! producer "user-events" "event-1"
                     {:user-id 999
                      :name "Test User"
                      :email "test@example.com"
                      :created-at "2024-09-01T12:00:00Z"})
      (println "✓ Message sent to user-events with users/default schema")
      
      (producer/send! producer "user-backups" "backup-1"
                     {:user-id 888
                      :name "Backup User"
                      :email "backup@example.com"
                      :created-at "2024-09-01T12:05:00Z"})
      (println "✓ Message sent to user-backups with users/default schema")
      
      (producer/flush! producer)
      (producer/close! producer)
      
      (catch Exception e
        (println "Producer error:" (.getMessage e))
        (producer/close! producer)))))

(defn schema-discovery-examples
  "Examples of schema discovery functions"
  []
  (setup-topic-schemas)
  
  (println "\n=== Schema Discovery ===")
  
  ;; Test topic-specific schema functions
  (println "All schemas:" (schema/list-schemas))
  (println "Schemas for users topic:" (schema/list-schemas-for-topic "users"))
  (println "Schemas for orders topic:" (schema/list-schemas-for-topic "orders"))
  
  ;; Test getting schemas for topics
  (let [users-schema (schema/get-schema-for-topic "users")
        orders-schema (schema/get-schema-for-topic "orders")
        notifications-schema (schema/get-schema-for-topic "notifications")]
    (println "Users schema found:" (not (nil? users-schema)))
    (println "Orders schema found:" (not (nil? orders-schema)))
    (println "Notifications schema found:" (not (nil? notifications-schema))))
  
  ;; Test validation for topics
  (let [valid-user {:user-id 123 :name "John" :email "john@test.com" :created-at "2024-01-01"}
        invalid-user {:user-id "not-a-number" :name "John"}]
    
    (println "Valid user validates:" (schema/validate-message-for-topic valid-user "users"))
    (println "Invalid user validates:" (schema/validate-message-for-topic invalid-user "users"))
    
    ;; Show detailed explanation
    (let [explanation (schema/explain-validation-for-topic invalid-user "users")]
      (println "Validation explanation:")
      (println "  Valid:" (:valid? explanation))
      (when-not (:valid? explanation)
        (doseq [error (:errors explanation)]
          (println "  Error:" (:field error) "-" (:error error)))))))

(defn example-validation-warnings
  "Example showing how validation warnings work and block sending/consuming"
  []
  (setup-topic-schemas)
  
  (println "\n=== Validation Warnings (Blocking Invalid Messages) ===")
  
  ;; Producer with auto-validation enabled
  (let [producer-config {:bootstrap-servers "localhost:9092"
                        :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :schemas true}
        
        producer (producer/create producer-config)]
    
    (try
      ;; Send valid message first
      (println "Sending valid message:")
      (let [result (producer/send! producer "users" "valid-user"
                                  {:user-id 123
                                   :name "John Doe"
                                   :email "john@example.com"
                                   :created-at "2024-09-01T10:00:00Z"})]
        (if result
          (println "✅ Valid message sent successfully!")
          (println "❌ Valid message was rejected!")))
      
      ;; Send invalid message - should warn and NOT send
      (println "\nSending invalid message (will show warnings and NOT send):")
      (let [result (producer/send! producer "users" "invalid-user"
                                  {:user-id "not-a-number"  ; Should be int
                                   :name 12345              ; Should be string
                                   :email nil               ; Should be string
                                   :extra-field "unexpected"})] ; Extra field
        (if result
          (println "❌ Invalid message was sent unexpectedly!")
          (println "✅ Invalid message was correctly rejected!")))
      
      ;; Send to topic without schema - no validation
      (println "\nSending to topic without schema but with validation enabled:")
      (let [result (producer/send! producer "unknown-topic" "any-key"
                                  {:any "data" :structure "works"})]
        (if result
          (println "❌ Message unexpectedly sent!")
          (println "✅ Message correctly rejected - no schema found for topic")))
      
      (producer/flush! producer)
      (producer/close! producer)
      
      (catch Exception e
        (println "Producer error:" (.getMessage e))
        (producer/close! producer)))))

(defn example-consumer-validation-warnings
  "Example showing how consumer validation warnings work and filter invalid messages"
  []
  (setup-topic-schemas)
  
  (println "\n=== Consumer Validation Warnings (Filtering Invalid Messages) ===")
  
  ;; First, produce some messages (valid and invalid) without validation
  (let [producer (producer/create {:bootstrap-servers "localhost:9092"
                                  :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                                  :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                                  :schemas false})]  ; No validation on producer for this test
    
    (println "Producing test messages without validation...")
    
    ;; Valid message
    (producer/send! producer "users" "valid-user"
                   {:user-id 123
                    :name "John Doe"
                    :email "john@example.com"
                    :created-at "2024-09-01T10:00:00Z"})
    
    ;; Invalid message
    (producer/send! producer "users" "invalid-user"
                   {:user-id "not-a-number"
                    :name 12345
                    :email nil})
    
    (producer/flush! producer)
    (producer/close! producer))
  
  ;; Now consume with validation
  (let [consumer (consumer/create {:bootstrap-servers "localhost:9092"
                                  :group-id "test-consumer-group"
                                  :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                                  :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                                  :auto-offset-reset "earliest"
                                  :schemas true})]  ; Enable validation
    
    (println "Consuming messages with schema validation...")
    (consumer/subscribe! consumer ["users"])
    
    ;; Poll for messages - should show warnings for invalid ones and filter them out
    (let [records (consumer/poll! consumer 2000)]
      (if (empty? records)
        (println "No valid messages consumed (invalid messages were filtered out)")
        (doseq [record records]
          (println (str "✅ Consumed valid message: " (:value record))))))
    
    (consumer/close! consumer)))

(defn example-mapping-rejection
  "Example showing message rejection when no schema mapping exists"
  []
  (setup-topic-schemas)
  
  (println "\n=== Schema Mapping Rejection Examples ===")
  
  ;; Create producer with schema mapping for specific topics only
  (let [producer-config {:bootstrap-servers "localhost:9092"
                        :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :schemas {"users" :users/default
                                 "orders" :orders/default}}  ; Only these topics have mappings
        
        producer (producer/create producer-config)]
    
    (println "\n--- Trying to send to mapped topic 'users' (should succeed) ---")
    (let [valid-user {:user-id 123
                     :name "Alice"
                     :email "alice@example.com"
                     :created-at "2023-10-01T10:00:00Z"}
          result (producer/send! producer "users" valid-user)]
      (println "Result:" result))
    
    (println "\n--- Trying to send to unmapped topic 'products' (should be rejected) ---")
    (let [product-data {:product-id "P123"
                       :name "Widget"
                       :price 29.99}
          result (producer/send! producer "products" product-data)]
      (println "Result:" result))
    
    (println "\n--- Trying to send to mapped topic 'orders' (should succeed) ---")
    (let [valid-order {:order-id "ORD-001"
                      :user-id 123
                      :total 99.99
                      :status :confirmed}
          result (producer/send! producer "orders" valid-order)]
      (println "Result:" result))
    
    (producer/close! producer)))

(defn example-auto-validation-no-mapping
  "Example showing auto-validation behavior with unmapped topics"
  []
  (setup-topic-schemas)
  
  (println "\n=== Auto-Validation with No Schema Mapping ===")
  
  ;; Create producer with auto schema validation 
  (let [producer-config {:bootstrap-servers "localhost:9092"
                        :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                        :schemas true}  ; Auto-validation mode
        
        producer (producer/create producer-config)]
    
    (println "\n--- Sending to topic with default schema 'users' (should succeed) ---")
    (let [valid-user {:user-id 456
                     :name "Bob"
                     :email "bob@example.com"
                     :created-at "2023-10-01T11:00:00Z"}
          result (producer/send! producer "users" valid-user)]
      (println "Result:" result))
    
    (println "\n--- Sending to topic without any schema 'analytics' (should be rejected) ---")
    (let [analytics-data {:event "click"
                         :user-id 456
                         :timestamp "2023-10-01T11:30:00Z"}
          result (producer/send! producer "analytics" analytics-data)]
      (println "Result:" result))
    
    (println "\n--- Sending to topic with direct schema 'notifications' (should succeed) ---")
    (let [valid-notification {:notification-id "N001"
                             :user-id 456
                             :message "Welcome to our platform!"
                             :sent-at "2023-10-01T12:00:00Z"}
          result (producer/send! producer "notifications" valid-notification)]
      (println "Result:" result))
    
    (producer/close! producer)))

(defn run-all-examples
  "Run all topic schema examples"
  []
  (println "🚀 Topic-Scoped Schema Validation Examples")
  (println (apply str (repeat 50 "=")))
  
  (example-auto-validation-producer)
  (example-specific-schema-mapping)
  (example-global-schema)
  (example-validation-warnings)  
  (example-consumer-validation-warnings)  ; Consumer example
  (example-mapping-rejection)            ; New: Schema mapping rejection
  (example-auto-validation-no-mapping)   ; New: Auto-validation without mapping
  (schema-discovery-examples)
  
  (println "\n✅ All topic schema examples completed!"))

(comment
  ;; Para executar os exemplos:
  (run-all-examples)
  
  ;; Executar exemplo específico:
  (example-auto-validation-producer)
  
  ;; Testar discovery de schemas:
  (schema-discovery-examples))
