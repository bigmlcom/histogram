;; Copyright 2013 BigML
;; Licensed under the Apache License, Version 2.0
;; http://www.apache.org/licenses/LICENSE-2.0

(ns bigml.histogram.test.regression
  (:use [bigml.histogram.core]
        [bigml.histogram.test.data]
        [clojure.test]))

(deftest regression
  (doseq [impl [:array :tree]]
    (let [hist1 (reduce insert!
                        (create :bins 5 :reservoir impl)
                        (normal-data 10000 :foobar))
          hist2 (reduce insert!
                        (create :bins 5 :gap-weighted? true :reservoir impl)
                        (normal-data 10000 :foobar))
          hist3 (-> (create :bins 5 :gap-weighted? true :reservoir impl)
                    (merge! hist1)
                    (merge! hist2))
          hist4 (reduce insert!
                        (create :bins 5 :reservoir impl)
                        (range 10000))]
      (is (= (bins hist1)
             '({:mean -3.017025009977355 :count 2}
               {:mean -1.565313137205623 :count 231}
               {:mean -0.5653174811340864 :count 5844}
               {:mean 0.9712898713224954 :count 3830}
               {:mean 1.9950539943162184 :count 93})))
      (is (= (uniform hist1 4)
             '(-0.6841830590540494 -0.05047358725433948 0.7801493555360859)))
      (is (= (percentiles hist1 0.1 0.25 0.5 0.75 0.9)
             {0.1 -1.0442045426617783
              0.25 -0.6841830590540494
              0.5 -0.05047358725433948
              0.75 0.7801493555360859
              0.9 1.2887574838312434}))

      (is (= (bins hist2)
             '({:mean -1.5221626138284334 :count 1441}
               {:mean -0.5620991088775851 :count 2831}
               {:mean 0.20273436802994288 :count 2877}
               {:mean 0.9430936908042826 :count 2020}
               {:mean 1.8419571067231262 :count 831})))
      (is (= (uniform hist2 4)
             '(-0.6869854304371428 0.013477745530167229 0.7162981909540775)))
      (is (= (percentiles hist2 0.1 0.25 0.5 0.75 0.9)
             {0.1 -1.35071327811129
              0.25 -0.6869854304371428
              0.5 0.013477745530167229
              0.75 0.7162981909540775
              0.9 1.3797534213678286}))

      (is (= (bins hist3)
             '({:mean -1.529903053310171 :count 1674}
               {:mean -0.5642671973464028 :count 8675}
               {:mean 0.20273436802994288 :count 2877}
               {:mean 0.9615537542888561 :count 5850}
               {:mean 1.8573662090458076 :count 924})))
      (is (= (uniform hist3 4)
             '(-0.6827233569614347 -0.12912504799511332 0.8039351406475959)))
      (is (= (percentiles hist3 0.1 0.25 0.5 0.75 0.9)
             {0.1 -1.1582109602794723
              0.25 -0.6827233569614347
              0.5 -0.12912504799511332
              0.75 0.8039351406475959
              0.9 1.2978436908963455}))

      (is (= (bins hist4)
             '({:mean 669.0 :count 1339}
               {:mean 2675.0 :count 2673}
               {:mean 4680.5 :count 1338}
               {:mean 6685.5 :count 2672}
               {:mean 9010.5 :count 1978})))
      (is (= (uniform hist4 4)
             '(2541.061374142397 5112.189357410602 7424.275271894938)))
      (is (= (percentiles hist4 0.1 0.25 0.5 0.75 0.9)
             {0.1 1114.7853492345948
              0.25 2541.061374142397
              0.5 5112.189357410602
              0.75 7424.275271894938
              0.9 8997.58286262782})))))
