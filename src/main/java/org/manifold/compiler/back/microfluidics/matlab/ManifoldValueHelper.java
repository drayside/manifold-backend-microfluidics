package org.manifold.compiler.back.microfluidics.matlab;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.Value;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.back.microfluidics.ProcessParameters;

public class ManifoldValueHelper {

  private static ManifoldValueHelper helper = new ManifoldValueHelper();
  private Map<String, Integer> valueCount;
  private Map<Value, String> variableNameCache;
  
  private ManifoldValueHelper() {
    this.valueCount = new HashMap<String, Integer>();
    this.variableNameCache = new HashMap<Value, String>();
  }
  
  private static String nextVariableNameForType(String typeName) {
    if (!helper.valueCount.containsKey(typeName)) {
      helper.valueCount.put(typeName, 0);
    }
    
    Integer newCount = helper.valueCount.get(typeName) + 1;
    helper.valueCount.put(typeName, newCount);
    String variableName = typeName + newCount.toString();
    return variableName;
  }
  
  public static String variableNameFromValue(Value value) {
    return helper.variableNameCache.get(value);
  }
  
  public static List<MatlabStatement> statementsForProcessParameters(
      ProcessParameters params) {
    List<MatlabStatement> stmts = new LinkedList<MatlabStatement>();
    stmts.add(new ImportStatement("types.Chip"));
    
    List<String> paramOrder = new LinkedList<String>();
    paramOrder.add("minimumNodeDistance");
    paramOrder.add("minimumChannelLength");
    paramOrder.add("maximumChipSizeX");
    paramOrder.add("maximumChipSizeY");
    paramOrder.add("criticalCrossingAngle");
    
    // TODO: Figure out how to convert doubles/ints to string for the template
    // Map<String, String> paramValues = new HashMap<String, String>();
    // paramValues.put("minimumNodeDistance", params.getMinimumNodeDistance().toString());
    
    stmts.add(new InstantiationStatement("chip", "Chip", paramOrder));
    return stmts;
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

    String variableName;
    if (!helper.variableNameCache.containsKey(value)) {
      variableName = nextVariableNameForType("node");
      helper.variableNameCache.put(value, variableName);
    } else {
      variableName = variableNameFromValue(value);
    }

    List<String> paramOrder = new LinkedList<String>();
    paramOrder.add("x");
    paramOrder.add("y");

    stmts.add(new InstantiationStatement(variableName, "Node", paramOrder));
    return stmts;
  }
  
  public static List<MatlabStatement> statementsForConnectionValue(
      ConnectionValue value) {
    List<MatlabStatement> stmts = new LinkedList<MatlabStatement>();
    stmts.add(new ImportStatement("types.Channel"));
    
    String variableName;
    if (!helper.variableNameCache.containsKey(value)) {
      variableName = nextVariableNameForType("channel");
      helper.variableNameCache.put(value, variableName);
    } else {
      variableName = variableNameFromValue(value);
    }

    List<String> paramOrder = new LinkedList<String>();
    paramOrder.add("len");
    
    stmts.add(new InstantiationStatement(variableName, "Channel", paramOrder));
    helper.variableNameCache.put(value, variableName);
    return stmts;
  }
}
