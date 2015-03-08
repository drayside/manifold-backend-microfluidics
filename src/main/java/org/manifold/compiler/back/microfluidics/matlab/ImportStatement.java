package org.manifold.compiler.back.microfluidics.matlab;

import java.io.IOException;
import java.io.Writer;

public class ImportStatement extends MatlabStatement {

  private String pkg;
  public String getPkg() {
    return pkg;
  }
  
  public ImportStatement(String pkg) {
    this.pkg = pkg;
  }
  
  @Override
  public int hashCode() {
    return getPkg().hashCode();
  }
  
  @Override
  public void write(Writer writer) throws IOException {
    writer.write("import " + getPkg() + ";");
  }

}
