(ns kafka-metamorphosis.integration-test
  "Integration tests that require Kafka to be running.
   These tests are skipped if Kafka is not available."
  (:require [clojure.test :refer [deftest is run-tests testing]]
            [kafka-metamorphosis.producer :as producer]
            [kafka-metamorphosis.consumer :as consumer]
            [kafka-metamorphosis.admin :as admin]
            [kafka-metamorphosis.schema :as schema]
            [kafka-metamorphosis.serializers :as serializers])
  (:import [java.util.concurrent TimeUnit]))

;; =============================================================================
;; Test Utilities
;; =============================================================================

(defn kafka-available?
  "Check if Kafka is available at localhost:9092"
  []
  (try
    (let [admin-client (admin/create-admin-client {:bootstrap-servers "localhost:9092"})]
      (try
        ;; Try to get cluster info with a short timeout
        (let [cluster-info (admin/get-cluster-info admin-client)]
          (admin/close! admin-client)
          (not (nil? cluster-info)))
        (catch Exception _e
          (admin/close! admin-client)
          false)))
    (catch Exception _e
      false)))

(defn skip-if-kafka-unavailable
  "Skip test if Kafka is not available"
  [test-fn]
  (if (kafka-available?)
    (test-fn)
    (do
      (println "⚠️  Skipping integration test - Kafka not available at localhost:9092")
      (is true "Kafka not available - test skipped"))))

