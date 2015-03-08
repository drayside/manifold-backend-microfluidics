package org.manifold.compiler.back.microfluidics.matlab;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public abstract class MatlabStatement {
  public abstract void write(Writer writer) throws IOException;
	
  @Override
  public String toString() {
    StringWriter sWrite = new StringWriter();
    try {
      write(sWrite);
      return sWrite.toString();
    } catch (IOException e) {
      throw new IllegalStateException(
          "could not convert matlab statement to string: " + e.getMessage());
    }
  }
}
