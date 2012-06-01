(defproject histogram "1.9.5"
  :description "Dynamic/streaming histograms"
  :source-path "src/clj"
  :java-source-path "src/java"
  :run-aliases {:examples histogram.test.examples}
  :dev-dependencies [[incanter/incanter-core "1.3.0"]
                     [incanter/incanter-charts "1.3.0"]]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [com.googlecode.json-simple/json-simple "1.1"]])
