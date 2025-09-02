# Schema Validation - Kafka Metamorphosis 🦋

## Overview

Kafka Metamorphosis provides a powerful and intuitive schema validation system that allows you to define, register, and validate message structures without the complexity of clojure.spec. Our custom implementation offers flexibility, performance, and excellent error reporting.

## Table of Contents

- [Quick Start](#quick-start)
- [Schema Definition](#schema-definition)
- [Validation](#validation)
- [Built-in Predicates](#built-in-predicates)
- [Custom Predicates](#custom-predicates)
- [Complex Structures](#complex-structures)
- [Collections](#collections)
- [Error Handling](#error-handling)
- [Integration with Kafka](#integration-with-kafka)
- [Examples](#examples)
- [Best Practices](#best-practices)

## Quick Start

```clojure
(require '[kafka-metamorphosis.schema :as schema])

;; Define a simple schema
(schema/defschema :user-schema
  {:user-id int?
   :name string?
   :email string?
   :active boolean?})

;; Validate a message
(schema/validate-message 
  {:user-id 123 :name "John Doe" :email "john@example.com" :active true}
  :user-schema) ; => true

;; Get detailed validation errors
(schema/explain-validation 
  {:user-id "invalid" :name "John"}  ; missing email, wrong type
  :user-schema)
; => {:valid? false :errors [...]}
```

## Schema Definition

### Basic Syntax

```clojure
(schema/defschema :schema-name
  {:field-name predicate-function
   :another-field another-predicate})
```

### Simple Schema Example

```clojure
(schema/defschema :product-schema
  {:product-id int?
   :name string?
   :price double?
   :in-stock boolean?
   :category string?})
```

## Validation

### Validate Messages

```clojure
;; Returns true/false
(schema/validate-message message :schema-name)

;; Get detailed explanation
(schema/explain-validation message :schema-name)
```

### Validation Results

```clojure
;; Success
(schema/validate-message {:user-id 123 :name "John"} :user-schema)
; => true

;; Failure
(schema/validate-message {:user-id "invalid"} :user-schema)
; => false

;; Detailed explanation
(schema/explain-validation {:user-id "invalid"} :user-schema)
; => {:valid? false
;     :errors [{:field "user-id" 
;               :error "Field validation failed" 
;               :value "invalid" 
;               :expected #function[clojure.core/int?]}]
;     :message {...}
;     :schema-id :user-schema}
```

## Built-in Predicates

### Core Clojure Predicates

```clojure
int?        ; Integer numbers
double?     ; Double numbers
string?     ; Strings
boolean?    ; Boolean values
keyword?    ; Keywords
map?        ; Maps
vector?     ; Vectors
seq?        ; Sequences
```

### Schema-specific Predicates

```clojure
;; Accept one of specific values
(schema/one-of :active :inactive :suspended)

;; Minimum collection size
(schema/min-count 1)   ; At least 1 item
(schema/min-count 5)   ; At least 5 items

;; Maximum collection size
(schema/max-count 10)  ; At most 10 items

;; Map with specific key-value types
(schema/map-of keyword? string?)  ; keyword keys, string values
(schema/map-of string? any?)      ; string keys, any values
```

## Custom Predicates

You can create custom validation functions:

```clojure
;; Email validation
(defn email? [s]
  (and (string? s) 
       (re-matches #".+@.+\..+" s)))

;; Positive number validation
(defn positive? [n]
  (and (number? n) (> n 0)))

;; CPF validation (Brazilian tax ID)
(defn valid-cpf? [cpf]
  (and (string? cpf)
       (re-matches #"\d{3}\.\d{3}\.\d{3}-\d{2}" cpf)))

;; Use in schema
(schema/defschema :customer-schema
  {:customer-id int?
   :email email?           ; custom predicate
   :balance positive?      ; custom predicate
   :cpf valid-cpf?         ; custom predicate
   :status (schema/one-of :active :inactive)})
```

## Complex Structures

### Nested Maps

```clojure
(schema/defschema :order-schema
  {:order-id string?
   :customer {:id int?
              :name string?
              :email string?
              :address {:street string?
                       :city string?
                       :zip-code string?
                       :country string?}}
   :total double?
   :status (schema/one-of :pending :confirmed :shipped)})

;; Validation
(schema/validate-message 
  {:order-id "ORD-001"
   :customer {:id 123
              :name "John Doe"
              :email "john@example.com"
              :address {:street "123 Main St"
                       :city "New York"
                       :zip-code "10001"
                       :country "USA"}}
   :total 99.99
   :status :pending}
  :order-schema) ; => true
```

### Error Paths for Nested Structures

When validation fails in nested structures, you get precise error paths:

```clojure
(schema/explain-validation 
  {:order-id "ORD-001"
   :customer {:id "invalid"        ; error here
              :name "John"
              :address {:street "123 Main St"
                       :city "New York"
                       ; zip-code missing  ; error here
                       :country "USA"}}
   :total 99.99}
  :order-schema)

; Errors will show:
; - "customer.id" - Field validation failed
; - "customer.address.zip-code" - Field is missing
```

## Collections

### Arrays of Objects

```clojure
(schema/defschema :cart-schema
  {:cart-id string?
   :items [{:product-id int?      ; Each item must have these fields
            :name string?
            :price double?
            :quantity int?}]
   :total double?})

;; Validation
(schema/validate-message 
  {:cart-id "cart-123"
   :items [{:product-id 1 :name "Book" :price 29.99 :quantity 2}
           {:product-id 2 :name "Pen" :price 5.99 :quantity 1}]
   :total 65.97}
  :cart-schema) ; => true
```

### Arrays of Simple Types

```clojure
(schema/defschema :user-schema
  {:user-id int?
   :name string?
   :tags [string?]        ; Array of strings
   :scores [double?]      ; Array of doubles
   :active boolean?})

;; Validation
(schema/validate-message 
  {:user-id 123
   :name "Alice"
   :tags ["premium" "loyal" "verified"]
   :scores [95.5 87.2 92.1]
   :active true}
  :user-schema) ; => true
```

### Collection Error Paths

```clojure
(schema/explain-validation 
  {:cart-id "cart-123"
   :items [{:product-id 1 :name "Book" :price 29.99 :quantity 2}
           {:product-id "invalid" :name "Pen" :quantity 1}]}  ; missing price, wrong type
  :cart-schema)

; Errors will show:
; - "items[1].product-id" - Field validation failed  
; - "items[1].price" - Field is missing
```

## Error Handling

### Error Structure

```clojure
{:valid? false
 :errors [{:field "field-name"
           :error "Error description"
           :value actual-value
           :expected predicate-function}]
 :message original-message
 :schema-id :schema-name}
```

### Common Error Types

```clojure
;; Missing field
{:field "email" :error "Field is missing"}

;; Type validation failed
{:field "user-id" :error "Field validation failed" :value "123" :expected int?}

;; Nested field error
{:field "customer.address.zip-code" :error "Field is missing"}

;; Collection item error
{:field "items[0].price" :error "Field validation failed" :value "invalid"}
```

## Integration with Kafka

### Producer with Schema Validation

```clojure
(require '[kafka-metamorphosis.core :as km]
         '[kafka-metamorphosis.schema :as schema])

;; Define schema
(schema/defschema :user-event-schema
  {:event-id string?
   :user-id int?
   :action string?
   :timestamp string?})

;; Send message with validation
(schema/send-schema-message! 
  "user-events"
  {:event-id "evt-123"
   :user-id 456
   :action "login"
   :timestamp "2024-09-01T10:30:00Z"}
  :user-event-schema)
```

### Consumer with Schema Validation

```clojure
;; Consume messages with automatic validation
(schema/consume-schema-messages! 
  "user-group" 
  ["user-events"] 
  :user-event-schema)
```

### Serializer Wrapping

```clojure
;; Wrap existing serializers with schema validation
(def validated-serializers
  (schema/with-schema-serializers 
    km/json-serializers
    :user-event-schema))

;; Use with producer
(km/create-producer (km/producer-config) validated-serializers)
```

## Examples

### E-commerce Order Schema

```clojure
(schema/defschema :ecommerce-order-schema
  {:order-id string?
   :customer {:id int?
              :name string?
              :email email?
              :tier (schema/one-of :bronze :silver :gold :platinum)}
   :items [{:product-id int?
            :sku string?
            :name string?
            :price double?
            :quantity (schema/min-count 1)
            :category string?}]
   :payment {:method (schema/one-of :credit-card :debit-card :pix :boleto)
            :amount double?
            :installments int?
            :status (schema/one-of :pending :approved :rejected)}
   :shipping {:address {:street string?
                       :city string?
                       :zip-code string?
                       :country string?}
             :method (schema/one-of :standard :express :same-day)
             :cost double?}
   :totals {:subtotal double?
           :shipping double?
           :taxes double?
           :discount double?
           :total double?}
   :timestamps {:created-at string?
               :updated-at string?}
   :metadata (schema/map-of keyword? string?)})
```

### Event Logging Schema

```clojure
(schema/defschema :system-event-schema
  {:event-id string?
   :timestamp string?
   :level (schema/one-of :debug :info :warn :error :fatal)
   :source string?
   :message string?
   :context {:request-id string?
            :user-id int?
            :session-id string?
            :ip-address string?}
   :metrics {:duration-ms int?
            :memory-mb double?
            :cpu-percent double?}
   :tags (schema/min-count 1)
   :metadata (schema/map-of string? any?)})
```

### User Profile Schema

```clojure
(schema/defschema :user-profile-schema
  {:user-id int?
   :username string?
   :email email?
   :profile {:first-name string?
            :last-name string?
            :birth-date string?
            :phone string?
            :avatar-url string?}
   :preferences {:language (schema/one-of :en :pt :es :fr)
                :timezone string?
                :notifications {:email boolean?
                               :sms boolean?
                               :push boolean?}
                :privacy {:profile-visible boolean?
                         :email-visible boolean?
                         :activity-tracking boolean?}}
   :addresses [{:type (schema/one-of :home :work :other)
               :street string?
               :city string?
               :state string?
               :zip-code string?
               :country string?
               :default boolean?}]
   :social-links (schema/map-of keyword? string?)
   :created-at string?
   :updated-at string?
   :status (schema/one-of :active :inactive :suspended :deleted)})
```

## Best Practices

### 1. Schema Organization

```clojure
;; Group related schemas
(defn define-user-schemas []
  (schema/defschema :user-profile-schema {...})
  (schema/defschema :user-preferences-schema {...})
  (schema/defschema :user-activity-schema {...}))

;; Call during application startup
(define-user-schemas)
```

### 2. Schema Versioning

```clojure
;; Include version in schema names
(schema/defschema :user-profile-v1-schema {...})
(schema/defschema :user-profile-v2-schema {...})

;; Or use metadata
(schema/defschema :user-profile-schema 
  ^{:version "2.0" :description "User profile with enhanced fields"}
  {...})
```

### 3. Reusable Predicates

```clojure
;; Define common predicates in a separate namespace
(ns myapp.schema.predicates)

(def email? 
  (fn [s] (and (string? s) (re-matches #".+@.+\..+" s))))

(def positive-int? 
  (fn [n] (and (int? n) (> n 0))))

(def uuid-string? 
  (fn [s] (and (string? s) (re-matches #"[0-9a-f-]{36}" s))))
```

### 4. Error Handling

```clojure
;; Always handle validation errors gracefully
(defn safe-process-message [message schema-id]
  (if (schema/validate-message message schema-id)
    (process-message message)
    (let [explanation (schema/explain-validation message schema-id)]
      (log/error "Invalid message" {:errors (:errors explanation)})
      {:error "Invalid message format" :details (:errors explanation)})))
```

### 5. Testing Schemas

```clojure
;; Test both valid and invalid cases
(deftest test-user-schema
  (testing "Valid user message"
    (is (schema/validate-message valid-user-message :user-schema)))
  
  (testing "Invalid user message"
    (is (false? (schema/validate-message invalid-user-message :user-schema)))
    
    (let [explanation (schema/explain-validation invalid-user-message :user-schema)]
      (is (false? (:valid? explanation)))
      (is (not (empty? (:errors explanation)))))))
```

### 6. Performance Considerations

```clojure
;; Pre-compile complex predicates
(def compiled-email-regex #".+@.+\..+")
(defn fast-email? [s] 
  (and (string? s) (re-matches compiled-email-regex s)))

;; Cache validation results for expensive operations
(def validation-cache (atom {}))

(defn cached-validate [message schema-id]
  (let [cache-key [message schema-id]]
    (if-let [cached-result (@validation-cache cache-key)]
      cached-result
      (let [result (schema/validate-message message schema-id)]
        (swap! validation-cache assoc cache-key result)
        result))))
```

## API Reference

### Core Functions

- `(defschema schema-id spec-map)` - Define and register a schema
- `(get-schema schema-id)` - Retrieve a registered schema
- `(list-schemas)` - List all registered schema IDs
- `(validate-message message schema-id)` - Validate a message (returns boolean)
- `(explain-validation message schema-id)` - Get detailed validation explanation

### Built-in Predicates

- `(one-of & values)` - Accept one of the specified values
- `(min-count n)` - Minimum collection size
- `(max-count n)` - Maximum collection size  
- `(map-of key-pred value-pred)` - Map with specific key-value predicates

### Kafka Integration

- `(send-schema-message! topic message schema-id)` - Send validated message
- `(consume-schema-messages! group topics schema-id)` - Consume with validation
- `(with-schema-serializers serializers schema-id)` - Wrap serializers
- `(with-schema-deserialization deserializers schema-id)` - Wrap deserializers

---

**🦋 Happy metamorphosis with validated schemas!**
