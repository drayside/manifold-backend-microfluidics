package org.manifold.compiler.back.microfluidics.matlab;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
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
    this.args = new LinkedList<String>();
    
    for (String arg: args) {
      this.args.add(arg);
    }
  }

  public String getCall() {
    StringBuilder builder = new StringBuilder();
    builder.append(getFunctionName());
    builder.append("(");

    String delimiter = "";
    for (String arg: getArgs()) {
      builder.append(delimiter).append(arg);
      delimiter = ",";
    }
    
    builder.append(");");
    return builder.toString();
  }
  
  @Override
  public void write(Writer writer) throws IOException {
    writer.write(getCall());
  }

}
