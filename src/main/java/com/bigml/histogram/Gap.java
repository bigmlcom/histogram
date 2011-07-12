package com.bigml.histogram;

public class Gap implements Comparable<Gap> {

  public Gap(double space, Bin startBin, Bin endBin) {
    _space = space;
    _startBin = startBin;
    _endBin = endBin;
  }

  public Bin getStartBin() {
    return _startBin;
  }
  
  public Bin getEndBin() {
    return _endBin;
  }

  public double getSpace() {
    return _space;
  }
  
  @Override
  public int compareTo(Gap t) {
    return Double.compare(this.getSpace(), t.getSpace());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Gap other = (Gap) obj;
    if (Double.doubleToLongBits(this._space) != Double.doubleToLongBits(other._space)) {
      return false;
    }
    if (this._startBin != other._startBin && (this._startBin == null || !this._startBin.equals(other._startBin))) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 23 * hash + (int) (Double.doubleToLongBits(this._space) ^ (Double.doubleToLongBits(this._space) >>> 32));
    hash = 23 * hash + (this._startBin != null ? this._startBin.hashCode() : 0);
    return hash;
  }
  
  private final double _space;
  private final Bin _startBin;
  private final Bin _endBin;
}
