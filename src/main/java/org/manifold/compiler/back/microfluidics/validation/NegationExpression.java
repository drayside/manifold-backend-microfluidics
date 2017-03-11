package org.manifold.compiler.back.microfluidics.validation;

import java.util.Set;

public class NegationExpression extends Expression {

  private final Expression subexpression;
  
  public NegationExpression(Expression subexpr) {
    this.subexpression = subexpr;
  }
  
  @Override
  public String toString() {
    return "-(" + subexpression.toString() + ")";
  }

  @Override
  public Set<Variable> freeVariables() {
    return subexpression.freeVariables();
  }
  
}
