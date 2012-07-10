package com.bigml.histogram;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.simple.JSONArray;

/**
 * Implements a Histogram as defined by the <a
 * href="http://jmlr.csail.mit.edu/papers/v11/ben-haim10a.html">
 * Streaming Parallel Decision Tree (SPDT)</a> algorithm. <p>The
 * Histogram consumes numeric points and maintains a running
 * approximation of the dataset using the given number of bins. The
 * methods <code>insert</code>, <code>sum</code>, and
 * <code>uniform</code> are described in detail in the SPDT paper.
 *
 * <p>The histogram has an <code>insert</code> method which uses two
 * parameters and an <code>extendedSum</code> method which add the
 * capabilities described in <a
 * href="http://research.engineering.wustl.edu/~tyrees/Publications_files/fr819-tyreeA.pdf">
 * Tyree's paper</a>. Along with Tyree's extension this histogram
 * supports inserts with categorical targets.
 *
 * @author Adam Ashenfelter (ashenfelter@bigml.com)
 */
public class Histogram<T extends Target> {

  public static final String DEFAULT_FORMAT_STRING = "#.#####";

  /**
   * Creates an empty Histogram with the defined number of bins.
   *
   * @param maxBins the maximum number of bins for this histogram
   * @param countWeightedGaps true if count weighted gaps are desired
   * @param categories if the histogram uses categorical targets
   * then a collection of the possible category targets may be
   * provided to increase performance
   * @param groups if the histogram uses a group target
   * then a collection group target types may be provided
   * @param freezeThreshold after this # of inserts, bin locations
   * will 'freeze', increasing the performance of future inserts
   */
  public Histogram(int maxBins, boolean countWeightedGaps,
          Collection<Object> categories, Collection<TargetType> groupTypes,
          Long freezeThreshold) {
    _maxBins = maxBins;
    _bins = new TreeMap<Double, Bin<T>>();
    _gaps = new TreeSet<Gap<T>>();
    _binsToGaps = new HashMap<Double, Gap<T>>();
    _decimalFormat = new DecimalFormat(DEFAULT_FORMAT_STRING);
    _countWeightedGaps = countWeightedGaps;
    _totalCount = 0;
    _missingCount = 0;
    _minimum = null;
    _maximum = null;
    _freezeThreshold = freezeThreshold;

    if (categories != null && !categories.isEmpty()) {
      _targetType = TargetType.categorical;
      _groupTypes = null;
      _indexMap = new HashMap<Object, Integer>();
      for (Object category : categories) {
        if (_indexMap.get(category) == null) {
          _indexMap.put(category, _indexMap.size());
        }
      }
    } else if (groupTypes != null && !groupTypes.isEmpty()) {
      _targetType = TargetType.group;
      _groupTypes = new ArrayList<TargetType>(groupTypes);
    } else {
      _groupTypes = null;
      _indexMap = null;
    }
  }

  /**
   * Creates an empty Histogram with the defined number of bins.
   *
   * @param maxBins the maximum number of bins for this histogram
   * @param countWeightedGaps true if count weighted gaps are desired
   */
  public Histogram(int maxBins, boolean countWeightedGaps) {
    this(maxBins, countWeightedGaps, null, null, null);
  }

  /**
   * Creates an empty Histogram with the defined number of bins.
   *
   * @param maxBins the maximum number of bins for this histogram
   */
  public Histogram(int maxBins) {
    this(maxBins, false);
  }

  /**
   * Inserts a new point into the histogram.
   * The histogram returns itself after modification.
   *
   * @param point the new point
   */
  public Histogram<T> insert(Double point) throws MixedInsertException {
    checkType(TargetType.none);
    processPointTarget(point, SimpleTarget.TARGET);
    return this;
  }

  /**
   * Inserts a new point with a numeric target into the histogram.
   * The histogram returns itself after modification.
   *
   * @param point the new point
   * @param target the numeric target
   */
  public Histogram<T> insert(Double point, double target) throws MixedInsertException {
    return insertNumeric(point, target);
  }

  /**
   * Inserts a new point with a categorical target into the histogram.
   * The histogram returns itself after modification.
   *
   * @param point the new point
   * @param target the categorical target
   */
  public Histogram<T> insert(Double point, String target) throws MixedInsertException {
    return insertCategorical(point, target);
  }

