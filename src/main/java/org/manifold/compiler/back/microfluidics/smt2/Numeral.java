package org.manifold.compiler.back.microfluidics.smt2;

import java.io.IOException;
import java.io.Writer;

public class Numeral implements SExpression {
  private long value;
  public long getValue() {
    return this.value;
  }
  
  public Numeral(long value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Numeral)) {
      return false;
    }
    Numeral that = (Numeral) other;
    return (this.getValue() == that.getValue());
  }
  
  @Override
  public void write(Writer writer) throws IOException {
    writer.write(Long.toString(getValue()));
  }

  @Override
  public void accept(SExpressionVisitor visitor) {
    visitor.visit(this);
  }
}
