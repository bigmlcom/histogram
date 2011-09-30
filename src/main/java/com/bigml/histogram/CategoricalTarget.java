package com.bigml.histogram;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CategoricalTarget extends Target<CategoricalTarget> {

  public CategoricalTarget(String category) {
    _target = new HashMap<String, Double>(1,1);
    _target.put(category, 1d);
  }
  
  public CategoricalTarget(HashMap<String, Double> targetCounts) {
    _target = targetCounts;
  }
  
  public HashMap<String, Double> getTargetCounts() {
    return _target;
  }
  
  @Override
  protected void addJSON(JSONArray binJSON, DecimalFormat format) {
    JSONObject counts = new JSONObject();
    for (Entry<String,Double> categoryCount : _target.entrySet()) {
      String category = categoryCount.getKey();
      double count = categoryCount.getValue();
      counts.put(category, Double.valueOf(format.format(count)));
    }
    binJSON.add(counts);
  }

  @Override
  protected CategoricalTarget combine(CategoricalTarget target) {
    HashMap<String, Double> counts1 = getTargetCounts();
    HashMap<String, Double> counts2 = target.getTargetCounts();
    
    HashSet<String> categories = new HashSet<String>();
    categories.addAll(counts1.keySet());
    categories.addAll(counts2.keySet());
    
    HashMap<String, Double> newTargetCounts = new HashMap<String, Double>();
    for (String category : categories) {
      Double count1 = counts1.get(category);
      count1 = (count1 == null) ? 0 : count1;
      
      Double count2 = counts2.get(category);
      count2 = (count2 == null) ? 0 : count2;
      
      newTargetCounts.put(category, count1 + count2);
    }
    
    return new CategoricalTarget(newTargetCounts);
  }

  @Override
  protected CategoricalTarget sumUpdate(CategoricalTarget target) {
    for (Entry<String,Double> categoryCount : target.getTargetCounts().entrySet()) {
      String category = categoryCount.getKey();
      
      Double oldCount = _target.get(category);
      oldCount = (oldCount == null) ? 0 : oldCount;

      double newCount = oldCount + categoryCount.getValue();
      _target.put(category, newCount);
    }
    
    return this;
  }

  @Override
  protected CategoricalTarget subtractUpdate(CategoricalTarget target) {
    for (Entry<String,Double> categoryCount : target.getTargetCounts().entrySet()) {
      String category = categoryCount.getKey();
      
      Double oldCount = _target.get(category);
      oldCount = (oldCount == null) ? 0 : oldCount;
      
      double newCount = oldCount - categoryCount.getValue();
      _target.put(category, newCount);
    }
    
    return this;
  }

  @Override
  protected CategoricalTarget multiplyUpdate(double multiplier) {
   for (Entry<String,Double> categoryCount : getTargetCounts().entrySet()) {
     categoryCount.setValue(categoryCount.getValue() * multiplier);
   }

   return this;
  }

  @Override
  protected CategoricalTarget clone() {
    return new CategoricalTarget(new HashMap<String, Double>(_target));
  }

  @Override
  protected CategoricalTarget init() {
    return new CategoricalTarget(new HashMap<String, Double>());
  }
  
  private HashMap<String, Double> _target;
}
