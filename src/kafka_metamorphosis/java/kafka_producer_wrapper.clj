(ns kafka-metamorphosis.java.kafka-producer-wrapper
  "Java-facing wrapper around kafka-metamorphosis.producer.

   Emits the concrete class
     io.github.caioclavico.kafkametamorphosis.KafkaProducerWrapper
   via :gen-class. Java callers use it as any normal Java class:

     try (KafkaProducerWrapper p = new KafkaProducerWrapper()) {
         p.publish(\"orders.new\", \"123\", \"{\\\"ticker\\\":\\\"PETR4\\\"}\");
     }

   No clojure.java.api.Clojure, no IFn.invoke, no reflection on the
   Java side."
  (:require [kafka-metamorphosis.producer :as producer]
            [kafka-metamorphosis.core :as core]
            [kafka-metamorphosis.java.interop :as interop])
  (:import [java.util Map])
  (:gen-class
    :name    io.github.caioclavico.kafkametamorphosis.KafkaProducerWrapper
    :prefix  "kpw-"
    :state   state
    :init    init
    :implements [java.lang.AutoCloseable]
    :constructors {[]              []
                   [String]        []
                   [java.util.Map] []}
    :methods [[publish     [String String String] void]
              [publish     [String String]        void]
              [publishJson [String String String] void]]))

(set! *warn-on-reflection* true)

;; ---------------------------------------------------------------------------
;; Construction
;; ---------------------------------------------------------------------------

(defn- build-producer
  "Create the underlying Clojure producer from a config source.
   Accepts: nil (defaults), String (bootstrap.servers), or java.util.Map."
  [src]
  (let [config (cond
                 (nil? src)                     (core/producer-config {})
                 (string? src)                  (core/producer-config src {})
                 (instance? java.util.Map src)  (interop/java-map->config src)
                 :else
                 (throw (IllegalArgumentException.
                          (str "Unsupported producer config type: " (class src)))))]
    (producer/create config)))

(defn kpw-init
  "Multi-arity init covering all declared :constructors signatures."
  ([]      [[] (atom {:producer (build-producer nil)})])
  ([arg]   [[] (atom {:producer (build-producer arg)})]))

;; ---------------------------------------------------------------------------
;; Instance methods
;; ---------------------------------------------------------------------------

(defn- producer-of [^io.github.caioclavico.kafkametamorphosis.KafkaProducerWrapper this]
  (:producer @(.state this)))

(defn kpw-publish-String-String-String
  [this ^String topic ^String key ^String value]
  (interop/with-kafka-ex
    (str "Failed to publish to topic '" topic "'")
    (producer/send! (producer-of this) topic key value)
    nil))

(defn kpw-publish-String-String
  [this ^String topic ^String value]
  (kpw-publish-String-String-String this topic nil value))

(defn kpw-publishJson
  [this ^String topic ^String key ^String json-value]
  ;; The JSON-aware publish path: identical wire format (StringSerializer),
  ;; but kept as a separate method so callers signal intent and future
  ;; serializer auto-detection / schema validation can hook in here.
  (interop/with-kafka-ex
    (str "Failed to publish JSON to topic '" topic "'")
    (producer/send! (producer-of this) topic key json-value)
    nil))

(defn kpw-close [this]
  (interop/with-kafka-ex
    "Failed to close producer"
    (producer/close! (producer-of this))))
