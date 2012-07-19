# Overview

This project is an implementation of the streaming, one-pass
histograms described in Ben-Haim's [Streaming Parallel Decision
Trees](http://jmlr.csail.mit.edu/papers/v11/ben-haim10a.html). Inspired
by Tyree's [Parallel Boosted Regression
Trees](http://research.engineering.wustl.edu/~tyrees/Publications_files/fr819-tyreeA.pdf),
the histograms are extended to track multiple values.

The histograms act as an approximation of the underlying dataset. They
can be used for learning, visualization, discretization, or analysis.
The histograms may be built independently and merged, convenient for
parallel and distributed algorithms.

# Building

1. Make sure you have Java 1.6 or newer
2. Install [leiningen](https://github.com/technomancy/leiningen)
3. Checkout the histogram project with git
4. Run `lein jar`

# Basics

In the following examples we use [Incanter](http://incanter.org/) to
generate data and for charting.

The simplest way to use a histogram is to `create` one and then
`insert!` points.  In the example below, `ex/normal-data` refers to a
sequence of 100K samples from a normal distribution (mean 0, variance
1).

```clojure
user> (ns examples
        (:use [histogram.core])
        (:require (histogram.test [examples :as ex])))
examples> (def hist (reduce insert! (create) ex/normal-data))
```

You can use the `sum` fn to find the approximate number of points less
than a given threshold:

```clojure
examples> (sum hist 0)
50044.02331
```

The `density` fn gives us an estimate of the point density at the
given location:

```clojure
examples> (density hist 0)
39687.56279
```

The `uniform` fn returns a list of points that separate the
distribution into equal population areas.  Here's an example that
produces quartiles:

```clojure
examples> (uniform hist 4)
(-0.67234 -0.00111 0.67133)
```

We can plot the sums and density estimates as functions.  The red line
represents the sum, the blue line represents the density.  If we
normalized the values (dividing by 100K), these lines approximate the
[cumulative distribution
function](http://en.wikipedia.org/wiki/Cumulative_distribution_function)
and the [probability distribution
function](http://en.wikipedia.org/wiki/Probability_density_function)
for the normal distribution.

```clojure
examples> (ex/sum-density-chart hist) ;; also see (ex/cdf-pdf-chart hist)
```
![Histogram from normal distribution]
(https://img.skitch.com/20120427-jhrhpshfm6pppu3t3bu4kt9g7e.png)

The histogram approximates distributions using a constant number of
bins. This bin limit is a parameter when creating a histogram
(`:bins`, defaults to 64). A bin contains a `:count` of the points
within the bin along with the `:mean` for the values in the bin. The
edges of the bin aren't captured. Instead the histogram assumes that
points are distributed evenly with half the points less than the mean
and half greater. This explains the fraction sum in the example below:

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
red line represents a histogram with 16 bins and the blue line
represents 64 bins.

```clojure
examples> (ex/multi-pdf-chart
           [(reduce insert! (create :bins 16) ex/normal-data)
            (reduce insert! (create :bins 64) ex/normal-data)])
```
![64 and 32 bins histograms]
(https://img.skitch.com/20120719-e9hhw8hu6ye74b8stg1fh8tyhf.png)

Another option when creating a histogram is to use *gap
weighting*. When `:gap-weighted?` is true, the histogram is encouraged
to spend more of its bins capturing the densest areas of the
distribution. For the normal distribution that means better resolution
near the mean and less resolution near the tails. The chart below
shows a histogram without gap weighting in blue and with gap weighting
in red.  Near the center of the distribution, red uses six bins in
roughly the same space that blue uses three.

```clojure
examples> (ex/multi-pdf-chart
           [(reduce insert! (create :bins 16 :gap-weighted? true)
                    ex/normal-data)
            (reduce insert! (create :bins 16 :gap-weighted? false)
                    ex/normal-data)])
```
![Gap weighting vs. No gap weighting]
(https://img.skitch.com/20120719-e82yxgkph9te4fucc5yuktfy1m.png)

# Merging

A strength of the histograms is their ability to merge with one
another. Histograms can be built on separate data streams and then
combined to give a better overall picture.

```clojure
examples> (let [samples (partition 1000 ex/normal-data)
                hist1 (reduce insert! (create :bins 16) (first samples))
                hist2 (reduce insert! (create :bins 16) (second samples))
                merged (-> (create :bins 16)
                           (merge! hist1)
                           (merge! hist2))]
            (ex/multi-density-chart [hist1 hist2 merged]))
```
![Merged histograms]
(https://img.skitch.com/20120427-18ndb278u2bmep8aqq9bc3m7qk.png)

# Targets

While a simple histogram is nice for capturing the distribution of a
single variable, it's often important to capture the correlation
between variables. To that end, the histograms can track a second
variable called the *target*.

The target may be either numeric or categorical. The `insert!` fn is
overloaded to accept either type of target. Each histogram bin will
contain information summarizing the target. For numerics the targets
sums are tracked.  For categoricals a map of counts is maintained.

```clojure
examples> (-> (create)
              (insert! 1 9)
              (insert! 2 8)
              (insert! 3 7)
              (insert! 3 6)
              (bins))
({:target {:sum 9.0, :missing-count 0.0}, :mean 1.0, :count 1}
 {:target {:sum 8.0, :missing-count 0.0}, :mean 2.0, :count 1}
 {:target {:sum 13.0, :missing-count 0.0}, :mean 3.0, :count 2})
examples> (-> (create)
              (insert! 1 :a)
              (insert! 2 :b)
              (insert! 3 :c)
              (insert! 3 :d)
              (bins))
({:target {:counts {:a 1.0}, :missing-count 0.0}, :mean 1.0, :count 1}
 {:target {:counts {:b 1.0}, :missing-count 0.0}, :mean 2.0, :count 1}
 {:target {:counts {:d 1.0, :c 1.0}, :missing-count 0.0}, :mean 3.0, :count 2})
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
target value is the sine of the input (but scaled and shifted to make
plotting easier). The density is in red and the average target value
is in blue:

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
(https://img.skitch.com/20120719-tfjnabp7t7sanskf4iqecp751d.png)

Continuing with the same histogram, we can see that `average-target`
produces values close to original target:

```clojure
examples> (def view-target (fn [x] {:actual (make-y x)
                                    :approx (average-target hist x)}))
examples> (view-target 0)
{:actual 0.0, :approx {:sum -0.04696, :missing-count 0.0}}
examples> (view-target (/ Math/PI 2))
{:actual 1.0, :approx {:sum 0.99698, :missing-count 0.0}}
examples> (view-target Math/PI)
{:actual 1.22464E-16, :approx {:sum -0.04881, :missing-count 0.0}}
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
({:target ({:counts {:a 1.0, :d 1.0}, :missing-count 0.0}
           {:sum 6.0, :missing-count 1.0}),
  :mean 1.0, :count 2}
 {:target ({:counts {:b 1.0}, :missing-count 0.0}
           {:sum 8.0, :missing-count 0.0}),
  :mean 2.0, :count 1}
 {:target ({:counts {:c 1.0}, :missing-count 0.0}
           {:sum 7.0, :missing-count 0.0}),
  :mean 3.0, :count 1})
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
