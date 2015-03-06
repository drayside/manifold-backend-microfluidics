package org.manifold.compiler.back.microfluidics.validation;

public class LinearTerm {

  // represent a term of the form A * X,
  // where A is a constant expression
  // and X is a variable
  
  private final Expression scalarExpr;
  private final Variable var;
  
  public Expression getScalarExpression() {
    return this.scalarExpr;
  }
  
  public Variable getVariable() {
    return this.var;
  }
  
  public LinearTerm(Expression scalarExpr, Variable var) {
    this.scalarExpr = scalarExpr;
    this.var = var;
  }
  
  public LinearTerm(Variable var) {
    // Coefficient of 1
    this(new Constant(1.0), var);
  }
  
}
