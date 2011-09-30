package com.bigml.histogram;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.simple.JSONArray;

/**
 * Implements a Histogram as defined by the <a href="http://jmlr.csail.mit.edu/papers/v11/ben-haim10a.html">
 * Streaming Parallel Decision Tree (SPDT)</a> algorithm.
 * 
 * <p>The Histogram consumes numeric points and maintains a running approximation of the dataset using
 * the given number of bins.  The methods <code>insert</code>, <code>sum</code>, and 
 * <code>uniform</code> are described in detail in the SPDT paper.  The method 
 * <code>createConstantWidthBins</code> is a convenience method for transforming the histogram into
 * evenly sized bins for visualization.
 * 
 * <p>The histogram has an <code>insert</code> method which uses two parameters and an 
 * <code>extendedSum</code> method which add the capabilities described in
 * <a href="http://research.engineering.wustl.edu/~tyrees/Publications_files/fr819-tyreeA.pdf">
 * Tyree's paper</a>.
 * 
 * @author Adam Ashenfelter (ashenfelter@bigml.com)
 */
public class Histogram<T extends Target> {

  public static final DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat("#.#####");

  /**
   * Creates an empty Histogram with the defined number of bins
   * @param  maxBins  the maximum number of bins for this histogram
   */
  public Histogram(int maxBins) {
    _maxBins = maxBins;
    _bins = new TreeMap<Double, Bin<T>>();
    _gaps = new TreeSet<Gap<T>>();
    _binsToGaps = new HashMap<Double, Gap<T>>();
  }

  /**
   * Creates a Histogram initialized with the given <code>bins</code>.  If the initial number of 
   * <code>bins</code> exceeds the <code>maxBins</code> then the bins are merged until the histogram
   * is valid.
   * @param  maxBins  the maximum number of bins for this histogram
   * @param  bins  the initial bins for the histogram
   */
  public Histogram(int maxBins, Collection<Bin<T>> bins) throws MixedInsertException {
    this(maxBins);
    for (Bin<T> bin : bins) {
      insertBin(bin);
    }
    mergeBins();
  }

  /**
   * Inserts a new point into the histogram
   * @param  point  the new point
   */
  public void insert(double point) throws MixedInsertException {
    checkType(TargetType.none);
    insert(new Bin(point, 1, new SimpleTarget()));
  }

  /**
   * Inserts a new point with a numeric target into the histogram
   * @param  point  the new point
   * @param  target  the numeric target
   */
  public void insert(double point, double target) throws MixedInsertException {
    checkType(TargetType.numeric);
    insert(new Bin(point, 1, new NumericTarget(target)));
  }

  /**
   * Inserts a new point with a categorical target into the histogram
   * @param  point  the new point
   * @param  target  the categorical target
   */
  public void insert(double point, String target) throws MixedInsertException {
    checkType(TargetType.categorical);
    insert(new Bin(point, 1, new CategoricalTarget(target)));
  }

  /**
   * Inserts a new bin into the histogram
   * @param  bin  the new bin
   */
  public void insert(Bin<T> bin) {
    insertBin(bin);
    mergeBins();
  }

  /**
   * Returns the approximate number of points less than <code>p_b</code>
   * @param  p_b the sum point
   */
  public double sum(double p_b) throws SumOutOfRangeException {
    return extendedSum(p_b).getCount();
  }

