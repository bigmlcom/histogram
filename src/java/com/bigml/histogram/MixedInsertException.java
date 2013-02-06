/**
 * Copyright 2013 BigML
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.bigml.histogram;

public class MixedInsertException extends Exception {

  public MixedInsertException() {
    super("Can't mix insert types");
  }
}
