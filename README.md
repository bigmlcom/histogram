# About

This project is an implementation of the streaming, one-pass
histograms described in Ben-Haim's [Streaming Parallel Decision
Trees](http://jmlr.csail.mit.edu/papers/v11/ben-haim10a.html). The
histogram includes the extension added by Tyree's [Parallel Boosted
Regression Trees]
(http://research.engineering.wustl.edu/~tyrees/Publications_files/fr819-tyreeA.pdf)
which allows the histogram to include numeric targets (useful for
regression trees). The histogram follows a similar approach to support
categorical targets (useful for classification trees).

The histograms act as an approximation of the underlying dataset.
They can be used for learning, visualization, discretization, or
analysis.  This includes finding the median or any other percentile in
one pass.  The histograms may be built independently and merged,
convenient for parallel and distributed algorithms.

# Building

1. Make sure you have Java 1.6 or newer
2. Install [leiningen](https://github.com/technomancy/leiningen)
3. Checkout the histogram project with git
4. Run `lein jar`

# Example

```java
long pointCount = 100000;
int histogramBins = 100;
Random random = new Random();
Histogram hist = new Histogram(histogramBins);

for (long i = 0; i < pointCount; i++) {
  hist.insert(random.nextGaussian());
}

//the sum at 0 should be about 50000
double sum = hist.sum(0);

//the split point between two uniform (by population) bins should be about 0
//this is an approximate median
double split = hist.uniform(2).get(0);
```

```clojure
(let [data (repeatedly 100000 #(rand))
      hist (reduce insert! (create) data)]
  (median hist))
```

# Performance

Insert time scales `log(n)` with respect to the number of bins in the
histogram.

![timing chart]
(https://docs.google.com/spreadsheet/oimg?key=0Ah2oAcudnjP4dG1CLUluRS1rcHVqU05DQ2Z4UVZnbmc&oid=2&zx=mppmmoe214jm)
