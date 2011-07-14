package com.bigml.histogram;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
 * @author Adam Ashenfelter (ashenfelter@bigml.com)
 */
public class Histogram {

  public static Histogram parseHistogramJSON(String jsonString) throws ParseException {
    JSONParser parser = new JSONParser();
    JSONArray binJSONArray = (JSONArray) parser.parse(jsonString);
    return parseHistogramJSON(binJSONArray);
  }

  public static Histogram parseHistogramJSON(String jsonString, int maxBins) throws ParseException {
    JSONParser parser = new JSONParser();
    JSONArray binJSONArray = (JSONArray) parser.parse(jsonString);
    return parseHistogramJSON(binJSONArray, maxBins);
  }

  public static Histogram parseHistogramJSON(JSONArray binJSONArray) {
    return parseHistogramJSON(binJSONArray, binJSONArray.size());
  }

  public static Histogram parseHistogramJSON(JSONArray binJSONArray, int maxBins) {
    ArrayList<Bin> bins = parseBins(binJSONArray);
    Histogram histogram = new Histogram(maxBins, bins);
    return histogram;
  }
  
  public static final DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat("#.#####");

  public class SumOutOfRange extends Exception {

    public SumOutOfRange(String string) {
      super(string);
    }
  }

  /**
   * Creates an empty Histogram with the defined number of bins
   * @param  maxBins  the maximum number of bins for this histogram
   */
  public Histogram(int maxBins) {
    _maxBins = maxBins;
    _bins = new TreeMap<Double, Bin>();
    _gaps = new TreeSet<Gap>();
    _binsToGaps = new HashMap<Double, Gap>();
  }

  /**
   * Creates a Histogram initialized with the given <code>bins</code>.  If the initial number of 
   * <code>bins</code> exceeds the <code>maxBins</code> then the bins are merged until the histogram
   * is valid.
   * @param  maxBins  the maximum number of bins for this histogram
   * @param  bins  the initial bins for the histogram
   */
  public Histogram(int maxBins, Collection<Bin> bins) {
    this(maxBins);
    for (Bin bin : bins) {
      insertBin(bin);
    }
    mergeBins();
  }
  
  /**
   * Inserts a new point into the histogram
   * @param  p  the new point
   */
  public void insert(double p) {
    Bin newBin = new Bin(p, 1d);
    insertBin(newBin);
    mergeBins();
  }

  /**
   * Returns the approximate number of points that are less than <code>p_b</code>
   * @param  p_b the sum point
   */
  public double sum(double p_b) throws SumOutOfRange {
    double min = _bins.firstKey();
    double max = _bins.lastKey();

    if (p_b < min || p_b > max) {
      throw new SumOutOfRange("Sum point " + p_b + " should be between " + min + " and " + max);
    } else if (p_b == max) {
      double totalCount = this.getTotalCount();
      double binCount = _bins.lastEntry().getValue().getWeight();
      return totalCount - (binCount / 2d);
    }

    Bin bin_i = _bins.floorEntry(p_b).getValue();
    Bin bin_i1 = _bins.higherEntry(p_b).getValue();

    double prevSum = 0;
    for (Bin bin : _bins.values()) {
      if (bin.equals(bin_i)) {
        break;
      }
      prevSum += bin.getCount();
    }

    double bDiff = p_b - bin_i.getMean();
    double pDiff = bin_i1.getMean() - bin_i.getMean();
    double m_b = bin_i.getCount() + (((bin_i1.getCount() - bin_i.getCount()) / pDiff) * bDiff);

    double sum = prevSum
            + (bin_i.getCount() / 2)
            + ((bin_i.getCount() + m_b) / 2) * (bDiff / pDiff);

    return sum;
  }

  /**
   * Returns a list containing split points that form bins with uniform membership
   * @param  numberOfBins the desired number of uniform bins
   */
  public ArrayList<Double> uniform(int numberOfBins) {
    ArrayList<Double> uniformBinSplits = new ArrayList<Double>();

    TreeMap<Double, Bin> binSumMap = createBinSumMap();
    double totalCount = getTotalCount();

    for (int i = 1; i < numberOfBins; i++) {
      double targetSum = totalCount * ((double) i / (double) numberOfBins);
      double binSplit = findPointForSum(targetSum, binSumMap);
      uniformBinSplits.add(binSplit);
    }

    return uniformBinSplits;
  }

