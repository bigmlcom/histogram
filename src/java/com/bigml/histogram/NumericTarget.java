/**
 * Copyright 2013 BigML
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.bigml.histogram;

import com.bigml.histogram.Histogram.TargetType;
import java.io.IOException;
import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class NumericTarget extends Target<NumericTarget> {

  public NumericTarget(Double target, Double sumSquares, double missingCount) {
    _sum = target;
    _sumSquares = sumSquares;
    _missingCount = missingCount;
  }

  public NumericTarget(Double target, double missingCount) {
    _sum = target;
    if (target != null) {
      _sumSquares = target * target;
    }
    _missingCount = missingCount;
  }

  public NumericTarget(Double target) {
    this(target, target == null ? 1 : 0);
  }

  public Double getSum() {
    return _sum;
  }

  public Double getSumSquares() {
    return _sumSquares;
  }

  @Override
  public double getMissingCount() {
    return _missingCount;
  }

  @Override
  public TargetType getTargetType() {
    return Histogram.TargetType.numeric;
  }

  @Override
  public String toString() {
    return String.valueOf(_sum) + "," + String.valueOf(_sumSquares);
  }

  @Override
  protected void addJSON(JSONArray binJSON, DecimalFormat format) {
    if (_sum == null) {
      binJSON.add(null);
    } else {
      binJSON.add(Utils.roundNumber(_sum, format));
      binJSON.add(Utils.roundNumber(_sumSquares, format));
    }
  }

  @Override
  protected void appendTo(final Appendable appendable, final DecimalFormat format) throws IOException {
    if (appendable == null) {
      throw new NullPointerException("appendable must not be null");
    }
    if (format == null) {
      throw new NullPointerException("format must not be null");
    }
    if (_sum != null) {
      appendable.append(format.format(_sum));
      appendable.append("\t");
      appendable.append(format.format(_sumSquares));
    }
  }

  @Override
  protected NumericTarget init() {
    return new NumericTarget(0d);
  }

  @Override
  protected NumericTarget clone() {
    return new NumericTarget(_sum, _sumSquares, _missingCount);
  }

  private Double _sum;
  private Double _sumSquares;
  private double _missingCount;

  @Override
  protected NumericTarget sum(NumericTarget target) {
    if (_sum == null && target.getSum() != null) {
      _sum = target.getSum();
      _sumSquares = target.getSumSquares();
    } else if (_sum != null && target.getSum() != null){
      _sum += target.getSum();
      _sumSquares += target.getSumSquares();
    }
    _missingCount += target.getMissingCount();
    return this;
  }

  @Override
  protected NumericTarget mult(double multiplier) {
    if (_sum != null) {
      _sum *= multiplier;
      _sumSquares *= multiplier;
    }
    _missingCount *= multiplier;
    return this;
  }
}
