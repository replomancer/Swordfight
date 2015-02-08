(ns swordfight.core-test
  (:use clojure.test
        [swordfight.core :only [initial-settings]]))


(deftest settings-test
  (testing "Oooh, a test"
    (is (false? (:xboard-mode initial-settings)))))
