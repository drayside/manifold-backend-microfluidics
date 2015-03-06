package org.manifold.compiler.back.microfluidics.validation;

import java.util.HashSet;
import java.util.Set;

public class Constant extends Expression {

  private final Double value;
  public Double getValue() {
    return this.value;
  }
  
  public Constant(Double value) {
    this.value = value;
  }
  
  @Override
  public boolean equals(Object aThat) {
    if (this == aThat) return true;
    if (!(aThat instanceof Constant)) return false;
    Constant that = (Constant) aThat;
    return (this.getValue().equals(that.getValue()));
  }
  
  @Override
  public int hashCode() {
    return value.hashCode();
  }
  
  @Override
  public String toString() {
    return value.toString();
  }

  @Override
  public Set<Variable> freeVariables() {
    return new HashSet<Variable>();
  }
  
}
