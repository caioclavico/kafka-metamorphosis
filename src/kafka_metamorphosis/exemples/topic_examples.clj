(ns kafka-metamorphosis.exemples.topic-examples
  "Examples for topic management with Kafka Metamorphosis"
  (:require [kafka-metamorphosis.admin :as admin]))

(defn quick-admin 
  "Create a quick admin client for examples"
  []
  (admin/create-admin-client {"bootstrap.servers" "localhost:9092"}))

(defn basic-topic-creation-example
  "Example of basic topic creation"
  []
  (println "🪲 Creating Topics Example")
  (println "=========================")
  
  (let [admin-client (quick-admin)]
    (try
      ;; Create a simple topic
      (println "\n1. Creating a simple topic...")
      (admin/create-topic-if-not-exists! admin-client "simple-topic")
      
      ;; Create a topic with multiple partitions
      (println "\n2. Creating a topic with 5 partitions...")
      (admin/create-topic-if-not-exists! admin-client "multi-partition-topic" 
                                         {:partitions 5})
      
      ;; Create a topic with custom configuration
      (println "\n3. Creating a compacted topic...")
      (admin/create-topic-if-not-exists! admin-client "compacted-topic"
                                         {:partitions 3
                                          :configs {"cleanup.policy" "compact"
                                                   "min.cleanable.dirty.ratio" "0.1"}})
      
      ;; List all topics
      (println "\n4. Listing all topics:")
      (let [topics (admin/list-topics admin-client)]
        (doseq [topic (sort topics)]
          (println "   📄" topic)))
      
      ;; Describe a topic
      (println "\n5. Describing multi-partition-topic:")
      (let [info (admin/describe-topic admin-client "multi-partition-topic")]
        (println "   Name:" (:name info))
        (println "   Partitions:" (count (:partitions info)))
        (println "   Partition details:")
        (doseq [{:keys [partition leader replicas]} (:partitions info)]
          (println "     Partition" partition "- Leader:" (:id leader) "Replicas:" replicas)))
      
      (println "\n✅ Topic creation examples completed!")
      
      (finally
        (admin/close! admin-client)))))

(defn topic-management-workflow
  "Complete workflow for topic management"
  []
  (println "🪲 Topic Management Workflow")
  (println "============================")
  
  (let [admin-client (quick-admin)
        topic-name "workflow-demo-topic"]
    (try
      ;; Step 1: Check if topic exists
      (println "\n1. Checking if topic exists...")
      (if (admin/topic-exists? admin-client topic-name)
        (println "   ✅ Topic" topic-name "already exists")
        (println "   ❌ Topic" topic-name "does not exist"))
      
      ;; Step 2: Create topic
      (println "\n2. Creating topic...")
      (admin/create-topic-if-not-exists! admin-client topic-name 
                                         {:partitions 3 
                                          :replication-factor 1})
      
      ;; Step 3: Verify creation
      (println "\n3. Verifying topic creation...")
      (if (admin/topic-exists? admin-client topic-name)
        (println "   ✅ Topic" topic-name "was created successfully")
        (println "   ❌ Topic" topic-name "creation failed"))
      
      ;; Step 4: Get topic details
      (println "\n4. Getting topic details...")
      (let [info (admin/describe-topic admin-client topic-name)]
        (println "   Topic:" (:name info))
        (println "   Partitions:" (count (:partitions info)))
        (println "   Internal:" (:internal? info)))
      
      ;; Step 5: Show cluster info
      (println "\n5. Cluster information:")
      (let [cluster (admin/get-cluster-info admin-client)]
        (println "   Cluster ID:" (:cluster-id cluster))
        (println "   Number of nodes:" (count (:nodes cluster))))
      
      (println "\n✅ Workflow completed!")
      
      (finally
        (admin/close! admin-client)))))

(defn cleanup-example-topics
  "Clean up example topics (use with caution!)"
  []
  (println "🪲 Cleaning up example topics...")
  
  (let [admin-client (quick-admin)
        example-topics ["simple-topic" 
                       "multi-partition-topic" 
                       "compacted-topic" 
                       "workflow-demo-topic"]]
    (try
      (doseq [topic example-topics]
        (when (admin/topic-exists? admin-client topic)
          (println "🗑️ Deleting" topic)
          (admin/delete-topic! admin-client topic)))
      
      (println "✅ Cleanup completed!")
      
      (finally
        (admin/close! admin-client)))))

(comment
  ;; To run the examples:
  
  ;; 1. Make sure Kafka is running on localhost:9092
  
  ;; 2. Run basic topic creation
  (basic-topic-creation-example)
  
  ;; 3. Run the management workflow
  (topic-management-workflow)
  
  ;; 4. List all topics directly
  (let [admin-client (quick-admin)]
    (try
      (admin/list-topics admin-client)
      (finally
        (admin/close! admin-client))))
  
  ;; 5. Describe a specific topic
  (let [admin-client (quick-admin)]
    (try
      (admin/describe-topic admin-client "multi-partition-topic")
      (finally
        (admin/close! admin-client))))
  
  ;; 6. Clean up when done (optional)
  (cleanup-example-topics)
  )
