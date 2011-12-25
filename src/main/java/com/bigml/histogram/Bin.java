package com.bigml.histogram;

import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class Bin<T extends Target> {
    
  public Bin(double mean, double count, T target) {
    _mean = mean;
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

  public void update(Bin bin) throws BinUpdateException {
    if (_mean != bin.getMean()) {
      throw new BinUpdateException("Bins must have matching means to update");
    }
    
    _count += bin.getCount();
    _target.sumUpdate(bin.getTarget());
  }

  @Override
  public String toString() {
    return toJSON(new DecimalFormat(Histogram.DEFAULT_FORMAT_STRING)).toJSONString();
  }
  
  public Bin combine(Bin<T> bin) {
    double count = getCount() + bin.getCount();
    double mean = (getWeight() + bin.getWeight()) / (double) count;
    T newTarget = (T) _target.init();
    newTarget.sumUpdate(_target);
    newTarget.sumUpdate(bin.getTarget());
    return new Bin<T>(mean, count, newTarget);
  }
    
  private T _target;
  private final double _mean;
  private double _count;

}
