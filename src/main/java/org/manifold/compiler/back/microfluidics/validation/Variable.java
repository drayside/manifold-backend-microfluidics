package org.manifold.compiler.back.microfluidics.validation;

import java.util.HashSet;
import java.util.Set;

public class Variable extends Expression {

  private final String name;
  public String getName() {
    return this.name;
  }
 
  public Variable(String name) {
    this.name = name;
  }
  
  @Override
  public boolean equals(Object aThat) {
    if (this == aThat) return true;
    if (!(aThat instanceof Variable)) return false;
    Variable that = (Variable) aThat;
    return (this.getName().equals(that.getName()));
  }
  
  @Override
  public int hashCode() {
    return name.hashCode();
  }
  
  @Override
  public String toString() {
    return name.toString();
  }
  
  @Override
  public Set<Variable> freeVariables() {
    Set<Variable> retval = new HashSet<Variable>();
    retval.add(this);
    return retval;
  }
  
}
