(defproject swordfight "0.8.0-SNAPSHOT"
  :description "Chess engine"
  :url "http://github.com/evalapply/Swordfight"
  :main swordfight.core
  :license {:name "BSD-2-clause"
            :url "http://opensource.org/licenses/BSD-2-Clause"}
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles {:dev {:source-paths ["dev"]
                   :dependencies [[midje "1.9.0-alpha6"]
                                  [com.taoensso/tufte "1.1.1"]]
                   :plugins [[lein-midje "3.2"] [lein-cljfmt "0.5.6"]]}})
