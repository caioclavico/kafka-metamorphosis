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
             (.getProperty props "key-serializer"))))))

(deftest test-normalize-config
  (testing "Converting kebab-case to dot notation"
    (let [config {:max-poll-records 500
                  :session-timeout-ms 30000
                  :bootstrap-servers "localhost:9092"}
          normalized (util/normalize-config config)]
      (is (= 500 (get normalized "max.poll.records")))
      (is (= 30000 (get normalized "session.timeout.ms")))
      (is (= "localhost:9092" (get normalized "bootstrap.servers"))))))

(deftest test-producer-config-creation
  (testing "Producer configuration creation"
    (let [config (core/producer-config)]
      (is (map? config))
      (is (contains? config :bootstrap-servers))
      (is (contains? config :key-serializer))
      (is (contains? config :value-serializer))
      (is (contains? config :acks)))))

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
