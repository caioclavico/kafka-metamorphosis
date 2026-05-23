(ns kafka-metamorphosis.java.kafka-consumer-wrapper
  "Java-facing wrapper around kafka-metamorphosis.consumer.

   Emits io.github.caioclavico.kafkametamorphosis.KafkaConsumerWrapper.

   Java usage:

     try (KafkaConsumerWrapper c =
              new KafkaConsumerWrapper(\"orders-group\")) {
         c.subscribe(List.of(\"orders.new\"));
         c.consume(1000L, record -> {
             System.out.println(record.value());
         });
     }"
  (:require [kafka-metamorphosis.consumer :as consumer]
            [kafka-metamorphosis.core :as core]
            [kafka-metamorphosis.java.interop :as interop])
  (:import [java.util Map List ArrayList HashMap]
           [io.github.caioclavico.kafkametamorphosis KafkaRecord MessageHandler])
  (:gen-class
    :name    io.github.caioclavico.kafkametamorphosis.KafkaConsumerWrapper
    :prefix  "kcw-"
    :state   state
    :init    init
    :implements [java.lang.AutoCloseable]
    :constructors {[String]                []   ;; group-id, defaults
                   [String String]         []   ;; group-id, bootstrap.servers
                   [java.util.Map]         []}  ;; full config
    :methods [[subscribe [java.util.List]                                       void]
              [poll      [long]                                                 java.util.List]
              [consume   [long io.github.caioclavico.kafkametamorphosis.MessageHandler] void]
              [stop      []                                                     void]]))

(set! *warn-on-reflection* true)

;; ---------------------------------------------------------------------------
;; Construction
;; ---------------------------------------------------------------------------

(defn- build-consumer
  "Build the underlying Clojure consumer from supported argument shapes."
  ([group-id]                          (consumer/create (core/consumer-config group-id)))
  ([group-id bootstrap-or-map]
   (cond
     (string? bootstrap-or-map)
     (consumer/create (core/consumer-config bootstrap-or-map group-id {}))

     (instance? java.util.Map bootstrap-or-map)
     (consumer/create
       (assoc (interop/java-map->config bootstrap-or-map) :group-id group-id))

     :else
     (throw (IllegalArgumentException.
              (str "Unsupported consumer arg type: " (class bootstrap-or-map)))))))

(defn- build-from-map [^Map m]
  (consumer/create (interop/java-map->config m)))

(defn kcw-init
  ([arg]
   (cond
     (string? arg)
     [[] (atom {:consumer (build-consumer arg) :stop? (atom false)})]

     (instance? java.util.Map arg)
     [[] (atom {:consumer (build-from-map arg) :stop? (atom false)})]

     :else
     (throw (IllegalArgumentException.
              (str "Unsupported consumer config type: " (class arg))))))
  ([^String group-id ^String bootstrap]
   [[] (atom {:consumer (build-consumer group-id bootstrap)
              :stop?    (atom false)})]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- consumer-of [^io.github.caioclavico.kafkametamorphosis.KafkaConsumerWrapper this]
  (:consumer @(.state this)))

(defn- stop-flag-of [^io.github.caioclavico.kafkametamorphosis.KafkaConsumerWrapper this]
  (:stop? @(.state this)))

(defn- ->kafka-record ^KafkaRecord [record]
  ;; `record` is a Clojure map produced by kafka-metamorphosis.consumer/poll!
  (let [^Map headers (HashMap.)]
    (doseq [[k v] (:headers record)]
      (.put headers (str k) (bytes v)))
    (KafkaRecord. (:topic record)
                  (int  (:partition record))
                  (long (:offset record))
                  (long (:timestamp record))
                  (some-> (:key record) str)
                  (some-> (:value record) str)
                  headers)))

;; ---------------------------------------------------------------------------
;; Instance methods
;; ---------------------------------------------------------------------------

(defn kcw-subscribe [this ^List topics]
  (interop/with-kafka-ex
    "Failed to subscribe to topics"
    (consumer/subscribe! (consumer-of this) (vec topics))))

(defn kcw-poll ^List [this ^long timeout-ms]
  (interop/with-kafka-ex
    "Failed to poll Kafka"
    (let [records (consumer/poll! (consumer-of this) timeout-ms)
          out     (ArrayList. (count records))]
      (doseq [r records]
        (.add out (->kafka-record r)))
      out)))

(defn kcw-consume [this ^long timeout-ms ^MessageHandler handler]
  (interop/with-kafka-ex
    "Failed during consume loop"
    (let [stop? (stop-flag-of this)]
      (reset! stop? false)
      (consumer/consume!
        (consumer-of this)
        {:poll-timeout timeout-ms
         :handler      (fn [record]
                         (.onMessage handler (->kafka-record record)))
         :stop-fn      (fn [] @stop?)}))))

(defn kcw-stop [this]
  (reset! (stop-flag-of this) true))

(defn kcw-close [this]
  (interop/with-kafka-ex
    "Failed to close consumer"
    (reset! (stop-flag-of this) true)
    (consumer/close! (consumer-of this))))
