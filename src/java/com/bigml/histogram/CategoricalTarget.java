package com.bigml.histogram;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map.Entry;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CategoricalTarget extends Target<CategoricalTarget> {

  public CategoricalTarget(Object category) {
    _target = new HashMap<Object, Double>(1,1);
    _target.put(category, 1d);
  }
  
  public CategoricalTarget(HashMap<Object, Double> targetCounts) {
    _target = targetCounts;
  }
  
  public HashMap<Object, Double> getTargetCounts() {
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
  protected CategoricalTarget sum(CategoricalTarget target) {
    for (Entry<Object, Double> categoryCount : target.getTargetCounts().entrySet()) {
      Object category = categoryCount.getKey();
      
      Double oldCount = _target.get(category);
      oldCount = (oldCount == null) ? 0 : oldCount;

      double newCount = oldCount + categoryCount.getValue();
      _target.put(category, newCount);
    }
    
    return this;
  }

  @Override
  protected CategoricalTarget mult(double multiplier) {
   for (Entry<Object, Double> categoryCount : getTargetCounts().entrySet()) {
     categoryCount.setValue(categoryCount.getValue() * multiplier);
   }

   return this;
  }

  @Override
  protected CategoricalTarget clone() {
    return new CategoricalTarget(new HashMap<Object, Double>(_target));
  }

  @Override
  protected CategoricalTarget init() {
    return new CategoricalTarget(new HashMap<Object, Double>());
  }
  
  private HashMap<Object, Double> _target;
}
