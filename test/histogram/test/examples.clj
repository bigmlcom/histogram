(ns histogram.test.examples
  (:require (histogram [core :as hst])
            (incanter [core :as core]
                      [charts :as charts]
                      [distributions :as dst])))

;; 100K samples from a normal distribution (mean 0 and variance 1)
(def normal-data (repeatedly 100000 #(dst/draw (dst/normal-distribution))))

(defn multi-pdf-chart [hists]
  (let [min (reduce min (map (comp :min hst/bounds) hists))
        max (reduce max (map (comp :max hst/bounds) hists))]
    (core/view
     (reduce (fn [c h]
               (charts/add-function c (hst/pdf h) min max))
             (charts/function-plot (hst/pdf (first hists)) min max)
             (next hists)))))

(defn sum-density-chart [hist]
  (let [{:keys [min max]} (hst/bounds hist true)]
    (core/view (-> (charts/function-plot #(hst/sum hist %) min max)
                   (charts/add-function #(hst/density hist %) min max)))))

(defn cdf-pdf-chart [hist]
  (let [{:keys [min max]} (hst/bounds hist true)]
    (core/view (-> (charts/function-plot (hst/cdf hist) min max)
                   (charts/add-function (hst/pdf hist) min max)))))

(defn pdf-target-chart [hist]
  (let [{:keys [min max]} (hst/bounds hist true)]
    (core/view
     (-> (charts/function-plot (hst/pdf hist) min max)
         (charts/add-function #(:sum (hst/average-target hist %)) min max)))))

;; Builds and charts a histogram for the normal distribution.
(defn- normal-example []
  (let [hist (reduce hst/insert! (hst/create) normal-data)]
    (println "Total sum of points less than 0:" (hst/sum hist 0))
    (println "Quartile splits:" (hst/uniform hist 4))
    (sum-density-chart hist)
    (cdf-pdf-chart hist)))

(defn- varying-bins-example []
  (multi-pdf-chart
   [(reduce hst/insert! (hst/create :bins 16) normal-data)
    (reduce hst/insert! (hst/create :bins 64) normal-data)]))

(defn- gap-weighted-example []
  (multi-pdf-chart
   [(reduce hst/insert! (hst/create :bins 16 :gap-weighted? true) normal-data)
    (reduce hst/insert! (hst/create :bins 16 :gap-weighted? false) normal-data)]))

(defn- numeric-target-example []
  (let [target-data (map (fn [x] [x (Math/sin x)])
                         normal-data)
        hist (reduce #(apply hst/insert! %1 %2)
                     (hst/create) target-data)]
    (pdf-target-chart hist)))

(defn -main [& args]
  (normal-example)
  (varying-bins-example)
  (gap-weighted-example)
  (numeric-target-example))
