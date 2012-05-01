(ns histogram.test.examples
  (:require (histogram [core :as hst])
            (incanter [core :as core]
                      [charts :as charts]
                      [distributions :as dst])))

;; 100K samples from a normal distribution (mean 0 and variance 1)
(def normal-data (repeatedly 100000 #(dst/draw (dst/normal-distribution))))

(defn multi-density-chart [hists]
  (let [bounds (hst/bounds (first hists) true)]
    (core/view
     (reduce (fn [c h]
               (charts/add-function c #(hst/density h %)
                                    (:min bounds)
                                    (:max bounds)))
             (charts/function-plot #(hst/density (first hists) %)
                                   (:min bounds)
                                   (:max bounds))
             (next hists)))))

(defn sum-density-chart [hist]
  (let [bounds (hst/bounds hist)]
    (core/view
     (-> (charts/function-plot #(hst/sum hist %)
                               (:min bounds)
                               (:max bounds))
         (charts/add-function #(hst/density hist %)
                              (:min bounds)
                              (:max bounds))))))

(defn extended-sum-chart [hist]
  (let [bounds (hst/bounds hist)]
    (core/view
     (-> (charts/function-plot #(:sum (hst/extended-sum hist %))
                               (:min bounds)
                               (:max bounds))
         (charts/add-function #(:sum (:target (hst/extended-sum hist %)))
                              (:min bounds)
                              (:max bounds))))))

(defn density-target-chart [hist]
  (let [bounds (hst/bounds hist)]
    (core/view
     (-> (charts/function-plot #(hst/density hist %)
                               (:min bounds)
                               (:max bounds))
         (charts/add-function #(:sum (hst/average-target hist %))
                              (:min bounds)
                              (:max bounds))))))

;; Builds and charts a histogram for the normal distribution.
(defn- normal-example []
  (let [hist (reduce hst/insert! (hst/create) normal-data)]
    (println "Total sum of points less than 0:" (hst/sum hist 0))
    (println "Quartile splits:" (hst/uniform hist 4))
    (sum-density-chart hist)))

(defn- varying-bins-example []
  (multi-density-chart
   [(reduce hst/insert! (hst/create :bins 16) normal-data)
    (reduce hst/insert! (hst/create :bins 64) normal-data)]))

(defn- gap-weighted-example []
  (multi-density-chart
   [(reduce hst/insert! (hst/create :bins 16 :gap-weighted? true) normal-data)
    (reduce hst/insert! (hst/create :bins 16 :gap-weighted? false) normal-data)]))

(defn- numeric-target-example []
  (let [target-data (map (fn [x] [x (+ 10000 (* 10000 (Math/sin x)))])
                         normal-data)
        hist (reduce #(apply hst/insert! %1 %2)
                     (hst/create) target-data)]
    (density-target-chart hist)))

(defn -main [& args]
  (normal-example)
  (varying-bins-example)
  (gap-weighted-example)
  (numeric-target-example))
