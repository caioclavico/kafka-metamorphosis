(defproject org.clojars.caioclavico/kafka-metamorphosis "0.2.0"
  :description "A comprehensive Clojure wrapper that transforms the Java Kafka APIs into an elegant, idiomatic Clojure interface"
  :url "https://github.com/caioclavico/kafka-metamorphosis"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  
  ;; SCM configuration for Maven
  :scm {:name "git"
        :url "https://github.com/caioclavico/kafka-metamorphosis"
        :connection "scm:git:git://github.com/caioclavico/kafka-metamorphosis.git"
        :developerConnection "scm:git:ssh://git@github.com/caioclavico/kafka-metamorphosis.git"}
  
  ;; Developer information
  :pom-addition [:developers
                 [:developer
                  [:id "caioclavico"]
                  [:name "Caio Clavico"]
                  [:url "https://github.com/caioclavico"]
                  [:email "caio@example.com"]]]
  
  ;; Deploy repositories configuration
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]]
  
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.apache.kafka/kafka-clients "3.7.0"]
                 [org.clojure/data.json "2.4.0"]]
                 
  :main ^:skip-aot kafka-metamorphosis.core
  :target-path "target/%s"
  :aot [kafka-metamorphosis.core]
  
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[midje "1.10.9"]]
                   :plugins [[lein-midje "3.2.1"]]}})
