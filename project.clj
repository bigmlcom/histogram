
(defproject bigml/histogram "3.2.1"
  :description "Streaming histograms for Clojure/Java"
  :min-lein-version "2.0.0"
  :url "https://github.com/bigmlcom/histogram"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :profiles {:dev {:dependencies [[incanter/incanter-core "1.4.1"]
                                  [incanter/incanter-charts "1.4.1"]]}}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.googlecode.json-simple/json-simple "1.1"]])
