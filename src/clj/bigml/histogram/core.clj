;; Copyright 2013 BigML
;; Licensed under the Apache License, Version 2.0
;; http://www.apache.org/licenses/LICENSE-2.0

(ns bigml.histogram.core
  (:import (com.bigml.histogram Histogram Histogram$TargetType Bin
                                Target SimpleTarget NumericTarget
                                ArrayCategoricalTarget GroupTarget
                                MapCategoricalTarget SumResult
                                MixedInsertException
                                Histogram$BinReservoirType)
           (java.util HashMap ArrayList)))

(def ^:private clj-to-reservoir-types
  {:array Histogram$BinReservoirType/array
   :tree Histogram$BinReservoirType/tree})

(def ^:private clj-to-java-types
  {:none Histogram$TargetType/none
   :numeric Histogram$TargetType/numeric
   :categorical Histogram$TargetType/categorical
   :group Histogram$TargetType/group})

(def ^:private java-to-clj-types
  (assoc (zipmap (vals clj-to-java-types) (keys clj-to-java-types))
    nil :unset))

(defn create
  "Creates a histogram.

   Optional parameters:
     :bins - Maximum bins to be used by the histogram (default 64)
     :gap-weighted? - Use gap weighting (true or false - default false)
     :categories - Collection of valid categories (improves performance)
     :group-types - A sequence of types (:numeric or :categorical) that
                    describing a group target.
     :freeze - After this # of inserts, bin locations will 'freeze',
               improving the performance of future inserts.
     :reservoir - Selects the bin reservoir type (:array or :tree).
                  Defaults to :array for <= 256 bins, otherwise :tree."
  [& {:keys [bins gap-weighted? categories group-types freeze reservoir]
      :or {bins 64 gap-weighted? false}}]
  (let [group-types (seq (map clj-to-java-types group-types))
        reservoir (clj-to-reservoir-types reservoir)]
    (Histogram. bins gap-weighted? categories group-types freeze reservoir)))

(defn histogram?
  "Returns true if the input is a histogram."
  [hist]
  (instance? Histogram hist))

(defn- java-target [target]
  (if (sequential? target)
    (GroupTarget. (ArrayList. (map java-target target)))
    (let [{:keys [sum sum-squares missing-count counts]} target]
      (cond (contains? target :sum)
            (NumericTarget. sum sum-squares missing-count)
            (contains? target :counts)
            (MapCategoricalTarget. (HashMap. counts) missing-count)
            (nil? target)
            SimpleTarget/TARGET))))

(defn- java-bin [bin]
  (let [{:keys [mean count target]} bin]
    (Bin. mean count ^Target (java-target target))))

(defn insert-bin!
  "Inserts a bin into the histogram."
  [^Histogram hist bin]
  (if (instance? Bin bin)
    (.insertBin hist ^Bin bin)
    (if (:mean bin)
      (.insertBin hist ^Bin (java-bin bin))
      (.insertMissing hist (:count bin) (java-target (:target bin))))))

(defn target-type
  "Returns the target-type of the histogram."
  [^Histogram hist]
  (java-to-clj-types (.getTargetType hist)))

(defn group-types
  "Returns the group types of the histogram."
  [^Histogram hist]
  (when-let [group-types (.getGroupTypes hist)]
    (map java-to-clj-types group-types)))

(defn- value-type [v]
  (cond (nil? v) :none
        (number? v) :numeric
        (string? v) :categorical
        (keyword? v) :categorical
        (coll? v) :group
        :else :invalid))

(defn- insert-type [hist _ & [v _]]
     (let [hist-type (target-type hist)
           value-type (value-type v)]
       (cond (= hist-type :unset) value-type
             (nil? v) hist-type
             (= hist-type value-type) hist-type
             :else :mixed)))

(defn- when-double [v]
  (when v (double v)))

(defn insert-simple!
  "Inserts a point into the histogram (no target)."
  [^Histogram hist p]
  (.insert hist (when-double p)))