  /**
   * Inserts a new point with a group of targets into the histogram.
   * A null group target is _not_ allowed.
   * The histogram returns itself after modification.
   *
   * @param point the new point
   * @param target the group targets
   */
  public Histogram<T> insert(Double point, Collection<Object> group) throws MixedInsertException {
    return insertGroup(point, group);
  }

  /**
   * Inserts a new point with a categorical target into the histogram.
   * Null target values are allowed.
   * The histogram returns itself after modification.
   *
   * @param point the new point
   * @param target the categorical target
   */
  public Histogram<T> insertCategorical(Double point, Object target)
          throws MixedInsertException {
    checkType(TargetType.categorical);
    Target catTarget;
    if (_indexMap == null) {
      catTarget = new MapCategoricalTarget(target);
    } else {
      catTarget = new ArrayCategoricalTarget(_indexMap, target);
    }
    processPointTarget(point, catTarget);
    return this;
  }

  /**
   * Inserts a new point with a numeric target into the histogram.
   * Null target values are allowed.
   * The histogram returns itself after modification.
   *
   * @param point the new point
   * @param target the categorical target
   */
  public Histogram<T> insertNumeric(Double point, Double target)
          throws MixedInsertException {
    checkType(TargetType.numeric);
    processPointTarget(point, new NumericTarget(target));
    return this;
  }

  /**
   * Inserts a new point with a group target into the histogram.
   * A null group target is _not_ allowed.
   * The histogram returns itself after modification.
   *
   * @param point the new point
   * @param target the categorical target
   */
  public Histogram<T> insertGroup(Double point, Collection<Object> group)
          throws MixedInsertException {
    checkType(TargetType.group);
    if (group == null) {
      throw new MixedInsertException();
    }

    GroupTarget groupTarget = new GroupTarget(group, _groupTypes);

    if (_groupTypes == null) {
      _groupTypes = new ArrayList<TargetType>();
      for (Target t : groupTarget.getGroupTarget()) {
        _groupTypes.add(t.getTargetType());
      }
    }

    processPointTarget(point, groupTarget);
    return this;
  }

  /**
   * Inserts a new bin into the histogram.
   * The histogram returns itself after modification.
   *
   * @param bin the new bin
   */
  public Histogram<T> insertBin(Bin<T> bin) {
    updateBins(bin);
    mergeBins();
    return this;
  }

  /**
   * Returns the target type for the histogram
   */
  public TargetType getTargetType() {
    return _targetType;
  }

  /**
   * Returns the target types for a group histogram
   */
  public ArrayList<TargetType> getGroupTypes() {
    return _groupTypes;
  }

  /**
   * Returns the maximum number of allowed bins.
   */
  public int getMaxBins() {
    return _maxBins;
  }

  /**
   * Returns the freeze threshold.
   */
  public Long getFreezeThreshold() {
    return _freezeThreshold;
  }

  /**
   * Returns whether gaps are count weighted.
   */
  public boolean isCountWeightedGaps() {
    return _countWeightedGaps;
  }
  
  /**
   * Returns the categories for an array-backed
   * categorical histogram
   */
  public List<Object> getTargetCategories() {
    List<Object> categories = null;
    if (_indexMap != null) {
      Object[] catArray = new Object[_indexMap.size()];
      for (Entry<Object, Integer> entry : _indexMap.entrySet()) {
        catArray[entry.getValue()] = entry.getKey();
      }
      categories = Arrays.asList(catArray);
    }
    return categories;
  }

  /**
   * Returns the approximate number of points less than
   * <code>p</code>.
   *
   * @param p the sum point
   */
  public double sum(double p) throws SumOutOfRangeException {
    return extendedSum(p).getCount();
  }

