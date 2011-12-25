package com.bigml.histogram;

import java.text.DecimalFormat;
import org.json.simple.JSONArray;

public abstract class Target<T extends Target> {

  protected abstract void addJSON(JSONArray binJSON, DecimalFormat format);
  protected abstract T sumUpdate(T target);
  protected abstract T subtractUpdate(T target);
  protected abstract T multiplyUpdate(double denominator);

  @Override
  protected abstract T clone();
  protected abstract T init();
}
