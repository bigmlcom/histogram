
(defproject bigml/histogram "5.0.0"
  :description "Streaming histograms for Clojure/Java"
  :min-lein-version "2.0.0"
  :url "https://github.com/bigmlcom/histogram"
  :license {:name "Apache License, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :javac-options ["-source" "1.8" "-target" "1.8"]
  :aliases {"lint"  ^{:doc "Run the Eastwood linter on source files"}
            ["do" "check," "eastwood"]
            "update" ^{:doc "Update dependencies in project.clj using ancient"}
            ["ancient" "upgrade" ":no-tests" ":check-clojure" ":all"]
            "distcheck" ^{:doc "Prepare a dist running all tests and lints"}
            ["do" "clean," "lint," "test"]}
  :profiles {:dev {:plugins [[jonase/eastwood "1.4.2"]
                             [lein-ancient "1.0.0-RC3"]]
                   :dependencies [[incanter/incanter-core "1.9.3"]
                                  [incanter/incanter-charts "1.9.3"]]}}
  :dependencies [[org.clojure/clojure "1.11.3"]
                 [org.clojure/core.rrb-vector "0.2.0"]
                 [com.googlecode.json-simple/json-simple "1.1.1"]])
