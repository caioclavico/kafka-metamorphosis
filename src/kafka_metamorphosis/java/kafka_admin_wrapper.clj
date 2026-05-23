(ns kafka-metamorphosis.java.kafka-admin-wrapper
  "Java-facing wrapper around kafka-metamorphosis.admin.

   Emits io.github.caioclavico.kafkametamorphosis.KafkaAdminWrapper."
  (:require [kafka-metamorphosis.admin :as admin]
            [kafka-metamorphosis.core :as core]
            [kafka-metamorphosis.java.interop :as interop])
  (:import [java.util Map Set HashSet]))

(set! *warn-on-reflection* true)

(gen-class
  :name    io.github.caioclavico.kafkametamorphosis.KafkaAdminWrapper
  :prefix  "kaw-"
  :state   state
  :init    init
  :implements [java.lang.AutoCloseable]
  :constructors {[]               []
                 [String]         []
                 [java.util.Map]  []}
  :methods [[createTopic            [String]                      void]
            [createTopic            [String int int]              void]
            [createTopicIfNotExists [String int int]              void]
            [deleteTopic            [String]                      void]
            [listTopics             []                            java.util.Set]
            [topicExists            [String]                      boolean]])

(defn- build-admin [src]
  (cond
    (nil? src)
    (admin/create-admin-client (core/admin-config))

    (string? src)
    (admin/create-admin-client (core/admin-config src))

    (instance? java.util.Map src)
    (admin/create-admin-client (interop/java-map->config src))

    :else
    (throw (IllegalArgumentException.
             (str "Unsupported admin config: " (class src))))))

(defn kaw-init
  ([]    [[] (atom {:client (build-admin nil)})])
  ([arg] [[] (atom {:client (build-admin arg)})]))

(defn- client-of [^io.github.caioclavico.kafkametamorphosis.KafkaAdminWrapper this]
  (:client @(.state this)))

(defn kaw-createTopic-String
  [this ^String topic]
  (interop/with-kafka-ex
    (str "Failed to create topic '" topic "'")
    (admin/create-topic! (client-of this) topic)))

(defn kaw-createTopic-String-int-int
  [this ^String topic ^Integer partitions ^Integer replication]
  (interop/with-kafka-ex
    (str "Failed to create topic '" topic "'")
    (admin/create-topic! (client-of this) topic
                         {:partitions         partitions
                          :replication-factor replication})))

(defn kaw-createTopicIfNotExists
  [this ^String topic ^Integer partitions ^Integer replication]
  (interop/with-kafka-ex
    (str "Failed to create topic-if-absent '" topic "'")
    (admin/create-topic-if-not-exists!
      (client-of this) topic
      {:partitions partitions :replication-factor replication})))

(defn kaw-deleteTopic
  [this ^String topic]
  (interop/with-kafka-ex
    (str "Failed to delete topic '" topic "'")
    (admin/delete-topic! (client-of this) topic)))

(defn kaw-listTopics ^Set [this]
  (interop/with-kafka-ex
    "Failed to list topics"
    (HashSet. ^java.util.Collection (admin/list-topics (client-of this)))))

(defn kaw-topicExists [this ^String topic]
  (interop/with-kafka-ex
    (str "Failed to check existence of '" topic "'")
    (boolean (admin/topic-exists? (client-of this) topic))))

(defn kaw-close [this]
  (interop/with-kafka-ex
    "Failed to close admin client"
    (.close ^java.lang.AutoCloseable (client-of this))))
