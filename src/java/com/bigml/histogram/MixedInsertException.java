package com.bigml.histogram;

public class MixedInsertException extends Exception {

  public MixedInsertException() {
    super("Can't mix insert types");
  }
}