  /**
   * Returns a <code>SumResult</code> object which contains the
   * approximate number of points less than <code>p</code> along
   * with the sum of their targets.
   *
   * @param p the sum point
   */
  public SumResult<T> extendedSum(double p) throws SumOutOfRangeException {
    SumResult<T> result;
    
    if (_bins.isEmpty()) {
      throw new SumOutOfRangeException("Cannot sum with an empty histogram.");
    }

    double min = _bins.firstKey();
    double max = _bins.lastKey();

    if (p < min) {
      Bin<T> bin = _bins.firstEntry().getValue();
      double distanceRatio = (1 - (bin.getMean() - p) / binGapRange(p, bin)) / 2;
      if (distanceRatio > 0) {
        double countSum = distanceRatio * bin.getCount();
        T targetSum = (T) bin.getTarget().clone().mult(distanceRatio);
        result = new SumResult<T>(countSum, targetSum);
      } else {
        result = new SumResult<T>(0, (T) bin.getTarget().init());
      }
    } else if (p > max) {
      Bin<T> bin = _bins.lastEntry().getValue();
      double distanceRatio = (1 - (p - bin.getMean()) / binGapRange(p, bin)) / 2;
      if (distanceRatio > 0) {
        double countSum = getTotalCount() - distanceRatio * bin.getCount();
        T targetSum = (T) getTotalTargetSum().sum(bin.getTarget().clone().mult(-distanceRatio));
        result = new SumResult<T>(countSum, targetSum);
      } else {
        result = new SumResult<T>(getTotalCount(), getTotalTargetSum());
      }      
    } else if (p == max) {
      Bin<T> lastBin = _bins.lastEntry().getValue();

      double totalCount = this.getTotalCount();
      double count = totalCount - (lastBin.getCount() / 2d);

      T targetSum = (T) getTotalTargetSum().sum(lastBin.getTarget().clone().mult(-0.5d));
      Entry<Double, Bin<T>> prevEntry = _bins.lowerEntry(lastBin.getMean());

      result = new SumResult<T>(count, targetSum);
    } else {
      Bin<T> bin_i = _bins.floorEntry(p).getValue();
      Bin<T> bin_i1 = _bins.higherEntry(p).getValue();

      double prevCount = 0;
      T prevTargetSum = (T) _bins.firstEntry().getValue().getTarget().init();

      for (Bin<T> bin : _bins.values()) {
        if (bin.equals(bin_i)) {
          break;
        }
        prevCount += bin.getCount();
        prevTargetSum.sum(bin.getTarget().clone());
      }

      double bDiff = p - bin_i.getMean();
      double pDiff = bin_i1.getMean() - bin_i.getMean();
      double bpRatio = bDiff / pDiff;

      NumericTarget countTarget = (NumericTarget) computeSum(bpRatio, new NumericTarget(prevCount),
              new NumericTarget(bin_i.getCount()), new NumericTarget(bin_i1.getCount()));
      double countSum = countTarget.getTarget();

      T targetSum = (T) computeSum(bpRatio, prevTargetSum, bin_i.getTarget(), bin_i1.getTarget());

      result = new SumResult<T>(countSum, targetSum);
    }

    return result;
  }

  /**
   * Returns the density estimate at point
   * <code>p</code>.
   *
   * @param p the density estimate point
   */
  public double density(double p) {
    return extendedDensity(p).getCount();
  }

  /**
   * Returns a <code>SumResult</code> object which contains the
   * density estimate at the point <code>p</code> along
   * with the density for the targets.
   *
   * @param p the density estimate point
   */
  public SumResult<T> extendedDensity(double p) {
    double countDensity;
    T targetDensity;

    Bin<T> exact = _bins.get(p);
    if (exact != null) {
      double higher = Double.longBitsToDouble(Double.doubleToLongBits(p) + 1);
      double lower = Double.longBitsToDouble(Double.doubleToLongBits(p) - 1);

      countDensity = (density(higher) + density(lower)) / 2;
      targetDensity = (T) exact.getTarget().clone().mult(countDensity);
    } else {
      Entry<Double, Bin<T>> lower = _bins.lowerEntry(p);
      Entry<Double, Bin<T>> higher = _bins.higherEntry(p);
      if (lower == null && higher == null) {
        countDensity = 0;
        targetDensity = null;
      } else if (lower == null || higher == null) {
        Bin<T> bin;
        if (lower != null) {
          bin = lower.getValue();
        } else {
          bin = higher.getValue();
        }

        if (Math.abs(p - bin.getMean()) < binGapRange(p, bin)) {
          countDensity = binGapDensity(p, bin);
          targetDensity = (T) bin.getTarget().clone().mult(countDensity);
        } else {
          countDensity = 0;
          targetDensity = null;
        }
      } else {
        Bin<T> hBin = higher.getValue();
        double hDensity = binGapDensity(p, hBin);

        Bin<T> lBin = lower.getValue();
        double lDensity = binGapDensity(p, lBin);

        countDensity = hDensity + lDensity;

        T lTargetDensity = (T) lBin.getTarget().clone().mult(lDensity);
        T hTargetDensity = (T) hBin.getTarget().clone().mult(hDensity);
        targetDensity = (T) lTargetDensity.sum(hTargetDensity);
      }
    }

    return new SumResult<T>(countDensity, targetDensity);
  }

