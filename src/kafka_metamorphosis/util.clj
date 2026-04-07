(ns kafka-metamorphosis.util
  "Utility functions for Kafka Metamorphosis")

(defn map->properties
  "Convert a Clojure map to Java Properties.
   Keys are converted to strings using (name k) and values to strings using (str v)."
  [config-map]
  (let [props (java.util.Properties.)]
    (doseq [[k v] config-map]
      (.put props (name k) (str v)))
    props))

(defn kebab->snake
  "Convert kebab-case keys to snake_case for Kafka configuration.
   Example: :max-poll-records -> max.poll.records"
  [k]
  (if (keyword? k)
    (clojure.string/replace (name k) "-" ".")
    k))

(defn kebab->dot
  "Convert kebab-case keyword to dot.case string.
   Example: :bootstrap-servers -> \"bootstrap.servers\""
  [k]
  (-> (name k)
      (clojure.string/replace #"-" ".")
      (clojure.string/replace #"_" ".")))

(defn normalize-config
  "Normalize configuration map by converting kebab-case keys to dot notation."
  [config-map]
  (into {} (map (fn [[k v]] [(kebab->dot k) v]) config-map)))
