(ns kafka-metamorphosis.schema-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [kafka-metamorphosis.schema :as schema]))

(deftest test-schema-registry-basic
  (testing "Schema registry basic functionality"
    ;; Test get-schema with non-existent schema
    (is (nil? (schema/get-schema :non-existent-schema-12345)))
    
    ;; Test list-schemas returns a collection
    (let [schemas (schema/list-schemas)]
      (is (coll? schemas)))))

(deftest test-defschema-basic
  (testing "Schema definition and registration basic functionality"
    ;; Test basic schema definition
    (let [schema-id (schema/defschema :user-schema-test-basic {:user-id int? :name string?})]
      (is (= :user-schema-test-basic schema-id))
      (is (contains? (set (schema/list-schemas)) :user-schema-test-basic)))
    
    ;; Test getting the created schema
    (let [retrieved-schema (schema/get-schema :user-schema-test-basic)]
      (is (not (nil? retrieved-schema)))
      (is (contains? retrieved-schema :spec))
      (is (map? (:spec retrieved-schema))))))

(deftest test-error-cases
  (testing "Error handling in schema functions"
    ;; Test validate-message with non-existent schema
    (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                          #"Schema not found"
                          (schema/validate-message {} :non-existent-schema-validate-test)))
    
    ;; Test explain-validation with non-existent schema
    (is (thrown-with-msg? clojure.lang.ExceptionInfo 
                          #"Schema not found"
                          (schema/explain-validation {} :non-existent-schema-explain-test)))))

(deftest test-validate-message-with-existing-schema
  (testing "Message validation with existing schemas"
    ;; Create a test schema
    (schema/defschema :user-validation-test {:user-id int? :name string? :email string?})
    
    ;; Test successful validation
    (let [valid-message {:user-id 123 :name "John Doe" :email "john@example.com"}]
      (is (true? (schema/validate-message valid-message :user-validation-test))))
    
    ;; Test validation failure - missing required field
    (let [invalid-message-missing {:user-id 123 :name "John Doe"}]
      (is (false? (schema/validate-message invalid-message-missing :user-validation-test))))
    
    ;; Test validation failure - wrong type
    (let [invalid-message-type {:user-id "not-a-number" :name "John Doe" :email "john@example.com"}]
      (is (false? (schema/validate-message invalid-message-type :user-validation-test))))
    
    ;; Test validation with extra fields (should pass - we only validate required fields)
    (let [message-extra-fields {:user-id 123 :name "John Doe" :email "john@example.com" :extra-field "should-not-be-here"}]
      (is (true? (schema/validate-message message-extra-fields :user-validation-test))))))

