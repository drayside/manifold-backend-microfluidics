package org.manifold.compiler.back.microfluidics.matlab;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class FunctionCall extends MatlabStatement {

  private String functionName;
  private List<String> args;
  
  public String getFunctionName() {
    return functionName;
  }
  
  public List<String> getArgs() {
    return args;
  }
  
  public FunctionCall(String functionName, List<String> args) {
    this.functionName = functionName;
    this.args = args;
  }
  
  @Override
  public void write(Writer writer) throws IOException {
    StringBuilder builder = new StringBuilder();
    builder.append(getFunctionName());
    builder.append("(");
    builder.append(toDelimitedString(getArgs(), ","));
    builder.append(");");
    writer.write(builder.toString());
  }

}
