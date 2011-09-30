package com.bigml.histogram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
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
    double expectedCount = (double) pointCount / 2d;
    Assert.assertTrue(Math.abs(count - expectedCount) < 0.01 * expectedCount);
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
    
    double split = (Double) hist.uniform(2).get(0);
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
  public void findBestSplit() throws MixedInsertException, SumOutOfRangeException {
    long pointCount = 100000;
    int histogramBins = 100;
    int candidateSplitSize = 100;
    double actualSplitPoint = 0.75;

    Random random = new Random(0);
    Histogram<NumericTarget> hist = new Histogram(histogramBins);
    
    for (long i = 0; i < pointCount; i++) {
      double point = random.nextDouble();
      if (point < actualSplitPoint) {
        hist.insert(point, random.nextGaussian());
      } else {
        hist.insert(point, random.nextGaussian() + 0.1d);
      }
    }

    double totalCount = hist.getTotalCount();
    double totalTargetSum = hist.getTotalTargetSum().getTarget();

    double bestSplit = Double.MAX_VALUE;
    double bestScore = Double.MAX_VALUE;

    ArrayList<Double> candidateSplits = hist.uniform(candidateSplitSize);

    for (double candidateSplit : candidateSplits) {
      SumResult<NumericTarget> result = hist.extendedSum(candidateSplit);
      double l = result.getTargetSum().getTarget();
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
  public void mixedInserts() throws MixedInsertException, SumOutOfRangeException {
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
    
    Histogram hist3 = new Histogram(10);
    hist3.insert(1, 2);
    try {
      hist3.insert(1, "foo");
      Assert.fail("Should throw MixedInsertException");
    } catch (MixedInsertException e) {
    }
  }
  
  @Test
  public void categoricalTargets() throws MixedInsertException, SumOutOfRangeException {
        
    int points = 100000;
    
    Random random = new Random(0);
    Histogram hist = new Histogram(100);
    
    for (int i = 0; i < points; i++) {
      double point = 6 * random.nextDouble() - 3;

      String fruit;
      if (point < -1) {
        fruit = "apple";
      } else if (point < 1) {
        fruit = "pear";
      } else {
        fruit = "grape";
      }
      
      hist.insert(point, fruit);
    }
    
    CategoricalTarget target = (CategoricalTarget) hist.extendedSum(0).getTargetSum();
    HashMap<String,Double> targetCounts = target.getTargetCounts();
    
    double apples = targetCounts.get("apple");
    double expectedApples = (double) points / 3d;
    Assert.assertTrue(Math.abs(apples - expectedApples) < 0.01 * expectedApples);

    double pears = targetCounts.get("pear");
    double expectedPears = (double) points / 6d;
    Assert.assertTrue(Math.abs(pears - expectedPears) < 0.01 * expectedPears);

    Assert.assertNull(targetCounts.get("grape"));
  }
}
