package com.bigml.histogram;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class ArrayCategoricalTarget extends Target<ArrayCategoricalTarget> implements CategoricalTarget {

  public ArrayCategoricalTarget(Map<Object, Integer> indexMap) {
    _indexMap = indexMap;
    _target = new double[indexMap.size()];
    Arrays.fill(_target, 0);
  }

  public ArrayCategoricalTarget(Map<Object, Integer> indexMap, Object category) throws MixedInsertException {
    this(indexMap);
    Integer index = indexMap.get(category);
    if (index == null) {
      throw new MixedInsertException();
    } else {
      _target[index]++;
    }
  }

  public void setIndexMap(Map<Object, Integer> indexMap) {
    _indexMap = indexMap;
  }
  
  public HashMap<Object, Double> getCounts() {
    HashMap<Object, Double> countMap = new HashMap<Object, Double>();
    for (Entry<Object, Integer> entry : _indexMap.entrySet()) {
      Object category = entry.getKey();
      Integer index = entry.getValue();
      countMap.put(category, _target[index]);
    }
    return countMap;
  }

  @Override
  protected void addJSON(JSONArray binJSON, DecimalFormat format) {
    JSONObject counts = new JSONObject();
    for (Entry<Object,Integer> categoryIndex : _indexMap.entrySet()) {
      Object category = categoryIndex.getKey();
      int index = categoryIndex.getValue();
      double count = _target[index];
      counts.put(category, Double.valueOf(format.format(count)));
    }
    binJSON.add(counts);
  }

  @Override
  protected ArrayCategoricalTarget sum(ArrayCategoricalTarget target) {
    for (int i = 0; i < _target.length; i++) {
      _target[i] += target._target[i];
    }
    
    return this;
  }

  @Override
  protected ArrayCategoricalTarget mult(double multiplier) {
    for (int i = 0; i < _target.length; i++) {
      _target[i] *= multiplier;
    }

   return this;
  }

  @Override
  protected ArrayCategoricalTarget clone() {
    ArrayCategoricalTarget rct = new ArrayCategoricalTarget(_indexMap);
    rct._target = (double[]) _target.clone();
    return rct;
  }

  @Override
  protected ArrayCategoricalTarget init() {
    return new ArrayCategoricalTarget(_indexMap);
  }
  
  private Map<Object, Integer> _indexMap;
  private double[] _target;
}
