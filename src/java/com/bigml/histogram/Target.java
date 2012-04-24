package com.bigml.histogram;

import com.bigml.histogram.Histogram.TargetType;
import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public abstract class Target<T extends Target> {

  public abstract double getMissingCount();
  public abstract TargetType getTargetType();
  
  protected abstract void addJSON(JSONArray binJSON, DecimalFormat format);
  protected abstract T sum(T target);
  protected abstract T mult(double multiplier);

  @Override
  protected abstract T clone();
  protected abstract T init();
}
