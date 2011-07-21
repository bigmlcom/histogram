package com.bigml.histogram;

public class SumResult {
  public SumResult(double count, Double targetSum) {
    _count = count;
    _targetSum = targetSum;
  }
  
  public double getCount() {
    return _count;
  }
  
  public Double getTargetSum() {
    return _targetSum;
  }
  
  private final double _count;
  private final Double _targetSum;
}
