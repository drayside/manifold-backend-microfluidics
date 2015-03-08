package org.manifold.compiler.back.microfluidics.matlab;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.manifold.compiler.back.microfluidics.CodeGenerationError;

public class InstantiationStatement extends MatlabStatement {

  private String variableName;
  private String expression;
  private Boolean isTemplate;
  
  // The following are only relevant if the object is currently a template
  // They contain information about the object that must be instantiated
  // when the template receives parameters
  private String objectName;
  private List<String> parameterOrder;
  private List<String> parameters;
  
  public String getVariableName() {
    return this.variableName;
  }

  public Boolean getIsTemplate() {
    return this.isTemplate;
  }
  
  public String getObjectName() {
    return getIsTemplate() ? this.objectName : null;
  }
  
  public List<String> getParameterOrder() {
    return getIsTemplate() ? this.parameterOrder : Collections.emptyList();
  }
  
  public String getExpression() {
    return getIsTemplate() ? getExpressionTemplate() : this.expression;
  }
  
  public List<String> getParameters() {
    return this.parameters;
  }
  
  public InstantiationStatement(String variableName,
      String objectName, List<String> parameterOrder) {
    this.variableName = variableName;
    this.expression = null;
    this.isTemplate = true;
    this.objectName = objectName;
    this.parameterOrder = parameterOrder;
    this.parameters = Collections.emptyList();
  }
    
  public InstantiationStatement(String variableName, String expression) {
    this.variableName = variableName;
    this.expression = expression;
    this.isTemplate = false;
  }
  
  public void fillTemplate(Map<String, String> parameters) {
    if (!getIsTemplate()) {
      throw new CodeGenerationError(
          "ObjectInstantiationStatement is not a template");
    }

    List<String> params = new LinkedList<String>();
    
    for (String param: getParameterOrder()) {
      String value = parameters.get(param);
      if (value == null) {
        throw new CodeGenerationError("Could not find parameter " + param +
            " to instantiate " + getObjectName());
      }
      
      params.add(value);
    }
    
    this.parameters = params;
    this.expression = buildExpression();
    this.isTemplate = false;
  }
  
  private String buildExpression() {
    if (!getIsTemplate()) {
      return null;
    }

    return new FunctionCall(getObjectName(), getParameters()).getCall();
  }
  
  @Override
  public void write(Writer writer) throws IOException {
    writer.write(writeStatement());
  }
  
  public String writeStatement() {
    String expr = getIsTemplate() ? getExpressionTemplate() : this.expression;

    StringBuilder builder = new StringBuilder();
    builder.append(this.variableName);
    builder.append(" = ");
    builder.append(expr);
    
    if (expr.charAt(expr.length() - 1) != ';') {
      builder.append(";");
    }
 
    return builder.toString();
  }
  
  private String getExpressionTemplate() {
    StringBuilder builder = new StringBuilder();
    builder.append(getObjectName());
    builder.append("(");

    String delimiter = "";
    for (String param: getParameterOrder()) {
      builder.append(delimiter);
      builder.append("{");
      builder.append(param);
      builder.append("}");
      delimiter = ",";
    }
    
    builder.append(");");
    return builder.toString();
  }

}
