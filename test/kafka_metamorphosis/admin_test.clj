(ns kafka-metamorphosis.admin-test
  "Simple test to verify admin functions work"
  (:require [clojure.test :refer [deftest testing is]]
            [kafka-metamorphosis.admin :as admin]
            [kafka-metamorphosis.core :as core]))

(deftest test-admin-config-creation
  (testing "Admin configuration creation"
    (let [config (core/admin-config)]
      (is (map? config))
      (is (contains? config :bootstrap-servers))))
  
  (testing "Admin config has expected keys"
    (let [config (core/admin-config)]
      (is (string? (:bootstrap-servers config))))))

(deftest test-admin-config-with-custom-server
  (testing "Admin configuration with custom server"
    (let [config (core/admin-config "custom-broker:9092")]
      (is (map? config))
      (is (= "custom-broker:9092" (:bootstrap-servers config)))))
  
  (testing "Admin config with multiple brokers"
    (let [config (core/admin-config "broker1:9092,broker2:9092,broker3:9092")]
      (is (= "broker1:9092,broker2:9092,broker3:9092" (:bootstrap-servers config))))))

(deftest test-admin-creation-function-exists
  (testing "Admin client creation function exists"
    (is (fn? admin/create-admin-client))
    (is (fn? admin/close!)))
  
  (testing "Admin operation functions exist"
    (is (fn? admin/create-topic!))
    (is (fn? admin/list-topics))
    (is (fn? admin/topic-exists?))
    (is (fn? admin/delete-topic!))))

(deftest test-admin-config-independence
  (testing "Multiple admin configs are independent"
    (let [config1 (core/admin-config "broker1:9092")
          config2 (core/admin-config "broker2:9092")]
      (is (not (identical? config1 config2)))
      (is (= "broker1:9092" (:bootstrap-servers config1)))
      (is (= "broker2:9092" (:bootstrap-servers config2))))))

(deftest test-admin-config-defaults
  (testing "Admin config uses reasonable defaults"
    (let [config (core/admin-config)]
      ;; Should have string serializers by default
      (is (contains? config :bootstrap-servers))
      ;; Should not have schema-related keys by default
      (is (not (contains? config :schemas))))))

;; Helper functions for testing with running Kafka (commented out)
(comment
  (defn test-admin-creation
    "Test if we can create an admin client without errors"
    []
    (try
      (let [config {:bootstrap-servers "localhost:9092"}
            admin-client (admin/create-admin-client config)]
        (println "✅ Admin client created successfully")
        (admin/close! admin-client)
        (println "✅ Admin client closed successfully")
        true)
      (catch Exception e
        (println "❌ Error creating admin client:" (.getMessage e))
        false)))

  ;; Test admin client creation (doesn't require running Kafka)
  (test-admin-creation))
