package org.manifold.compiler.back.microfluidics.smt2;

// Helper class for generating valid QF_NRA S-expressions.
public class QFNRA {
  
  public static SExpression add(SExpression e1, SExpression e2) {
    SExpression exprs[] = new SExpression[] {
      new Symbol("+"),
      e1,
      e2
    };
    return new ParenList(exprs);
  }
  
  public static SExpression subtract(SExpression e1, SExpression e2) {
    SExpression exprs[] = new SExpression[] {
      new Symbol("-"),
      e1,
      e2
    };
    return new ParenList(exprs);
  }
  
  public static SExpression pow(SExpression base, SExpression exp) {
    SExpression exprs[] = new SExpression[] {
      new Symbol("^"),
      base,
      exp
    };
    return new ParenList(exprs);
  }
  
  public static SExpression assertEqual(SExpression e1, SExpression e2) {
    SExpression equalExprs[] = new SExpression[] {
      new Symbol("="),
      e1,
      e2
    };
    SExpression equalExpr = new ParenList(equalExprs);
    SExpression assertExprs[] = new SExpression[] {
      new Symbol("assert"),
      equalExpr
    };
    return new ParenList(assertExprs);
  }
  
  public static SExpression assertGreaterEqual(SExpression e1, SExpression e2) {
    SExpression equalExprs[] = new SExpression[] {
      new Symbol(">="),
      e1,
      e2
    };
    SExpression equalExpr = new ParenList(equalExprs);
    SExpression assertExprs[] = new SExpression[] {
      new Symbol("assert"),
      equalExpr
    };
    return new ParenList(assertExprs);
  }
}
