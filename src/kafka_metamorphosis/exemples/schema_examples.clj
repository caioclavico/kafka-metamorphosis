(ns kafka-metamorphosis.exemples.schema-examples
  "Examples of schema definitions and usage with Kafka Metamorphosis"
  (:require [kafka-metamorphosis.schema :as schema]))

;; ============================================================================
;; 1. SCHEMAS BÁSICOS
;; ============================================================================

(defn basic-schemas-example
  "Examples of basic schema definitions"
  []
  (println "🪲 Basic Schema Examples")
  (println "========================")

  ;; Schema básico para usuário
  (schema/defschema :user-schema
    {:user-id int?
     :name string?
     :email string?
     :age int?
     :active boolean?})

  (println "\n✅ User Schema created")
  
  ;; Testando validação
  (let [valid-user {:user-id 123 
                   :name "João Silva" 
                   :email "joao@example.com"
                   :age 30
                   :active true}
        invalid-user {:user-id "not-a-number"  ; erro: deveria ser int
                     :name "Maria"
                     :email "maria@example.com"}]
    
    (println "Valid user validation:" (schema/validate-message valid-user :user-schema))
    (println "Invalid user validation:" (schema/validate-message invalid-user :user-schema))
    
    ;; Explicação detalhada do erro
    (let [explanation (schema/explain-validation invalid-user :user-schema)]
      (println "\nValidation errors:")
      (doseq [error (:errors explanation)]
        (println "  -" (:field error) ":" (:error error))))))

;; ============================================================================
;; 2. SCHEMAS COM PREDICADOS CUSTOMIZADOS
;; ============================================================================

