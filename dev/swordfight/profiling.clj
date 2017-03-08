(ns swordfight.profiling
  (:require [taoensso.tufte :refer [add-handler!]]))

(def filename "swordfight-profiling.log")

(add-handler! :spitting-handler "*"
              (fn [m]
                (let [{:keys [stats-str_ ?id ?data]} m
                      stats-str (force stats-str_)]
                  (spit filename
                        (str
                         "\n\n"
                         (java.util.Date.)
                         (when ?id   (str "\nid: "   ?id))
                         (when ?data (str "\ndata: " ?data))
                         "\n" stats-str) :append true))))
