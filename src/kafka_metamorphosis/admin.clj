(ns kafka-metamorphosis.admin
  "Administrative functions for Kafka - topic management, cluster info, etc."
  (:require [kafka-metamorphosis.util :as util])
  (:import [org.apache.kafka.clients.admin AdminClient NewTopic]
           [java.util Collection Collections]))

(defn create-admin-client
  "Create a Kafka AdminClient with the given configuration.
   
   Example config:
   {:bootstrap-servers \"localhost:9092\"}"
  [config]
  (let [normalized-config (util/normalize-config config)
        props (util/map->properties normalized-config)]
    (AdminClient/create props)))

(defn create-topic!
  "Create a new Kafka topic.
   
   Options:
   :partitions - number of partitions (default: 1)
   :replication-factor - replication factor (default: 1)
   :configs - map of topic configurations
   
   Usage:
   (create-topic! admin-client \"my-topic\")
   (create-topic! admin-client \"my-topic\" {:partitions 3 :replication-factor 2})
   (create-topic! admin-client \"my-topic\" {:partitions 3 
                                            :configs {\"cleanup.policy\" \"compact\"}})"
  ([admin-client topic-name]
   (create-topic! admin-client topic-name {}))
  ([admin-client topic-name {:keys [partitions replication-factor configs]
                              :or {partitions 1 replication-factor 1 configs {}}}]
   (let [new-topic (NewTopic. topic-name partitions (short replication-factor))]
     (when (seq configs)
       (.configs new-topic configs))
     (let [result (.createTopics admin-client [new-topic])]
       (.get (.all result))
       (println "✅ Topic" topic-name "created successfully")))))

(defn delete-topic!
  "Delete a Kafka topic.
   
   Usage:
   (delete-topic! admin-client \"my-topic\")"
  [admin-client topic-name]
  (let [result (.deleteTopics admin-client [topic-name])]
    (.get (.all result))
    (println "🗑️ Topic" topic-name "deleted successfully")))

(defn list-topics
  "List all topics in the Kafka cluster.
   Returns a set of topic names.
   
   Usage:
   (list-topics admin-client)"
  [admin-client]
  (let [result (.listTopics admin-client)]
    (-> result .names .get set)))

(defn describe-topic
  "Get detailed information about a topic.
   
   Usage:
   (describe-topic admin-client \"my-topic\")"
  [admin-client topic-name]
  (let [result (.describeTopics admin-client [topic-name])
        topic-description (.get (.get (.values result) topic-name))]
    {:name (.name topic-description)
     :internal? (.isInternal topic-description)
     :partitions (map (fn [partition-info]
                       {:partition (.partition partition-info)
                        :leader (when-let [leader (.leader partition-info)]
                                 {:id (.id leader)
                                  :host (.host leader)
                                  :port (.port leader)})
                        :replicas (map #(.id %) (.replicas partition-info))
                        :in-sync-replicas (map #(.id %) (.isr partition-info))})
                     (.partitions topic-description))}))

(defn topic-exists?
  "Check if a topic exists.
   
   Usage:
   (topic-exists? admin-client \"my-topic\")"
  [admin-client topic-name]
  (contains? (list-topics admin-client) topic-name))

(defn create-topic-if-not-exists!
  "Create a topic only if it doesn't already exist.
   
   Usage:
   (create-topic-if-not-exists! admin-client \"my-topic\" {:partitions 3})"
  ([admin-client topic-name]
   (create-topic-if-not-exists! admin-client topic-name {}))
  ([admin-client topic-name options]
   (if (topic-exists? admin-client topic-name)
     (println "ℹ️ Topic" topic-name "already exists")
     (create-topic! admin-client topic-name options))))

(defn get-cluster-info
  "Get basic information about the Kafka cluster.
   
   Usage:
   (get-cluster-info admin-client)"
  [admin-client]
  (let [result (.describeCluster admin-client)
        cluster-id (.get (.clusterId result))
        nodes (.get (.nodes result))
        controller (.get (.controller result))]
    {:cluster-id cluster-id
     :controller {:id (.id controller)
                  :host (.host controller)
                  :port (.port controller)}
     :nodes (map (fn [node]
                  {:id (.id node)
                   :host (.host node)
                   :port (.port node)
                   :rack (.rack node)})
                nodes)}))

(defn close!
  "Close the AdminClient and release resources."
  [admin-client]
  (.close admin-client))
