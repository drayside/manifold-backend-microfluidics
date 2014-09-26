package org.manifold.compiler.back.microfluidics.smt2;

import java.util.List;

public class AssertionChecker {

  // If true, and an expression that does not match (assert (x)) is found,
  // verification automatically fails. Otherwise, ignore it.
  private boolean nonAssertionsAreErrors = false;
  public void setNonAssertionsAreErrors(boolean b) {
    nonAssertionsAreErrors = b;
  }
  
  // TODO delta for comparisons
  
  private ExprEvalVisitor evaluator = new ExprEvalVisitor();
  
  public void addBinding(Symbol var, Double value) {
    evaluator.addBinding(var, value);
  }
  
  public AssertionChecker() { }
  
  private SExpression lastFailingExpression = null;
  public SExpression getLastFailingExpression() {
    return lastFailingExpression;
  }
  
  public boolean verify(List<SExpression> exprs) {
    for (SExpression expr : exprs) {
      if (!verify(expr)) {
        lastFailingExpression = expr;
        return false;
      }
    }
    return true;
  }
  
  public boolean verify(SExpression expr) {
    if (isNonAssertion(expr)) {
      if (nonAssertionsAreErrors) {
        return false;
      } else {
        return true;
      }
    }
    SExpression term = ((ParenList) expr).getExprs().get(1);
    if (((ParenList) term).getExprs().size() != 3) {
      // malformed
      return false;
    }
    SExpression booleanSym = ((ParenList) term).getExprs().get(0);
    if (!(booleanSym instanceof Symbol)) {
      // malformed
      return false;
    }
    SExpression eLeft = ((ParenList) term).getExprs().get(1);
    double valLeft, valRight;
    SExpression eRight = ((ParenList) term).getExprs().get(2);
    try {
      eLeft.accept(evaluator);
      valLeft = evaluator.getValue();
      eRight.accept(evaluator);
      valRight = evaluator.getValue();
    } catch (Exception e) {
      return false;
    }
    if (booleanSym.equals(new Symbol("="))) {
      return (valLeft == valRight);
    } else if (booleanSym.equals(new Symbol("<"))) {
      return (valLeft < valRight);
    } else if (booleanSym.equals(new Symbol("<="))) {
      return (valLeft <= valRight);
    } else if (booleanSym.equals(new Symbol(">"))) {
      return (valLeft > valRight);
    } else if (booleanSym.equals(new Symbol(">="))) {
      return (valLeft >= valRight);
    } else {
      // unknown operator
      return false;
    }
  }
  
  public static boolean isNonAssertion(SExpression assertion) {
    if (!(assertion instanceof ParenList)) {
      return true;
    }
    if (((ParenList) assertion).getExprs().size() != 2) {
      return true;
    }
    SExpression assertSym = ((ParenList) assertion).getExprs().get(0);
    if (!(assertSym instanceof Symbol)) {
      return true;
    }
    if (!((Symbol) assertSym).equals(new Symbol("assert"))) {
      return true;
    }
    SExpression term = ((ParenList) assertion).getExprs().get(1);
    if (!(term instanceof ParenList)) {
      return true;
    }    
    return false;
  }
  
}
