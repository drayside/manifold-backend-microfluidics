package org.manifold.compiler.back.microfluidics.smt2;

import java.io.IOException;
import java.io.Writer;

public class Symbol extends SExpression {
 
  private String name;
  public String getName() {
    return name;
  }
  
  private static String otherLegalChars = "+-/*=%?!.%_!&^<>@";
  
  private void validateName(String name) {
    // A symbol is a non-empty sequence of letters, digits, and the characters
    // + - / * = % ? ! . $ _ ~ & ^ < > @
    // that does not start with a digit.
    if (name.isEmpty()) {
      throw new IllegalArgumentException(
          "symbol name cannot be empty");
    }
    if (Character.isDigit(name.charAt(0))) {
      throw new IllegalArgumentException(
          "symbol name cannot start with a digit");
    }
    for (int i = 0; i < name.length(); ++i) {
      char c = name.charAt(i);
      if (Character.isAlphabetic(c)) {
        continue;
      } else if (Character.isDigit(c)) {
        continue;
      } else if (otherLegalChars.indexOf(c) == -1) {
        throw new IllegalArgumentException("character '" + c + "'"
            + " cannot appear in a symbol name");
      }
    }
  }
  
  public Symbol(String name) {
    validateName(name);
    this.name = name;
  }
  
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof Symbol)) {
      return false;
    }
    Symbol that = (Symbol) other;
    return (this.getName().equals(that.getName()));
  }
  
  @Override
  public int hashCode() {
    return getName().hashCode();
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
