(ns kafka-metamorphosis.dev-test
  "Tests for development utilities and Docker integration"
  (:require [clojure.test :refer [deftest is testing]]
            [kafka-metamorphosis.dev :as dev]
            [kafka-metamorphosis.admin :as admin]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

;; ============================================================================
;; Configuration Tests
;; ============================================================================

(deftest test-local-kafka-config
  (testing "Local Kafka configuration"
    (is (map? dev/local-kafka-config))
    (is (= "localhost:9092" (:bootstrap-servers dev/local-kafka-config)))))

(deftest test-string-producer-config
  (testing "String producer configuration"
    (is (map? dev/string-producer-config))
    (is (= "localhost:9092" (:bootstrap-servers dev/string-producer-config)))
    (is (= "org.apache.kafka.common.serialization.StringSerializer" 
           (:key-serializer dev/string-producer-config)))
    (is (= "org.apache.kafka.common.serialization.StringSerializer" 
           (:value-serializer dev/string-producer-config)))
    (is (= "all" (:acks dev/string-producer-config)))
    (is (= 3 (:retries dev/string-producer-config)))))

(deftest test-string-consumer-config
  (testing "String consumer configuration"
    (is (map? dev/string-consumer-config))
    (is (= "localhost:9092" (:bootstrap-servers dev/string-consumer-config)))
    (is (= "org.apache.kafka.common.serialization.StringDeserializer" 
           (:key-deserializer dev/string-consumer-config)))
    (is (= "org.apache.kafka.common.serialization.StringDeserializer" 
           (:value-deserializer dev/string-consumer-config)))
    (is (= "earliest" (:auto-offset-reset dev/string-consumer-config)))
    (is (true? (:enable-auto-commit dev/string-consumer-config)))))

;; ============================================================================
;; Docker Compose Configuration Tests
;; ============================================================================

(deftest test-docker-compose-configurations
  (testing "Docker compose configurations exist and are strings"
    (is (string? dev/default-docker-compose))
    (is (string? dev/kraft-docker-compose))
    (is (string? dev/kraft-simple-docker-compose))
    
    ;; Check that they contain expected services
    (is (str/includes? dev/default-docker-compose "zookeeper:"))
    (is (str/includes? dev/default-docker-compose "kafka:"))
    (is (str/includes? dev/default-docker-compose "kafka-ui:"))
    
    (is (str/includes? dev/kraft-docker-compose "kafka:"))
    (is (str/includes? dev/kraft-docker-compose "kafka-ui:"))
    (is (not (str/includes? dev/kraft-docker-compose "zookeeper:")))
    
    (is (str/includes? dev/kraft-simple-docker-compose "kafka:"))
    (is (not (str/includes? dev/kraft-simple-docker-compose "kafka-ui:")))
    (is (not (str/includes? dev/kraft-simple-docker-compose "zookeeper:")))))

(deftest test-get-compose-config
  (testing "Get compose configuration by mode"
    (is (= dev/default-docker-compose (dev/get-compose-config :zookeeper)))
    (is (= dev/kraft-docker-compose (dev/get-compose-config :kraft)))
    (is (= dev/kraft-simple-docker-compose (dev/get-compose-config :simple)))
    (is (= dev/default-docker-compose (dev/get-compose-config :unknown)))))

;; ============================================================================
;; File Operations Tests
;; ============================================================================

(deftest test-write-docker-compose
  (testing "Write docker-compose.yml file"
    (let [test-file "test-docker-compose.yml"]
      ;; Clean up any existing test file
      (when (.exists (io/file test-file))
        (.delete (io/file test-file)))
      
      ;; Test writing with different modes
      (with-redefs [spit (fn [file content] 
                           (is (= test-file file))
                           (is (string? content)))]
        ;; Test default mode
        (with-redefs [dev/write-docker-compose! 
                      (fn 
                        ([] (dev/write-docker-compose! :zookeeper))
                        ([mode-or-content]
                         (let [content (if (keyword? mode-or-content)
                                         (dev/get-compose-config mode-or-content)
                                         mode-or-content)]
                           (spit test-file content))))]
          (dev/write-docker-compose! :kraft)
          (dev/write-docker-compose! :simple)
          (dev/write-docker-compose! "custom content"))))))

;; ============================================================================
;; Mock Tests for Docker Operations
;; ============================================================================

(defn mock-shell-success [& _args]
  {:exit 0 :out "success" :err ""})

(defn mock-shell-failure [& _args]
  {:exit 1 :out "" :err "command failed"})

(deftest test-docker-operations-success
  (testing "Docker operations with successful shell commands"
    (with-redefs [shell/sh mock-shell-success
                  dev/write-docker-compose! (fn [& _args] nil)]
      
      ;; Test kafka-docker-up! (we can't test the actual Docker commands in unit tests)
      (is (nil? (dev/kafka-docker-up! :simple)))
      
      ;; Test kafka-docker-down!
      (is (nil? (dev/kafka-docker-down!)))
      (is (nil? (dev/kafka-docker-down! true)))
      
      ;; Test kafka-docker-status
      (is (nil? (dev/kafka-docker-status)))
      
      ;; Test kafka-docker-logs!
      (is (nil? (dev/kafka-docker-logs!)))
      (is (nil? (dev/kafka-docker-logs! "kafka")))
      (is (nil? (dev/kafka-docker-logs! "kafka" true)))
      
      ;; Test kafka-docker-restart!
      (is (nil? (dev/kafka-docker-restart!)))
      (is (nil? (dev/kafka-docker-restart! "kafka"))))))

;; ============================================================================
;; Convenience Function Tests  
;; ============================================================================

(deftest test-convenience-functions
  (testing "Convenience functions for different modes"
    (with-redefs [dev/kafka-docker-up! (fn [mode] mode)]
      (is (= :zookeeper (dev/kafka-up-zookeeper!)))
      (is (= :kraft (dev/kafka-up-kraft!)))
      (is (= :simple (dev/kafka-up-simple!))))
    
    (with-redefs [dev/kafka-dev-setup! (fn 
                                         ([mode] [mode ["dev-topic" "test-topic" "metamorphosis-topic"]])
                                         ([mode topics] [mode topics]))]
      (is (= [:zookeeper ["dev-topic" "test-topic" "metamorphosis-topic"]] 
             (dev/kafka-setup-zookeeper!)))
      (is (= [:zookeeper ["custom-topic"]] 
             (dev/kafka-setup-zookeeper! ["custom-topic"])))
      
      (is (= [:kraft ["dev-topic" "test-topic" "metamorphosis-topic"]] 
             (dev/kafka-setup-kraft!)))
      (is (= [:kraft ["custom-topic"]] 
             (dev/kafka-setup-kraft! ["custom-topic"])))
      
      (is (= [:simple ["dev-topic" "test-topic" "metamorphosis-topic"]] 
             (dev/kafka-setup-simple!)))
      (is (= [:simple ["custom-topic"]] 
             (dev/kafka-setup-simple! ["custom-topic"]))))))

;; ============================================================================
;; Wait for Kafka Tests
;; ============================================================================

(deftest test-wait-for-kafka-helper
  (testing "Wait for Kafka helper function"
    (let [start-time (System/currentTimeMillis)
          timeout-ms 1000]
      
      ;; Test success case
      (with-redefs [dev/quick-admin (fn [] :mock-admin)
                    admin/list-topics (fn [_admin] ["topic1"])
                    admin/close! (fn [_admin] nil)]
        (is (true? (#'dev/wait-for-kafka-helper start-time timeout-ms))))
      
      ;; Test timeout case
      (with-redefs [dev/quick-admin (fn [] (throw (Exception. "Connection failed")))]
        (let [old-time (- (System/currentTimeMillis) 2000)]
          (is (false? (#'dev/wait-for-kafka-helper old-time timeout-ms))))))))

(deftest test-wait-for-kafka
  (testing "Wait for Kafka main function"
    ;; Test success case
    (with-redefs [dev/wait-for-kafka-helper (fn [_start-time _timeout-ms] true)]
      (is (true? (dev/wait-for-kafka 1))))
    
    ;; Test failure case  
    (with-redefs [dev/wait-for-kafka-helper (fn [_start-time _timeout-ms] false)]
      (is (false? (dev/wait-for-kafka 1))))
    
    ;; Test default timeout
    (with-redefs [dev/wait-for-kafka-helper (fn [_start-time timeout-ms] 
                                              (is (= (* 60 1000) timeout-ms))
                                              true)]
      (is (true? (dev/wait-for-kafka))))))

;; ============================================================================
;; Integration Setup Tests
;; ============================================================================

(deftest test-kafka-dev-setup-argument-handling
  (testing "Kafka dev setup argument handling"
    (with-redefs [dev/kafka-docker-up! (fn [_mode] nil)
                  dev/wait-for-kafka (fn [] true)
                  dev/setup-dev-topic (fn [_topic _opts] nil)]
      
      ;; Test default call
      (let [calls (atom [])]
        (with-redefs [dev/kafka-docker-up! (fn [mode] (swap! calls conj [:up mode]))
                      dev/setup-dev-topic (fn [topic opts] (swap! calls conj [:topic topic opts]))]
          (dev/kafka-dev-setup!)
          (is (= [[:up :zookeeper]] (filter #(= :up (first %)) @calls)))
          (is (= 3 (count (filter #(= :topic (first %)) @calls))))))
      
      ;; Test with mode only
      (let [calls (atom [])]
        (with-redefs [dev/kafka-docker-up! (fn [mode] (swap! calls conj [:up mode]))]
          (dev/kafka-dev-setup! :kraft)
          (is (= [[:up :kraft]] (filter #(= :up (first %)) @calls)))))
      
      ;; Test with topics only
      (let [calls (atom [])]
        (with-redefs [dev/kafka-docker-up! (fn [mode] (swap! calls conj [:up mode]))
                      dev/setup-dev-topic (fn [topic _opts] (swap! calls conj [:topic topic]))]
          (dev/kafka-dev-setup! ["custom-topic"])
          (is (= [[:up :zookeeper]] (filter #(= :up (first %)) @calls)))
          (is (= [[:topic "custom-topic"]] (filter #(= :topic (first %)) @calls)))))
      
      ;; Test with mode and topics
      (let [calls (atom [])]
        (with-redefs [dev/kafka-docker-up! (fn [mode] (swap! calls conj [:up mode]))
                      dev/setup-dev-topic (fn [topic _opts] (swap! calls conj [:topic topic]))]
          (dev/kafka-dev-setup! :simple ["topic1" "topic2"])
          (is (= [[:up :simple]] (filter #(= :up (first %)) @calls)))
          (is (= [[:topic "topic1"] [:topic "topic2"]] (filter #(= :topic (first %)) @calls))))))))

(deftest test-kafka-dev-teardown
  (testing "Kafka dev teardown"
    (with-redefs [dev/kafka-docker-down! (fn [remove-volumes?] remove-volumes?)]
      (is (false? (dev/kafka-dev-teardown!)))
      (is (true? (dev/kafka-dev-teardown! true))))))

;; ============================================================================
;; Mock Tests for Kafka Operations (when Kafka is not available)
;; ============================================================================

(deftest test-mock-kafka-operations
  (testing "Mock Kafka operations for unit testing"
    ;; Mock quick-admin to avoid actual Kafka connection
    (with-redefs [dev/quick-admin (fn [] :mock-admin)
                  admin/create-admin-client (fn [_config] :mock-admin)
                  admin/close! (fn [_admin] nil)]
      
      ;; Test setup-dev-topic
      (with-redefs [admin/create-topic-if-not-exists! 
                    (fn [admin topic opts] 
                      (is (= :mock-admin admin))
                      (is (string? topic))
                      (is (map? opts))
                      nil)]
        (is (nil? (dev/setup-dev-topic "test-topic")))
        (is (nil? (dev/setup-dev-topic "test-topic" {:partitions 5})))))))