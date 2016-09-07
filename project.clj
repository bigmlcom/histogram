
(defproject bigml/histogram "4.1.3"
  :description "Streaming histograms for Clojure/Java"
  :min-lein-version "2.0.0"
  :url "https://github.com/bigmlcom/histogram"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-source" "1.6" "-target" "1.6"]
  :aliases {"lint" ["do" "check," "eastwood"]
            "distcheck" ["do" "clean," "lint," "test"]}
  :profiles {:dev {:plugins [[jonase/eastwood "0.2.3"]]
                   :dependencies [[incanter/incanter-core "1.9.1"]
                                  [incanter/incanter-charts "1.9.1"]]}}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.googlecode.json-simple/json-simple "1.1.1"]])
