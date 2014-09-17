package org.manifold.compiler.back.microfluidics.smt2;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.ArrayList;

public class ParenList implements SExpression {

  private List<SExpression> exprs = new ArrayList<>();
  
  public ParenList() { }
  
  public ParenList(SExpression expr) {
    this.exprs.add(expr);
  }
  
  public ParenList(SExpression exprs[]) {
    for (SExpression expr : exprs) {
      this.exprs.add(expr);
    }
  }
  
  public ParenList(List<SExpression> exprs) {
    this.exprs.addAll(exprs);
  }
  
  @Override
  public void write(Writer writer) throws IOException {
    writer.write('(');
    for (SExpression expr : exprs) {
      writer.write(' ');
      expr.write(writer);
    }
    writer.write(' ');
    writer.write(')');
  }

  @Override
  public void accept(SExpressionVisitor visitor) {
    visitor.visit(this);
  }

}
