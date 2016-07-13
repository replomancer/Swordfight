(defproject swordfight "0.2.0-SNAPSHOT"
  :description "Chess engine"
  :url "http://example.com/FIXME"
  :main swordfight.core
  :license {:name "2-clause BSD"
            :url ""}
  :dependencies [[org.clojure/clojure "1.9.0-alpha9"]]
  :profiles {:dev {:dependencies [[midje "1.9.0-alpha3"]]
                   :plugins [[lein-midje "3.2"]]}})
