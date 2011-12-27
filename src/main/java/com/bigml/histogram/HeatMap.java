package com.bigml.histogram;

import java.util.Collection;

public class HeatMap {

  public HeatMap() {
    this(128, 128);
  }

  public HeatMap(int xHistSize, int yHistSize) {
    _hist = new Histogram<SimpleHistogramTarget>(xHistSize);
    _mainHistSize = xHistSize;
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

  public double density(double x, double y) {
    SumResult<SimpleHistogramTarget> xResult = _hist.extendedDensity(x);
    if (xResult.getTargetSum() == null) {
      return 0;
    } else {
      return xResult.getTargetSum().getTarget().density(y);
    }
  }

  public double density(Number x, Number y) {
    return density(x.doubleValue(), y.doubleValue());
  }

  public Histogram<SimpleHistogramTarget> getHistogram() {
    return _hist;
  }

  public HeatMap merge(HeatMap heatMap) {
    try {
      _hist.merge(heatMap.getHistogram());
    } catch (MixedInsertException ex) {
    }
    return this;
  }
  
  public static HeatMap merge(Collection<HeatMap> heatMaps) {
    int maxMainHist = -Integer.MAX_VALUE;
    int maxTargetHist = -Integer.MAX_VALUE;
    for (HeatMap hm : heatMaps) {
      maxMainHist = Math.max(maxMainHist, hm._mainHistSize);
      maxTargetHist = Math.max(maxTargetHist, hm._targetHistSize);
    }

    Histogram<SimpleHistogramTarget> tempHist =
            new Histogram<SimpleHistogramTarget>(heatMaps.size() * maxMainHist);

    Histogram<SimpleHistogramTarget> newHist = null;
    try {
      for (HeatMap hm : heatMaps) {
        tempHist.merge(hm.getHistogram());
      }

      newHist = new Histogram<SimpleHistogramTarget>(maxMainHist);
      newHist.merge(tempHist);
    } catch (MixedInsertException ex) {
    }

    return new HeatMap(maxMainHist, maxTargetHist, newHist);
  }

  private HeatMap(int mainHistSize, int targetHistSize, Histogram<SimpleHistogramTarget> hist) {
    _hist = hist;
    _mainHistSize = mainHistSize;
    _targetHistSize = targetHistSize;
  }
  private final Histogram<SimpleHistogramTarget> _hist;
  private final int _mainHistSize;
  private final int _targetHistSize;
}
