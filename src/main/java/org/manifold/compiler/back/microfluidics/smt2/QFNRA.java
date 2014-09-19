package org.manifold.compiler.back.microfluidics.smt2;

// Helper class for generating valid QF_NRA S-expressions.
public class QFNRA {
  
  private static SExpression infix(SExpression e1, String op, SExpression e2) {
    SExpression exprs[] = new SExpression[] {
      new Symbol(op),
      e1,
      e2
    };
    return new ParenList(exprs);
  }
  
  public static SExpression add(SExpression e1, SExpression e2) {
    return infix(e1, "+", e2);
  }
  
  public static SExpression subtract(SExpression e1, SExpression e2) {
    return infix(e1, "-", e2);
  }
  
  public static SExpression pow(SExpression base, SExpression exp) {
    return infix(base, "^", exp);
  }
  
  public static SExpression assertThat(SExpression term) {
    SExpression assertExprs[] = new SExpression[] {
      new Symbol("assert"),
      term
    };
    return new ParenList(assertExprs);
  }
  
  public static SExpression assertEqual(SExpression e1, SExpression e2) {
    return assertThat(infix(e1, "=", e2));
  }
  
  public static SExpression assertLessThan(SExpression e1, SExpression e2) {
    return assertThat(infix(e1, "<", e2));
  }
  
  public static SExpression assertGreater(SExpression e1, SExpression e2) {
    return assertThat(infix(e1, ">", e2));
  }
  
  public static SExpression assertLessThanEqual(
      SExpression e1, SExpression e2) {
    return assertThat(infix(e1, "<=", e2));
  }
  
  public static SExpression assertGreaterEqual(SExpression e1, SExpression e2) {
    return assertThat(infix(e1, ">=", e2));
  }
}
