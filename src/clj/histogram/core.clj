(ns histogram.core
  (:import (com.bigml.histogram Histogram Histogram$TargetType Bin
                                Target SimpleTarget NumericTarget
                                ArrayCategoricalTarget GroupTarget
                                MapCategoricalTarget SumResult
                                MixedInsertException)
           (java.util HashMap ArrayList)))

(def ^:private clj-to-java-types
  {:none Histogram$TargetType/none
   :numeric Histogram$TargetType/numeric
   :categorical Histogram$TargetType/categorical
   :group Histogram$TargetType/group})

(def ^:private java-to-clj-types
  (assoc (into {} (map (fn [[k v]] [v k]) clj-to-java-types))
    nil :unset))

(defn create
  "Creates a histogram.

   Optional parameters:
     :bins - Maximum bins to be used by the histogram (default 64)
     :gap-weighted? - Use gap weighting (true or false - default false)
     :categories - Collection of valid categories (improves performance)"
  [& {:keys [bins gap-weighted? categories group-types]
      :or {bins 64 gap-weighted? false}}]
  (let [group-types (when group-types (map clj-to-java-types group-types))]
    (Histogram. bins gap-weighted? categories group-types)))

(defn histogram?
  "Returns true if the input is a histogram."
  [hist]
  (instance? Histogram hist))

(defn- java-target [target]
  (let [target-val (or (:sum target) (:counts target))
        missing-count (:missing-count target)]
    (cond (number? target-val)
          (NumericTarget. target-val missing-count)
          (map? target-val)
          (MapCategoricalTarget. (HashMap. target-val) missing-count)
          (sequential? target-val)
          (GroupTarget. (ArrayList. (map java-target target-val)))
          (nil? target)
          SimpleTarget/TARGET)))

(defn- java-bin [bin]
  (let [{:keys [mean count target]} bin]
    (Bin. mean count ^Target (java-target target))))

(defn insert-bin!
  "Inserts a bin into the histogram."
  [^Histogram hist bin]
  (if (instance? Bin bin)
    (.insert hist ^Bin bin)
    (.insert hist ^Bin (java-bin bin))))

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
          (= hist-type value-type) hist-type
          :else :mixed)))

(defn insert-categorical!
  "Inserts a point with a categorical target into the histogram."
  [^Histogram hist p v]
  (.insertCategorical hist (double p) v))

(defn insert-numeric!
  "Inserts a point with a categorical target into the histogram."
  [^Histogram hist p v]
  (.insertNumeric hist (double p) (when v (double v))))

(defn insert-group!
  "Inserts a point with a group target into the histogram."
  [^Histogram hist p v]
  (.insertGroup hist (double p) v))

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
  {:sum (.getTarget target)
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

(defn total-count
  "Returns the count of the points summarized by the histogram."
  [^Histogram hist]
  (.getTotalCount hist))

(defn total-target-sum
  "Returns the sum of the targets for each bin in the histogram."
  [^Histogram hist]
  (scrub-target (.getTotalTargetSum hist)))

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
  "Returns an approximate median for the points inserted into the
   histogram."
  [^Histogram hist]
  (first (uniform hist 2)))

(defn mean
  [^Histogram hist]
  "Returns the mean for the points inserted into the histogram."
  (when-not (empty? (.getBins hist))
    (.getMean ^Bin (reduce (fn [^Bin b1 ^Bin b2] (.combine b1 b2))
                           (.getBins hist)))))

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
