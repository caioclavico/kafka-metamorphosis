(ns kafka-metamorphosis.serializers-test
  (:require [clojure.test :refer [deftest testing is]]
            [kafka-metamorphosis.serializers :as serializers]))

(deftest test-string-serializers
  (testing "String serializers configuration"
    (is (map? serializers/string-serializers))
    (is (contains? serializers/string-serializers :key-serializer))
    (is (contains? serializers/string-serializers :value-serializer)))
  
  (testing "String serializers have correct values"
    (is (= "org.apache.kafka.common.serialization.StringSerializer"
           (:key-serializer serializers/string-serializers)))
    (is (= "org.apache.kafka.common.serialization.StringSerializer"
           (:value-serializer serializers/string-serializers)))))

(deftest test-string-deserializers
  (testing "String deserializers configuration"
    (is (map? serializers/string-deserializers))
    (is (contains? serializers/string-deserializers :key-deserializer))
    (is (contains? serializers/string-deserializers :value-deserializer)))
  
  (testing "String deserializers have correct values"
    (is (= "org.apache.kafka.common.serialization.StringDeserializer"
           (:key-deserializer serializers/string-deserializers)))
    (is (= "org.apache.kafka.common.serialization.StringDeserializer"
           (:value-deserializer serializers/string-deserializers)))))

(deftest test-simple-json-serializers
  (testing "Simple JSON serializers configuration"
    (is (map? serializers/simple-json-serializers))
    (is (contains? serializers/simple-json-serializers :key-serializer))
    (is (contains? serializers/simple-json-serializers :value-serializer))
    (is (contains? serializers/simple-json-serializers :json-mode)))
  
  (testing "Simple JSON serializers enable json-mode"
    (is (true? (:json-mode serializers/simple-json-serializers)))))

(deftest test-simple-json-deserializers
  (testing "Simple JSON deserializers configuration"
    (is (map? serializers/simple-json-deserializers))
    (is (contains? serializers/simple-json-deserializers :key-deserializer))
    (is (contains? serializers/simple-json-deserializers :value-deserializer))
    (is (contains? serializers/simple-json-deserializers :json-mode)))
  
  (testing "Simple JSON deserializers enable json-mode"
    (is (true? (:json-mode serializers/simple-json-deserializers)))))

(deftest test-avro-serializers
  (testing "Avro serializers configuration"
    (let [config (serializers/avro-serializers "http://localhost:8081")]
      (is (map? config))
      (is (contains? config :key-serializer))
      (is (contains? config :value-serializer))
      (is (contains? config :schema-registry-url))
      (is (= "http://localhost:8081" (:schema-registry-url config)))))
  
  (testing "Avro serializers with different schema registry URLs"
    (let [config1 (serializers/avro-serializers "http://localhost:8081")
          config2 (serializers/avro-serializers "http://schema-registry:8081")]
      (is (= "http://localhost:8081" (:schema-registry-url config1)))
      (is (= "http://schema-registry:8081" (:schema-registry-url config2)))))
  
  (testing "Avro serializers use Confluent serializers"
    (let [config (serializers/avro-serializers "http://localhost:8081")]
      (is (string? (:key-serializer config)))
      (is (string? (:value-serializer config))))))

(deftest test-avro-deserializers
  (testing "Avro deserializers configuration"
    (let [config (serializers/avro-deserializers "http://localhost:8081")]
      (is (map? config))
      (is (contains? config :key-deserializer))
      (is (contains? config :value-deserializer))
      (is (contains? config :schema-registry-url))
      (is (= "http://localhost:8081" (:schema-registry-url config)))))
  
  (testing "Avro deserializers have specific-avro-reader setting"
    (let [config (serializers/avro-deserializers "http://localhost:8081")]
      (is (contains? config :specific-avro-reader))))
  
  (testing "Avro deserializers with custom reader setting"
    (let [config (serializers/avro-deserializers "http://localhost:8081")]
      (is (true? (:specific-avro-reader config))))))

