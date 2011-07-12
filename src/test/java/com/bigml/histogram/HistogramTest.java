package com.bigml.histogram;

import com.bigml.histogram.Histogram.SumOutOfRange;
import java.util.Random;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

public class HistogramTest {

  @Test
  public void sum() throws SumOutOfRange {
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
  public void uniform() throws SumOutOfRange {
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
  public void jsonConversion() throws SumOutOfRange, ParseException {
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
}
