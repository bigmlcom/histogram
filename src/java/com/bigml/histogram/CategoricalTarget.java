package com.bigml.histogram;

import java.util.Map;

public interface CategoricalTarget {
  public Map<Object, Double> getCounts();
}
