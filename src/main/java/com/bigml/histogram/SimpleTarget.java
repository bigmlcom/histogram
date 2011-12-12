package com.bigml.histogram;

import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public class SimpleTarget extends Target<SimpleTarget> {

  @Override
  protected void addJSON(JSONArray binJSON, DecimalFormat format) {
  }

  @Override
  protected SimpleTarget combine(SimpleTarget target) {
    return target;
  }
  
  @Override
  protected SimpleTarget init() {
    return new SimpleTarget();
  }
  
  @Override
  protected SimpleTarget clone() {
    return new SimpleTarget();
  }
  
  @Override
  protected SimpleTarget sumUpdate(SimpleTarget bin) {
    return this;
  }

  @Override
  protected SimpleTarget subtractUpdate(SimpleTarget target) {
    return this;
  }

  @Override
  protected SimpleTarget multiplyUpdate(double multiplier) {
    return this;
  }
}
