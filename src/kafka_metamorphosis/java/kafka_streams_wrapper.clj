(ns kafka-metamorphosis.java.kafka-streams-wrapper
  "Java-facing wrapper around kafka-metamorphosis.streams.

   Emits io.github.caioclavico.kafkametamorphosis.KafkaStreamsWrapper."
  (:require [kafka-metamorphosis.streams :as streams]
            [kafka-metamorphosis.java.interop :as interop])
  (:import [java.util Map List ArrayList]
           [org.apache.kafka.streams KafkaStreams])
  (:gen-class
    :name    io.github.caioclavico.kafkametamorphosis.KafkaStreamsWrapper
    :prefix  "ksw-"
    :state   state
    :init    init
    :implements [java.lang.AutoCloseable]
    :constructors {[]              []
                   [String]        []
                   [java.util.Map] []}
    :methods [[createBuilder        []                               java.lang.Object]
              [stream              [java.lang.Object java.util.List]     java.lang.Object]
              [stream              [java.lang.Object String]            java.lang.Object]
              [mapValues           [java.lang.Object java.util.function.Function] java.lang.Object]
              [filter              [java.lang.Object java.util.function.BiPredicate] java.lang.Object]
              [branch              [java.lang.Object java.util.List]     java.util.List]
              [to                  [java.lang.Object String]            void]
              [createKafkaStreams [java.lang.Object java.util.Map]       java.lang.Object]
              [start               []                               void]
              [cleanup             []                               void]
              [state               []                               String]]))

(set! *warn-on-reflection* true)

(defn- build-init-state [src]
  (let [config (cond
                 (nil? src)                    (streams/streams-config)
                 (string? src)                 (streams/streams-config src)
                 (instance? java.util.Map src) (interop/java-map->config src)
                 :else
                 (throw (IllegalArgumentException.
                          (str "Unsupported streams config type: " (class src)))))]
    {:config config
     :builder (streams/create-builder)
     :streams nil}))

(defn ksw-init
  ([]      [[] (atom (build-init-state nil))])
  ([arg]   [[] (atom (build-init-state arg))]))

(defn- state-of [^io.github.caioclavico.kafkametamorphosis.KafkaStreamsWrapper this]
  (:state @(.state this)))

(defn- set-state! [^io.github.caioclavico.kafkametamorphosis.KafkaStreamsWrapper this key value]
  (swap! (.state this) assoc key value))

(defn ksw-createBuilder [this]
  (interop/with-kafka-ex
    "Failed to create streams builder"
    (:builder @(.state this))))

(defn ksw-stream [this ^java.util.List topics]
  (interop/with-kafka-ex
    "Failed to create KStream"
    (let [builder (:builder @(.state this))]
      (if (instance? java.lang.String topics)
        (streams/stream builder topics)
        (streams/stream builder (interop/java-list->vec topics))))))

(defn ksw-mapValues [this ^java.lang.Object stream ^java.util.function.Function fn]
  (interop/with-kafka-ex
    "Failed to map values"
    (.mapValues ^java.lang.Object stream fn)))

(defn ksw-filter [this ^java.lang.Object stream ^java.util.function.BiPredicate pred]
  (interop/with-kafka-ex
    "Failed to filter stream"
    (.filter ^java.lang.Object stream pred)))

(defn ksw-branch [this ^java.lang.Object stream ^java.util.List preds]
  (interop/with-kafka-ex
    "Failed to branch stream"
    (let [pred-array (into-array java.util.function.BiPredicate (map identity preds))]
      (ArrayList. (.branch ^java.lang.Object stream pred-array)))))

(defn ksw-to [this ^java.lang.Object stream ^String topic]
  (interop/with-kafka-ex
    "Failed to write stream to topic"
    (.to ^java.lang.Object stream topic)))

(defn ksw-createKafkaStreams [this ^java.util.Map config]
  (interop/with-kafka-ex
    "Failed to create KafkaStreams instance"
    (let [state @(.state this)
          builder (:builder state)
          stream-config (if config
                          (interop/java-map->config config)
                          (:config state))
          topology (streams/build-topology builder)
          ks (streams/create topology stream-config)]
      (set-state! this :streams ks)
      ks)))

(defn ksw-start [this]
  (interop/with-kafka-ex
    "Failed to start KafkaStreams"
    (let [ks (:streams @(.state this))]
      (.start ^KafkaStreams ks))))

(defn ksw-close [this]
  (interop/with-kafka-ex
    "Failed to close KafkaStreams"
    (let [ks (:streams @(.state this))]
      (when ks
        (.close ^KafkaStreams ks)))))

(defn ksw-cleanup [this]
  (interop/with-kafka-ex
    "Failed to cleanup KafkaStreams"
    (let [ks (:streams @(.state this))]
      (when ks
        (.cleanUp ^KafkaStreams ks)))))

(defn ksw-state [this]
  (interop/with-kafka-ex
    "Failed to get KafkaStreams state"
    (let [ks (:streams @(.state this))]
      (when ks
        (str (.state ^KafkaStreams ks))))))
