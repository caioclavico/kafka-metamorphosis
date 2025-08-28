(ns kafka-metamorphosis.admin-test
  "Simple test to verify admin functions work"
  (:require [clojure.test :refer [deftest testing is]]
            [kafka-metamorphosis.admin :as admin]
            [kafka-metamorphosis.core :as core]))

(deftest test-admin-config-creation
  (testing "Admin configuration creation"
    (let [config (core/admin-config)]
      (is (map? config))
      (is (contains? config :bootstrap-servers)))))

(deftest test-admin-config-with-custom-server
  (testing "Admin configuration with custom server"
    (let [config (core/admin-config "custom-broker:9092")]
      (is (map? config))
      (is (= "custom-broker:9092" (:bootstrap-servers config))))))

;; Integration test that doesn't require Kafka
(deftest test-admin-creation-function-exists
  (testing "Admin client creation function exists"
    (is (fn? admin/create-admin-client))
    (is (fn? admin/close!))))

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
