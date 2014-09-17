package org.manifold.compiler.back.microfluidics.smt2;

import java.io.IOException;
import java.io.Writer;

public class Symbol implements SExpression {
  private String name;
  public String getName() {
    return name;
  }
  
  private void validateName(String name) {
    // A symbol is a non-empty sequence of letters, digits,
    // and the characters + - / * = % ? ! . $ ~ & ^ < > @
    // that does not start with a digit.
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("symbol cannot be empty");
    }
    if (Character.isDigit(name.charAt(0))) {
      throw new IllegalArgumentException("symbol cannot start with digit");
    }
    String otherCharacters = "+-/*=%?!.$~&^<>@";
    for (int i = 0; i < name.length(); ++i) {
      char c = name.charAt(i);
      if (Character.isAlphabetic(c) || Character.isDigit(c)) {
        continue;
      }
      if (otherCharacters.indexOf(c) != -1) {
        continue;
      }
      throw new IllegalArgumentException(
          "symbol cannot contain character '" + c + "'");
    }
  }
  
  public Symbol(String name) {
    validateName(name);
    this.name = name;
  }
  
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (!(other instanceof Symbol)) return false;
    Symbol that = (Symbol) other;
    return (this.getName().equals(that.getName()));
  }

  @Override
  public void write(Writer writer) throws IOException {
    writer.write(getName());
  }
  
  @Override
  public void accept(SExpressionVisitor visitor) {
    visitor.visit(this);
  }
}
