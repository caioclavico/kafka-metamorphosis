(ns kafka-metamorphosis.core-test
  (:require [clojure.test :refer :all]
            [kafka-metamorphosis.core :refer :all]
            [kafka-metamorphosis.producer :as producer]
            [kafka-metamorphosis.consumer :as consumer]))

(deftest test-core-exports
  (testing "Core namespace exports main functions"
    (is (= producer/create create-producer))
    (is (= consumer/create create-consumer))))

(deftest test-main-function
  (testing "Main function runs without error"
    (is (nil? (-main)))))
