/**
 * Copyright 2013 BigML
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.bigml.histogram;

import com.bigml.histogram.Histogram.TargetType;
import java.io.IOException;
import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public abstract class Target<T extends Target> {

  public abstract double getMissingCount();
  public abstract TargetType getTargetType();
  
  protected abstract void addJSON(JSONArray binJSON, DecimalFormat format);
  protected abstract void appendTo(Appendable appendable, DecimalFormat format) throws IOException;
  protected abstract T sum(T target);
  protected abstract T mult(double multiplier);

  @Override
  protected abstract T clone();
  protected abstract T init();
}
