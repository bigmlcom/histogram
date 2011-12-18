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
        target = new NumericTarget((Number) value);
      } else {
        target = new CategoricalTarget(value);
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
  protected GroupTarget combine(GroupTarget group) {
    ArrayList<Target> newGroup = new ArrayList<Target>();
    for (int i = 0; i < _target.size(); i++) {
      newGroup.add(_target.get(i).combine(group.getGroupTarget().get(i)));
    }
    return new GroupTarget(newGroup);
  }

  @Override
  protected GroupTarget sumUpdate(GroupTarget group) {
    for (int i = 0; i < _target.size(); i++) {
      _target.get(i).sumUpdate(group.getGroupTarget().get(i));
    }
    return this;
  }

  @Override
  protected GroupTarget subtractUpdate(GroupTarget group) {
    for (int i = 0; i < _target.size(); i++) {
      _target.get(i).subtractUpdate(group.getGroupTarget().get(i));
    }
    return this;
  }

  @Override
  protected GroupTarget multiplyUpdate(double multiplier) {
    for (Target target : _target) {
      target.multiplyUpdate(multiplier);
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
