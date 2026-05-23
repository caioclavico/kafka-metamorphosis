(defproject org.clojars.caioclavico/kafka-metamorphosis "0.4.3"
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
                 [org.apache.kafka/kafka-clients "4.3.0"]
                 [org.clojure/data.json "2.4.0"]
                 ;; SLF4J facade — picks up whatever backend the consuming
                 ;; Java/Spring Boot app provides (logback, log4j2, …).
                 [org.clojure/tools.logging "1.3.0"]]

  ;; Java source code for the Java-friendly facade (POJOs / functional interfaces).
  ;; Plain Java types here MUST NOT reference any gen-class output, so javac can run
  ;; before AOT (default Leiningen order: ["javac" "compile"]).
  :java-source-paths ["java"]
  :javac-options ["-target" "11" "-source" "11" "-Xlint:-options"]

  :main ^:skip-aot kafka-metamorphosis.core
  :target-path "target/%s"

  ;; AOT-compile the Clojure namespaces that emit Java-callable classes via gen-class.
  ;; These produce the public Java API classes under
  ;; io.github.caioclavico.kafkametamorphosis.*
  :aot [kafka-metamorphosis.core
        kafka-metamorphosis.java.kafka-producer-wrapper
        kafka-metamorphosis.java.kafka-consumer-wrapper
        kafka-metamorphosis.java.kafka-admin-wrapper]

  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[midje "1.10.9"]]
                   :plugins [[lein-midje "3.2.1"]]}})
