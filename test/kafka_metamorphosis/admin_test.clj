(ns kafka-metamorphosis.admin-test
  "Simple test to verify admin functions work"
  (:require [kafka-metamorphosis.admin :as admin]))

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

(comment
  ;; Test admin client creation (doesn't require running Kafka)
  (test-admin-creation)
  )