  /**
   * Returns a <code>SumResult</code> object which contains the approximate number of points less
   * than <code>p_b</code> along with the sum of their targets.
   * @param  p_b the sum point
   */
  public SumResult<T> extendedSum(double p_b) throws SumOutOfRangeException {
    SumResult<T> result = null;

    double min = _bins.firstKey();
    double max = _bins.lastKey();

    if (p_b < min || p_b > max) {
      throw new SumOutOfRangeException("Sum point " + p_b + " should be between " + min + " and " + max);
    } else if (p_b == max) {
      Bin<T> lastBin = _bins.lastEntry().getValue();

      double totalCount = this.getTotalCount();
      double count = totalCount - (lastBin.getCount() / 2d);

      T targetSum = (T) getTotalTargetSum().subtractUpdate(lastBin.getTarget().clone().multiplyUpdate(0.5d));
      result = new SumResult<T>(count, targetSum);

      return result;
    }

    Bin<T> bin_i = _bins.floorEntry(p_b).getValue();
    Bin<T> bin_i1 = _bins.higherEntry(p_b).getValue();

    double prevCount = 0;
    T prevTargetSum = (T) _bins.firstEntry().getValue().getTarget().init();

    for (Bin<T> bin : _bins.values()) {
      if (bin.equals(bin_i)) {
        break;
      }
      prevCount += bin.getCount();
      prevTargetSum.sumUpdate(bin.getTarget());
    }

    double bDiff = p_b - bin_i.getMean();
    double pDiff = bin_i1.getMean() - bin_i.getMean();
    double bpRatio = bDiff / pDiff;
    double m_b = bin_i.getCount() + (((bin_i1.getCount() - bin_i.getCount()) / pDiff) * bDiff);

    double countSum = prevCount
            + (bin_i.getCount() / 2)
            + ((bin_i.getCount() + m_b) / 2) * bpRatio;

    T targetSum_m_b = (T) bin_i1.getTarget().clone().subtractUpdate(bin_i.getTarget())
            .multiplyUpdate(bDiff / pDiff).sumUpdate(bin_i.getTarget());
    T targetSum = (T) prevTargetSum.sumUpdate(bin_i.getTarget().clone().multiplyUpdate(0.5))
            .sumUpdate(targetSum_m_b.sumUpdate(bin_i.getTarget()).multiplyUpdate(bpRatio / 2d));

    result = new SumResult<T>(countSum, targetSum);

    return result;
  }

  /**
   * Returns a list containing split points that form bins with uniform membership
   * @param  numberOfBins the desired number of uniform bins
   */
  public ArrayList<Double> uniform(int numberOfBins) {
    ArrayList<Double> uniformBinSplits = new ArrayList<Double>();

    TreeMap<Double, Bin<T>> binSumMap = createBinSumMap();
    double totalCount = getTotalCount();

    double gapSize = totalCount / (double) numberOfBins;
    double minGapSize = Math.max(_bins.firstEntry().getValue().getCount(),
            _bins.lastEntry().getValue().getCount()) / 2;

    int splits = numberOfBins;
    if (gapSize < minGapSize) {
      splits = (int) (totalCount / minGapSize);
      gapSize = totalCount / (double) splits;
    }

    for (int i = 1; i < splits; i++) {
      double targetSum = (double) i * gapSize;
      double binSplit = findPointForSum(targetSum, binSumMap);
      uniformBinSplits.add(binSplit);
    }

    return uniformBinSplits;
  }

  /**
   * Merges a histogram into the current histogram
   * @param  histogram the histogram to be merged
   */
  public void mergeHistogram(Histogram<T> histogram) {
    for (Bin<T> bin : histogram.getBins()) {
      insertBin(bin);
    }
    mergeBins();
  }

  /**
   * Returns the total number of points in the histogram
   */
  public double getTotalCount() {
    double count = 0;
    for (Bin<T> bin : _bins.values()) {
      count += bin.getCount();
    }
    return count;
  }

  /**
   * Returns the collection of bins that form the histogram
   */
  public Collection<Bin<T>> getBins() {
    return _bins.values();
  }

  public JSONArray toJSON(DecimalFormat format) {
    JSONArray bins = new JSONArray();
    for (Bin<T> bin : getBins()) {
      bins.add(bin.toJSON(format));
    }
    return bins;
  }

  public String toJSONString(DecimalFormat format) {
    return toJSON(format).toJSONString();
  }

  @Override
  public String toString() {
    return toJSONString(DEFAULT_DECIMAL_FORMAT);
  }

  public T getTotalTargetSum() {
    T target = null;
    for (Bin<T> bin : _bins.values()) {
      if (target == null) {
        target = (T) bin.getTarget().init();
      }
      target.sumUpdate(bin.getTarget());
    }
    return target;
  }

  private void checkType(TargetType newType) throws MixedInsertException {
    if (_targetType == null) {
      _targetType = newType;
    } else if (_targetType != newType) {
      throw new MixedInsertException();
    }
  }

  private void insertBin(Bin<T> bin) {
    Bin<T> existingBin = _bins.get(bin.getMean());
    if (existingBin != null) {
      try {
        existingBin.update(bin);
      } catch (BinUpdateException ex) {
      }
    } else {
      updateGaps(bin);
      _bins.put(bin.getMean(), bin);
    }
  }

