(ns histogram.test.core
  (:import [java.lang Math]
           [java.util Random])
  (:use [histogram.core]
        [clojure.test]))

(defn- about= [v1 v2 epsilon]
  (>= epsilon (Math/abs (double (- v1 v2)))))

(defn- normal-data [size]
  (let [^Random rnd (Random.)]
    (repeatedly size #(.nextGaussian rnd))))

(defn- rand-data [size]
  (repeatedly size #(rand)))

(defn- cat-data [size with-missing]
  (repeatedly size
              #(let [x (rand)
                     y (cond (< x (/ 1 3)) :apple
                          (< x (/ 2 3)) :orange
                          :else :grape)]
                 [x (if (or (not with-missing) (> 0.5 (rand)))
                      y nil)])))

(defn- group-data [size with-missing]
  (map list (repeatedly #(rand)) (cat-data size with-missing)))

(deftest sum-test
  (let [points 10000]
    (is (about= (sum (reduce insert! (create) (normal-data points)) 0)
                (/ points 2)
                (/ points 50)))))

(deftest median-mean-test
  (let [points 10000]
    (is (about= (median (reduce insert! (create) (rand-data points)))
                0.5 0.05))
    (is (about= (median (reduce insert! (create) (normal-data points)))
                0 0.05))
    (is (about= (mean (reduce insert! (create) (normal-data points)))
                0 0.05))))

(deftest mean-test
  (let [points 1001]
    (is (== (/ (dec points) 2)
            (mean (reduce insert! (create) (range points)))))))


(deftest merge-test
  (is (empty? (bins (merge! (create) (create)))))
  (is (seq (bins (merge! (insert! (create) 1) (create)))))
  (is (seq (bins (merge! (create) (insert! (create) 1)))))
  (let [points 1000
        hist-count 10
        hists (repeatedly hist-count
                          #(reduce insert! (create) (normal-data points)))
        merged-hist (reduce merge! hists)]
    (is (about= (sum merged-hist 0)
                (/ (* points hist-count) 2)
                (/ (* points hist-count) 50)))))

(deftest mixed-test
  (let [insert-pair #(apply insert! (apply insert! (create) %1) %2)
        vals [[1] [1 2] [1 :a] [1 [:a]]]]
    (doseq [x vals y vals]
      (if (= x y)
        (is (insert-pair x y))
        (is (thrown? Throwable (insert-pair x y))))))
  (is (thrown? Throwable (insert! (create :categories [:a :b]) :c)))
  (is (thrown? Throwable (merge! (create :categories [:a :b])
                                 (create :categories [:b :c])))))

(deftest density-test
  (let [hist (reduce insert! (create) [1 2 2 3])]
    (is (about= 0.0 (density hist 0.0) 1E-10))
    (is (about= 0.0 (density hist 0.5) 1E-10))
    (is (about= 0.5 (density hist 1.0) 1E-10))
    (is (about= 1.5 (density hist 1.5) 1E-10))
    (is (about= 2.0 (density hist 2.0) 1E-10))
    (is (about= 1.5 (density hist 2.5) 1E-10))
    (is (about= 0.5 (density hist 3.0) 1E-10))
    (is (about= 0.0 (density hist 3.5) 1E-10))
    (is (about= 0.0 (density hist 4.0) 1E-10))))

(deftest categorical-test
  (let [points 10000
        hist (reduce (fn [h [x y]] (insert! h x y))
                     (create)
                     (cat-data points false))
        ext-sum (extended-sum hist 0.5)]
    (is (about= (:apple (:counts (:target ext-sum)))
                (/ points 3)
                (/ points 50)))
    (is (about= (:orange (:counts (:target ext-sum)))
                (/ points 6)
                (/ points 50)))))

(deftest categorical-array-test
  (let [points 10000
        hist (reduce (fn [h [x y]] (insert! h x y))
                     (create :categories [:apple :orange :grape])
                     (cat-data points false))
        ext-sum (extended-sum hist 0.5)]
    (is (about= (:apple (:counts (:target ext-sum)))
                (/ points 3)
                (/ points 50)))
    (is (about= (:orange (:counts (:target ext-sum)))
                (/ points 6)
                (/ points 50)))
    (is (about= 3333 (:orange (:counts (total-target-sum hist))) 150))
    (is (about= 3333 (:grape (:counts (total-target-sum hist))) 150))
    (is (about= 3333 (:apple (:counts (total-target-sum hist))) 150))))

(deftest group-test
  (let [points 10000
        data (group-data points false)
        hist (reduce (fn [h [x y]] (insert! h x y))
                     (create)
                     data)
        target (:target (extended-sum hist 0.5))]
    (is (= (target-type hist) :group))
    (is (= (group-types hist) '(:numeric :categorical)))
    (is (about= (:sum (first target))
                (/ points 4)
                (/ points 50)))
    (is (about= (:sum-squares (first target))
                (reduce + (map #(* % %)
                               (take (int (/ (count data) 2))
                                     (map first data))))
                150))
    (is (about= (:orange (:counts (second target)))
                (/ points 6)
                (/ points 50)))))

(deftest weighted-gap-test
  ;; Histograms using weighted gaps are less eager to merge bins with
  ;; large counts.  This test builds weighted and non-weighted
  ;; histograms using samples from a normal distribution.  The
  ;; non-weighted histogram should spend more of its bins capturing
  ;; the tails of the distribution.  With that in mind this test makes
  ;; sure the bins bracketing the weighted histogram have larger
  ;; counts than the bins bracketing the non-weighted histogram.
  (let [points 10000
        weighted (bins (reduce insert!
                               (create :bins 32 :gap-weighted? true)
                               (normal-data points)))
        classic (bins (reduce insert!
                              (create :bins 32 :gap-weighted? false)
                              (normal-data points)))]
    (> (+ (:count (first weighted) (last weighted)))
       (+ (:count (first classic) (last classic))))))

(deftest round-trip-test
  (let [points 10000
        hist1 (reduce (fn [h [x y]] (insert! h x y))
                     (create)
                     (cat-data points false))
        hist2 (reduce insert-bin! (create) (bins hist1))]
    (= (bins hist1) (bins hist2))))

(deftest hist-test
  (is (histogram? (create)))
  (is (not (histogram? "forjao"))))

(deftest weighted-test
  (let [data [1 2 2 3 4]
        hist (reduce insert!
                     (create :bins 3 :gap-weighted? true)
                     data)]
    (is (== (total-count hist) (count data)))))

(deftest numeric-missing-test
  (let [data [[1 1] [1 nil] [4 2] [6 nil]]
        result (bins (reduce (partial apply insert-numeric!)
                             (create :bins 2)
                             data))]
    (is (= result
           '({:mean 1.0
              :count 2
              :target {:sum 1.0 :sum-squares 1.0 :missing-count 1.0}}
             {:mean 5.0
              :count 2
              :target {:sum 2.0 :sum-squares 4.0 :missing-count 1.0}})))))

(deftest categorical-missing-test
  (let [data [[1 :foo] [1 nil] [4 :bar] [6 nil]]
        result (bins (reduce (partial apply insert-categorical!)
                             (create :bins 2 :categories [:foo :bar])
                             data))]
    (is (= result
           '({:mean 1.0
              :count 2
              :target {:counts {:foo 1.0 :bar 0.0} :missing-count 1.0}}
             {:mean 5.0
              :count 2
              :target {:counts {:foo 0.0 :bar 1.0} :missing-count 1.0}})))))

(deftest group-missing-test
  (let [points 10000
        hist (reduce (fn [h [x y]] (insert! h x y))
                     (create :bins 4 :group-types [:numeric :categorical])
                     (group-data points true))
        ext-sum (extended-sum hist 0.5)]
    (is (about= (:sum (first (:target ext-sum)))
                (/ points 4)
                (/ points 50)))
    (is (about= (:missing-count (second (:target ext-sum)))
                (/ points 4)
                (/ points 50)))
    (is (about= (:orange (:counts (second (:target ext-sum))))
                (/ points 12)
                (/ points 50)))))

(deftest input-missing-test
  (let [data [[1 :foo] [nil :bar] [4 :bar] [nil :foo] [nil nil]]
        hist (reduce (partial apply insert-categorical!)
                     (create :bins 2 :categories [:foo :bar])
                     data)]
    (is (= (missing-bin hist)
           {:count 3
            :target {:counts {:bar 1.0, :foo 1.0}
                     :missing-count 1.0}}))))

(deftest missing-merge-test
  (is (merge! (insert-numeric! (create :bins 8) nil 4)
              (create :bins 8)))
  (let [hist1 (insert! (create) nil 1)
        hist2 (insert! (create) nil 2)
        merged (merge! (merge! (create) hist1) hist2)]
    (is (== 2 (:count (missing-bin merged))))
    (is (== 3 (:sum (:target (missing-bin merged)))))))

(deftest missing-bin-test
  (let [bin1 (-> (create)
                 (insert-numeric! nil 3)
                 (missing-bin))
        bin2 (-> (create)
                 (insert-bin! bin1)
                 (missing-bin))]
    (is (= bin1 bin2))))

(deftest min-max-test
  (let [hist (create)]
    (is (nil? (minimum hist)))
    (is (nil? (maximum hist))))
  (let [hist (reduce insert!
                     (create)
                     (repeatedly 1000 #(rand-int 10)))]
    (is (== 0 (minimum hist)))
    (is (== 9 (maximum hist))))
  (let [hist1 (reduce insert! (create) (range 0 4))
        hist2 (reduce insert! (create) (range 2 6))
        merged (-> (create) (merge! hist1) (merge! hist2))]
    (is (== 0 (minimum merged)))
    (is (== 5 (maximum merged)))))

(deftest transform-test
  (let [hist (-> (create)
                 (insert! 1 [2 3 :a])
                 (insert! 1 [9 2 :b])
                 (insert! 4 [5 nil nil]))]
    (is (= (hist-to-clj hist)
           (hist-to-clj (clj-to-hist (hist-to-clj hist))))))
  (let [hist1 (reduce (fn [h [x y]] (insert! h x y))
                      (create :bins 8 :gap-weighted? true
                              :categories [:apple :orange :grape])
                      (cat-data 1000 false))
        hist1 (insert! hist1 nil :apple)
        hist2 (clj-to-hist (hist-to-clj hist1))]
    (is (= (bins hist1) (bins hist2)))
    (is (= (missing-bin hist1) (missing-bin hist2)))
    (is (= (minimum hist1) (minimum hist2)))
    (is (= (maximum hist1) (maximum hist2)))))

(deftest variance-test
  (is (nil? (variance (insert! (create) 1))))
  (is (= 3.5 (variance (reduce insert! (create) [1 2 3 4 5 6]))))
  (is (about= (variance (reduce insert! (create) (normal-data 10000)))
              1 0.05)))

(deftest negative-zero-test
  (is (= 1 (count (bins (reduce insert! (create) [0.0 -0.0]))))))

(deftest freeze-test
  (let [points 100000
        hist (reduce insert! (create :freeze 500) (normal-data points))]
    (is (about= (sum hist 0) (/ points 2) (/ points 50)))
    (is (about= (median hist) 0 0.05))
    (is (about= (mean hist) 0 0.05))
    (is (about= (variance hist) 1 0.05))))

(deftest correct-counts
  (let [data [605 760 610 615 605 780 605 905]
        hist (reduce insert!
                     (create :bins 4 :gap-weighted? true)
                     data)]
    (is (== (count data)
            (total-count hist)
            (reduce + (map :count (bins hist)))))))
