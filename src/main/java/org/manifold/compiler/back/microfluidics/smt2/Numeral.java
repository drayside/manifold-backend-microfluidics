package org.manifold.compiler.back.microfluidics.smt2;

import java.io.IOException;
import java.io.Writer;

/**
 * Defines an real numerical integer value in the form of an SExpression to
 * allow for it to be compatible with the QF_NRA form required for dReal
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class Numeral extends SExpression {
  private final long value;
  
  /**
   * Get the real numerical value of this object
   * @return value - A real number
   */
  public long getValue() {
    return this.value;
  }
  
  /**
   * Construct the Number to have its real numerical value be set to value
   * @param value  A real number
   */
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