  private TreeMap<Double, Bin<T>> createBinSumMap() {
    TreeMap<Double, Bin<T>> binSumMap = new TreeMap<Double, Bin<T>>();
    for (Bin<T> bin : _bins.values()) {
      try {
        double sum = sum(bin.getMean());
        binSumMap.put(sum, bin);
      } catch (SumOutOfRangeException e) {
      }
    }
    return binSumMap;
  }

  private double findPointForSum(double s, TreeMap<Double, Bin<T>> binSumMap) {
    Entry<Double, Bin<T>> sumEntry = binSumMap.floorEntry(s);
    double sumP_i = sumEntry.getKey();
    Bin<T> bin_i = sumEntry.getValue();
    double p_i = bin_i.getMean();
    double m_i = bin_i.getCount();

    Double sumP_i1 = binSumMap.navigableKeySet().higher(sumP_i);
    if (sumP_i1 == null) {
      sumP_i1 = binSumMap.navigableKeySet().floor(sumP_i);
    }

    Bin<T> bin_i1 = binSumMap.get(sumP_i1);
    double p_i1 = bin_i1.getMean();
    double m_i1 = bin_i1.getCount();

    double d = s - sumP_i;
    double a = m_i1 - m_i;

    double u;
    if (a == 0) {
      double offset = d / ((m_i + m_i1) / 2);
      u = p_i + (offset * (p_i1 - p_i));
    } else {
      double b = 2 * m_i;
      double c = -2 * d;
      double z = findZ(a, b, c);
      u = (p_i + (p_i1 - p_i) * z);
    }

    return u;
  }

  private void updateGaps(Bin<T> newBin) {
    Entry<Double, Bin<T>> prevEntry = _bins.floorEntry(newBin.getMean());
    if (prevEntry != null) {
      updateGaps(prevEntry.getValue(), newBin);
    }

    Entry<Double, Bin<T>> nextEntry = _bins.ceilingEntry(newBin.getMean());
    if (nextEntry != null) {
      updateGaps(newBin, nextEntry.getValue());
    }
  }

  private void updateGaps(Bin<T> previousBin, Bin<T> nextBin) {
    double space = nextBin.getMean() - previousBin.getMean();
    Gap<T> newGap = new Gap<T>(space, previousBin, nextBin);

    Gap<T> previousGap = _binsToGaps.get(previousBin.getMean());
    if (previousGap != null) {
      _gaps.remove(previousGap);
    }

    _binsToGaps.put(previousBin.getMean(), newGap);
    _gaps.add(newGap);
  }

  private void mergeBins() {
    while (_bins.size() > _maxBins) {
      Gap<T> smallestGap = _gaps.pollFirst();
      Bin<T> newBin = smallestGap.getStartBin().combine(smallestGap.getEndBin());

      Gap<T> followingGap = _binsToGaps.get(smallestGap.getEndBin().getMean());
      if (followingGap != null) {
        _gaps.remove(followingGap);
      }

      _bins.remove(smallestGap.getStartBin().getMean());
      _bins.remove(smallestGap.getEndBin().getMean());

      _binsToGaps.remove(smallestGap.getStartBin().getMean());
      _binsToGaps.remove(smallestGap.getEndBin().getMean());

      updateGaps(newBin);
      _bins.put(newBin.getMean(), newBin);
    }
  }

  private static Double findZ(double a, double b, double c) {
    Double resultRoot = null;
    ArrayList<Double> candidateRoots = solveQuadratic(a, b, c);

    for (Double candidateRoot : candidateRoots) {
      if (candidateRoot >= 0 && candidateRoot <= 1) {
        resultRoot = candidateRoot;
        break;
      }
    }

    return resultRoot;
  }

  /* Simple quadratic solver - doesn't handle edge cases */
  private static ArrayList<Double> solveQuadratic(double a, double b, double c) {
    double discriminantSquareRoot = Math.sqrt(Math.pow(b, 2) - (4 * a * c));
    ArrayList<Double> roots = new ArrayList<Double>();
    roots.add((-b + discriminantSquareRoot) / (2 * a));
    roots.add((-b - discriminantSquareRoot) / (2 * a));
    return roots;
  }

  private enum TargetType {none, numeric, categorical};
  
  private TargetType _targetType;
  private final int _maxBins;
  private final TreeMap<Double, Bin<T>> _bins;
  private final TreeSet<Gap<T>> _gaps;
  private final HashMap<Double, Gap<T>> _binsToGaps;
}
