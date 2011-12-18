package com.bigml.histogram;

import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class NumericTarget extends Target<NumericTarget> {
  
  public NumericTarget(double target) {
    _target = target;
  }

  public NumericTarget(Number target) {
    _target = target.doubleValue();
  }

  public double getTarget() {
    return _target;
  }
  
  @Override
  protected void addJSON(JSONArray binJSON, DecimalFormat format) {
    binJSON.add(Double.valueOf(format.format(_target)));
  }

  @Override
  protected NumericTarget combine(NumericTarget target) {
    double sum = getTarget() + target.getTarget();
    return new NumericTarget(sum);
  }
  
  @Override
  protected NumericTarget init() {
    return new NumericTarget(0d);
  }

  @Override
  protected NumericTarget clone() {
    return new NumericTarget(_target);
  }

  private double _target;

  @Override
  protected NumericTarget sumUpdate(NumericTarget target) {
    this._target += target.getTarget();
    return this;
  }
  
  @Override
  protected NumericTarget subtractUpdate(NumericTarget target) {
    this._target -= target.getTarget();
    return this;
  }

  @Override
  protected NumericTarget multiplyUpdate(double multiplier) {
    _target *= multiplier;
    return this;
  }

}
