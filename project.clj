
(defproject bigml/histogram "4.1.1"
  :description "Streaming histograms for Clojure/Java"
  :min-lein-version "2.0.0"
  :url "https://github.com/bigmlcom/histogram"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :aliases {"lint" ["do" "check," "eastwood"]
            "distcheck" ["do" "clean," "lint," "test"]}
  :profiles {:dev {:plugins [[jonase/eastwood "0.1.4"]]
                   :dependencies [[incanter/incanter-core "1.9.0"]
                                  [incanter/incanter-charts "1.9.0"]]}}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.googlecode.json-simple/json-simple "1.1.1"]])
