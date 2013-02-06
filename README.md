# Overview

This project is an implementation of the streaming, one-pass
histograms described in Ben-Haim's [Streaming Parallel Decision
Trees](http://jmlr.csail.mit.edu/papers/v11/ben-haim10a.html). Inspired
by Tyree's [Parallel Boosted Regression
Trees](http://research.engineering.wustl.edu/~tyrees/Publications_files/fr819-tyreeA.pdf),
the histograms are extended so they may track multiple values.

The histograms act as an approximation of the underlying dataset. They
can be used for learning, visualization, discretization, or analysis.
The histograms may be built independently and merged, making them
convenient for parallel and distributed algorithms.

While the core of this library is implemented in Java, it includes a
full featured Clojure wrapper. This readme focuses on the Clojure
interface, but Java developers can find documented methods in
`com.bigml.histogram.Histogram`.

# Installation

`histogram` is available as a Maven artifact from
[Clojars](http://clojars.org/bigml/histogram).

For [Leiningen](https://github.com/technomancy/leiningen):

```clojure
[bigml/histogram "3.0.0"]
```

For [Maven](http://maven.apache.org/):

```xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
<dependency>
  <groupId>bigml</groupId>
  <artifactId>histogram</artifactId>
  <version>3.0.0</version>
</dependency>
```

# Basics

In the following examples we use [Incanter](http://incanter.org/) to
generate data and for charting.

The simplest way to use a histogram is to `create` one and then
`insert!` points.  In the example below, `ex/normal-data` refers to a
sequence of 200K samples from a normal distribution (mean 0, variance
1).

```clojure
user> (ns examples
        (:use [bigml.histogram.core])
        (:require (bigml.histogram.test [examples :as ex])))
examples> (def hist (reduce insert! (create) ex/normal-data))
```

You can use the `sum` fn to find the approximate number of points less
than a given threshold:

```clojure
examples> (sum hist 0)
99814.63248
```

The `density` fn gives us an estimate of the point density at the
given location:

```clojure
examples> (density hist 0)
80936.98291
```

The `uniform` fn returns a list of points that separate the
distribution into equal population areas.  Here's an example that
produces quartiles:

```clojure
examples> (uniform hist 4)
(-0.66904 0.00229 0.67605)
```
Arbritrary percentiles can be found using `percentiles`:

```clojure
examples> (percentiles hist 0.5 0.95 0.99)
{0.5 0.00229, 0.95 1.63853, 0.99 2.31390}
```

We can plot the sums and density estimates as functions.  The red line
represents the sum, the blue line represents the density.  If we
normalized the values (dividing by 200K), these lines approximate the
[cumulative distribution
function](http://en.wikipedia.org/wiki/Cumulative_distribution_function)
and the [probability distribution
function](http://en.wikipedia.org/wiki/Probability_density_function)
for the normal distribution.

```clojure
examples> (ex/sum-density-chart hist) ;; also see (ex/cdf-pdf-chart hist)
```
![Histogram from normal distribution]
(https://www.evernote.com/shard/s4/sh/acee03ad-4f5f-4fbf-82f1-5dc7301fc260/85e8b22b12e02a302198110ca77a89b2/res/eb14258d-2830-4362-aca4-590c83866946/skitch.png)

The histogram approximates distributions using a constant number of
bins. This bin limit is a parameter when creating a histogram
(`:bins`, defaults to 64). A bin contains a `:count` of the points
within the bin along with the `:mean` for the values in the bin. The
edges of the bin aren't captured. Instead the histogram assumes that
points of a bin are distributed with half the points less than the bin
mean and half greater. This explains the fractional sum in the example
below:

```clojure
examples> (def hist (-> (create :bins 3)
                        (insert! 1)
                        (insert! 2)
                        (insert! 3)))
examples> (bins hist)
({:mean 1.0, :count 1} {:mean 2.0, :count 1} {:mean 3.0, :count 1})
examples> (sum hist 2)
1.5
```

As mentioned earlier, the bin limit constrains the number of unique
bins a histogram can use to capture a distribution. The histogram
above was created with a limit of just three bins. When we add a
fourth unique value it will create a fourth bin and then merge the
nearest two.

```clojure
examples> (bins (insert! hist 0.5))
({:mean 0.75, :count 2} {:mean 2.0, :count 1} {:mean 3.0, :count 1})
```

A larger bin limit means a higher quality picture of the distribution,
but it also means a larger memory footprint.  In the chart below, the
red line represents a histogram with 8 bins and the blue line
represents 64 bins.

```clojure
examples> (ex/multi-pdf-chart
           [(reduce insert! (create :bins 8) ex/mixed-normal-data)
            (reduce insert! (create :bins 64) ex/mixed-normal-data)])
```
![8 and 64 bins histograms]
(https://www.evernote.com/shard/s4/sh/580f6e58-2e9d-42f1-8288-d84013fa962d/38b9d70534c37ff680c68bd2f251d710/res/9451f44b-e364-499e-af78-a827729b9612/skitch.png)

Another option when creating a histogram is to use *gap
weighting*. When `:gap-weighted?` is true, the histogram is encouraged
to spend more of its bins capturing the densest areas of the
distribution. For the normal distribution that means better resolution
near the mean and less resolution near the tails. The chart below
shows a histogram without gap weighting in blue and with gap weighting
in red.  Near the center of the distribution, red uses more bins and
better captures the gaussian distribution's true curve.

```clojure
examples> (ex/multi-pdf-chart
           [(reduce insert! (create :bins 8 :gap-weighted? true)
                    ex/normal-data)
            (reduce insert! (create :bins 8 :gap-weighted? false)
                    ex/normal-data)])
```
![Gap weighting vs. No gap weighting]
(https://www.evernote.com/shard/s4/sh/526873b6-e8dc-458a-a7e5-805faa66d9a0/e6144df90e693bdba08256bab325f241/res/6acb5020-0e4e-4bc5-8185-249f7f090a88/skitch.png)

# Merging

A strength of the histograms is their ability to merge with one
another. Histograms can be built on separate data streams and then
combined to give a better overall picture.

In this example, the blue line shows a density distribution from a
histogram after merging 300 noisy histograms. The red shows one of the
original histograms:

```clojure
examples> (let [samples (partition 1000 ex/mixed-normal-data)
                hists (map #(reduce insert! (create) %) samples)
                merged (reduce merge! (create) (take 300 hists))]
            (ex/multi-pdf-chart [(first hists) merged]))
```
![Merged histograms]
(https://www.evernote.com/shard/s4/sh/3ed19b0c-11a9-4c21-a751-5d5cdaede224/f0429a26c621f89ccaa52e6c476d98bb/res/a6ea07e9-90d5-4309-968b-e1095505a13d/skitch.png)

# Targets

While a simple histogram is nice for capturing the distribution of a
single variable, it's often important to capture the correlation
between variables. To that end, the histograms can track a second
variable called the *target*.

The target may be either numeric or categorical. The `insert!` fn is
overloaded to accept either type of target. Each histogram bin will
contain information summarizing the target. For numeric targets the
sum and sum-of-squares are tracked.  For categoricals, a map of
counts is maintained.

```clojure
examples> (-> (create)
              (insert! 1 9)
              (insert! 2 8)
              (insert! 3 7)
              (insert! 3 6)
              (bins))
({:target {:sum 9.0, :sum-squares 81.0, :missing-count 0.0},
  :mean 1.0,
  :count 1}
 {:target {:sum 8.0, :sum-squares 64.0, :missing-count 0.0},
  :mean 2.0,
  :count 1}
 {:target {:sum 13.0, :sum-squares 85.0, :missing-count 0.0},
  :mean 3.0,
  :count 2})
examples> (-> (create)
              (insert! 1 :a)
              (insert! 2 :b)
              (insert! 3 :c)
              (insert! 3 :d)
              (bins))
({:target {:counts {:a 1.0}, :missing-count 0.0},
  :mean 1.0,
  :count 1}
 {:target {:counts {:b 1.0}, :missing-count 0.0},
  :mean 2.0,
  :count 1}
 {:target {:counts {:d 1.0, :c 1.0}, :missing-count 0.0},
  :mean 3.0,
  :count 2})
```

Mixing target types isn't allowed:

```clojure
examples> (-> (create)
              (insert! 1 :a)
              (insert! 2 999))
Can't mix insert types
  [Thrown class com.bigml.histogram.MixedInsertException]
```

`insert-numeric!` and `insert-categorical!` allow target types to be
set explicitly:

```clojure
examples> (-> (create)
              (insert-categorical! 1 1)
              (insert-categorical! 1 2)
              (bins))
({:target {:counts {2 1.0, 1 1.0}, :missing-count 0.0}, :mean 1.0, :count 2})
```

The `extended-sum` fn works similarly to `sum`, but returns a result
that includes the target information:

```clojure
examples> (-> (create)
              (insert! 1 :a)
              (insert! 2 :b)
              (insert! 3 :c)
              (extended-sum 2))
{:sum 1.5, :target {:counts {:c 0.0, :b 0.5, :a 1.0}, :missing-count 0.0}}
```

The `average-target` fn returns the average target value given a
point. To illustrate, the following histogram captures a dataset where
the input field is a sample from the normal distribution while the
target value is the sine of the input. The density is in red and the
average target value is in blue:

```clojure
examples> (def make-y (fn [x] (Math/sin x)))
examples> (def hist (let [target-data (map (fn [x] [x (make-y x)])
                                           ex/normal-data)]
                      (reduce (fn [h [x y]] (insert! h x y))
                              (create)
                              target-data)))
examples> (ex/pdf-target-chart hist)
```
![Numeric target]
(https://www.evernote.com/shard/s4/sh/34b51a39-b090-4f33-93a2-7be1112b869e/e3d276b3adb347b3c493a295bb585f8d/res/4a254bbb-6111-43cb-826d-f09d8d3048c7/skitch.png)

Continuing with the same histogram, we can see that `average-target`
produces values close to original target:

```clojure
examples> (def view-target (fn [x] {:actual (make-y x)
                                    :approx (:sum (average-target hist x))}))
examples> (view-target 0)
{:actual 0.0, :approx -0.00051}
examples>  (view-target (/ Math/PI 2))
{:actual 1.0, :approx 0.9968169965429206}
examples> (view-target Math/PI)
{:actual 0.0, :approx 0.00463}
```

# Missing Values

Information about missing values is captured whenever the input field
or the target is `nil`. The `missing-bin` fn retrieves information
summarizing the instances with a missing input. For a basic histogram,
that is simply the count:

```clojure
examples> (-> (create)
              (insert! nil)
              (insert! 7)
              (insert! nil)
              (missing-bin))
{:count 2}
```

For a histogram with a target, the `missing-bin` includes target
information:

```clojure
examples> (-> (create)
              (insert! nil :a)
              (insert! 7 :b)
              (insert! nil :c)
              (missing-bin))
{:target {:counts {:a 1.0, :c 1.0}, :missing-count 0.0}, :count 2}
```

Targets can also be missing, in which case the target `missing-count`
is incremented:

```clojure
examples> (-> (create)
              (insert! nil :a)
              (insert! 7 :b)
              (insert! nil nil)
              (missing-bin))
{:target {:counts {:a 1.0}, :missing-count 1.0}, :count 2}
```

# Array-backed Categorical Targets

By default a histogram with categorical targets stores the category
counts as Java HashMaps. Building and merging HashMaps can be
expensive. Alternatively the category counts can be backed by an
array. This can give better performance but requires the set of
possible categories to be declared when the histogram is created. To
do this, set the `:categories` parameter:

```clojure
examples> (def categories (map (partial str "c") (range 50)))
examples> (def data (vec (repeatedly 100000
                                     #(vector (rand) (str "c" (rand-int 50))))))
examples> (doseq [hist [(create) (create :categories categories)]]
            (time (reduce (fn [h [x y]] (insert! h x y))
                          hist
                          data)))
"Elapsed time: 1295.402 msecs"
"Elapsed time: 516.72 msecs"
```

# Group Targets

Group targets allow the histogram to track multiple targets at the
same time. Each bin contains a sequence of target
information. Optionally, the target types in the group can be declared
when creating the histogram. Declaring the types on creation allows
the targets to be missing in the first insert:

```clojure
examples> (-> (create :group-types [:categorical :numeric])
              (insert! 1 [:a nil])
              (insert! 2 [:b 8])
              (insert! 3 [:c 7])
              (insert! 1 [:d 6])
              (bins))
({:target
  ({:counts {:d 1.0, :a 1.0}, :missing-count 0.0}
   {:sum 6.0, :sum-squares 36.0, :missing-count 1.0}),
  :mean 1.0,
  :count 2}
 {:target
  ({:counts {:b 1.0}, :missing-count 0.0}
   {:sum 8.0, :sum-squares 64.0, :missing-count 0.0}),
  :mean 2.0,
  :count 1}
 {:target
  ({:counts {:c 1.0}, :missing-count 0.0}
   {:sum 7.0, :sum-squares 49.0, :missing-count 0.0}),
  :mean 3.0,
  :count 1})
```

# Freezing a Histogram

While the ability to adapt to non-stationary data streams is a
strength of the histograms, it is also computationally expensive. If
your data stream is stationary, you can increase the histogram's
performance by setting the `:freeze` parameter. After the number of
inserts into the histogram have exceeded the `:freeze` parameter, the
histogram bins are locked into place. As the bin means no longer
shift, inserts become computationally cheap. However the quality of
the histogram can suffer if the `:freeze` parameter is too small.

```clojure
examples> (time (reduce insert! (create) ex/normal-data))
"Elapsed time: 391.857 msecs"
examples> (time (reduce insert! (create :freeze 1024) ex/normal-data))
"Elapsed time: 99.92 msecs"
```

# Performance

Insert time scales `log(n)` with respect to the number of bins in the
histogram.

![timing chart]
(https://docs.google.com/spreadsheet/oimg?key=0Ah2oAcudnjP4dG1CLUluRS1rcHVqU05DQ2Z4UVZnbmc&oid=2&zx=mppmmoe214jm)

# License

Copyright (C) 2013 BigML Inc.

Distributed under the Apache License, Version 2.0.
