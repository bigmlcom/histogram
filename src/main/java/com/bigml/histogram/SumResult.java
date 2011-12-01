package com.bigml.histogram;

import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class SumResult<T extends Target> {
  public SumResult(double count, T targetSum) {
    _count = count;
    _targetSum = targetSum;
  }
  
  public double getCount() {
    return _count;
  }
  
  public T getTargetSum() {
    return _targetSum;
  }
  
  public JSONArray toJSON(DecimalFormat format) {
    JSONArray jsonArray = new JSONArray();
    jsonArray.add(Double.valueOf(format.format(_count)));
    _targetSum.addJSON(jsonArray, format);
    return jsonArray;
  }
  
  @Override
  public String toString() {
    return toJSON(new DecimalFormat(Histogram.DEFAULT_FORMAT_STRING)).toString();
  }
  
  private final double _count;
  private final T _targetSum;
}
