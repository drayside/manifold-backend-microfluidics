package org.manifold.compiler.back.microfluidics.smt2;

import java.util.LinkedList;
import java.util.List;

/**
 * Helper class for generating valid QF_NRA S-expressions which are the format
 * read by dReal when performing SMT solving QF_NRA stands for Quantifier-Free
 * Nonlinear Real Arithmetic
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class QFNRA {
  
	/**
	 * Translates a normal human readable equation into one readable by dReal
	 * Normal equation: e1 * e2
	 * dReal readable equivalent: (* e1 e2)
	 * 
	 * @param e1  First expression, on the left
	 * @param op  Operator
	 * @param e2  Second expression, on the right
	 * @return ParenList containing the new expression
	 */
  private static SExpression infix(SExpression e1, String op, SExpression e2) {
    SExpression exprs[] = new SExpression[] {
      new Symbol(op),
      e1,
      e2
    };
    return new ParenList(exprs);
  }
  
  /**
   * Initialize the SMT statement and give it a header to tell 
   * dReal that the set-logic is QF_NRA
   * 
   * @return ParenList containing the header Symbols
   */
  public static SExpression useQFNRA() {
    SExpression exprs[] = new SExpression[] {
      new Symbol("set-logic"),
      new Symbol("QF_NRA")
    };
    return new ParenList(exprs);
  }
  
  /**
   * Declare a variable in the real domain
   * @param var  variable name to be defined of type Symbol
   * @return ParenList containing the variable declaration 
   */
  public static SExpression declareRealVariable(Symbol var) {
    SExpression exprs[] = new SExpression[] {
      new Symbol("declare-fun"),
      var,
      new ParenList(),
      new Symbol("Real")
    };
    return new ParenList(exprs);
  }
  
  /**
   * Inserts an add statement to the SMT expression to be passed into dReal
   * 
   * @param e1  First expression
   * @param e2  Second expression
   * @return ParenList containing the add expression 
   */
  public static SExpression add(SExpression e1, SExpression e2) {
    return infix(e1, "+", e2);
  }
  
  /**
   * Syntax for adding multiple values in dReal is (+ e1 e2 e3 ... eN)
   * 
   * @param terms  List of the expressions all being added together
   * @return ParenList containing the correct add expression
   */
  public static SExpression add(List<SExpression> terms) {
    List<SExpression> exprs = new LinkedList<SExpression>();
    exprs.add(new Symbol("+"));
    exprs.addAll(terms);
    return new ParenList(exprs);
  }
  
  /**
   * Inserts a subtraction statement into SMT expression passed into dReal
   * 
   * @param e1  First expression
   * @param e2  Second expression
   * @return ParenList containing the subtraction expression 
   */
  public static SExpression subtract(SExpression e1, SExpression e2) {
    return infix(e1, "-", e2);
  }
  
  /**
   * Inserts a multiplication statement into SMT expression passed into dReal
   * 
   * @param e1  First expression
   * @param e2  Second expression
   * @return ParenList containing the multiplication expression 
   */
  public static SExpression multiply(SExpression e1, SExpression e2) {
    return infix(e1, "*", e2);
  }
  
  /**
   * Inserts a division statement into SMT expression passed into dReal
   * 
   * @param e1  First expression
   * @param e2  Second expression
   * @return ParenList containing the division expression 
   */
  public static SExpression divide(SExpression e1, SExpression e2) {
    return infix(e1, "/", e2);
  }
  
  /**
   * Inserts an exponential statement into SMT expression passed into dReal
   * 
   * @param base  Base of the exponential
   * @param exp  Exponent of the exponential
   * @return ParenList containing the exponential expression
   */
  public static SExpression pow(SExpression base, SExpression exp) {
    return infix(base, "^", exp);
  }
  
  /**
   * Inserts an arcsin expression into SMT expression passed into dReal
   * 
   * @param argument  Angle to have the arcsin calculated for
   * @return ParenList containing the arcsin expression
   */
  public static SExpression arcsin(SExpression argument) {
    SExpression fExprs[] = new SExpression[] {
      new Symbol("arcsin"),
      argument
    };
    return new ParenList(fExprs);
  }
  
  /**
   * Inserts an assertion into the expression passed into dReal
   * 
   * @param term  dReal readable equality or inequality like (<= 3.0 x1) 
   * @return ParenList containing the assert statement
   */
  public static SExpression assertThat(SExpression term) {
    SExpression assertExprs[] = new SExpression[] {
      new Symbol("assert"),
      term
    };
    return new ParenList(assertExprs);
  }
  
  /**
   * Inserts equality assertion into the expression passed into dReal
   * 
   * @param term  dReal readable equality like (= 3.0 x1) 
   * @return ParenList containing the assert statement
   */
  public static SExpression assertEqual(SExpression e1, SExpression e2) {
    return assertThat(equal(e1, e2));
  }
  
  /**
   * Inserts an inequality assertion into the expression passed into dReal
   * 
   * @param term  dReal readable equality or inequality like (< 3.0 x1) 
   * @return ParenList containing the assert statement
   */
  public static SExpression assertLessThan(SExpression e1, SExpression e2) {
    return assertThat(lessThan(e1, e2));
  }
  
  /**
   * Inserts an inequality assertion into the expression passed into dReal
   * 
   * @param term  dReal readable equality or inequality like (> 3.0 x1) 
   * @return ParenList containing the assert statement
   */
  public static SExpression assertGreater(SExpression e1, SExpression e2) {
    return assertThat(greater(e1, e2));
  }
  
  /**
   * Inserts an inequality assertion into the expression passed into dReal
   * 
   * @param term  dReal readable equality or inequality like (<= 3.0 x1) 
   * @return ParenList containing the assert statement
   */
  public static SExpression assertLessThanEqual(
      SExpression e1, SExpression e2) {
    return assertThat(lessThanEqual(e1, e2));
  }
  
  /**
   * Inserts an inequality assertion into the expression passed into dReal
   * 
   * @param term  dReal readable equality or inequality like (>= 3.0 x1) 
   * @return ParenList containing the assert statement
   */
  public static SExpression assertGreaterEqual(SExpression e1, SExpression e2) {
    return assertThat(greaterEqual(e1, e2));
  }
  
  /**
   * Inserts an if-then-else (ite) condition, the form of the output is as
   * follows: (ite (and (= x!1 11) (= x!2 false)) 21 0), so if variable
   * x!1 = 11 AND x!2 = False then 21 is output, otherwise 0 is output
   * 
   * 
   * @param cond  Condition to be met to do t, if not met then do f
   * @param t  Then value to be output if cond is met
   * @param e  Else value to be output if cond is not met
   * @return ParenList containing the ite statement
   */
  public static SExpression conditional(
      SExpression cond, SExpression t, SExpression e) {
    SExpression exprs[] = new SExpression[] {
      new Symbol("ite"), // "if-then-else"
      cond,
      t,
      e
    };
    return new ParenList(exprs);
  }
  
  /**
   * Inserts an equality statement into the SMT expression passed into dReal
   * 
   * @param e1  First expression
   * @param e2  Second expression
   * @return ParenList containing the equality statement 
   */
  public static SExpression equal(SExpression e1, SExpression e2) {
    return infix(e1, "=", e2);
  }
  
  /**
   * Inserts a less than inequality statement into the SMT expression
   * passed into dReal
   * 
   * @param e1  First expression
   * @param e2  Second expression
   * @return ParenList containing the inequality statement 
   */
  public static SExpression lessThan(SExpression e1, SExpression e2) {
    return (infix(e1, "<", e2));
  }
  
  /**
   * Inserts a greater than inequality statement into SMT expression
   * passed into dReal
   * 
   * @param e1  First expression
   * @param e2  Second expression
   * @return ParenList containing the inequality statement 
   */
  public static SExpression greater(SExpression e1, SExpression e2) {
    return (infix(e1, ">", e2));
  }
  
  /**
   * Inserts a less than or equal inequality statement into SMT expression
   * passed into dReal
   * 
   * @param e1  First expression
   * @param e2  Second expression
   * @return ParenList containing the inequality statement 
   */
  public static SExpression lessThanEqual(SExpression e1, SExpression e2) {
    return (infix(e1, "<=", e2));
  }
  
  /**
   * Inserts a greater than or equal inequality statement into SMT expression
   * passed into dReal
   * 
   * @param e1  First expression
   * @param e2  Second expression
   * @return ParenList containing the inequality statement 
   */
  public static SExpression greaterEqual(SExpression e1, SExpression e2) {
    return (infix(e1, ">=", e2));
  }
  
}
