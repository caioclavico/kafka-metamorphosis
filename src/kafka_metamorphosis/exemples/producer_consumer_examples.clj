(ns kafka-metamorphosis.exemples.producer-consumer-examples
  "Example usage of Kafka Metamorphosis"
  (:require [kafka-metamorphosis.producer :as producer]
            [kafka-metamorphosis.consumer :as consumer]
            [kafka-metamorphosis.admin :as admin]))

(defn setup-example-topic
  "Create the topic used in examples if it doesn't exist"
  []
  (let [admin-client (admin/create-admin-client {:bootstrap-servers "localhost:9092"})]
    (try
      (admin/create-topic-if-not-exists! admin-client "metamorphosis-topic" 
                                         {:partitions 3 :replication-factor 1})
      (finally
        (admin/close! admin-client)))))

(defn simple-producer-example
  "Example of creating a producer and sending messages"
  []
  (println "🪲 Setting up topic...")
  (setup-example-topic)
  
  (let [config {:bootstrap-servers "localhost:9092"
                :key-serializer "org.apache.kafka.common.serialization.StringSerializer"
                :value-serializer "org.apache.kafka.common.serialization.StringSerializer"
                :acks "all"
                :retries 3}
        p (producer/create config)]
    
    (println "🪲 Sending messages to Kafka...")
    
    ;; Send some messages
    (doseq [i (range 5)]
      (let [key (str "key-" i)
            value (str "Hello Kafka! Message " i)]
        (producer/send! p "metamorphosis-topic" key value)
        (println "Sent:" key "->" value)))
    
    ;; Send an async message with callback
    (producer/send-async! p "metamorphosis-topic" "async-key" "Async message!"
                          (fn [metadata exception]
                            (if exception
                              (println "❌ Error sending async message:" (.getMessage exception))
                              (println "✅ Async message sent to partition:" (.partition metadata)))))
    
    ;; Flush and close
    (producer/flush! p)
    (producer/close! p)
    (println "Producer closed.")))

(defn simple-consumer-example
  "Example of creating a consumer and reading messages"
  []
  (let [config {:bootstrap-servers "localhost:9092"
                :group-id "metamorphosis-group"
                :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                :auto-offset-reset "earliest"
                :enable-auto-commit true}
        c (consumer/create config)]
    
    (println "🪲 Starting consumer...")
    
    ;; Subscribe to topic
    (consumer/subscribe! c ["metamorphosis-topic"])
    
    ;; Poll for messages a few times
    (dotimes [_ 3]
      (let [records (consumer/poll! c 2000)]
        (if (empty? records)
          (println "No messages received this poll...")
          (doseq [record records]
            (println "Received:" (:key record) "->" (:value record)
                     "from partition" (:partition record)
                     "at offset" (:offset record))))))
    
    (consumer/close! c)
    (println "Consumer closed.")))

(defn continuous-consumer-example
  "Example of continuous consumption with a handler"
  []
  (let [config {:bootstrap-servers "localhost:9092"
                :group-id "metamorphosis-continuous-group"
                :key-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                :value-deserializer "org.apache.kafka.common.serialization.StringDeserializer"
                :auto-offset-reset "earliest"}
        c (consumer/create config)
        message-count (atom 0)]
    
    (println "🪲 Starting continuous consumer (will stop after 10 messages)...")
    
    (consumer/subscribe! c ["metamorphosis-topic"])
    
    ;; Consume continuously until we get 10 messages
    (consumer/consume! c 
                       {:poll-timeout 1000
                        :handler (fn [record]
                                  (swap! message-count inc)
                                  (println "Message" @message-count ":"
                                          (:key record) "->" (:value record)))
                        :stop-fn #(>= @message-count 10)})
    
    (consumer/close! c)
    (println "Continuous consumer stopped after" @message-count "messages.")))

(defn run-examples
  "Run all examples"
  []
  (println "🦋 Kafka Metamorphosis Examples")
  (println "================================")
  (println)
  
  (println "1. Simple Producer Example:")
  (simple-producer-example)
  (println)
  
  (Thread/sleep 2000) ; Give Kafka time to process
  
  (println "2. Simple Consumer Example:")
  (simple-consumer-example)
  (println)
  
  (println "3. Continuous Consumer Example:")
  (continuous-consumer-example)
  (println)
  
  (println "🎉 All examples completed!"))

(comment
  ;; To run the examples (requires running Kafka):
  ;; 1. Start Kafka locally on localhost:9092
  ;; 2. Create topic: kafka-topics --create --topic metamorphosis-topic --bootstrap-server localhost:9092
  ;; 3. Run: (run-examples)
  )