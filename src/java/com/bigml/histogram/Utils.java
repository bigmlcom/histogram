/**
 * Copyright 2013 BigML
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package com.bigml.histogram;

import java.text.DecimalFormat;
import java.text.ParseException;

public class Utils {

  public static Number roundNumber(Number number, DecimalFormat format) {
    /*
     * DecimalFormat.format() produces an IllegalArgumentException if the 
     * input cannot be formatted. We ignore ParseExceptions expecting that
     * any number which can be formatted can also be parsed by DecimalFormat.
     */
    Number formattedNumber = null;
    try {
      formattedNumber = format.parse(format.format(number));
    } catch (ParseException ex) {
    }
    return formattedNumber;
  }
}
