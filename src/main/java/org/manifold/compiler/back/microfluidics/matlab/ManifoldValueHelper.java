package org.manifold.compiler.back.microfluidics.matlab;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.Value;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;


/**
 * Collection of helpers to generate the appropriate statements
 * to interact with Manifold values in Matlab
 */
public class ManifoldValueHelper {

  private static ManifoldValueHelper helper = new ManifoldValueHelper();
  private Map<String, Integer> valueCount;
  
  private ManifoldValueHelper() {
    this.valueCount = new HashMap<String, Integer>();
  }
  
  private static String variableNameForValue(String valueKey) {
    if (!helper.valueCount.containsKey(valueKey)) {
      helper.valueCount.put(valueKey, 0);
    }
    
    Integer newCount = helper.valueCount.get(valueKey) + 1;
    helper.valueCount.put(valueKey, newCount);
    String variableName = valueKey + newCount.toString();
    return variableName;
  }

  public static List<MatlabStatement> statementsForValue(Value value) {
    List<MatlabStatement> stmts;
    
    if (value instanceof NodeValue) {
      stmts = statementsForNodeValue((NodeValue) value);
    } else if (value instanceof ConnectionValue) {
      stmts = statementsForConnectionValue((ConnectionValue) value);
    } else {
      throw new CodeGenerationError(
          "Could not generate matlab statements for type " +
          value.getType().toString());
    }
    
    return stmts;
  }
  
  public static List<MatlabStatement> statementsForNodeValue(NodeValue value) {
    List<MatlabStatement> stmts = new LinkedList<MatlabStatement>();
    stmts.add(new ImportStatement("types.Node"));

    // Create a templatized initialization of the Node class since we don't
    // have co-ordinates for the node yet.
    String variableName = variableNameForValue("node");
    List<String> paramOrder = new LinkedList<String>();
    paramOrder.add("x");
    paramOrder.add("y");

    stmts.add(new InstantiationStatement(variableName, "Node", paramOrder));

    return stmts;
  }
  
  public static List<MatlabStatement> statementsForConnectionValue(
      ConnectionValue value) {
    List <MatlabStatement> stmts = new LinkedList<MatlabStatement>();
    
    stmts.add(new ImportStatement("types.Channel"));
    
    String variableName = variableNameForValue("channel");
    List<String> paramOrder = new LinkedList<String>();
    paramOrder.add("len");
    
    stmts.add(new InstantiationStatement(variableName, "Channel", paramOrder));
    
    return stmts;
  }
}
