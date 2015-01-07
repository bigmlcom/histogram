/**
 * Copyright 2013 BigML
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.bigml.histogram;

import java.io.Serializable;
import java.util.Map;

public interface CategoricalTarget extends Serializable {
  public Map<Object, Double> getCounts();
}
