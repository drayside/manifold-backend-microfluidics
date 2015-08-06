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
  
  public static SExpression useQFNRA() {
    SExpression exprs[] = new SExpression[] {
        new Symbol("set-logic"),
        new Symbol("QF_NRA")
    };
    return new ParenList(exprs);
  }
  
  public static SExpression declareRealVariable(Symbol var) {
    SExpression exprs[] = new SExpression[] {
        new Symbol("declare-fun"),
        var,
        new ParenList(),
        new Symbol("Real")
    };
    return new ParenList(exprs);
  }
  
  public static SExpression add(SExpression e1, SExpression e2) {
    return infix(e1, "+", e2);
  }
  
  public static SExpression subtract(SExpression e1, SExpression e2) {
    return infix(e1, "-", e2);
  }
  
  public static SExpression multiply(SExpression e1, SExpression e2) {
    return infix(e1, "*", e2);
  }
  
  public static SExpression divide(SExpression e1, SExpression e2) {
    return infix(e1, "/", e2);
  }
  
  public static SExpression pow(SExpression base, SExpression exp) {
    return infix(base, "^", exp);
  }
  
  public static SExpression arcsin(SExpression argument) {
    SExpression fExprs[] = new SExpression[] {
      new Symbol("arcsin"),
      argument
    };
    return new ParenList(fExprs);
  }
  
  public static SExpression log(SExpression argument) {
    SExpression fExprs[] = new SExpression[] {
      new Symbol("log"),
      argument
    };
    return new ParenList(fExprs);
  }
  
  public static SExpression exp(SExpression argument) {
    SExpression fExprs[] = new SExpression[] {
      new Symbol("exp"),
      argument
    };
    return new ParenList(fExprs);
  }
  
  public static SExpression sqrt(SExpression argument) {
    SExpression fExprs[] = new SExpression[] {
      new Symbol("sqrt"),
      argument
    };
    return new ParenList(fExprs);
  }
  
  public static SExpression or(SExpression e1, SExpression e2) {
    SExpression exprs[] = new SExpression[] {
      new Symbol("or"),
      e1,
      e2
    };
    return new ParenList(exprs);
  }
  
  public static SExpression min(SExpression e1, SExpression e2) {
    SExpression exprs[] = new SExpression[] {
      new Symbol("min"),
      e1,
      e2
    };
    return new ParenList(exprs);
  }
  
  public static SExpression assertThat(SExpression term) {
    SExpression assertExprs[] = new SExpression[] {
      new Symbol("assert"),
      term
    };
    return new ParenList(assertExprs);
  }
  
  public static SExpression assertEqual(SExpression e1, SExpression e2) {
    return assertThat(equal(e1, e2));
  }
  
  public static SExpression assertLessThan(SExpression e1, SExpression e2) {
    return assertThat(lessThan(e1, e2));
  }
  
  public static SExpression assertGreater(SExpression e1, SExpression e2) {
    return assertThat(greater(e1, e2));
  }
  
  public static SExpression assertLessThanEqual(
      SExpression e1, SExpression e2) {
    return assertThat(lessThanEqual(e1, e2));
  }
  
  public static SExpression assertGreaterEqual(SExpression e1, SExpression e2) {
    return assertThat(greaterEqual(e1, e2));
  }
  
  public static SExpression conditional(
      SExpression cond, SExpression t, SExpression f) {
    SExpression exprs[] = new SExpression[] {
      new Symbol("ite"), // "if-then-else"
      cond,
      t,
      f
    };
    return new ParenList(exprs);
  }
  
  public static SExpression equal(SExpression e1, SExpression e2) {
    return infix(e1, "=", e2);
  }
  
  public static SExpression lessThan(SExpression e1, SExpression e2) {
    return (infix(e1, "<", e2));
  }
  
  public static SExpression greater(SExpression e1, SExpression e2) {
    return (infix(e1, ">", e2));
  }
  
  public static SExpression lessThanEqual(SExpression e1, SExpression e2) {
    return (infix(e1, "<=", e2));
  }
  
  public static SExpression greaterEqual(SExpression e1, SExpression e2) {
    return (infix(e1, ">=", e2));
  }
  
}
