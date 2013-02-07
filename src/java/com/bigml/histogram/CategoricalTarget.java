/**
 * Copyright 2013 BigML
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.bigml.histogram;

import java.util.Map;

public interface CategoricalTarget {
  public Map<Object, Double> getCounts();
}