(defn insert-categorical!
  "Inserts a point with a categorical target into the histogram."
  [^Histogram hist p v]
  (.insertCategorical hist (when-double p) v))

(defn insert-numeric!
  "Inserts a point with a categorical target into the histogram."
  [^Histogram hist p v]
  (.insertNumeric hist (when-double p) (when-double v)))

(defn insert-group!
  "Inserts a point with a group target into the histogram."
  [^Histogram hist p v]
  (.insertGroup hist (when-double p) v))

(defmulti insert!
  "Inserts a point and an optional target into the histogram.  The
   point must be a number and the target may be a number, string,
   keyword, or collection of the previous targets."
  insert-type)

(defmethod insert! :none
  ([^Histogram hist p]
     (insert-simple! hist p))
  ([^Histogram hist p _]
     (throw (Exception. "Unset histogram can't accept nil a target"))))

(defmethod insert! :numeric [^Histogram hist p v]
  (insert-numeric! hist p v))

(defmethod insert! :categorical [^Histogram hist p v]
  (insert-categorical! hist p v))

(defmethod insert! :group [^Histogram hist p v]
  (insert-group! hist p v))

(defmethod insert! :mixed [_ & _]
  (throw (MixedInsertException.)))

(defmethod insert! :default [_ & v]
  (throw (Exception. (apply str "Invalid insert: " (interpose " " v)))))

(defmulti ^:private scrub-target class)

(defmethod scrub-target :default [_]
  nil)

(defmethod scrub-target NumericTarget [^NumericTarget target]
  {:sum (.getSum target)
   :sum-squares (.getSumSquares target)
   :missing-count (.getMissingCount target)})

(defmethod scrub-target MapCategoricalTarget [^MapCategoricalTarget target]
  {:counts (dissoc (into {} (.getCounts target)) nil)
   :missing-count (.getMissingCount target)})

(defmethod scrub-target ArrayCategoricalTarget [^ArrayCategoricalTarget target]
  {:counts (into {} (.getCounts target))
   :missing-count (.getMissingCount target)})

(defmethod scrub-target GroupTarget [^GroupTarget target]
  (map scrub-target (.getGroupTarget target)))

(defn- scrub-bin [^Bin bin]
  (let [bin-map {:mean (.getMean bin)
                 :count (long (.getCount bin))}
        target (scrub-target (.getTarget bin))]
    (if target (assoc bin-map :target target) bin-map)))

(defn max-bins
  "Returns the maximum allowed bins for the histogram."
  [^Histogram hist]
  (.getMaxBins hist))

(defn bin-count
  "Returns the current number of bins."
  [^Histogram hist]
  (count (.getBins hist)))

(defn total-count
  "Returns the count of the points summarized by the histogram."
  [^Histogram hist]
  (.getTotalCount hist))

(defn total-target-sum
  "Returns the sum of the targets for each bin in the histogram."
  [^Histogram hist]
  (scrub-target (.getTotalTargetSum hist)))

(defn merge!
  "Merges the second histogram into the first."
  [^Histogram hist1 ^Histogram hist2]
  (.merge hist1 hist2))

(defn sum
  "Returns the approximate number of points occuring in the histogram
   equal or less than the given point."
  [^Histogram hist p]
  (.sum hist (double p)))

(defn uniform
  "Returns the split points that would separate the histogram into
   the supplied number of bins with equal population."
  [^Histogram hist max-bins]
  (seq (.uniform hist max-bins)))

(defn median
  "Returns an approximate median for the points inserted into the
   histogram."
  [^Histogram hist]
  (first (uniform hist 2)))

(defn percentiles
  "Returns a map of percentiles and their associated locations."
  [^Histogram hist & percentiles]
  (into (sorted-map)
        (.percentiles hist (into-array (map double percentiles)))))

