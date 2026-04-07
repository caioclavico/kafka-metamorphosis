(ns kafka-metamorphosis.serializers
  "Kafka Serializer and Deserializer configurations
   
   This namespace provides various serializer and deserializer configurations
   for different data formats including String, JSON, Avro, and Protobuf."
  (:require [clojure.data.json :as json]))

;; ============================================================================
;; Basic Serializers/Deserializers
;; ============================================================================

(def string-serializers
  "Common string serializer configuration"
  {:key-serializer "org.apache.kafka.common.serialization.StringSerializer"
   :value-serializer "org.apache.kafka.common.serialization.StringSerializer"})

(def string-deserializers
  "Common string deserializer configuration"
  {:key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
   :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"})

;; ============================================================================
;; JSON Serializers/Deserializers
;; ============================================================================

(def json-serializers
  "Common JSON serializer configuration using Confluent's JSON serializer"
  {:key-serializer "org.apache.kafka.common.serialization.StringSerializer"
   :value-serializer "io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer"})

(def json-deserializers
  "Common JSON deserializer configuration using Confluent's JSON deserializer"
  {:key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
   :value-deserializer "io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer"})

(def simple-json-serializers
  "Simple JSON serializer configuration (no schema registry required)"
  {:key-serializer "org.apache.kafka.common.serialization.StringSerializer"
   :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
   ;; We'll handle JSON conversion in application code
   :json-mode true})

(def simple-json-deserializers
  "Simple JSON deserializer configuration (no schema registry required)"
  {:key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
   :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
   ;; We'll handle JSON parsing in application code
   :json-mode true})

;; ============================================================================
;; Avro Serializers/Deserializers
;; ============================================================================

(defn avro-serializers
  "Avro serializer configuration for use with Schema Registry"
  [schema-registry-url]
  {:key-serializer "org.apache.kafka.common.serialization.StringSerializer"
   :value-serializer "io.confluent.kafka.serializers.KafkaAvroSerializer"
   :schema-registry-url schema-registry-url})

(defn avro-deserializers
  "Avro deserializer configuration for use with Schema Registry"
  [schema-registry-url]
  {:key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
   :value-deserializer "io.confluent.kafka.serializers.KafkaAvroDeserializer"
   :schema-registry-url schema-registry-url
   :specific-avro-reader true})

;; ============================================================================
;; Protobuf Serializers/Deserializers
;; ============================================================================

(defn protobuf-serializers
  "Protobuf serializer configuration for use with Schema Registry"
  [schema-registry-url]
  {:key-serializer "org.apache.kafka.common.serialization.StringSerializer"
   :value-serializer "io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer"
   :schema-registry-url schema-registry-url})

(defn protobuf-deserializers
  "Protobuf deserializer configuration for use with Schema Registry"
  [schema-registry-url]
  {:key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
   :value-deserializer "io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer"
   :schema-registry-url schema-registry-url})

;; ============================================================================
;; JSON Utility Functions
;; ============================================================================

(defn to-json
  "Convert a Clojure data structure to JSON string"
  [data]
  (json/write-str data))

(defn from-json
  "Parse JSON string to Clojure data structure"
  [json-str]
  (when json-str
    (json/read-str json-str :key-fn keyword)))
