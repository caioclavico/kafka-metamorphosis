(ns kafka-metamorphosis.schema
  "Schema management for Kafka messages"
  (:require [kafka-metamorphosis.core :as c]
            [clojure.string :as str]))

;; Schema registry
(defonce ^:private schema-registry (atom {}))

(defn defschema
  "Define and register a schema with the given ID and specification map.
   
   Returns the schema ID."
  [schema-id spec-map]
  (swap! schema-registry assoc schema-id {:spec spec-map})
  schema-id)

(defn get-schema
  "Get a registered schema"
  [schema-id]
  (get @schema-registry schema-id))

(defn get-schema-for-topic
  "Get a schema for a specific topic.
   
   Looks for schemas in this order:
   1. :topic-name/default
   2. :topic-name (direct topic schema)
   
   Examples:
   - (get-schema-for-topic \"users\") -> looks for :users/default or :users"
  [topic-name]
  (let [topic-keyword (keyword topic-name)
        default-schema-id (keyword topic-name "default")]
    (or (get-schema default-schema-id)
        (get-schema topic-keyword))))

(defn list-schemas-for-topic
  "List all schemas registered for a specific topic"
  [topic-name]
  (let [topic-prefix (str topic-name "/")
        all-schemas (keys @schema-registry)]
    (filter (fn [schema-id]
              (and (keyword? schema-id)
                   (or (= (name schema-id) topic-name)
                       (str/starts-with? (str schema-id) (str ":" topic-prefix)))))
            all-schemas)))

(defn list-schemas
  "List all registered schemas"
  []
  (keys @schema-registry))

(defn- validate-field
  "Validate a single field against its predicate"
  [value predicate]
  (try
    (predicate value)
    (catch Exception _e
      false)))

(declare validate-against-spec)

(defn- validate-collection
  "Validate a collection where each item should match the spec"
  [collection item-spec]
  (every? (fn [item]
            (if (map? item-spec)
              (validate-against-spec item item-spec)
              (validate-field item item-spec)))
          collection))

(defn- validate-against-spec
  "Validate a message against a spec map"
  [message spec-map]
  (every? (fn [[field-key predicate]]
            (let [field-value (get message field-key)]
              (cond
                (nil? field-value)
                false

                (and (map? predicate) (map? field-value))
                (validate-against-spec field-value predicate)

                (and (vector? predicate) (= 1 (count predicate)) (coll? field-value))
                (validate-collection field-value (first predicate))
                
                (fn? predicate)
                (validate-field field-value predicate)
                
                :else
                false)))
          spec-map))

(defn validate-message
  "Validate a message against a schema"
  [message schema-id]
  (let [schema (get-schema schema-id)]
    (if schema
      (let [spec-map (:spec schema)]
        (validate-against-spec message spec-map))
      (throw (ex-info "Schema not found" {:schema-id schema-id :available (list-schemas)})))))

(defn validate-message-for-topic
  "Validate a message against a topic's schema.
   
   Automatically looks for topic-scoped schemas:
   - (validate-message-for-topic message \"users\") -> uses :users/default or :users"
  [message topic-name]
  (let [schema (get-schema-for-topic topic-name)]
    (if schema
      (let [spec-map (:spec schema)]
        (validate-against-spec message spec-map))
      ;; Return true if no schema found (optional validation)
      true)))

(declare explain-field)

(defn- explain-against-spec
  "Get detailed explanation for spec validation"
  [message spec-map path]
  (reduce (fn [acc [field-key predicate]]
            (let [field-value (get message field-key)
                  field-errors (explain-field field-value predicate field-key path)]
              (concat acc field-errors)))
          []
          spec-map))

(defn- explain-field
  "Get detailed explanation for a field validation"
  [field-value predicate field-key path]
  (let [full-path (if (empty? path) (name field-key) (str path "." (name field-key)))]
    (cond
      (nil? field-value)
      [{:field full-path
        :error "Field is missing"
        :expected predicate}]
      
      (and (map? predicate) (map? field-value))
      (explain-against-spec field-value predicate full-path)
      
      (and (vector? predicate) (= 1 (count predicate)) (coll? field-value))
      (let [item-spec (first predicate)]
        (reduce (fn [acc [idx item]]
                  (let [item-path (str full-path "[" idx "]")]
                    (if (map? item-spec)
                      (concat acc (explain-against-spec item item-spec item-path))
                      (if (validate-field item item-spec)
                        acc
                        (conj acc {:field item-path
                                  :error "Item validation failed"
                                  :value item
                                  :expected item-spec})))))
                []
                (map-indexed vector field-value)))
      
      (fn? predicate)
      (if (validate-field field-value predicate)
        []
        [{:field full-path
          :error "Field validation failed"
          :value field-value
          :expected predicate}])
      
      :else
      [{:field full-path
        :error "Invalid predicate type"
        :value field-value
        :expected predicate}])))

(defn explain-validation
  "Get detailed validation explanation"
  [message schema-id]
  (let [schema (get-schema schema-id)]
    (if schema
      (let [spec-map (:spec schema)
            errors (explain-against-spec message spec-map "")]
        {:valid? (empty? errors)
         :errors errors
         :message message
         :schema-id schema-id})
      (throw (ex-info "Schema not found" {:schema-id schema-id :available (list-schemas)})))))

(defn explain-validation-for-topic
  "Get detailed validation explanation for a topic's schema"
  [message topic-name]
  (let [schema (get-schema-for-topic topic-name)]
    (if schema
      (let [spec-map (:spec schema)
            schema-id (or (keyword topic-name "default") (keyword topic-name))
            errors (explain-against-spec message spec-map "")]
        {:valid? (empty? errors)
         :errors errors
         :message message
         :topic topic-name
         :schema-id schema-id})
      {:valid? true :errors [] :message message :topic topic-name})))

(defn map-of
  "Create a predicate for maps with specific key-value predicates"
  [key-pred value-pred]
  (fn [m]
    (and (map? m)
         (every? (fn [[k v]]
                   (and (key-pred k) (value-pred v)))
                 m))))

(defn one-of
  "Create a predicate that accepts one of the given values"
  [& values]
  (fn [x] (contains? (set values) x)))

(defn min-count
  "Create a predicate for minimum collection size"
  [n]
  (fn [coll] (and (coll? coll) (>= (count coll) n))))

(defn max-count
  "Create a predicate for maximum collection size"
  [n]
  (fn [coll] (and (coll? coll) (<= (count coll) n))))

(defn schema-ref
  "Create a reference to another schema for validation"
  [schema-id]
  (fn [value]
    (validate-message value schema-id)))

(defn any-of
  "Create a predicate that validates against any of the given schemas"
  [& schema-ids]
  (fn [value]
    (some (fn [schema-id]
            (try
              (validate-message value schema-id)
              (catch Exception _e false)))
          schema-ids)))

(defn all-of
  "Create a predicate that validates against all of the given schemas"
  [& schema-ids]
  (fn [value]
    (every? (fn [schema-id]
              (try
                (validate-message value schema-id)
                (catch Exception _e false)))
            schema-ids)))

(defn with-schema-serializers
  "Wrap serializers with schema validation"
  [serializers schema-id]
  (let [original-value-serializer (:value-serializer serializers)]
    (assoc serializers 
           :value-serializer 
           (fn [topic data]
             (when-not (validate-message data schema-id)
               (throw (ex-info "Schema validation failed"
                               {:schema-id schema-id
                                :data data
                                :explanation (explain-validation data schema-id)})))
             (original-value-serializer topic data)))))

(defn with-schema-deserialization
  "Wrap deserializers with schema validation"
  [deserializers schema-id]
  (let [original-value-deserializer (:value-deserializer deserializers)]
    (assoc deserializers
           :value-deserializer
           (fn [topic data]
             (let [deserialized (original-value-deserializer topic data)]
               (when-not (validate-message deserialized schema-id)
                 (throw (ex-info "Schema validation failed on deserialization"
                                 {:schema-id schema-id
                                  :data deserialized
                                  :explanation (explain-validation deserialized schema-id)})))
               deserialized)))))

(defn consume-schema-messages!
  "Consume messages with schema validation."
  ([group-id topics schema-id]
   (consume-schema-messages! group-id topics schema-id {}))
  ([group-id topics schema-id opts]
   (let [messages (c/consume-messages! group-id topics opts)]
     (map (fn [message]
            (let [value (:value message)]
              (when-not (validate-message value schema-id)
                (throw (ex-info "Message validation failed"
                               {:schema-id schema-id
                                :message message
                                :explanation (explain-validation value schema-id)})))
              message))
          messages))))

(defn send-schema-message!
  "Send a message validated against a schema."
  ([topic value schema-id]
   (send-schema-message! topic nil value schema-id {}))
  ([topic key value schema-id]
   (send-schema-message! topic key value schema-id {}))
  ([topic key value schema-id producer-opts]
   (when-not (validate-message value schema-id)
     (throw (ex-info "Message validation failed"
                     {:schema-id schema-id
                      :value value
                      :explanation (explain-validation value schema-id)})))
   (c/send-message! topic key value producer-opts)))