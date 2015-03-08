package org.manifold.compiler.back.microfluidics.matlab;

import java.io.IOException;
import java.io.Writer;

import org.manifold.compiler.back.microfluidics.smt2.Symbol;

public class ImportStatement extends MatlabStatement {

  private String pkg;
  public String getPkg() {
    return pkg;
  }
  
  public ImportStatement(String pkg) {
    this.pkg = pkg;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    
    if (!(other instanceof ImportStatement)) {
      return false;
    }
    
    ImportStatement that = (ImportStatement) other;
    return (this.getPkg().equals(that.getPkg()));
  }

  @Override
  public void write(Writer writer) throws IOException {
    writer.write("import " + getPkg() + ";");
  }

}
