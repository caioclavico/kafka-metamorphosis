(ns kafka-metamorphosis.util-test
  "Tests for utility functions"
  (:require [clojure.test :refer [deftest is testing]]
            [kafka-metamorphosis.util :as util]))

(deftest test-map->properties
  (testing "Converting Clojure map to Java Properties"
    (let [config {:bootstrap-servers "localhost:9092"
                  :key-serializer "org.apache.kafka.common.serialization.StringSerializer"}
          props (util/map->properties config)]
      (is (= "localhost:9092" (.getProperty props "bootstrap-servers")))
      (is (= "org.apache.kafka.common.serialization.StringSerializer" 
             (.getProperty props "key-serializer")))))
  
  (testing "Empty map converts to empty Properties"
    (let [props (util/map->properties {})]
      (is (= 0 (.size props)))
      (is (instance? java.util.Properties props))))
  
  (testing "Numeric values converted to strings"
    (let [config {:retries 3 :batch-size 16384 :linger-ms 1}
          props (util/map->properties config)]
      (is (= "3" (.getProperty props "retries")))
      (is (= "16384" (.getProperty props "batch-size")))
      (is (= "1" (.getProperty props "linger-ms")))))
  
  (testing "Boolean values converted to strings"
    (let [config {:enable-auto-commit true :auto-offset-reset "earliest"}
          props (util/map->properties config)]
      (is (= "true" (.getProperty props "enable-auto-commit")))
      (is (= "earliest" (.getProperty props "auto-offset-reset")))))
  
  (testing "All keys converted to strings"
    (let [config {:key1 "value1" :key2 "value2" :key3 "value3"}
          props (util/map->properties config)]
      (is (= "value1" (.getProperty props "key1")))
      (is (= "value2" (.getProperty props "key2")))
      (is (= "value3" (.getProperty props "key3"))))))

(deftest test-kebab->dot
  (testing "Kebab-case to dot-case conversion"
    (is (= "bootstrap.servers" (util/kebab->dot :bootstrap-servers)))
    (is (= "max.poll.records" (util/kebab->dot :max-poll-records)))
    (is (= "session.timeout.ms" (util/kebab->dot :session-timeout-ms)))
    (is (= "key.deserializer" (util/kebab->dot :key-deserializer)))
    (is (= "value.serializer" (util/kebab->dot :value-serializer))))
  
  (testing "Single word keywords"
    (is (= "retries" (util/kebab->dot :retries)))
    (is (= "acks" (util/kebab->dot :acks))))
  
  (testing "Multiple hyphens"
    (is (= "fetch.min.bytes" (util/kebab->dot :fetch-min-bytes)))
    (is (= "fetch.max.wait.ms" (util/kebab->dot :fetch-max-wait-ms))))
  
  (testing "Underscores converted to dots"
    (is (= "broker.id" (util/kebab->dot :broker_id))))
  
  (testing "Non-keyword inputs"
    (is (= "not.a.keyword" (util/kebab->dot :not-a-keyword)))
    (is (= "string.value" (util/kebab->dot "string-value")))))

(deftest test-kebab->snake
  (testing "Kebab-case to snake-case conversion"
    (is (= "bootstrap.servers" (util/kebab->snake :bootstrap-servers)))
    (is (= "max.poll.records" (util/kebab->snake :max-poll-records)))
    (is (= "session.timeout.ms" (util/kebab->snake :session-timeout-ms))))
  
  (testing "Single word keywords"
    (is (= "retries" (util/kebab->snake :retries))))
  
  (testing "Non-keyword inputs"
    (is (= "string-value" (util/kebab->snake "string-value")))))

(deftest test-normalize-config
  (testing "Normalizing Clojure config to Kafka format"
    (let [config {:bootstrap-servers "localhost:9092"
                  :max-poll-records 500
                  :session-timeout-ms 30000}
          normalized (util/normalize-config config)]
      (is (= "localhost:9092" (get normalized "bootstrap.servers")))
      (is (= 500 (get normalized "max.poll.records")))
      (is (= 30000 (get normalized "session.timeout.ms")))))
  
  (testing "Empty config normalization"
    (let [normalized (util/normalize-config {})]
      (is (map? normalized))
      (is (= 0 (count normalized)))))
  
  (testing "Config with various value types"
    (let [config {:string-val "test"
                  :int-val 42
                  :bool-val true
                  :double-val 3.14}
          normalized (util/normalize-config config)]
      (is (= "test" (get normalized "string.val")))
      (is (= 42 (get normalized "int.val")))
      (is (= true (get normalized "bool.val")))
      (is (= 3.14 (get normalized "double.val")))))
  
  (testing "Preserves all values correctly"
    (let [config {:key-one "value1"
                  :key-two 2
                  :key-three false}
          normalized (util/normalize-config config)]
      (is (= 3 (count normalized)))
      (doseq [key (keys normalized)]
        (is (string? key) "All keys should be strings"))))
  
  (testing "Multiple underscores and hyphens"
    (let [config {:fetch-min-bytes 1024 :broker_id 1}
          normalized (util/normalize-config config)]
      (is (= 1024 (get normalized "fetch.min.bytes")))
      (is (= 1 (get normalized "broker.id"))))))

(deftest test-properties-roundtrip
  (testing "Converting map to properties and back"
    (let [original {:bootstrap-servers "localhost:9092"
                    :retries 3
                    :acks "all"}
          normalized (util/normalize-config original)
          props (util/map->properties normalized)]
      (doseq [[k v] normalized]
        (is (= (str v) (.getProperty props k))))))
  
  (testing "All values are properly stringified"
    (let [config {:int-val 123 :bool-val true :string-val "test"}
          props (util/map->properties config)]
      (is (string? (.getProperty props "int-val")))
      (is (string? (.getProperty props "bool-val")))
      (is (string? (.getProperty props "string-val"))))))

(deftest test-edge-cases
  (testing "Keys with consecutive hyphens or underscores"
    (let [config {:key--with--double-hyphens 1 :key__with__underscores 2}
          normalized (util/normalize-config config)]
      (is (contains? normalized "key..with..double.hyphens"))
      (is (contains? normalized "key..with..underscores"))))
  
  (testing "Empty string values"
    (let [config {:empty-key ""}
          props (util/map->properties config)]
      (is (= "" (.getProperty props "empty-key")))))
  
  (testing "Null-like values"
    (let [config {:null-key nil}
          props (util/map->properties config)]
      ;; nil becomes an empty string via (str nil)
      (is (= "" (.getProperty props "null-key"))))))

(deftest test-config-building-workflow
  (testing "Typical workflow: kebab->dot -> normalize -> map->properties"
    (let [raw-config {:bootstrap-servers "localhost:9092"
                      :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                      :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                      :acks "all"}
          ;; Typically done by normalize-config internally
          normalized (util/normalize-config raw-config)
          props (util/map->properties normalized)]
      (is (map? normalized))
      (is (instance? java.util.Properties props))
      (is (= "localhost:9092" (.getProperty props "bootstrap.servers"))))))
