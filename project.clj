(defproject kafka-metamorphosis "0.1.0-SNAPSHOT"
  :description "A Clojure wrapper that transforms the Java Kafka driver into a friendly, idiomatic Clojure interface"
  :url "https://github.com/your-username/kafka-metamorphosis"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.apache.kafka/kafka-clients "3.7.0"]
                 [org.clojure/data.json "2.4.0"]]
  :main ^:skip-aot kafka-metamorphosis.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
