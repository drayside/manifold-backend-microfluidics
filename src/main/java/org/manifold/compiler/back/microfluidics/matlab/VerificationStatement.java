package org.manifold.compiler.back.microfluidics.matlab;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * A verification statement is simply a function call with a binary
 * output. A global variable ensures that all verifications pass by
 * computing the logical AND of every verification's output
 */
public class VerificationStatement extends MatlabStatement {
  
  private String functionName;
  private List<String> args;
  
  public VerificationStatement(String functionName, List<String> args) {
    this.functionName = functionName;
    this.args = args;
  }

  @Override
  public void write(Writer writer) throws IOException {
    String variableName = "res";
    String rhs;

    StringBuilder builder = new StringBuilder();
    builder.append(variableName);
    builder.append(" & ");
    builder.append(new FunctionCall(this.functionName, this.args).toString());
    rhs = builder.toString();

    AssignmentStatement assignment = new AssignmentStatement(variableName, rhs);
    
    builder = new StringBuilder();
    builder.append(assignment.toString());
    builder.append("\n");
    builder.append("if(" + variableName + " == 0)\n");
    builder.append("\terror('Failed " + functionName + "')\n");
    builder.append("end\n");
    writer.write(builder.toString());
  }

}
