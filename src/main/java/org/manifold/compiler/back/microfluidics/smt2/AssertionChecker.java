package org.manifold.compiler.back.microfluidics.smt2;

import java.util.List;

/**
 * 
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class AssertionChecker {

  // 
  private boolean nonAssertionsAreErrors = false;
  /**
   * If nonAssertionsAreErrors is true, and an expression that does not match (assert (x)) is found, 
   * verification automatically fails. Otherwise, ignore it.
   * 
   * @param b  Set to true if terms without assertions 
   */
  public void setNonAssertionsAreErrors(boolean b) {
    nonAssertionsAreErrors = b;
  }
  
  private double delta = 0.000001;
  /**
   * Change the value of delta, that value between two numbers for them to be considered equivalent.
   * Default value is 0.000001
   * 
   * @param d  Value to set delta equal to
   */
  public void setDelta(double d) {
    delta = d;
  }
  
  private ExprEvalVisitor evaluator = new ExprEvalVisitor();
  
  /**
   * Bind a variable to a value, will be visited while verifying to confirm that variable is
   * still equal to this value
   * 
   * @param var  Symbol to be bound to a value
   * @param value  Value that this Symbol is bound to
   */
  public void addBinding(Symbol var, Double value) {
    evaluator.addBinding(var, value);
  }
  
  public AssertionChecker() { }
  
  private SExpression lastExpression = null;
  /**
   * Gets the last expression verified
   * 
   * @return The last SExpression verified, whether or not it was verified correctly or not 
   */
  public SExpression getLastExpression() {
    return lastExpression;
  }
  
  private double lastLHS = Double.NaN;
  private double lastRHS = Double.NaN;
  /**
   * Gets the left hand side of the last expression verified
   * 
   * @return The LHS of the last SExpression verified, whether or not it was verified correctly or not 
   */
  public double getLastLHS() {
    return lastLHS;
  }
  /**
   * Gets the right hand side of the last expression verified
   * 
   * @return The RHS of the last SExpression verified, whether or not it was verified correctly or not 
   */
  public double getLastRHS() {
    return lastRHS;
  }
  
  /**
   * Calls verify on each SExpression in the list
   * 
   * @param exprs
   * @return True if all SExpressions are verified
   */
  public boolean verify(List<SExpression> exprs) {
    for (SExpression expr : exprs) {
      if (!verify(expr)) {
        return false;
      }
    }
    return true;
  }
  
  /**
   * Verifies that all expressions in the SExpressions are 
   * @param expr
   * @return False if malformed or (in)equality is false, True if (in)equality is true
   */
  public boolean verify(SExpression expr) {
    lastExpression = expr;
    lastLHS = Double.NaN;
    lastRHS = Double.NaN;
    if (isNonAssertion(expr)) {
      if (nonAssertionsAreErrors) {
        return false;
      } else {
        return true;
      }
    }
    // Check to make sure term is in the form 'operator var/val var/val' where var/val is either a variable or value
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
    double valLeft, valRight;
    SExpression eLeft = ((ParenList) term).getExprs().get(1);
    SExpression eRight = ((ParenList) term).getExprs().get(2);
    try {
      eLeft.accept(evaluator);
      valLeft = evaluator.getValue();
      lastLHS = valLeft;
      eRight.accept(evaluator);
      valRight = evaluator.getValue();
      lastRHS = valRight;
    } catch (Exception e) {
      // One of the vals is not is ExprEvalVisitor 
      return false;
    }
    if (booleanSym.equals(new Symbol("="))) {
      return Math.abs(valLeft - valRight) < delta;
    } else if (booleanSym.equals(new Symbol("<"))) {
      return valLeft < valRight;
    } else if (booleanSym.equals(new Symbol("<="))) {
      return valLeft <= valRight;
    } else if (booleanSym.equals(new Symbol(">"))) {
      return valLeft > valRight;
    } else if (booleanSym.equals(new Symbol(">="))) {
      return valLeft >= valRight;
    } else {
      // unknown operator
      return false;
    }
  }
  
  /**
   * Checks that an SExpression is an assertion, which is when a variable is forced to be related to another
   * in some way, either equality or inequality. Relation can also be to a number, i.e. x > 5
   * 
   * @param assertion  The expression that needs to be determined if its an assertion
   * @return True or False
   */
  public static boolean isNonAssertion(SExpression assertion) {
    // TODO: I think this can be entirely replaced by an instanceof call if assertions were made into a superclass
    // assertion and each specific assertion extended this
    // Assertion must be a ParenList to possibly be an assertion since it requires at least 2 expressions
    if (!(assertion instanceof ParenList)) {
      return true;
    }
    // Assertion must include at least 2 expressions
    if (((ParenList) assertion).getExprs().size() != 2) {
      return true;
    }
    // First expression must be a Symbol that says 'assert'
    SExpression assertSym = ((ParenList) assertion).getExprs().get(0);
    if (!(assertSym instanceof Symbol)) {
      return true;
    }
    if (!((Symbol) assertSym).equals(new Symbol("assert"))) {
      return true;
    }
    // Second term must be a ParenList, but it can contain any number of terms
    SExpression term = ((ParenList) assertion).getExprs().get(1);
    if (!(term instanceof ParenList)) {
      return true;
    }
    return false;
  }
  
}