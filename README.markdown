# About

This project is an implementation of the streaming, one-pass histograms described in Ben-Haim's [Streaming Parallel Decision Trees](http://jmlr.csail.mit.edu/papers/v11/ben-haim10a.html).

The histograms act as an approximation of the underlying dataset.  They can be used for visualization, discretization, or analysis.  This includes finding the median or any other percentile in one pass.  The histograms may be built independently and combined, making them a good fit for map-reduce algorithms.



