(ns histogram.core
  (:import (com.bigml.histogram Histogram Histogram$TargetType Bin
                                Target SimpleTarget NumericTarget
                                CategoricalTarget GroupTarget SumResult
                                MixedInsertException)))

(defn create
  "Creates a histogram.

   Two optional parameters may be provided.  The first parameters sets
   the maximum number of bins used by the histogram (default 64).  The
   second parameter determines whether the histogram uses gap
   weighting (true or false - default false)."
  ([]
     (create 64))
  ([bins]
     (create bins false))
  ([bins gap-weighted?]
     (Histogram. bins gap-weighted?)))

(defn target-type
  "Returns the target-type of the histogram."
  [^Histogram hist]
  (condp = (.getTargetType hist)
    Histogram$TargetType/none :none
    Histogram$TargetType/numeric :numeric
    Histogram$TargetType/categorical :categorical
    Histogram$TargetType/group :group
    nil :unset))

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
          (= hist-type value-type) hist-type
          :else :mixed)))

(defn insert-categorical!
  "Inserts a point with a categorical target into the histogram."
  [^Histogram hist p v]
  (.insertCategorical hist (double p) v))

(defmulti insert!
  "Inserts a point and an optional target into the histogram.  The
   point must be a number and the target may be a number, string,
   keyword, or collection of the previous targets."
  insert-type)

(defmethod insert! :none [^Histogram hist p]
  (.insert hist (double p)))

(defmethod insert! :numeric [^Histogram hist p v]
  (.insert hist (double p) (double v)))

(defmethod insert! :categorical [^Histogram hist p v]
  (insert-categorical! hist p v))

(defmethod insert! :group [^Histogram hist p v]
  (.insert hist (double p) v))

(defmethod insert! :mixed [_ & _]
  (throw (MixedInsertException.)))

(defmethod insert! :default [_ & v]
  (throw (Exception. (apply str "Invalid insert: " (interpose " " v)))))

(defmulti ^:private scrub-target class)

(defmethod scrub-target :default [_]
  nil)

(defmethod scrub-target NumericTarget [^NumericTarget target]
  (.getTarget target))

(defmethod scrub-target CategoricalTarget [^CategoricalTarget target]
  (into {} (.getTargetCounts target)))

(defmethod scrub-target GroupTarget [^GroupTarget target]
  (map scrub-target (.getGroupTarget target)))

(defn- scrub-bin [^Bin bin]
  (let [bin-map {:mean (.getMean bin)
                 :count (long (.getCount bin))}
        target (scrub-target (.getTarget bin))]
    (if target (assoc bin-map :target target) bin-map)))

(defn bins
  "Returns the bins contained in the histogram."
  [^Histogram hist]
  (map scrub-bin (.getBins hist)))

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
  "Returns an approximate median."
  [^Histogram hist]
  (first (uniform hist 2)))

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

(defn bounds
  "Returns the bounds of the histogram, nil if the histogram is empty.
   An optional parameter may be supplied to enable a small buffer to
   the bounds (true or false - default false)."
  ([^Histogram hist]
     (bounds hist false))
  ([^Histogram hist buffer?]
     (when-let [bins (seq (bins hist))]
       (let [l-mean (:mean (last bins))
             f-mean (:mean (first bins))]
         (if (and buffer? (second bins))
           {:min (- f-mean (* 1.1 (- (:mean (second bins)) f-mean)))
            :max (+ l-mean (* 1.1 (- l-mean (:mean (last (drop-last bins))))))}
           {:min f-mean
            :max l-mean})))))
