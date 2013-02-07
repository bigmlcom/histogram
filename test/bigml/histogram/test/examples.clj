;; Copyright 2013 BigML
;; Licensed under the Apache License, Version 2.0
;; http://www.apache.org/licenses/LICENSE-2.0

(ns bigml.histogram.test.examples
  (:require (bigml.histogram [core :as hst])
            (incanter [core :as core]
                      [charts :as charts]
                      [distributions :as dst])))

;; Mixed samples from four normal distributions
(def mixed-normal-data
  (shuffle (concat (repeatedly 160000 #(dst/draw (dst/normal-distribution 0 0.2)))
                   (repeatedly 80000 #(dst/draw (dst/normal-distribution 1 0.2)))
                   (repeatedly 40000 #(dst/draw (dst/normal-distribution 2 0.2)))
                   (repeatedly 20000 #(dst/draw (dst/normal-distribution 3 0.2))))))

(def normal-data
  (repeatedly 200000 #(dst/draw (dst/normal-distribution 0 1))))

(defn multi-pdf-chart [hists]
  (let [min (reduce min (map (comp :min hst/bounds) hists))
        max (reduce max (map (comp :max hst/bounds) hists))]
    (core/view
     (reduce (fn [c h]
               (charts/add-function c (hst/pdf h) min max))
             (charts/function-plot (hst/pdf (first hists)) min max)
             (next hists)))))

(defn sum-density-chart [hist]
  (let [{:keys [min max]} (hst/bounds hist)]
    (core/view (-> (charts/function-plot #(hst/sum hist %) min max)
                   (charts/add-function #(hst/density hist %) min max)))))

(defn cdf-pdf-chart [hist]
  (let [{:keys [min max]} (hst/bounds hist)]
    (core/view (-> (charts/function-plot (hst/cdf hist) min max)
                   (charts/add-function (hst/pdf hist) min max)))))

(defn pdf-target-chart [hist]
  (let [{:keys [min max]} (hst/bounds hist)]
    (core/view
     (-> (charts/function-plot (hst/pdf hist) min max)
         (charts/add-function #(:sum (hst/average-target hist %)) min max)))))

;; Builds and charts a histogram for the normal distribution.
(defn- normal-example []
  (let [hist (reduce hst/insert! (hst/create :bins 32) normal-data)]
    (println "Total sum of points less than 0:" (hst/sum hist 0))
    (println "Quartile splits:" (hst/uniform hist 4))
    (cdf-pdf-chart hist)))

(defn- varying-bins-example []
  (multi-pdf-chart
   [(reduce hst/insert! (hst/create :bins 8) mixed-normal-data)
    (reduce hst/insert! (hst/create :bins 64) mixed-normal-data)]))

(defn- gap-weighted-example []
  (multi-pdf-chart
   [(reduce hst/insert! (hst/create :bins 8 :gap-weighted? true) normal-data)
    (reduce hst/insert! (hst/create :bins 8 :gap-weighted? false) normal-data)]))

(defn- numeric-target-example []
  (let [target-data (map (fn [x] [x (Math/sin x)])
                         normal-data)
        hist (reduce #(apply hst/insert! %1 %2)
                     (hst/create) target-data)]
    (pdf-target-chart hist)))
