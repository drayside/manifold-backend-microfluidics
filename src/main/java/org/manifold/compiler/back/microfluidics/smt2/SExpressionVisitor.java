package org.manifold.compiler.back.microfluidics.smt2;

public interface SExpressionVisitor {
  public void visit(Symbol s);
  public void visit(Numeral n);
  public void visit(Decimal d);
  public void visit(ParenList l);
}
