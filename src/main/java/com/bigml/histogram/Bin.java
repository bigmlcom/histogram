package com.bigml.histogram;

import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class Bin {

  public static Bin combine(Bin bin1, Bin bin2) throws MixedInsertException {
    double totalCount = bin1.getCount() + bin2.getCount();
    double newMean = (bin1.getWeight() + bin2.getWeight()) / (double) totalCount;

    Bin combinedBin;
    if (bin1.hasTarget() ^ bin2.hasTarget()) {
      throw new MixedInsertException();
    } else if (bin1.hasTarget() && bin2.hasTarget()) {
      double newTargetSum = bin1.getTargetSum() + bin2.getTargetSum();
      combinedBin = new Bin(newMean, totalCount, newTargetSum);
    } else {
      combinedBin = new Bin(newMean, totalCount);
    }
    return combinedBin;
  }

  public Bin(double mean, double count) {
    this(mean, count, null);
  }

  public Bin(double mean, double count, Double targetSum) {
    _mean = mean;
    _count = count;
    _targetSum = targetSum;
  }

  public JSONArray toJSON(DecimalFormat format) {
    JSONArray jsonArray = new JSONArray();
    jsonArray.add(Double.valueOf(format.format(_mean)));
    jsonArray.add(Double.valueOf(format.format(_count)));
    if (_targetSum != null) {
      jsonArray.add(Double.valueOf(format.format(_targetSum)));
    }
    return jsonArray;
  }

  public void setCount(double count) {
    _count = count;
  }

  public double getCount() {
    return _count;
  }

  public double getMean() {
    return _mean;
  }
  
  public Double getTargetSum() {
    return _targetSum;
  }
  
  public boolean hasTarget() {
    return _targetSum != null;
  }

  public double getWeight() {
    return _mean * (double) _count;
  }

  @Override
  public String toString() {
    return toJSON(Histogram.DEFAULT_DECIMAL_FORMAT).toJSONString();
  }
  private final double _mean;
  private double _count;
  private Double _targetSum;
}
