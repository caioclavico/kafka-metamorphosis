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
    (let [config (core/producer-config {} serializers/string-serializers)]
      (is (contains? config :key-serializer))
      (is (contains? config :value-serializer)))
    
    ;; Test with JSON serializers
    (let [config (core/producer-config {} serializers/simple-json-serializers)]
      (is (contains? config :key-serializer))
      (is (contains? config :json-mode)))))

(deftest test-json-producer-config
  (testing "JSON producer configuration"
    (let [config (core/json-producer-config)]
      (is (map? config))
      (is (contains? config :bootstrap-servers))
      (is (contains? config :json-mode)))))

(deftest test-json-consumer-config
  (testing "JSON consumer configuration"
    (let [config (core/json-consumer-config "test-group")]
      (is (map? config))
      (is (contains? config :bootstrap-servers))
      (is (contains? config :group-id))
      (is (contains? config :json-mode))
      (is (= "test-group" (:group-id config))))))

(deftest test-health-check-without-kafka
  (testing "Health check returns proper error when Kafka is not available"
    (let [health (core/health-check "localhost:9999")] ; Port that should be unavailable
      (is (map? health))
      (is (contains? health :status))
      (is (= :unhealthy (:status health)))
      (is (contains? health :error)))))
