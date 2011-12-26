package com.bigml.histogram;

public class HeatMap {
  
  public HeatMap() {
    this(128, 128);
  }
  
  public HeatMap(int xHistSize, int yHistSize) {
    _hist = new Histogram<SimpleHistogramTarget>(xHistSize);
    _targetHistSize = yHistSize;
  }
  
  public HeatMap insert(double x, double y) {
    SimpleHistogramTarget target = new SimpleHistogramTarget(_targetHistSize, y);
    _hist.insert(new Bin<SimpleHistogramTarget>(x, 1, target));
    return this;
  }
  
  public HeatMap insert(Number x, Number y) {
    return insert(x.doubleValue(), y.doubleValue());
  }

  public Histogram<SimpleHistogramTarget> getHistogram() {
    return _hist;
  }
  
  private final Histogram<SimpleHistogramTarget> _hist;
  private final int _targetHistSize;
}
