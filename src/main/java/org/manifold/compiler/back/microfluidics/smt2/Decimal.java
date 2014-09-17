package org.manifold.compiler.back.microfluidics.smt2;

import java.io.IOException;
import java.io.Writer;

public class Decimal extends SExpression {

  private String repr;
  public String getRepresentation() {
    return repr;
  }
  public double getValue() {
    return Double.parseDouble(getRepresentation());
  }
  
  private void validateRepr(String repr) {
    // (numeral).0*(numeral)
    // where a numeral is either 0 or a non-empty sequence of digits
    // not starting with 0
    if (repr == null || repr.isEmpty()) {
      throw new IllegalArgumentException(
          "decimal representation cannot be empty");
    }
    int decimalIdx = repr.indexOf('.');
    if (decimalIdx == -1) {
      decimalIdx = repr.length();
      repr = repr.concat(".0");
    }
    // make sure there is some string left after the decimal point
    if (decimalIdx + 1 == repr.length()) {
      throw new IllegalArgumentException(
          "decimal representation '" + repr 
          + "' must have a numeral after decimal point");
    }
    String integerPart = repr.substring(0, decimalIdx);
    try {
      Integer.parseInt(integerPart);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "malformed decimal representation '" + repr + "'");
    }
    String decimalPart = repr.substring(decimalIdx + 1);
    // each character of decimalPart must be a digit
    for (int i = 0; i < decimalPart.length(); ++i) {
      char c = decimalPart.charAt(i);
      if (!Character.isDigit(c)) {
        throw new IllegalArgumentException(
            "decimal representation '" + repr 
            + "' cannot contain non-digit character '" + c + "'");
      }
    }
    this.repr = repr;
  }
  
  public Decimal(String repr) {
    validateRepr(repr);
    this.repr = repr;
  }
  
  public Decimal(double value) {
    this.repr = Double.toString(value);
  }
  
  @Override
  public void write(Writer writer) throws IOException {
    writer.write(getRepresentation());
  }

  @Override
  public void accept(SExpressionVisitor visitor) {
    visitor.visit(this);
  }

}
