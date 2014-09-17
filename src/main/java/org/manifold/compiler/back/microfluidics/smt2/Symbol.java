package org.manifold.compiler.back.microfluidics.smt2;

import java.io.IOException;
import java.io.Writer;

public class Symbol extends SExpression {
  // TODO symbol names do not always need to be escaped
  // so only add |s if it is absolutely necessary
  
  private String name;
  public String getName() {
    return name;
  }
  
  private void validateName(String name) {
    // A symbol is a any sequence of printable ASCII characters (including 
    // space, tab, and line-breaking characters) except for the 
    // backslash character \, that starts and ends with | 
    // and does not otherwise contain |.
    if (!name.startsWith("|")) {
      throw new IllegalArgumentException("symbol must start with '|'");
    }
    if (!name.endsWith("|")) {
      throw new IllegalArgumentException("symbol must end with '|'");
    }
    if (name.indexOf('\\') != -1) {
      throw new IllegalArgumentException("symbol cannot contain '\\'");
    }
  }
  
  public Symbol(String name) {
    // If the name isn't delimited by |s, add them.
    if (!name.startsWith("|")) {
      name = "|" + name;
    }
    if (!name.endsWith("|")) {
      name = name + "|";
    }
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
