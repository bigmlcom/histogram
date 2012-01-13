package com.bigml.histogram;

import java.util.Random;
import org.junit.Assert;
import org.junit.Test;

public class HeatMapTest {

  @Test
  public void heatTest() throws MixedInsertException, SumOutOfRangeException {
    Random random = new Random();
    int points = 20000;
    HeatMap heatMap = new HeatMap(128, 128);
    
    for (int i = 0; i < points; i++) {
      heatMap.insert(random.nextGaussian(), random.nextGaussian());
    }

    Assert.assertTrue(heatMap.density(0, 0) > heatMap.density(-1, -1));
    Assert.assertTrue(heatMap.density(0, 0) > heatMap.density(1, 1));
    
    SumResult<SimpleHistogramTarget> sum = heatMap.getHistogram().extendedSum(0);

    double expectedSumX = points / 2;
    double actualSumX = sum.getCount();

    double expectedSumY = expectedSumX / 2;
    double actualSumY = sum.getTargetSum().getTarget().extendedSum(0).getCount();

    Assert.assertTrue(Math.abs(expectedSumX - actualSumX) < 0.02 * expectedSumX);
    Assert.assertTrue(Math.abs(expectedSumY - actualSumY) < 0.04 * expectedSumY);
  }
}