(deftest test-protobuf-serializers
  (testing "Protobuf serializers configuration"
    (let [config (serializers/protobuf-serializers "http://localhost:8081")]
      (is (map? config))
      (is (contains? config :key-serializer))
      (is (contains? config :value-serializer))
      (is (contains? config :schema-registry-url))
      (is (= "http://localhost:8081" (:schema-registry-url config)))))
  
  (testing "Protobuf serializers with different schema registry URLs"
    (let [config (serializers/protobuf-serializers "https://schema-registry.example.com")]
      (is (= "https://schema-registry.example.com" (:schema-registry-url config))))))

(deftest test-protobuf-deserializers
  (testing "Protobuf deserializers configuration"
    (let [config (serializers/protobuf-deserializers "http://localhost:8081")]
      (is (map? config))
      (is (contains? config :key-deserializer))
      (is (contains? config :value-deserializer))
      (is (contains? config :schema-registry-url))
      (is (= "http://localhost:8081" (:schema-registry-url config))))))

(deftest test-json-utilities
  (testing "JSON utility functions"
    ;; Test to-json
    (let [data {:name "John" :age 30}
          json-str (serializers/to-json data)]
      (is (string? json-str))
      (is (re-find #"John" json-str))
      (is (re-find #"30" json-str)))
    
    ;; Test from-json
    (let [json-str "{\"name\":\"John\",\"age\":30}"
          data (serializers/from-json json-str)]
      (is (map? data))
      (is (= "John" (:name data)))
      (is (= 30 (:age data))))
    
    ;; Test round-trip
    (let [original {:test "data" :number 42}
          json-str (serializers/to-json original)
          parsed (serializers/from-json json-str)]
      (is (= original parsed)))
    
    ;; Test from-json with nil
    (is (nil? (serializers/from-json nil))))
  
  (testing "JSON utilities with nested structures"
    (let [data {:user {:name "Alice" :email "alice@example.com"}
                :tags ["tag1" "tag2"]}
          json-str (serializers/to-json data)
          parsed (serializers/from-json json-str)]
      (is (= data parsed))))
  
  (testing "JSON utilities with special characters"
    (let [data {:message "Hello \"World\"" :emoji "🎉"}
          json-str (serializers/to-json data)
          parsed (serializers/from-json json-str)]
      (is (= data parsed))))
  
  (testing "JSON utilities with empty collections"
    (let [data {:empty-list [] :empty-map {} :empty-string ""}
          json-str (serializers/to-json data)
          parsed (serializers/from-json json-str)]
      (is (= data parsed)))))

(deftest test-serializer-immutability
  (testing "Serializer configurations are independent"
    (let [config1 (serializers/avro-serializers "http://localhost:8081")
          config2 (serializers/avro-serializers "http://localhost:8081")]
      (is (not (identical? config1 config2)))
      (is (= config1 config2))))
  
  (testing "Different serializer configs are not equal"
    (let [config1 (serializers/avro-serializers "http://localhost:8081")
          config2 (serializers/protobuf-serializers "http://localhost:8081")]
      (is (not (= (:value-serializer config1) (:value-serializer config2)))))))

(deftest test-schema-registry-url-handling
  (testing "Schema registry URLs with different formats"
    ;; HTTP
    (let [config (serializers/avro-serializers "http://registry:8081")]
      (is (= "http://registry:8081" (:schema-registry-url config))))
    
    ;; HTTPS
    (let [config (serializers/avro-serializers "https://registry.prod.com")]
      (is (= "https://registry.prod.com" (:schema-registry-url config))))
    
    ;; With authentication (URL format)
    (let [config (serializers/avro-serializers "http://user:pass@registry:8081")]
      (is (= "http://user:pass@registry:8081" (:schema-registry-url config)))))
  
  (testing "Multiple schema registries"
    (let [urls ["http://registry1:8081" "http://registry2:8081" "http://registry3:8081"]]
      (doseq [url urls]
        (let [config (serializers/avro-serializers url)]
          (is (= url (:schema-registry-url config))))))))
