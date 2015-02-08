(ns swordfight.core-test
  (:use clojure.test
        [swordfight.core :only [initial-settings]]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(deftest settings-test
  (testing "Oooh, a test"
    (is ((:xboard-mode initial-settings) false))))
