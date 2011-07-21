package com.bigml.histogram;

public class MixedInsertException extends Exception {

  public MixedInsertException() {
    super("Can't mix insert types (two value bins and three value bins)");
  }
}