;; Predicados customizados
(defn email? [s]
  (and (string? s) 
       (re-matches #".+@.+\..+" s)))

(defn positive-number? [n]
  (and (number? n) (> n 0)))

(defn valid-cpf? [cpf]
  (and (string? cpf)
       (re-matches #"\d{3}\.\d{3}\.\d{3}-\d{2}" cpf)))

(defn custom-predicates-example
  "Examples using custom predicates"
  []
  (println "\n🪲 Custom Predicates Examples")
  (println "==============================")

  ;; Schema com predicados customizados
  (schema/defschema :customer-schema
    {:customer-id int?
     :name string?
     :email email?  ; predicado customizado
     :cpf valid-cpf?  ; predicado customizado
     :balance positive-number?  ; predicado customizado
     :status (schema/one-of :active :inactive :suspended)
     :tags (schema/min-count 1)
     :preferences (schema/map-of keyword? boolean?)})

  (println "✅ Customer Schema with custom predicates created")
  
  ;; Testando validação
  (let [valid-customer {:customer-id 456
                       :name "Ana Costa"
                       :email "ana@empresa.com"
                       :cpf "123.456.789-01"
                       :balance 1500.50
                       :status :active
                       :tags ["premium" "loyal"]
                       :preferences {:notifications true :marketing false}}]
    
    (println "Customer validation:" (schema/validate-message valid-customer :customer-schema))))

;; ============================================================================
;; 3. SCHEMAS COM ESTRUTURAS ANINHADAS
;; ============================================================================

(defn nested-structures-example
  "Examples with nested map structures"
  []
  (println "\n🪲 Nested Structures Examples")
  (println "==============================")

  ;; Schema com estrutura aninhada complexa
  (schema/defschema :order-schema
    {:order-id string?
     :customer {:personal {:id int?
                          :name string?
                          :email string?
                          :phone string?}
                :address {:street string?
                         :number string?
                         :city string?
                         :state string?
                         :zip-code string?
                         :country string?}}
     :payment {:method (schema/one-of :credit-card :debit-card :pix :boleto)
              :amount double?
              :installments int?
              :status (schema/one-of :pending :approved :rejected)}
     :shipping {:method (schema/one-of :standard :express :same-day)
               :cost double?
               :estimated-days int?}
     :total double?
     :status (schema/one-of :draft :confirmed :shipped :delivered :cancelled)
     :created-at string?})

  (println "✅ Order Schema with nested structures created")
  
  ;; Testando validação
  (let [valid-order {:order-id "ORD-2024-001"
                    :customer {:personal {:id 123
                                         :name "Carlos Silva"
                                         :email "carlos@example.com"
                                         :phone "+5511999999999"}
                              :address {:street "Rua das Flores"
                                       :number "123"
                                       :city "São Paulo"
                                       :state "SP"
                                       :zip-code "01234-567"
                                       :country "Brasil"}}
                    :payment {:method :credit-card
                             :amount 299.99
                             :installments 3
                             :status :approved}
                    :shipping {:method :express
                              :cost 25.00
                              :estimated-days 2}
                    :total 324.99
                    :status :confirmed
                    :created-at "2024-09-01T10:30:00Z"}]
    
    (println "Order validation:" (schema/validate-message valid-order :order-schema))))

;; ============================================================================
;; 4. SCHEMAS COM COLEÇÕES
;; ============================================================================

(defn collections-example
  "Examples with collections and arrays"
  []
  (println "\n🪲 Collections Examples")
  (println "=======================")

  ;; Schema com coleções de objetos
  (schema/defschema :shopping-cart-schema
    {:cart-id string?
     :user-id int?
     :items [{:product-id int?
              :sku string?
              :name string?
              :price double?
              :quantity int?
              :category string?
              :metadata (schema/map-of keyword? string?)}]
     :totals {:subtotal double?
             :discount double?
             :shipping double?
             :taxes double?
             :total double?}
     :coupons [string?]  ; array de strings simples
     :currency (schema/one-of :BRL :USD :EUR)
     :updated-at string?})

  (println "✅ Shopping Cart Schema with collections created")
  
  ;; Testando validação
  (let [valid-cart {:cart-id "cart-xyz-789"
                   :user-id 456
                   :items [{:product-id 1 
                           :sku "BOOK-CLJ-001"
                           :name "Clojure Programming" 
                           :price 89.90 
                           :quantity 2
                           :category "Books"
                           :metadata {:author "Stuart Halloway"
                                     :publisher "O'Reilly"
                                     :pages "624"}}
                          {:product-id 2 
                           :sku "PEN-BLU-001"
                           :name "Blue Pen" 
                           :price 5.50 
                           :quantity 3
                           :category "Office"
                           :metadata {:color "blue"
                                     :brand "BIC"}}]
                   :totals {:subtotal 196.30
                           :discount 10.00
                           :shipping 15.00
                           :taxes 20.13
                           :total 221.43}
                   :coupons ["WELCOME10" "FREESHIP"]
                   :currency :BRL
                   :updated-at "2024-09-01T14:45:00Z"}]
    
    (println "Shopping cart validation:" (schema/validate-message valid-cart :shopping-cart-schema))))

;; ============================================================================
;; 5. SCHEMAS PARA EVENTOS E LOGS
;; ============================================================================

(defn events-example
  "Examples for event and logging schemas"
  []
  (println "\n🪲 Events and Logs Examples")
  (println "============================")

  ;; Schema para eventos de sistema
  (schema/defschema :system-event-schema
    {:event-id string?
     :event-type keyword?
     :timestamp string?
     :source string?
     :severity (schema/one-of :debug :info :warn :error :fatal)
     :user-id int?
     :session-id string?
     :action {:type string?
             :resource string?
             :method string?
             :params (schema/map-of keyword? any?)}
     :context {:ip-address string?
              :user-agent string?
              :request-id string?
              :trace-id string?}
     :metrics {:duration-ms int?
              :memory-used-mb double?
              :cpu-usage-percent double?}
     :tags (schema/min-count 1)
     :metadata (schema/map-of string? any?)})

  (println "✅ System Event Schema created")
  
  ;; Schema para audit logs
  (schema/defschema :audit-log-schema
    {:log-id string?
     :timestamp string?
     :operation (schema/one-of :create :read :update :delete)
     :resource {:type string?
               :id string?
               :name string?}
     :actor {:user-id int?
            :username string?
            :role (schema/one-of :admin :user :system :api)}
     :changes [{:field string?
               :old-value any?
               :new-value any?}]
     :result (schema/one-of :success :failure :partial)
     :ip-address string?
     :compliance-flags (schema/map-of keyword? boolean?)})

  (println "✅ Audit Log Schema created")
  
  ;; Testando evento
  (let [system-event {:event-id "evt-2024-12345"
                     :event-type :user-login
                     :timestamp "2024-09-01T15:30:00Z"
                     :source "web-application"
                     :severity :info
                     :user-id 789
                     :session-id "sess-abc123def456"
                     :action {:type "authentication"
                             :resource "user-session"
                             :method "password"
                             :params {:remember-me true
                                     :two-factor false}}
                     :context {:ip-address "192.168.1.100"
                              :user-agent "Mozilla/5.0 Chrome/91.0"
                              :request-id "req-xyz789"
                              :trace-id "trace-abc123"}
                     :metrics {:duration-ms 250
                              :memory-used-mb 45.2
                              :cpu-usage-percent 5.8}
                     :tags ["authentication" "security" "user-activity"]
                     :metadata {"feature-flags" ["new-login" "enhanced-security"]
                               "experiment-group" "A"
                               "client-version" "2.1.0"}}]
    
    (println "System event validation:" (schema/validate-message system-event :system-event-schema))))

;; ============================================================================
;; 6. SCHEMAS PARA E-COMMERCE COMPLEXO
;; ============================================================================

(defn ecommerce-example
  "Complex e-commerce schema example"
  []
  (println "\n🪲 E-commerce Complex Example")
  (println "==============================")

  ;; Schema muito complexo para transação de e-commerce
  (schema/defschema :transaction-schema
    {:transaction-id string?
     :type (schema/one-of :sale :refund :exchange :cancellation)
     :customer {:id int?
               :segment (schema/one-of :bronze :silver :gold :platinum)
               :profile {:personal {:name string?
                                   :email string?
                                   :phone string?
                                   :birth-date string?}
                        :preferences {:language (schema/one-of :pt :en :es)
                                     :currency (schema/one-of :BRL :USD :EUR)
                                     :notifications {:email boolean?
                                                   :sms boolean?
                                                   :push boolean?}}
                        :addresses [{:type (schema/one-of :billing :shipping :both)
                                    :label string?
                                    :street string?
                                    :number string?
                                    :complement string?
                                    :neighborhood string?
                                    :city string?
                                    :state string?
                                    :zip-code string?
                                    :country string?
                                    :default boolean?}]}}
     :products [{:id int?
                :sku string?
                :name string?
                :brand string?
                :category {:main string?
                          :subcategory string?
                          :tags (schema/min-count 1)}
                :pricing {:list-price double?
                         :sale-price double?
                         :cost double?
                         :margin-percent double?}
                :inventory {:stock-quantity int?
                           :reserved-quantity int?
                           :warehouse-location string?}
                :attributes (schema/map-of keyword? string?)
                :quantities {:ordered int?
                            :shipped int?
                            :delivered int?
                            :returned int?}}]
     :financial {:subtotal double?
                :discounts [{:type (schema/one-of :coupon :promotion :loyalty)
                            :code string?
                            :amount double?
                            :percent double?}]
                :shipping {:method string?
                          :carrier string?
                          :cost double?
                          :insurance double?}
                :taxes [{:type string?
                        :rate double?
                        :amount double?}]
                :total double?
                :currency string?}
     :payment {:methods [{:type (schema/one-of :credit-card :debit-card :pix :boleto :wallet)
                         :provider string?
                         :amount double?
                         :installments int?
                         :status (schema/one-of :pending :authorized :captured :failed :refunded)}]
              :fraud-analysis {:score double?
                              :status (schema/one-of :approved :rejected :manual-review)
                              :factors (schema/map-of keyword? any?)}}
     :fulfillment {:status (schema/one-of :pending :processing :shipped :delivered :cancelled)
                  :tracking [{:carrier string?
                             :tracking-number string?
                             :estimated-delivery string?
                             :events [{:timestamp string?
                                      :status string?
                                      :location string?
                                      :description string?}]}]}
     :audit {:created-at string?
            :created-by string?
            :updated-at string?
            :updated-by string?
            :version int?
            :changelog [{:timestamp string?
                        :user string?
                        :action string?
                        :details (schema/map-of keyword? any?)}]}})

  (println "✅ Complex Transaction Schema created (very detailed!)"))

;; ============================================================================
;; FUNÇÕES DE EXEMPLO PRINCIPAL
;; ============================================================================

(defn run-all-examples
  "Run all schema examples"
  []
  (println "🪲 Kafka Metamorphosis - Schema Examples")
  (println "=========================================")
  
  (basic-schemas-example)
  (custom-predicates-example)
  (nested-structures-example)
  (collections-example)
  (events-example)
  (ecommerce-example)
  
  (println "\n✅ All schema examples completed!")
  (println "\nRegistered schemas:")
  (doseq [schema-id (schema/list-schemas)]
    (println "  -" schema-id)))

(defn validation-examples
  "Examples of validation and error explanation"
  []
  (println "\n🪲 Validation Examples")
  (println "======================")

  ;; Criar um schema para demonstração
  (schema/defschema :demo-schema
    {:id int?
     :name string?
     :email string?
     :profile {:age int?
              :active boolean?}
     :tags (schema/min-count 1)})

  ;; Exemplo de mensagem válida
  (let [valid-msg {:id 123
                  :name "Test User"
                  :email "test@example.com"
                  :profile {:age 25 :active true}
                  :tags ["user" "active"]}]
    
    (println "\n✅ Valid message:")
    (println "Input:" valid-msg)
    (println "Valid?" (schema/validate-message valid-msg :demo-schema)))

  ;; Exemplo de mensagem inválida com explicação
  (let [invalid-msg {:id "not-a-number"  ; erro
                    :name "Test User"
                    ; :email missing     ; erro
                    :profile {:age "not-a-number"  ; erro
                             :active "not-boolean"} ; erro
                    :tags []}]  ; erro - min-count 1
    
    (println "\n❌ Invalid message:")
    (println "Input:" invalid-msg)
    (println "Valid?" (schema/validate-message invalid-msg :demo-schema))
    
    (println "\nDetailed explanation:")
    (let [explanation (schema/explain-validation invalid-msg :demo-schema)]
      (doseq [error (:errors explanation)]
        (println "  •" (:field error) "-" (:error error))))))

(comment
  ;; Para executar os exemplos:
  
  ;; Executar todos os exemplos
  (run-all-examples)
  
  ;; Executar apenas exemplos básicos
  (basic-schemas-example)
  
  ;; Executar exemplos de validação com erros
  (validation-examples)
  
  ;; Ver schemas registrados
  (schema/list-schemas)
  
  ;; Testar um schema específico
  (schema/validate-message 
    {:user-id 123 :name "Test" :email "test@example.com" :age 30 :active true}
    :user-schema))
