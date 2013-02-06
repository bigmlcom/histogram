/**
 * Copyright 2013 BigML
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.bigml.histogram;

import com.bigml.histogram.Histogram.TargetType;
import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class SimpleHistogramTarget extends Target<SimpleHistogramTarget> {
  
  public SimpleHistogramTarget(int maxBins) {
    _maxBins = maxBins;
    _target = new Histogram<SimpleTarget>(maxBins, true);
  }
  
  public SimpleHistogramTarget(int maxBins, double target) {
    this(maxBins);
    try {
      _target.insert(target);
    } catch (MixedInsertException ex) {
    }
  }

  public SimpleHistogramTarget(int maxBins, Histogram<SimpleTarget> simpleHist) {
    _maxBins = maxBins;
    _target = simpleHist;
  }
  
  public Histogram<SimpleTarget> getTarget() {
    return _target;
  }

    /* Missing values not allowed for SimpleHistogramTarget */
  @Override
  public double getMissingCount() {
    return 0;
  }

  @Override
  public TargetType getTargetType() {
    return Histogram.TargetType.histogram;
  }

  @Override
  public String toString() {
    return _target.toString();
  }
  
  @Override
  protected void addJSON(JSONArray binJSON, DecimalFormat format) {
    binJSON.add(_target.toJSON(format));
  }
  
  @Override
  protected SimpleHistogramTarget init() {
    return new SimpleHistogramTarget(_maxBins, new Histogram<SimpleTarget>(_maxBins, true));
  }

  @Override
  protected SimpleHistogramTarget clone() {
    Histogram<SimpleTarget> newHist = new Histogram<SimpleTarget>(_maxBins, true);
    for (Bin bin : _target.getBins()) {
      newHist.insertBin(new Bin(bin));
    }
    return new SimpleHistogramTarget(_maxBins, newHist);
  }

  @Override
  protected SimpleHistogramTarget sum(SimpleHistogramTarget target) {
    try {
      _target.merge(target.getTarget());
    } catch (MixedInsertException ex) {
    }
    return this;
  }
  
  @Override
  protected SimpleHistogramTarget mult(double multiplier) {
    for (Bin bin : _target.getBins()) {
      try {
        bin.update(new Bin(bin.getMean(), multiplier * bin.getCount(), SimpleTarget.TARGET));
      } catch (BinUpdateException ex) {
      }
    }
    return this;
  }
  
  private Histogram<SimpleTarget> _target;
  private int _maxBins;
}