(defn sample
  "Returns a sequence of samples from the distribution approximated by
   the histogram."
  [hist & [sample-size]]
  (repeatedly (or sample-size 1)
              #(second (first (percentiles hist (rand))))))

(defn extended-sum
  "Returns the approximate number of points occuring in the histogram
   equal or less than the given point, along with the sum of the
   targets."
  [^Histogram hist p]
  (let [^SumResult result (.extendedSum hist (double p))]
    {:sum (.getCount result)
     :target (scrub-target (.getTargetSum result))}))

(defn density
  "Returns an estimate of the histogram's density at the given point."
  [^Histogram hist p]
  (.density hist (double p)))

(defn extended-density
  "Returns an estimate of the histogram's density at the given point,
   along with the density of the targets."
  [^Histogram hist p]
  (let [^SumResult result (.extendedDensity hist (double p))]
    {:density (.getCount result)
     :target (scrub-target (.getTargetSum result))}))

(defn average-target
  "Returns the average (or expected) target for the given point."
  [^Histogram hist p]
  (scrub-target (.averageTarget hist p)))

(defn missing-bin
  "Retrieves information about inserts with missing input points."
  [^Histogram hist]
  (let [missing-map {:count (.getMissingCount hist)}
        target (scrub-target (.getMissingTarget hist))]
    (if target (assoc missing-map :target target) missing-map)))

(defn bins
  "Returns the bins contained in the histogram. A missing bin (mean is
   nil) is included if it's non-empty."
  [^Histogram hist]
  (map scrub-bin (.getBins hist)))

(defn minimum
  "Returns the minimum value inserted into the histogram."
  [^Histogram hist]
  (.getMinimum hist))

(defn maximum
  "Returns the maximum value inserted into the histogram."
  [^Histogram hist]
  (.getMaximum hist))

(defn bounds
  "Returns the bounds of the histogram, nil if the histogram is empty."
  [^Histogram hist]
  (when-let [bins (seq (bins hist))]
    {:min (minimum hist)
     :max (maximum hist)}))

(defn hist-to-clj
  "Transforms a Histogram object into a Clojure map representing the
  histogram."
  [^Histogram hist]
  (into {} (remove (comp nil? second)
                   {:max-bins (.getMaxBins hist)
                    :gap-weighted? (.isCountWeightedGaps hist)
                    :freeze (.getFreezeThreshold hist)
                    :group-types (map (comp keyword str)
                                      (seq (.getGroupTypes hist)))
                    :categories (seq (.getTargetCategories hist))
                    :bins (bins hist)
                    :missing-bin (when (pos? (.getMissingCount hist))
                                   (missing-bin hist))
                    :minimum (minimum hist)
                    :maximum (maximum hist)})))

(defn clj-to-hist
  "Transforms a Clojure map representing a histogram into a Histogram
  object."
  [hist-map]
  (let [{:keys [max-bins gap-weighted? freeze group-types categories
                bins missing-bin maximum minimum]} hist-map
        hist (create :bins max-bins :gap-weighted? gap-weighted?
                     :freeze freeze :group-types group-types
                     :categories categories)]
    (doseq [bin bins]
      (insert-bin! hist bin))
    (when minimum (.setMinimum hist minimum))
    (when maximum (.setMaximum hist maximum))
    (when missing-bin (insert-bin! hist missing-bin))
    hist))

(defn mean
  "Returns the mean over the points inserted into the histogram."
  [^Histogram hist]
  (:mean (first (-> (create :bins 1)
                    (merge! hist)
                    (bins)))))

(defn cdf
  "Returns the cumulative distribution function for the histogram."
  [^Histogram hist]
  (let [total (total-count hist)]
    #(/ (sum hist %) total)))

(defn pdf
  "Returns the probability density function for the histogram."
  [^Histogram hist]
  (let [total (total-count hist)]
    #(/ (density hist %) total)))

(defn variance
  "Returns an estimate of the variance for the histogram."
  [^Histogram hist]
  (let [h-mean (mean hist)
        h-count (total-count hist)]
    (when (> h-count 1)
      (/ (reduce (fn [v {:keys [mean count]}]
                   (let [diff (- mean h-mean)]
                     (+ v (* count diff diff))))
                 0
                 (bins hist))
         (dec h-count)))))
