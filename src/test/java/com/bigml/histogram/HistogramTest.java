package com.bigml.histogram;

import java.util.ArrayList;
import java.util.Random;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

public class HistogramTest {

  @Test
  public void sum() throws SumOutOfRangeException, MixedInsertException {
    long pointCount = 100000;
    int histogramBins = 100;
    Random random = new Random(0);
    Histogram hist = new Histogram(histogramBins);

    for (long i = 0; i < pointCount; i++) {
      hist.insert(random.nextDouble());
    }

    double count = hist.sum(0.5);
    Assert.assertTrue(count > (0.49 * pointCount) && count < (0.51 * pointCount));
  }

  @Test
  public void uniform() throws SumOutOfRangeException, MixedInsertException {
    long pointCount = 100000;
    int histogramBins = 100;
    Random random = new Random(0);
    Histogram hist = new Histogram(histogramBins);

    for (long i = 0; i < pointCount; i++) {
      hist.insert(random.nextGaussian());
    }

    double split = hist.uniform(2).get(0);
    Assert.assertTrue(split > -0.01 && split < 0.01);
  }

  @Test
  public void merge() throws Exception {
    long pointCount = 100000;
    int histogramBins = 100;
    Random random = new Random(0);
    Histogram hist1 = new Histogram(histogramBins);
    Histogram hist2 = new Histogram(histogramBins);
    Histogram hist3 = new Histogram(histogramBins);

    for (long i = 0; i < pointCount; i++) {
      hist1.insert(random.nextGaussian());
      hist2.insert(random.nextGaussian());
      hist3.insert(random.nextGaussian());
    }

    hist1.mergeHistogram(hist2);
    hist1.mergeHistogram(hist3);
    Assert.assertTrue(hist1.getBins().size() == histogramBins);

    double lessThanZeroSum = hist1.sum(0);
    Assert.assertTrue((lessThanZeroSum > 149000 && lessThanZeroSum < 151000));
  }

  @Test
  public void standardJsonConversion() throws SumOutOfRangeException, ParseException, MixedInsertException {
    long pointCount = 100000;
    int histogramBins = 100;
    Random random = new Random(0);
    Histogram hist = new Histogram(histogramBins);

    for (long i = 0; i < pointCount; i++) {
      hist.insert(random.nextDouble());
    }

    Histogram testHist = Histogram.parseHistogramJSON(hist.toString());

    double count = testHist.sum(0.5);
    Assert.assertTrue(count > (0.49 * pointCount) && count < (0.51 * pointCount));
  }

  @Test
  public void extendedJsonConversion() throws SumOutOfRangeException, ParseException, MixedInsertException {
    long pointCount = 100000;
    int histogramBins = 100;
    Random random = new Random(0);
    Histogram hist = new Histogram(histogramBins);

    for (long i = 0; i < pointCount; i++) {
      hist.insert(random.nextDouble(), 1);
    }

    Histogram testHist = Histogram.parseHistogramJSON(hist.toString());

    SumResult result = testHist.extendedSum(0.5);
    Assert.assertTrue(result.getCount() > (0.49 * pointCount) && result.getCount() < (0.51 * pointCount));
    Assert.assertTrue(result.getTargetSum() > (0.49 * pointCount) && result.getTargetSum() < (0.51 * pointCount));
  }

  @Test
  public void findBestSplit() throws MixedInsertException, SumOutOfRangeException {
    long pointCount = 100000;
    int histogramBins = 100;
    int candidateSplitSize = 100;
    double actualSplitPoint = 0.75;

    Random random = new Random(0);
    Histogram hist = new Histogram(histogramBins);

    for (long i = 0; i < pointCount; i++) {
      double point = random.nextDouble();
      if (point < actualSplitPoint) {
        hist.insert(point, random.nextGaussian());
      } else {
        hist.insert(point, random.nextGaussian() + 0.1d);
      }
    }

    double totalCount = hist.getTotalCount();
    double totalTargetSum = hist.getTotalTargetSum();

    double bestSplit = Double.MAX_VALUE;
    double bestScore = Double.MAX_VALUE;

    ArrayList<Double> candidateSplits = hist.uniform(candidateSplitSize);

    for (double candidateSplit : candidateSplits) {
      SumResult result = hist.extendedSum(candidateSplit);
      double l = result.getTargetSum();
      double m = result.getCount();
      double leftSplit = Math.pow(l, 2) / m;
      double rightSplit = Math.pow((totalTargetSum - l), 2) / (totalCount - m);
      double score = -leftSplit - rightSplit;
      if (score < bestScore) {
        bestScore = score;
        bestSplit = candidateSplit;
      }
    }

    Assert.assertTrue(bestSplit > (0.99 * actualSplitPoint) && bestSplit < (1.01 * actualSplitPoint));
  }

  @Test
  public void mixedInserts() throws MixedInsertException {
    Histogram hist1 = new Histogram(10);
    hist1.insert(1);
    try {
      hist1.insert(1, 2);
      Assert.fail("Should throw MixedInsertException");
    } catch (MixedInsertException e) {
    }

    Histogram hist2 = new Histogram(10);
    hist2.insert(1, 2);
    try {
      hist2.insert(1);
      Assert.fail("Should throw MixedInsertException");
    } catch (MixedInsertException e) {
    }
  }
}
