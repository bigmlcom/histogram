package com.bigml.histogram;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map.Entry;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class MapCategoricalTarget extends Target<MapCategoricalTarget> implements CategoricalTarget {

  public MapCategoricalTarget(Object category) {
    _target = new HashMap<Object, Double>(1,1);
    _target.put(category, 1d);
  }
  
  public MapCategoricalTarget(HashMap<Object, Double> targetCounts) {
    _target = targetCounts;
  }
  
  public HashMap<Object, Double> getCounts() {
    return _target;
  }
  
  @Override
  protected void addJSON(JSONArray binJSON, DecimalFormat format) {
    JSONObject counts = new JSONObject();
    for (Entry<Object,Double> categoryCount : _target.entrySet()) {
      Object category = categoryCount.getKey();
      double count = categoryCount.getValue();
      counts.put(category, Double.valueOf(format.format(count)));
    }
    binJSON.add(counts);
  }

  @Override
  protected MapCategoricalTarget sum(MapCategoricalTarget target) {
    for (Entry<Object, Double> categoryCount : target.getCounts().entrySet()) {
      Object category = categoryCount.getKey();
      
      Double oldCount = _target.get(category);
      oldCount = (oldCount == null) ? 0 : oldCount;

      double newCount = oldCount + categoryCount.getValue();
      _target.put(category, newCount);
    }
    
    return this;
  }

  @Override
  protected MapCategoricalTarget mult(double multiplier) {
   for (Entry<Object, Double> categoryCount : getCounts().entrySet()) {
     categoryCount.setValue(categoryCount.getValue() * multiplier);
   }

   return this;
  }

  @Override
  protected MapCategoricalTarget clone() {
    return new MapCategoricalTarget(new HashMap<Object, Double>(_target));
  }

  @Override
  protected MapCategoricalTarget init() {
    return new MapCategoricalTarget(new HashMap<Object, Double>());
  }
  
  private HashMap<Object, Double> _target;
}
