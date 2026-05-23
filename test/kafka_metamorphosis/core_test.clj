(ns kafka-metamorphosis.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [kafka-metamorphosis.core :as core]
            [kafka-metamorphosis.serializers :as serializers]))

(deftest test-configuration-builders
  (testing "Configuration builder functions"
    ;; Test producer config
    (let [config (core/producer-config)]
      (is (map? config))
      (is (contains? config :bootstrap-servers))
      (is (contains? config :key-serializer)))
    
    ;; Test consumer config
    (let [config (core/consumer-config "test-group")]
      (is (map? config))
      (is (contains? config :bootstrap-servers))
      (is (contains? config :group-id))
      (is (= "test-group" (:group-id config))))
    
    ;; Test admin config
    (let [config (core/admin-config)]
      (is (map? config))
      (is (contains? config :bootstrap-servers)))))

(deftest test-configuration-with-serializers
  (testing "Configuration with different serializers"
    ;; Test with string serializers
    (let [config (core/producer-config "localhost:9092" {} serializers/string-serializers)]
      (is (contains? config :key-serializer))
      (is (contains? config :value-serializer)))
    
    ;; Test with JSON serializers
    (let [config (core/producer-config "localhost:9092" {} serializers/simple-json-serializers)]
      (is (contains? config :key-serializer))
      (is (contains? config :json-mode)))))

(deftest test-json-producer-config
  (testing "JSON producer configuration"
    (let [config (core/json-producer-config)]
      (is (map? config))
      (is (contains? config :bootstrap-servers))
      (is (contains? config :json-mode))))
  
  (testing "JSON producer config with custom servers"
    (let [config (core/json-producer-config "broker1:9092" {})]
      (is (= "broker1:9092" (:bootstrap-servers config)))
      (is (contains? config :json-mode)))))

(deftest test-json-consumer-config
  (testing "JSON consumer configuration"
    (let [config (core/json-consumer-config "test-group")]
      (is (map? config))
      (is (contains? config :bootstrap-servers))
      (is (contains? config :group-id))
      (is (contains? config :json-mode))
      (is (= "test-group" (:group-id config)))))
  
  (testing "JSON consumer config with custom servers"
    (let [config (core/json-consumer-config "broker:9092" "test-group" {})]
      (is (= "broker:9092" (:bootstrap-servers config)))
      (is (= "test-group" (:group-id config))))))

(deftest test-health-check-without-kafka
  (testing "Health check returns proper error when Kafka is not available"
    (let [health (core/health-check "localhost:9999")] ; Port that should be unavailable
      (is (map? health))
      (is (contains? health :status))
      (is (= :unhealthy (:status health)))
      (is (contains? health :error))))
  
  (testing "Health check with default server"
    (let [health (core/health-check)]
      (is (map? health))
      (is (contains? health :status)))))

(deftest test-admin-configuration-variants
  (testing "Admin config with different parameters"
    ;; Default admin config
    (let [config (core/admin-config)]
      (is (map? config))
      (is (contains? config :bootstrap-servers)))
    
    ;; Custom admin config
    (let [config (core/admin-config "custom:9092")]
      (is (= "custom:9092" (:bootstrap-servers config))))
    
    (is true)))

(deftest test-producer-config-variants
  (testing "Producer config with multiple parameter combinations"
    ;; Default
    (let [config (core/producer-config)]
      (is (contains? config :bootstrap-servers)))
    
    ;; With server
    (let [config (core/producer-config "broker:9092" {})]
      (is (= "broker:9092" (:bootstrap-servers config))))
    
    ;; With options
    (let [config (core/producer-config {:acks "all" :retries 3})]
      (is (= "all" (:acks config)))
      (is (= 3 (:retries config))))))

(deftest test-consumer-config-variants
  (testing "Consumer config with multiple parameter combinations"
    ;; Just group-id
    (let [config (core/consumer-config "group1")]
      (is (= "group1" (:group-id config)))
      (is (contains? config :bootstrap-servers)))
    
    ;; With server
    (let [config (core/consumer-config "broker:9092" "group2" {})]
      (is (= "group2" (:group-id config)))
      (is (= "broker:9092" (:bootstrap-servers config))))
    
    ;; With options
    (let [config (core/consumer-config "group3" {:auto-offset-reset "latest"})]
      (is (= "group3" (:group-id config)))
      (is (= "latest" (:auto-offset-reset config))))))

(deftest test-configuration-immutability
  (testing "Configurations are independent maps"
    (let [config1 (core/producer-config)
          config2 (core/producer-config)]
      (is (not (identical? config1 config2)))
      (is (= config1 config2)))
    
    (let [config1 (core/consumer-config "group1")
          config2 (core/consumer-config "group1")]
      (is (not (identical? config1 config2)))
      (is (= config1 config2)))))
