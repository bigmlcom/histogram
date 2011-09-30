package com.bigml.histogram;

public class Gap<T extends Target> implements Comparable<Gap> {

  public Gap(double space, Bin<T> startBin, Bin<T> endBin) {
    _space = space;
    _startBin = startBin;
    _endBin = endBin;
  }

  public Bin<T> getStartBin() {
    return _startBin;
  }
  
  public Bin<T> getEndBin() {
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
  private final Bin<T> _startBin;
  private final Bin<T> _endBin;
}