  /**
   * Returns a <code>Target</code> object representing the
   * average (or expected) target value for point <code>p</code>.
   *
   * @param p the density estimate point
   */
  public T averageTarget(double p) {
    T averageTarget;

    Bin<T> exact = _bins.get(p);
    if (exact != null) {
      double higher = Double.longBitsToDouble(Double.doubleToLongBits(p) + 1);
      double lower = Double.longBitsToDouble(Double.doubleToLongBits(p) - 1);

      averageTarget = (T) averageTarget(lower).sum(averageTarget(higher)).mult(0.5);
    } else {
      Entry<Double, Bin<T>> lower = _bins.lowerEntry(p);
      Entry<Double, Bin<T>> higher = _bins.higherEntry(p);
      if (lower == null && higher == null) {
        averageTarget = null;
      } else if (lower == null || higher == null) {
        Bin<T> bin;
        if (lower != null) {
          bin = lower.getValue();
        } else {
          bin = higher.getValue();
        }

        if (Math.abs(p - bin.getMean()) < binGapRange(p, bin)) {
          double targetCount = bin.getCount() - bin.getTarget().getMissingCount();
          averageTarget = (T) bin.getTarget().clone().mult(1 / targetCount);
        } else {
          averageTarget = null;
        }
      } else {
        Bin<T> hBin = higher.getValue();
        double hCount = hBin.getCount() - hBin.getTarget().getMissingCount();
        T hTarget = (T) hBin.getTarget();

        Bin<T> lBin = lower.getValue();
        double lCount = lBin.getCount() - lBin.getTarget().getMissingCount();
        T lTarget = (T) lBin.getTarget();

        double totalCount = lCount + hCount;
        averageTarget = (T) lTarget.clone().sum(hTarget).mult(1 / totalCount);
      }
    }

    return averageTarget;
  }

  /**
   * Returns a list containing split points that form bins with
   * uniform membership.
   *
   * @param numberOfBins the desired number of uniform bins
   */
  public ArrayList<Double> uniform(int numberOfBins) {
    ArrayList<Double> uniformBinSplits = new ArrayList<Double>();
    double totalCount = getTotalCount();

    if (totalCount > 0) {
      TreeMap<Double, Bin<T>> binSumMap = createBinSumMap();

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
    }
    return uniformBinSplits;
  }

  /**
   * Merges a histogram into the current histogram.
   * The histogram returns itself after modification.
   *
   * @param histogram the histogram to be merged
   */
  public Histogram merge(Histogram<T> histogram) throws MixedInsertException {
    if (_indexMap == null && histogram._indexMap != null) {
      if (getBins().isEmpty()) {
        _indexMap = histogram._indexMap;
      } else {
        throw new MixedInsertException();
      }
    }

    if (_indexMap != null && !_indexMap.equals(histogram._indexMap)) {
      throw new MixedInsertException();
    } else if (!histogram.getBins().isEmpty()) {
      checkType(histogram.getTargetType());
      for (Bin<T> bin : histogram.getBins()) {
        Bin<T> newBin = new Bin<T>(bin);
        if (_indexMap != null) {
          ((ArrayCategoricalTarget) newBin.getTarget()).setIndexMap(_indexMap);
        }
        updateBins(new Bin<T>(bin));
      }
      mergeBins();
    }

    if (_minimum == null) {
      _minimum = histogram.getMinimum();
    } else if (histogram.getMinimum() != null){
      _minimum = Math.min(_minimum, histogram.getMinimum());
    }

    if (_maximum == null) {
      _maximum = histogram.getMaximum();
    } else if (histogram.getMaximum() != null){
      _maximum = Math.max(_maximum, histogram.getMaximum());
    }

    if (_missingTarget == null) {
      _missingTarget = (T) histogram.getMissingTarget();
    } else if (histogram.getMissingTarget() != null) {
      _missingTarget.sum(histogram.getMissingTarget());
    }
    _missingCount += histogram.getMissingCount();
    _totalCount += histogram.getMissingCount();
    return this;
  }

