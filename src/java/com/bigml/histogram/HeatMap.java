/**
 * Copyright 2013 BigML
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.bigml.histogram;

/**
 * An experimental implementation of a dynamic heat map using
 * nested histograms.
 *
 * @author Adam Ashenfelter (ashenfelter@bigml.com)
 */
public class HeatMap {

  public HeatMap() {
    this(64, 64);
  }

  public HeatMap(int xHistSize, int yHistSize) {
    _hist = new Histogram<SimpleHistogramTarget>(xHistSize, true);
    _targetHistSize = yHistSize;
  }

  public HeatMap insert(double x, double y) {
    SimpleHistogramTarget target = new SimpleHistogramTarget(_targetHistSize, y);
    _hist.insertBin(new Bin<SimpleHistogramTarget>(x, 1, target));
    return this;
  }

  public double density(double x, double y) {
    SumResult<SimpleHistogramTarget> xResult = _hist.extendedDensity(x);
    if (xResult.getTargetSum() == null) {
      return 0;
    } else {
      return xResult.getTargetSum().getTarget().density(y);
    }
  }

  public Histogram<SimpleHistogramTarget> getHistogram() {
    return _hist;
  }

  public HeatMap merge(HeatMap heatMap) {
    try {
      _hist.merge(heatMap.getHistogram());
    } catch (MixedInsertException ex) {
    }
    return this;
  }
  
  private final Histogram<SimpleHistogramTarget> _hist;
  private final int _targetHistSize;
}
