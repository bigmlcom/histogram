
(defproject bigml/histogram "3.1.0"
  :description "Streaming histograms for Clojure/Java"
  :min-lein-version "2.0.0"
  :url "https://github.com/bigmlcom/histogram"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-target" "1.6" "-source" "1.6"]
  :profiles {:dev {:dependencies [[incanter/incanter-core "1.3.0"]
                                  [incanter/incanter-charts "1.3.0"]]}}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.googlecode.json-simple/json-simple "1.1"]])