  /**
   * Returns the total number of points in the histogram.
   */
  public double getTotalCount() {
    return _totalCount;
  }

  /**
   * Returns the collection of bins that form the histogram.
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
    return toJSONString(_decimalFormat);
  }

  public T getTotalTargetSum() {
    T target = null;
    for (Bin<T> bin : _bins.values()) {
      if (target == null) {
        target = (T) bin.getTarget().init();
      }
      target.sum(bin.getTarget().clone());
    }
    return target;
  }

  public long getMissingCount() {
    return _missingCount;
  }

  public T getMissingTarget() {
    return _missingTarget;
  }

  /**
   * Inserts count and target information for missing inputs.
   * The histogram returns itself after modification.
   *
   * @param count the number of missing values
   * @param count the target sum for the missing values
   */
  public Histogram<T> insertMissing(long count, T target) {
    if (_missingTarget == null) {
      _missingTarget = (T) target;
    } else {
      _missingTarget.sum(target);
    }
   _missingCount += count;
   return this;
  }

  /**
   * Returns the minimum value inserted into the histogram.
   */
  public Double getMinimum() {
    return _minimum;
  }

  /**
   * Returns the maximum value inserted into the histogram.
   */
  public Double getMaximum() {
    return _maximum;
  }

  /**
   * Sets the minimum input value for the histogram. This
   * method should only be used for histograms created
   * by inserting pre-existing bins.
   *
   * @param minimum the minimum value observed by the histogram
   */
  public Histogram setMinimum(Double minimum) {
    _minimum = minimum;
    return this;
  }

  /**
   * Sets the maximum input value for the histogram. This
   * method should only be used for histograms created
   * by inserting pre-existing bins.
   *
   * @param maximum the maximum value observed by the histogram
   */
  public Histogram setMaximum(Double maximum) {
    _maximum = maximum;
    return this;
  }

  private void checkType(TargetType newType) throws MixedInsertException {
    if (_targetType == null) {
      _targetType = newType;
    } else if (_targetType != newType || newType == null) {
      throw new MixedInsertException();
    }
  }

  private void processPointTarget(Double point, Target target) {
    if (point == null) {
      insertMissing(1, (T) target);
    } else {
      if (_minimum == null || _minimum > point) {
        _minimum = point;
      }
      if (_maximum == null || _maximum < point) {
        _maximum = point;
      }

      insertBin(new Bin(point, 1, target));
    }
  }

