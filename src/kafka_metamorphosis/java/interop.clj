(ns kafka-metamorphosis.java.interop
  "Internal helpers for the Java-facing facade. NOT part of the public API.
   Centralizes type-coercion between Java collections and Clojure data so the
   individual gen-class wrappers stay small and free of reflection."
  (:require [clojure.tools.logging :as log])
  (:import [java.util Map List]))

(set! *warn-on-reflection* true)

(defn java-map->config
  "Convert a java.util.Map<String,Object> into a Clojure config map with
   keyword keys. Accepts both kebab-case and dot-notation Kafka keys; the
   underlying util/normalize-config will canonicalize them."
  [^Map m]
  (when m
    (persistent!
      (reduce
        (fn [acc e]
          (let [k (.getKey ^java.util.Map$Entry e)
                v (.getValue ^java.util.Map$Entry e)]
            (assoc! acc (keyword (str k)) v)))
        (transient {})
        (.entrySet m)))))

(defn java-list->vec
  "Convert a java.util.List<String> into a Clojure vector of strings."
  [^List xs]
  (vec xs))

(defmacro with-kafka-ex
  "Wrap a body so that any Throwable is rethrown as a
   io.github.caioclavico.kafkametamorphosis.KafkaMetamorphosisException with
   the given message. Keeps the Java-facing API exception-stable."
  [msg & body]
  `(try
     (do ~@body)
     (catch io.github.caioclavico.kafkametamorphosis.KafkaMetamorphosisException e# (throw e#))
     (catch Throwable t#
       (log/error t# ~msg)
       (throw (io.github.caioclavico.kafkametamorphosis.KafkaMetamorphosisException.
                ~msg t#)))))
