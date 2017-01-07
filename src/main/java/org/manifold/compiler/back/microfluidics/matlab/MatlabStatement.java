package org.manifold.compiler.back.microfluidics.matlab;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

public abstract class MatlabStatement {
  public abstract void write(Writer writer) throws IOException;
  
  protected String toDelimitedString(List<String> values, String delimiter) {
    StringBuilder builder = new StringBuilder();
    String d = "";
    
    for (String val: values) {
      builder.append(d).append(val);
      d = delimiter;
    }
    
    return builder.toString();
  }
	
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