(defmacro deftest-integration
  "Define an integration test that only runs if Kafka is available"
  [name & body]
  `(deftest ~name
     (skip-if-kafka-unavailable
       (fn [] ~@body))))

;; =============================================================================
;; Test Setup and Teardown
;; =============================================================================

(def test-topic "integration-test-topic")
(def test-group-id "integration-test-group")

(defn setup-test-environment!
  "Setup test environment with topic and schemas"
  []
  (let [admin-client (admin/create-admin-client {:bootstrap-servers "localhost:9092"})]
    (try
      ;; Create test topic
      (admin/create-topic-if-not-exists! admin-client test-topic
                                         {:partitions 3 :replication-factor 1})
      
      ;; Setup test schemas
      (schema/defschema :integration-test-topic/default
        {:id int?
         :name string?
         :email string?
         :timestamp string?})
      
      (schema/defschema :integration-test-topic/user-profile
        {:id int?
         :name string?
         :email string?
         :bio string?
         :age int?})
      
      (schema/defschema :orders-test
        {:order-id string?
         :user-id int?
         :total double?
         :status (schema/one-of :pending :confirmed :shipped :delivered)})
      
      (finally
        (admin/close! admin-client)))))

(defn cleanup-test-environment!
  "Cleanup test environment"
  []
  (let [admin-client (admin/create-admin-client {:bootstrap-servers "localhost:9092"})]
    (try
      ;; Delete test topic if it exists
      (when (admin/topic-exists? admin-client test-topic)
        (admin/delete-topic! admin-client test-topic))
      (finally
        (admin/close! admin-client)))))

;; =============================================================================
;; Integration Tests
;; =============================================================================

(deftest-integration test-producer-consumer-basic-flow
  (testing "Basic producer-consumer flow without schemas"
    (setup-test-environment!)
    
    (let [producer-config {:bootstrap-servers "localhost:9092"
                          :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                          :value-serializer "org.apache.kafka.common.serialization.StringSerializer"}
          consumer-config {:bootstrap-servers "localhost:9092"
                          :group-id (str test-group-id "-basic")
                          :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                          :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                          :auto-offset-reset "earliest"}
          
          producer (producer/create producer-config)
          consumer (consumer/create consumer-config)]
      
      (try
        ;; Send messages
        (dotimes [i 5]
          (let [message (str "test-message-" i)]
            (producer/send! producer test-topic (str "key-" i) message)))
        
        (producer/flush! producer)
        
        ;; Consume messages
        (consumer/subscribe! consumer [test-topic])
        (Thread/sleep 1000) ; Give some time for subscription
        
        (let [records (consumer/poll! consumer 5000)
              messages (map :value records)]
          
          (is (= 5 (count records)) "Should receive 5 messages")
          (is (every? #(re-matches #"test-message-\d" %) messages) "All messages should match pattern"))
        
        (finally
          (producer/close! producer)
          (consumer/close! consumer)
          (cleanup-test-environment!))))))

(deftest-integration test-schema-validation-auto-mode
  (testing "Schema validation in auto mode ({:schemas true})"
    (setup-test-environment!)
    
    (let [producer-config {:bootstrap-servers "localhost:9092"
                          :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                          :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                          :schemas true}  ; Auto validation!
          consumer-config {:bootstrap-servers "localhost:9092"
                          :group-id (str test-group-id "-auto-schema")
                          :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                          :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                          :auto-offset-reset "earliest"
                          :schemas true}  ; Auto validation!
          
          producer (producer/create producer-config)
          consumer (consumer/create consumer-config)]
      
      (try
        ;; Test valid message - should be sent
        (let [valid-user {:id 123
                         :name "John Doe"
                         :email "john@example.com"
                         :timestamp "2024-09-01T10:00:00Z"}
              result (producer/send! producer test-topic "user-123" valid-user)]
          (is (not (nil? result)) "Valid message should be sent successfully"))
        
        ;; Test invalid message - should be rejected
        (let [invalid-user {:id "not-a-number"  ; Invalid type
                           :name "Invalid User"}
              result (producer/send! producer test-topic "user-invalid" invalid-user)]
          (is (nil? result) "Invalid message should be rejected"))
        
        ;; Test message to topic without schema - should be rejected
        (let [unschematized-data {:some "data"}
              result (producer/send! producer "unknown-topic" "key" unschematized-data)]
          (is (nil? result) "Message to topic without schema should be rejected"))
        
        (producer/flush! producer)
        
        ;; Consume and verify only valid messages are received
        (consumer/subscribe! consumer [test-topic])
        (Thread/sleep 1000)
        
        (let [records (consumer/poll! consumer 5000)]
          (is (= 1 (count records)) "Should receive only 1 valid message")
          
          (when (seq records)
            (let [consumed-data (-> records first :value (serializers/from-json))]
              (is (= 123 (:id consumed-data)) "Consumed message should have correct ID")
              (is (= "John Doe" (:name consumed-data)) "Consumed message should have correct name"))))
        
        (finally
          (producer/close! producer)
          (consumer/close! consumer)
          (cleanup-test-environment!))))))

(deftest-integration test-schema-validation-mapping-mode
  (testing "Schema validation with per-topic mapping"
    (setup-test-environment!)
    
    (let [producer-config {:bootstrap-servers "localhost:9092"
                          :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                          :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                          :schemas {test-topic :integration-test-topic/default
                                   "orders-test" :orders-test}}
          consumer-config {:bootstrap-servers "localhost:9092"
                          :group-id (str test-group-id "-mapping")
                          :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                          :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                          :auto-offset-reset "earliest"
                          :schemas {test-topic :integration-test-topic/default
                                   "orders-test" :orders-test}}
          
          producer (producer/create producer-config)
          consumer (consumer/create consumer-config)]
      
      (try
        ;; Test message to mapped topic - should succeed
        (let [valid-user {:id 456
                         :name "Alice Smith"
                         :email "alice@example.com"
                         :timestamp "2024-09-01T11:00:00Z"}
              result (producer/send! producer test-topic "user-456" valid-user)]
          (is (not (nil? result)) "Message to mapped topic should be sent"))
        
        ;; Test message to unmapped topic - should be rejected
        (let [unmapped-data {:some "data"}
              result (producer/send! producer "unmapped-topic" "key" unmapped-data)]
          (is (nil? result) "Message to unmapped topic should be rejected"))
        
        ;; Test invalid message to mapped topic - should be rejected
        (let [invalid-user {:id "invalid"
                           :name "Invalid User"}
              result (producer/send! producer test-topic "invalid" invalid-user)]
          (is (nil? result) "Invalid message to mapped topic should be rejected"))
        
        (producer/flush! producer)
        
        ;; Consume and verify
        (consumer/subscribe! consumer [test-topic])
        (Thread/sleep 1000)
        
        (let [records (consumer/poll! consumer 5000)]
          (is (= 1 (count records)) "Should receive only 1 valid message")
          
          (when (seq records)
            (let [consumed-data (-> records first :value (serializers/from-json))]
              (is (= 456 (:id consumed-data)) "Should receive the valid user message"))))
        
        (finally
          (producer/close! producer)
          (consumer/close! consumer)
          (cleanup-test-environment!))))))

(deftest-integration test-producer-consumer-with-multiple-schemas
  (testing "Producer-consumer flow with multiple schema versions"
    (setup-test-environment!)
    
    (let [producer-config {:bootstrap-servers "localhost:9092"
                          :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                          :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                          :schemas {test-topic :integration-test-topic/user-profile}}
          consumer-config {:bootstrap-servers "localhost:9092"
                          :group-id (str test-group-id "-multi-schema")
                          :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                          :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                          :auto-offset-reset "earliest"
                          :schemas {test-topic :integration-test-topic/user-profile}}
          
          producer (producer/create producer-config)
          consumer (consumer/create consumer-config)]
      
      (try
        ;; Send user profile messages
        (let [users [{:id 1 :name "User 1" :email "user1@test.com" :bio "Bio 1" :age 25}
                     {:id 2 :name "User 2" :email "user2@test.com" :bio "Bio 2" :age 30}
                     {:id 3 :name "User 3" :email "user3@test.com" :bio "Bio 3" :age 35}]]
          
          (doseq [user users]
            (let [result (producer/send! producer test-topic (str "user-" (:id user)) user)]
              (is (not (nil? result)) (str "User " (:id user) " should be sent successfully")))))
        
        ;; Try to send invalid user (missing required fields)
        (let [invalid-user {:id 4 :name "Incomplete User"}  ; Missing email, bio, age
              result (producer/send! producer test-topic "user-4" invalid-user)]
          (is (nil? result) "Incomplete user should be rejected"))
        
        (producer/flush! producer)
        
        ;; Consume and verify all valid messages
        (consumer/subscribe! consumer [test-topic])
        (Thread/sleep 1000)
        
        (let [records (consumer/poll! consumer 5000)
              consumed-users (map #(-> % :value (serializers/from-json)) records)]
          
          (is (= 3 (count records)) "Should receive 3 valid user messages")
          
          (doseq [user consumed-users]
            (is (int? (:id user)) "ID should be an integer")
            (is (string? (:name user)) "Name should be a string")
            (is (string? (:email user)) "Email should be a string")
            (is (string? (:bio user)) "Bio should be a string")
            (is (int? (:age user)) "Age should be an integer")))
        
        (finally
          (producer/close! producer)
          (consumer/close! consumer)
          (cleanup-test-environment!))))))

(deftest-integration test-async-producer-with-validation
  (testing "Asynchronous producer with schema validation"
    (setup-test-environment!)
    
    (let [producer-config {:bootstrap-servers "localhost:9092"
                          :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                          :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                          :schemas true}
          
          producer (producer/create producer-config)
          results (atom [])
          latch (java.util.concurrent.CountDownLatch. 3)]
      
      (try
        ;; Send async messages with callback
        (dotimes [i 3]
          (let [user {:id (inc i)
                     :name (str "Async User " (inc i))
                     :email (str "async" (inc i) "@test.com")
                     :timestamp "2024-09-01T12:00:00Z"}
                callback (fn [metadata exception]
                          (swap! results conj {:metadata metadata :exception exception})
                          (.countDown latch))]
            
            (producer/send-async! producer test-topic (str "async-" i) user callback)))
        
        ;; Send invalid async message
        (let [invalid-user {:id "invalid"}
              result (producer/send-async! producer test-topic "invalid" invalid-user
                                          (fn [metadata exception]
                                            (swap! results conj {:metadata metadata :exception exception})
                                            (.countDown latch)))]
          (is (nil? result) "Invalid async message should return nil"))
        
        ;; Wait for async operations to complete
        (is (.await latch 10 TimeUnit/SECONDS) "Async operations should complete within 10 seconds")
        
        (let [final-results @results]
          (is (= 3 (count final-results)) "Should have 3 callback results")
          (is (every? #(nil? (:exception %)) final-results) "No exceptions should occur")
          (is (every? #(not (nil? (:metadata %))) final-results) "All should have metadata"))
        
        (finally
          (producer/close! producer)
          (cleanup-test-environment!))))))

(deftest-integration test-consumer-filter-invalid-messages
  (testing "Consumer filtering invalid messages in auto-validation mode"
    (setup-test-environment!)
    
    ;; First, send mixed valid/invalid messages without validation
    (let [producer-no-validation (producer/create {:bootstrap-servers "localhost:9092"
                                                  :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                                                  :value-serializer "org.apache.kafka.common.serialization.StringSerializer"})]
      (try
        ;; Send valid JSON that matches schema
        (producer/send! producer-no-validation test-topic "valid-1" 
                       (serializers/to-json {:id 1 :name "Valid User" :email "valid@test.com" :timestamp "2024-09-01"}))
        
        ;; Send invalid JSON (doesn't match schema)
        (producer/send! producer-no-validation test-topic "invalid-1" 
                       (serializers/to-json {:id "not-number" :name "Invalid"}))
        
        ;; Send completely invalid JSON
        (producer/send! producer-no-validation test-topic "invalid-2" "{invalid json")
        
        ;; Send another valid message
        (producer/send! producer-no-validation test-topic "valid-2" 
                       (serializers/to-json {:id 2 :name "Another Valid" :email "valid2@test.com" :timestamp "2024-09-01"}))
        
        (producer/flush! producer-no-validation)
        
        (finally
          (producer/close! producer-no-validation))))
    
    ;; Now consume with validation enabled - should filter invalid messages
    (let [consumer-config {:bootstrap-servers "localhost:9092"
                          :group-id (str test-group-id "-filter")
                          :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                          :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                          :auto-offset-reset "earliest"
                          :schemas true}  ; Enable validation filtering
          
          consumer (consumer/create consumer-config)]
      
      (try
        (consumer/subscribe! consumer [test-topic])
        (Thread/sleep 1000)
        
        (let [records (consumer/poll! consumer 10000)  ; Longer timeout for processing
              valid-records (filter #(not (nil? %)) records)]
          
          ;; Should only receive valid messages (consumer filters invalid ones)
          (is (<= (count valid-records) 2) "Should receive at most 2 valid messages")
          
          (doseq [record valid-records]
            (let [data (serializers/from-json (:value record))]
              (is (int? (:id data)) "ID should be integer in valid messages")
              (is (string? (:name data)) "Name should be string in valid messages"))))
        
        (finally
          (consumer/close! consumer)
          (cleanup-test-environment!))))))

;; =============================================================================
;; Test Runner
;; =============================================================================

(defn run-integration-tests
  "Run all integration tests if Kafka is available"
  []
  (if (kafka-available?)
    (do
      (println "🚀 Running integration tests with Kafka at localhost:9092")
      (run-tests 'kafka-metamorphosis.integration-test))
    (println "⚠️  Skipping integration tests - Kafka not available at localhost:9092")))

(comment
  ;; To run integration tests manually:
  (run-integration-tests)
  
  ;; To check if Kafka is available:
  (kafka-available?))
