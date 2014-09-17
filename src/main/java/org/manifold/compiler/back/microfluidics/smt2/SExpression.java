package org.manifold.compiler.back.microfluidics.smt2;

import java.io.IOException;
import java.io.Writer;

public interface SExpression {
  public void write(Writer writer) throws IOException;
  public void accept(SExpressionVisitor visitor);
}
