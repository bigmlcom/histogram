;; Copyright 2013 BigML
;; Licensed under the Apache License, Version 2.0
;; http://www.apache.org/licenses/LICENSE-2.0

(ns bigml.histogram.test.data
  (:import [java.util Random])
  (:use [bigml.histogram.core]))

(defn- ^Random make-rnd [seed]
  (Random. (hash (or seed (rand)))))

(defn normal-data [size & [seed]]
  (let [rnd (make-rnd seed)]
    (repeatedly size #(.nextGaussian rnd))))

(defn rand-data [size & [seed]]
  (let [rnd (make-rnd seed)]
    (repeatedly size #(.nextDouble rnd))))

(defn cat-data [size with-missing & [seed]]
  (let [rnd (make-rnd seed)]
    (repeatedly size
                #(let [x (.nextDouble rnd)
                       y (cond (< x (/ 1 3)) :apple
                               (< x (/ 2 3)) :orange
                               :else :grape)]
                   [x (if (or (not with-missing) (> 0.5 (.nextDouble rnd)))
                        y nil)]))))

(defn group-data [size with-missing & [seed]]
  (let [rnd (make-rnd seed)]
    (map list
         (repeatedly #(.nextDouble rnd))
         (cat-data size with-missing seed))))
