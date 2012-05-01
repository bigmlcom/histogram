package com.bigml.histogram;

import com.bigml.histogram.Histogram.TargetType;
import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class NumericTarget extends Target<NumericTarget> {
  
  public NumericTarget(Double target, double missingCount) {
    _target = target;
    _missingCount = missingCount;
  }
  
  public NumericTarget(Double target) {
    this(target, target == null ? 1 : 0);
  }

  public Double getTarget() {
    return _target;
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
    return String.valueOf(_target);
  }

  @Override
  protected void addJSON(JSONArray binJSON, DecimalFormat format) {
    if (_target == null) {
      binJSON.add(null);
    } else {
      binJSON.add(Double.valueOf(format.format(_target)));
    }
  }
  
  @Override
  protected NumericTarget init() {
    return new NumericTarget(0d);
  }

  @Override
  protected NumericTarget clone() {
    return new NumericTarget(_target, _missingCount);
  }

  private Double _target;
  private double _missingCount;

  @Override
  protected NumericTarget sum(NumericTarget target) {
    if (_target == null && target.getTarget() != null) {
      _target = target.getTarget();
    } else if (_target != null && target.getTarget() != null){
      this._target += target.getTarget();
    }
    _missingCount += target.getMissingCount();
    return this;
  }
  
  @Override
  protected NumericTarget mult(double multiplier) {
    if (_target != null) {
      _target *= multiplier;
    }
    _missingCount *= multiplier;
    return this;
  }
}
