package com.bigml.histogram;

import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class SimpleTarget extends Target<SimpleTarget> {
  public static final SimpleTarget TARGET = new SimpleTarget();

  @Override
  protected void addJSON(JSONArray binJSON, DecimalFormat format) {
  }
  
  @Override
  protected SimpleTarget init() {
    return this;
  }
  
  @Override
  protected SimpleTarget clone() {
    return this;
  }
  
  @Override
  protected SimpleTarget sum(SimpleTarget bin) {
    return this;
  }

  @Override
  protected SimpleTarget mult(double multiplier) {
    return this;
  }
  
  private SimpleTarget() {}
}
