package org.manifold.compiler.back.microfluidics.smt2;

import java.io.IOException;
import java.io.Writer;

/**
 * Defines an real decimal value in the as an SExpression that is in QF_NRA form
 * that is required for dReal to determine if a solution is possible
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class Decimal extends SExpression {

  private String repr;
  
  /**
   * Gets the QF_NRA representation of a decimal
   * 
   * @return repr - QF_NRA representation of a decimal
   */
  public String getRepresentation() {
    return repr;
  }
  
  /**
   * Convert the string of the decimal representation in QF_NRA form to a Double
   * 
   * @return Double of the value of this decimal
   */
  public double getValue() {
    return Double.parseDouble(getRepresentation());
  }
  
  /**
   * Check value entered if its a string to ensure that it is actually a decimal
   * 
   * @param repr - Input string representing a decimal
   */
  private void validateRepr(String repr) {
    // (numeral).0*(numeral)
    // where a numeral is either 0 or a non-empty sequence of digits
    // not starting with 0
    if (repr == null || repr.isEmpty()) {
      throw new IllegalArgumentException(
          "decimal representation cannot be empty");
    }
    int decimalIdx = repr.indexOf('.');
    // if there is no decimal then the number entered is an integer, so append a
    // .0 to it to make it a decimal
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
    // each character of integerPart must be a digit
    try {
      Integer.parseInt(integerPart);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "malformed decimal representation '" + repr + "'");
    }

    String decimalPart = repr.substring(decimalIdx + 1);
    // each character of decimalPart must be a digit
    try {
      Integer.parseInt(decimalPart);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "malformed decimal representation '" + repr + "'");
    }
    this.repr = repr;
  }
  
  /**
   * A decimal number for use in an SExpresion to send to dReal to solve, 
   * also checks that the input string is actually an decimal
   * 
   * @param repr  String representing a decimal
   */
  public Decimal(String repr) {
    validateRepr(repr);
    this.repr = repr;
  }
  
  /**
   * A decimal number for use in an SExpresion to send to dReal to solve
   * 
   * @param value  Double that is stored as a string representing the decimal
   */
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
