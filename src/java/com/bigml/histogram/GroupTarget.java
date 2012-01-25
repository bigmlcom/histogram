package com.bigml.histogram;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import org.json.simple.JSONArray;

public class GroupTarget extends Target<GroupTarget> {
  
  public GroupTarget(ArrayList<Target> group) {
    _target = group;
  }
  
  public GroupTarget(Collection<Object> values) {
    ArrayList<Target> group = new ArrayList<Target>();
    for (Object value : values) {
      Target target;
      if (value instanceof Number) {
        target = new NumericTarget(((Number) value).doubleValue());
      } else {
        target = new MapCategoricalTarget(value);
      }
      group.add(target);
    }
    _target = group;
  }
  
  public ArrayList<Target> getGroupTarget() {
    return _target;
  }
    
  @Override
  protected void addJSON(JSONArray binJSON, DecimalFormat format) {
    JSONArray targetsJSON = new JSONArray();
    for (Target target : _target) {
      target.addJSON(targetsJSON, format);
    }
    binJSON.add(targetsJSON);
  }
  
  @Override
  protected GroupTarget sum(GroupTarget group) {
    for (int i = 0; i < _target.size(); i++) {
      _target.get(i).sum(group.getGroupTarget().get(i));
    }
    return this;
  }

  @Override
  protected GroupTarget mult(double multiplier) {
    for (Target target : _target) {
      target.mult(multiplier);
    }    
    return this;
  }

  @Override
  protected GroupTarget clone() {
    ArrayList<Target> newGroup = new ArrayList<Target>();
    for (Target target : _target) {
      newGroup.add(target.clone());
    }
    return new GroupTarget(new ArrayList<Target>(newGroup));
  }

  @Override
  protected GroupTarget init() {
    ArrayList<Target> newGroup = new ArrayList<Target>();
    for (Target target : _target) {
      newGroup.add(target.init());
    }
    return new GroupTarget(new ArrayList<Target>(newGroup));
  }
  
  private ArrayList<Target> _target;
}
