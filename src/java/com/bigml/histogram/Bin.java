package com.bigml.histogram;

import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class Bin<T extends Target> {

  public Bin(double mean, double count, T target) {
    /* Hack to avoid Java's negative zero */
    if (mean == 0d) {
      _mean = 0d;
    } else {
      _mean = mean;
    }
    _count = count;
    _target = target;
  }

  public Bin(Bin<T> bin) {
    this(bin.getMean(), bin.getCount(), (T) bin.getTarget().clone());
  }

  public JSONArray toJSON(DecimalFormat format) {
    JSONArray binJSON = new JSONArray();
    binJSON.add(Double.valueOf(format.format(_mean)));
    binJSON.add(Double.valueOf(format.format(_count)));
    _target.addJSON(binJSON, format);
    return binJSON;
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

  public T getTarget() {
    return _target;
  }

  public void sumUpdate(Bin bin) {
    _count += bin.getCount();
    _target.sum(bin.getTarget());
  }

  public void update(Bin bin) throws BinUpdateException {
    if (_mean != bin.getMean()) {
      throw new BinUpdateException("Bins must have matching means to update");
    }

    _count = bin.getCount();
    _target = (T) bin.getTarget();
  }

  @Override
  public String toString() {
    return toJSON(new DecimalFormat(Histogram.DEFAULT_FORMAT_STRING)).toJSONString();
  }

  public Bin combine(Bin<T> bin) {
    double count = getCount() + bin.getCount();
    double mean = (getWeight() + bin.getWeight()) / (double) count;
    T newTarget = (T) _target.init();
    newTarget.sum(_target);
    newTarget.sum(bin.getTarget());
    return new Bin<T>(mean, count, newTarget);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Bin<T> other = (Bin<T>) obj;
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
    hash = 71 * hash + (int) (Double.doubleToLongBits(this._mean) ^ (Double.doubleToLongBits(this._mean) >>> 32));
    hash = 71 * hash + (int) (Double.doubleToLongBits(this._count) ^ (Double.doubleToLongBits(this._count) >>> 32));
    return hash;
  }

  private T _target;
  private final double _mean;
  private double _count;

}
