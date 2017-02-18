(defproject swordfight "0.7.0-SNAPSHOT"
  :description "Chess engine"
  :url "http://example.com/FIXME"
  :main swordfight.core
  :license {:name "2-clause BSD"
            :url ""}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]]
  :profiles {:dev {:dependencies [[midje "1.9.0-alpha6"]]
                   :plugins [[lein-midje "3.2"] [lein-cljfmt "0.5.6"]]}})