  /**
   * Returns a list of bins that have a constant width (useful for visualization)
   * @param  numberOfBins the desired number of bins
   */
  public ArrayList<Bin> createConstantWidthBins(int numberOfBins) {
    ArrayList<Bin> bins = new ArrayList<Bin>();

    try {
      double totalCount = getTotalCount();

      double min = _bins.firstKey();
      double max = _bins.lastKey();
      double range = max - min;
      double increment = range / (double) numberOfBins;

      double startCount = 0;
      double binCenter = min + (increment / 2);
      for (int i = 0; i < numberOfBins - 1; i++) {
        double binEnd = binCenter + (increment / 2);
        double endCount = sum(binEnd);
        double binCount = endCount - startCount;
        Bin newBin = new Bin(binCenter, binCount);
        bins.add(newBin);

        startCount = endCount;
        binCenter += increment;
      }

      double lastBinCount = totalCount - startCount;
      Bin lastBin = new Bin(binCenter, lastBinCount);
      bins.add(lastBin);

    } catch (SumOutOfRange ex) {
    }

    return bins;
  }

  /**
   * Merges a histogram into the current histogram
   * @param  histogram the histogram to be merged
   */
  public void mergeHistogram(Histogram histogram) {
    for (Bin bin : histogram.getBins()) {
      insertBin(bin);
    }
    mergeBins();
  }

  /**
   * Returns the total number of points in the histogram
   */
  public double getTotalCount() {
    double count = 0;
    for (Bin bin : _bins.values()) {
      count += bin.getCount();
    }
    return count;
  }

  /**
   * Returns the collection of bins that form the histogram
   */
  public Collection<Bin> getBins() {
    return _bins.values();
  }

  public JSONArray toJSON(DecimalFormat format) {
    JSONArray bins = new JSONArray();
    for (Bin bin : getBins()) {
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

  private void insertBin(Bin bin) {
    Bin existingBin = _bins.get(bin.getMean());
    if (existingBin != null) {
      existingBin.setCount(existingBin.getCount() + bin.getCount());
    } else {
      updateGaps(bin);
      _bins.put(bin.getMean(), bin);
    }
  }

  private TreeMap<Double, Bin> createBinSumMap() {
    TreeMap<Double, Bin> binSumMap = new TreeMap<Double, Bin>();
    for (Bin bin : _bins.values()) {
      try {
        double sum = sum(bin.getMean());
        binSumMap.put(sum, bin);
      } catch (SumOutOfRange e) {
      }
    }
    return binSumMap;
  }

  private double findPointForSum(double s, TreeMap<Double, Bin> binSumMap) {
    Entry<Double, Bin> sumEntry = binSumMap.floorEntry(s);
    double sumP_i = sumEntry.getKey();
    Bin bin_i = sumEntry.getValue();
    double p_i = bin_i.getMean();
    double m_i = bin_i.getCount();

    double sumP_i1 = binSumMap.navigableKeySet().higher(sumP_i);
    Bin bin_i1 = binSumMap.get(sumP_i1);
    double p_i1 = bin_i1.getMean();
    double m_i1 = bin_i1.getCount();

    double d = s - sumP_i;
    double a = m_i1 - m_i;
    double b = 2 * m_i;
    double c = -2 * d;
    double z = findZ(a, b, c);
    double u = (p_i + (p_i1 - p_i) * z);

    return u;
  }

  private void updateGaps(Bin newBin) {
    Entry<Double, Bin> prevEntry = _bins.floorEntry(newBin.getMean());
    if (prevEntry != null) {
      updateGaps(prevEntry.getValue(), newBin);
    }

    Entry<Double, Bin> nextEntry = _bins.ceilingEntry(newBin.getMean());
    if (nextEntry != null) {
      updateGaps(newBin, nextEntry.getValue());
    }
  }

  private void updateGaps(Bin previousBin, Bin nextBin) {
    double space = nextBin.getMean() - previousBin.getMean();
    Gap newGap = new Gap(space, previousBin, nextBin);

    Gap previousGap = _binsToGaps.get(previousBin.getMean());
    if (previousGap != null) {
      _gaps.remove(previousGap);
    }

    _binsToGaps.put(previousBin.getMean(), newGap);
    _gaps.add(newGap);
  }

  private void mergeBins() {
    while (_bins.size() > _maxBins) {
      Gap smallestGap = _gaps.pollFirst();
      Bin newBin = Bin.combine(smallestGap.getStartBin(), smallestGap.getEndBin());

      Gap followingGap = _binsToGaps.get(smallestGap.getEndBin().getMean());
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

  private static ArrayList<Bin> parseBins(JSONArray binJSONArray) {
    ArrayList<Bin> bins = new ArrayList<Bin>();

    Iterator iter = binJSONArray.iterator();
    while (iter.hasNext()) {
      JSONArray binJSON = (JSONArray) iter.next();
      double mean = Double.valueOf(binJSON.get(0).toString());
      double count = Double.valueOf(binJSON.get(1).toString());
      bins.add(new Bin(mean, count));
    }

    return bins;
  }
  
  private final int _maxBins;
  private final TreeMap<Double, Bin> _bins;
  private final TreeSet<Gap> _gaps;
  private final HashMap<Double, Gap> _binsToGaps;
}
