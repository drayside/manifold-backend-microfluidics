package org.manifold.compiler.back.microfluidics.matlab;

import java.io.IOException;
import java.io.Writer;

public class AssignmentStatement extends MatlabStatement {
  
  protected String variableName;
  protected String rhs;
  
  public String getVariableName() {
    return variableName;
  }
  
  public String getRhs() {
    return rhs;
  }
  
  public AssignmentStatement(String variableName) {
    this.variableName = variableName;
  }
  
  public AssignmentStatement(String variableName, String rhs) {
    this.variableName = variableName;
    
    if (rhs.charAt(rhs.length() - 1) != ';') {
      rhs = rhs.concat(";");
    }
    this.rhs = rhs;
  }

  protected String getRhsString() {
    return rhs;
  }
  
  @Override
  public void write(Writer writer) throws IOException {
    StringBuilder builder = new StringBuilder();
    builder.append(variableName);
    builder.append(" = ");
    builder.append(getRhsString());
    
    writer.write(builder.toString());
  }

}
