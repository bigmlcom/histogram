package com.bigml.histogram;

import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class Bin implements Comparable<Bin> {

  public Bin(double mean, double count) {
    _mean = mean;
    _count = count;
  }
  
  public JSONArray toJSON(DecimalFormat format) {
    JSONArray jsonArray = new JSONArray();
    jsonArray.add(Double.valueOf(format.format(_mean)));
    jsonArray.add(Double.valueOf(format.format(_count)));
    return jsonArray;
  }
  
  public void setCount(double count){
    _count = count;
  }

  public double getCount() {
    return _count;
  }

  public double getMean() {
    return _mean;
  }

  public double getWeight() {
    return _mean * (double) _count;
  }

  @Override
  public int compareTo(Bin t) {
    return Double.compare(this.getMean(), t.getMean());
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Bin other = (Bin) obj;
    if (Double.doubleToLongBits(this._mean) != Double.doubleToLongBits(other._mean)) {
      return false;
    }
    if (Double.doubleToLongBits(this._count) != Double.doubleToLongBits(other._count)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 43 * hash + (int) (Double.doubleToLongBits(this._mean) ^ (Double.doubleToLongBits(this._mean) >>> 32));
    hash = 43 * hash + (int) (Double.doubleToLongBits(this._count) ^ (Double.doubleToLongBits(this._count) >>> 32));
    return hash;
  }

  public static Bin combine(Bin bin1, Bin bin2) {
    double totalCount = bin1.getCount() + bin2.getCount();
    double newMean = (bin1.getWeight() + bin2.getWeight()) / (double) totalCount;
    return new Bin(newMean, totalCount);
  }
  
  private final double _mean;
  private double _count;
}
