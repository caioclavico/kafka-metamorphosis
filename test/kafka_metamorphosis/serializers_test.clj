(ns kafka-metamorphosis.serializers-test
  (:require [clojure.test :refer [deftest testing is]]
            [kafka-metamorphosis.serializers :as serializers]))

(deftest test-string-serializers
  (testing "String serializers configuration"
    (is (map? serializers/string-serializers))
    (is (contains? serializers/string-serializers :key-serializer))
    (is (contains? serializers/string-serializers :value-serializer))))

(deftest test-string-deserializers
  (testing "String deserializers configuration"
    (is (map? serializers/string-deserializers))
    (is (contains? serializers/string-deserializers :key-deserializer))
    (is (contains? serializers/string-deserializers :value-deserializer))))

(deftest test-simple-json-serializers
  (testing "Simple JSON serializers configuration"
    (is (map? serializers/simple-json-serializers))
    (is (contains? serializers/simple-json-serializers :key-serializer))
    (is (contains? serializers/simple-json-serializers :value-serializer))
    (is (contains? serializers/simple-json-serializers :json-mode))))

(deftest test-simple-json-deserializers
  (testing "Simple JSON deserializers configuration"
    (is (map? serializers/simple-json-deserializers))
    (is (contains? serializers/simple-json-deserializers :key-deserializer))
    (is (contains? serializers/simple-json-deserializers :value-deserializer))
    (is (contains? serializers/simple-json-deserializers :json-mode))))

(deftest test-avro-serializers
  (testing "Avro serializers configuration"
    (let [config (serializers/avro-serializers "http://localhost:8081")]
      (is (map? config))
      (is (contains? config :key-serializer))
      (is (contains? config :value-serializer))
      (is (contains? config :schema-registry-url))
      (is (= "http://localhost:8081" (:schema-registry-url config))))))

(deftest test-avro-deserializers
  (testing "Avro deserializers configuration"
    (let [config (serializers/avro-deserializers "http://localhost:8081")]
      (is (map? config))
      (is (contains? config :key-deserializer))
      (is (contains? config :value-deserializer))
      (is (contains? config :schema-registry-url))
      (is (contains? config :specific-avro-reader))
      (is (= "http://localhost:8081" (:schema-registry-url config))))))

(deftest test-protobuf-serializers
  (testing "Protobuf serializers configuration"
    (let [config (serializers/protobuf-serializers "http://localhost:8081")]
      (is (map? config))
      (is (contains? config :key-serializer))
      (is (contains? config :value-serializer))
      (is (contains? config :schema-registry-url))
      (is (= "http://localhost:8081" (:schema-registry-url config))))))

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
    (is (nil? (serializers/from-json nil)))))
