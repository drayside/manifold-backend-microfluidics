package org.manifold.compiler.back.microfluidics.matlab;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.manifold.compiler.back.microfluidics.CodeGenerationError;

public class InstantiationStatement extends AssignmentStatement {
  
  private Boolean isTemplate;
  private String objectName;
  private List<String> paramOrder;
  private List<String> params;
  
  public Boolean getIsTemplate() {
    return isTemplate;
  }
  
  public InstantiationStatement(String variableName,
      String objectName, List<String> paramOrder) {
    super(variableName);
    this.isTemplate = true;
    this.objectName = objectName;
    this.paramOrder = paramOrder;
    this.params = new LinkedList<String>();
    this.rhs = getRhsString();
  }
  
  public void fillTemplate(Map<String, String> params) {
    for (String param: this.paramOrder) {
      String value = params.get(param);
      if (value == null) {
        throw new CodeGenerationError(
            "Could not find value for param " + param);
      }
      
      this.params.add(value);
      this.isTemplate = false;
    }
  }
  
  @Override
  protected String getRhsString() {
    StringBuilder builder = new StringBuilder();
    builder.append(objectName);
    builder.append("(");
    
    String args = getIsTemplate() ? getTemplateArgs() : getInstantiationArgs();
    builder.append(args);
    
    builder.append(");");
    
    return builder.toString();
  }
  
  private String getTemplateArgs() {
    List<String> templateParams = new LinkedList<String>();
    for (String param: paramOrder) {
      templateParams.add("{" + param + "}");
    }
    
    return toDelimitedString(templateParams, ",");
  }
  
  private String getInstantiationArgs() {
    return toDelimitedString(params, ",");
  }
}