  private void updateBins(Bin<T> bin) {
    _totalCount += bin.getCount();
    Bin<T> existingBin = _bins.get(bin.getMean());
    if (_freezeThreshold != null 
            && _totalCount > _freezeThreshold
            && _bins.size() == _maxBins) {
      Double floorDiff = Double.MAX_VALUE;
      Entry<Double, Bin<T>> floorEntry = _bins.floorEntry(bin.getMean());
      if (floorEntry != null) {
        floorDiff = Math.abs(floorEntry.getValue().getMean() - bin.getMean());
      }
      Double ceilDiff = Double.MAX_VALUE;
      Entry<Double, Bin<T>> ceilEntry = _bins.ceilingEntry(bin.getMean());
      if (ceilEntry != null) {
        ceilDiff = Math.abs(ceilEntry.getValue().getMean() - bin.getMean());
      }
      if (floorDiff <= ceilDiff) {
        floorEntry.getValue().sumUpdate(bin);
      } else {
        ceilEntry.getValue().sumUpdate(bin);
      }
    } else if (existingBin != null) {
      existingBin.sumUpdate(bin);
      if (_countWeightedGaps) {
        updateGaps(existingBin);
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

  private double binGapRange(double p, Bin<T> bin) {
    Entry<Double, Bin<T>> lower = _bins.lowerEntry(bin.getMean());
    Entry<Double, Bin<T>> higher = _bins.higherEntry(bin.getMean());

    double range;
    if (lower == null && higher == null) {
      range = 0;
    } else if (lower == null) {
      range = higher.getValue().getMean() - bin.getMean();
    } else if (higher == null) {
      range = bin.getMean() - lower.getValue().getMean();
    } else {
      if (p < bin.getMean()) {
        range = bin.getMean() - lower.getValue().getMean();
      } else {
        range = higher.getValue().getMean() - bin.getMean();
      }
    }
    return range;
  }

  private double binGapDensity(double p, Bin<T> bin) {
    double range = binGapRange(p, bin);
    if (range == 0) {
      return 0;
    } else {
      return (bin.getCount() / 2) / range;
    }
  }

  // m = i + (i1 - i) * r
  // s = p + i/2 + (m + i) * r/2
  // s = p + i/2 + (i + (i1 - i) * r + i) * r/2
  // s = p + i/2 + (i + r*i1 - r*i + i) * r/2
  // s = p + i/2 + r/2*i + r^2/2*i1 - r^2/2*i + r/2*i
  // s = p + i/2 + r/2*i + r/2*i - r^2/2*i + r^2/2*i1
  // s = p + i/2 + r*i - r^2/2*i + r^2/2*i1
  // s = p + (1/2 + r - r^2/2)*i + r^2/2*i1
  private <U extends Target> Target computeSum(double r, U p, U i, U i1) {
    double i1Term = 0.5 * r * r;
    double iTerm = 0.5 + r - i1Term;
    return (U) p.sum(i.clone().mult(iTerm)).sum(i1.clone().mult(i1Term));
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
    Entry<Double, Bin<T>> prevEntry = _bins.lowerEntry(newBin.getMean());
    if (prevEntry != null) {
      updateGaps(prevEntry.getValue(), newBin);
    }

    Entry<Double, Bin<T>> nextEntry = _bins.higherEntry(newBin.getMean());
    if (nextEntry != null) {
      updateGaps(newBin, nextEntry.getValue());
    }
  }

  private void updateGaps(Bin<T> prevBin, Bin<T> nextBin) {
    double gapWeight = nextBin.getMean() - prevBin.getMean();
    if (_countWeightedGaps) {
      gapWeight *= Math.log(Math.E + Math.min(prevBin.getCount(), nextBin.getCount()));
    }

    Gap<T> newGap = new Gap<T>(prevBin, nextBin, gapWeight);

    Gap<T> prevGap = _binsToGaps.get(prevBin.getMean());
    if (prevGap != null) {
      _gaps.remove(prevGap);
    }

    _binsToGaps.put(prevBin.getMean(), newGap);
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

  /*
   * Simple quadratic solver - doesn't handle edge cases
   */
  private static ArrayList<Double> solveQuadratic(double a, double b, double c) {
    double discriminantSquareRoot = Math.sqrt(Math.pow(b, 2) - (4 * a * c));
    ArrayList<Double> roots = new ArrayList<Double>();
    roots.add((-b + discriminantSquareRoot) / (2 * a));
    roots.add((-b - discriminantSquareRoot) / (2 * a));
    return roots;
  }

  public enum TargetType {none, numeric, categorical, group, histogram};
  private TargetType _targetType;
  private final int _maxBins;
  private final TreeMap<Double, Bin<T>> _bins;
  private final TreeSet<Gap<T>> _gaps;
  private final HashMap<Double, Gap<T>> _binsToGaps;
  private final DecimalFormat _decimalFormat;
  private final boolean _countWeightedGaps;
  private ArrayList<TargetType> _groupTypes;
  private HashMap<Object, Integer> _indexMap;
  private long _totalCount;
  private long _missingCount;
  private T _missingTarget;
  private Double _minimum;
  private Double _maximum;
  private Long _freezeThreshold;
}
