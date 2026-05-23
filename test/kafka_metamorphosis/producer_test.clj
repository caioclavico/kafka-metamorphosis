(ns kafka-metamorphosis.producer-test
  (:require [clojure.test :refer [deftest testing is]]
            [kafka-metamorphosis.producer :as producer]
            [kafka-metamorphosis.util :as util]
            [kafka-metamorphosis.core :as core]))

(deftest test-map->properties
  (testing "Converting Clojure map to Java Properties"
    (let [config {:bootstrap-servers "localhost:9092"
                  :key-serializer "org.apache.kafka.common.serialization.StringSerializer"}
          props (util/map->properties config)]
      (is (= "localhost:9092" (.getProperty props "bootstrap-servers")))
      (is (= "org.apache.kafka.common.serialization.StringSerializer" 
             (.getProperty props "key-serializer")))))
  
  (testing "Converting empty map to Properties"
    (let [props (util/map->properties {})]
      (is (= 0 (.size props)))))
  
  (testing "Converting nested values to strings"
    (let [config {:retries 3 :batch-size 16384 :linger-ms 1}
          props (util/map->properties config)]
      (is (= "3" (.getProperty props "retries")))
      (is (= "16384" (.getProperty props "batch-size")))
      (is (= "1" (.getProperty props "linger-ms"))))))

(deftest test-normalize-config
  (testing "Converting kebab-case to dot notation"
    (let [config {:max-poll-records 500
                  :session-timeout-ms 30000
                  :bootstrap-servers "localhost:9092"}
          normalized (util/normalize-config config)]
      (is (= 500 (get normalized "max.poll.records")))
      (is (= 30000 (get normalized "session.timeout.ms")))
      (is (= "localhost:9092" (get normalized "bootstrap.servers")))))
  
  (testing "Empty config"
    (let [normalized (util/normalize-config {})]
      (is (map? normalized))
      (is (= 0 (count normalized)))))
  
  (testing "Config with single underscore"
    (let [config {:broker_id 1}
          normalized (util/normalize-config config)]
      (is (contains? normalized "broker.id"))))
  
  (testing "Config with multiple hyphens"
    (let [config {:fetch-min-bytes 1024 :fetch-max-wait-ms 500}
          normalized (util/normalize-config config)]
      (is (= 1024 (get normalized "fetch.min.bytes")))
      (is (= 500 (get normalized "fetch.max.wait.ms"))))))

(deftest test-producer-config-creation
  (testing "Producer configuration creation with defaults"
    (let [config (core/producer-config)]
      (is (map? config))
      (is (contains? config :bootstrap-servers))
      (is (contains? config :key-serializer))
      (is (contains? config :value-serializer))
      (is (contains? config :acks))))
  
  (testing "Producer configuration with custom servers"
    (let [config (core/producer-config "broker1:9092,broker2:9092" {})]
      (is (= "broker1:9092,broker2:9092" (:bootstrap-servers config)))))
  
  (testing "Producer configuration with custom options"
    (let [config (core/producer-config {:retries 5 :batch-size 32768})]
      (is (= 5 (:retries config)))
      (is (= 32768 (:batch-size config))))))

(deftest test-kebab->dot-conversion
  (testing "Kebab to dot conversion utility"
    (is (= "bootstrap.servers" (util/kebab->dot :bootstrap-servers)))
    (is (= "max.poll.records" (util/kebab->dot :max-poll-records)))
    (is (= "session.timeout.ms" (util/kebab->dot :session-timeout-ms)))
    (is (= "key.deserializer" (util/kebab->dot :key-deserializer)))
    (is (= "value.serializer" (util/kebab->dot :value-serializer)))))

(deftest test-kebab->snake-conversion
  (testing "Kebab to snake conversion utility"
    (is (= "bootstrap.servers" (util/kebab->snake :bootstrap-servers)))
    (is (= "max.poll.records" (util/kebab->snake :max-poll-records)))))

;; Note: These tests would require a running Kafka instance
;; For now, they serve as documentation of the expected API

(comment
  ;; Integration test example (requires running Kafka)
  (def test-config
    {:bootstrap-servers "localhost:9092"
     :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
     :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
     :acks "all"
     :retries 3})

  (def p (producer/create test-config))
  
  ;; Test synchronous send
  (producer/send! p "test-topic" "test-key" "test-value")
  
  ;; Test asynchronous send
  (producer/send-async! p "test-topic" "test-key" "test-value"
                        (fn [metadata exception]
                          (if exception
                            (println "Error:" (.getMessage exception))
                            (println "Success! Partition:" (.partition metadata)))))
  
  (producer/close! p))