(deftest test-explain-validation-with-existing-schema
  (testing "Validation explanation with existing schemas"
    ;; Create a test schema
    (schema/defschema :user-explain-test {:user-id int? :name string? :email string?})
    
    ;; Test explanation for valid message
    (let [valid-message {:user-id 123 :name "John Doe" :email "john@example.com"}
          explanation (schema/explain-validation valid-message :user-explain-test)]
      (is (true? (:valid? explanation)))
      (is (empty? (:errors explanation))))
    
    ;; Test explanation for invalid message
    (let [invalid-message {:user-id "not-a-number" :name "John Doe"}
          explanation (schema/explain-validation invalid-message :user-explain-test)]
      (is (false? (:valid? explanation)))
      (is (seq (:errors explanation)))
      (is (some #(= (:field %) "email") (:errors explanation))) ; missing email
      (is (some #(= (:field %) "user-id") (:errors explanation)))))) ; wrong type

(deftest test-complex-schema-validation
  (testing "Validation with more complex schema structures"
    ;; Create a schema with nested structure
    (schema/defschema :product-validation-test 
                     {:product-id int?
                      :name string?
                      :price double?
                      :categories [string?]
                      :in-stock boolean?})
    
    ;; Test successful validation
    (let [valid-product {:product-id 789
                        :name "Laptop"
                        :price 999.99
                        :categories ["Electronics" "Computers"]
                        :in-stock true}]
      (is (true? (schema/validate-message valid-product :product-validation-test))))
    
    ;; Test validation failure with multiple errors
    (let [invalid-product {:product-id 789
                          :name "Laptop"
                          :price "not-a-number"
                          :categories "not-a-collection"
                          :in-stock "not-a-boolean"}]
      (is (false? (schema/validate-message invalid-product :product-validation-test))))))

(deftest test-nested-map-validation
  (testing "Validation with nested maps"
    ;; Create a schema with nested map
    (schema/defschema :order-nested-test
                     {:order-id int?
                      :user {:user-id int? :name string? :email string?}
                      :total double?})
    
    ;; Test successful validation with nested map
    (let [valid-order {:order-id 123
                      :user {:user-id 456 :name "John Doe" :email "john@example.com"}
                      :total 99.99}]
      (is (true? (schema/validate-message valid-order :order-nested-test))))
    
    ;; Test validation failure in nested map
    (let [invalid-order {:order-id 123
                        :user {:user-id "not-a-number" :name "John Doe"}  ; missing email, wrong type
                        :total 99.99}]
      (is (false? (schema/validate-message invalid-order :order-nested-test))))))

(deftest test-collection-validation
  (testing "Validation with collections of maps"
    ;; Create a schema with collection of maps
    (schema/defschema :cart-collection-test
                     {:cart-id string?
                      :items [{:product-id int? :name string? :quantity int?}]})
    
    ;; Test successful validation with collection
    (let [valid-cart {:cart-id "cart-123"
                     :items [{:product-id 1 :name "Item 1" :quantity 2}
                            {:product-id 2 :name "Item 2" :quantity 1}]}]
      (is (true? (schema/validate-message valid-cart :cart-collection-test))))
    
    ;; Test validation failure in collection item
    (let [invalid-cart {:cart-id "cart-123"
                       :items [{:product-id "not-a-number" :name "Item 1" :quantity 2}
                              {:product-id 2 :name "Item 2"}]}]  ; missing quantity
      (is (false? (schema/validate-message invalid-cart :cart-collection-test))))))

(deftest test-custom-predicates
  (testing "Validation with custom predicates"
    ;; Test one-of predicate
    (schema/defschema :status-test 
                     {:id int?
                      :status (schema/one-of :pending :confirmed :shipped :delivered)})
    
    (let [valid-status {:id 123 :status :pending}]
      (is (true? (schema/validate-message valid-status :status-test))))
    
    (let [invalid-status {:id 123 :status :invalid}]
      (is (false? (schema/validate-message invalid-status :status-test))))
    
    ;; Test min-count predicate
    (schema/defschema :tags-test
                     {:id int?
                      :tags (schema/min-count 1)})
    
    (let [valid-tags {:id 123 :tags ["tag1" "tag2"]}]
      (is (true? (schema/validate-message valid-tags :tags-test))))
    
    (let [invalid-tags {:id 123 :tags []}]
      (is (false? (schema/validate-message invalid-tags :tags-test))))
    
    ;; Test map-of predicate
    (schema/defschema :metadata-test
                     {:id int?
                      :metadata (schema/map-of keyword? string?)})
    
    (let [valid-metadata {:id 123 :metadata {:key1 "value1" :key2 "value2"}}]
      (is (true? (schema/validate-message valid-metadata :metadata-test))))
    
    (let [invalid-metadata {:id 123 :metadata {:key1 123 :key2 "value2"}}]  ; wrong value type
      (is (false? (schema/validate-message invalid-metadata :metadata-test))))))

(deftest test-explain-complex-validation
  (testing "Detailed explanation for complex validation failures"
    (schema/defschema :complex-explain-test
                     {:user {:id int? :name string?}
                      :items [{:id int? :name string?}]
                      :metadata (schema/map-of keyword? string?)})
    
    (let [invalid-message {:user {:id "not-a-number" :name "John"}  ; wrong type
                          :items [{:id 1 :name "Item 1"}
                                 {:id "not-a-number" :name "Item 2"}]  ; wrong type in collection
                          :metadata {:key1 "value1" :key2 123}}  ; wrong value type in map
          explanation (schema/explain-validation invalid-message :complex-explain-test)]
      
      (is (false? (:valid? explanation)))
      (is (seq (:errors explanation)))
      
      ;; Check that errors contain proper field paths - using the actual format from implementation
      (let [error-fields (set (map :field (:errors explanation)))]
        (is (contains? error-fields "user.id"))
        (is (some #(str/starts-with? % "items[") error-fields))
        (is (contains? error-fields "metadata"))))))