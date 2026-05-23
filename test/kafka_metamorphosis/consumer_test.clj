(ns kafka-metamorphosis.consumer-test
  "Tests for consumer functions and configuration"
  (:require [clojure.test :refer [deftest is testing]]
            [kafka-metamorphosis.consumer :as consumer]
            [kafka-metamorphosis.core :as core]
            [kafka-metamorphosis.serializers :as serializers]))

(deftest test-consumer-config-creation
  (testing "Consumer configuration creation with group-id"
    (let [config (core/consumer-config "test-group")]
      (is (map? config))
      (is (= "test-group" (:group-id config)))
      (is (contains? config :bootstrap-servers))
      (is (contains? config :key-deserializer))
      (is (contains? config :value-deserializer))))
  
  (testing "Consumer config with default bootstrap servers"
    (let [config (core/consumer-config "my-group")]
      (is (string? (:bootstrap-servers config)))))
  
  (testing "Consumer config contains auto-offset-reset"
    (let [config (core/consumer-config "group")]
      (is (contains? config :auto-offset-reset)))))

(deftest test-consumer-config-with-servers
  (testing "Consumer config with custom bootstrap servers"
    (let [config (core/consumer-config "broker:9092" "group" {})]
      (is (= "broker:9092" (:bootstrap-servers config)))
      (is (= "group" (:group-id config)))))
  
  (testing "Consumer config with multiple brokers"
    (let [config (core/consumer-config "broker1:9092,broker2:9092" "group" {})]
      (is (= "broker1:9092,broker2:9092" (:bootstrap-servers config))))))

(deftest test-consumer-config-with-options
  (testing "Consumer config with custom options"
    (let [config (core/consumer-config "group" {:auto-offset-reset "latest"
                                                 :enable-auto-commit false})]
      (is (= "latest" (:auto-offset-reset config)))
      (is (false? (:enable-auto-commit config)))))
  
  (testing "Consumer config merges default and custom options"
    (let [config (core/consumer-config "group" {:session-timeout-ms 10000})]
      (is (= 10000 (:session-timeout-ms config)))
      (is (= "group" (:group-id config))))))

(deftest test-consumer-creation-function-exists
  (testing "Consumer creation functions exist"
    (is (fn? consumer/create))
    (is (fn? consumer/subscribe!))
    (is (fn? consumer/poll!))
    (is (fn? consumer/close!)))
  
  (testing "Consumer operations functions exist"
    (is (fn? consumer/seek!))
    (is (fn? consumer/assign!))
    (is (fn? consumer/consume!))
    (is (fn? consumer/commit-sync!))
    (is (fn? consumer/commit-async!))))

(deftest test-json-consumer-config
  (testing "JSON consumer configuration creation"
    (let [config (core/json-consumer-config "json-group")]
      (is (map? config))
      (is (= "json-group" (:group-id config)))
      (is (contains? config :json-mode))))
  
  (testing "JSON consumer config with custom brokers"
    (let [config (core/json-consumer-config "broker:9092" "group" {})]
      (is (= "broker:9092" (:bootstrap-servers config)))
      (is (contains? config :json-mode)))))

(deftest test-consumer-config-independence
  (testing "Multiple consumer configs are independent"
    (let [config1 (core/consumer-config "group1")
          config2 (core/consumer-config "group2")]
      (is (not (identical? config1 config2)))
      (is (= "group1" (:group-id config1)))
      (is (= "group2" (:group-id config2)))))
  
  (testing "Consumer configs with same group-id are equal"
    (let [config1 (core/consumer-config "group")
          config2 (core/consumer-config "group")]
      (is (= config1 config2)))))

(deftest test-consumer-config-defaults
  (testing "Consumer config has sensible defaults"
    (let [config (core/consumer-config "group")]
      ;; Should have string deserializers by default
      (is (contains? config :key-deserializer))
      (is (contains? config :value-deserializer))
      ;; Should have auto-offset-reset
      (is (contains? config :auto-offset-reset))))
  
  (testing "Consumer config earliest offset reset"
    (let [config (core/consumer-config "group" {:auto-offset-reset "earliest"})]
      (is (= "earliest" (:auto-offset-reset config)))))
  
  (testing "Consumer config latest offset reset"
    (let [config (core/consumer-config "group" {:auto-offset-reset "latest"})]
      (is (= "latest" (:auto-offset-reset config))))))

(deftest test-consumer-config-with-serializers
  (testing "Consumer config with different deserializers"
    (let [config (core/consumer-config "localhost:9092" "group" {} serializers/string-deserializers)]
      (is (contains? config :key-deserializer))
      (is (contains? config :value-deserializer)))
    
    (let [config (core/consumer-config "localhost:9092" "group" {} serializers/simple-json-deserializers)]
      (is (contains? config :key-deserializer))
      (is (contains? config :value-deserializer))
      (is (contains? config :json-mode)))))
