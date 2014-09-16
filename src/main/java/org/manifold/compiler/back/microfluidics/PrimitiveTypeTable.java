package org.manifold.compiler.back.microfluidics;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionType;
import org.manifold.compiler.NodeTypeValue;
import org.manifold.compiler.PortTypeValue;
import org.manifold.compiler.UndeclaredIdentifierException;
import org.manifold.compiler.middle.Schematic;

// Identifies correspondence between TypeValues and specific domain primitives.
public class PrimitiveTypeTable {
  private PortTypeValue microfluidPortType = null;
  public PortTypeValue getMicrofluidPortType() {
    return microfluidPortType;
  }
  
  private NodeTypeValue controlPointNodeType = null;
  public NodeTypeValue getControlPointNodeType() {
    return controlPointNodeType;
  }
  
  private NodeTypeValue pressureControlPointNodeType = null;
  public NodeTypeValue getPressureControlPointNodeType() {
    return pressureControlPointNodeType;
  }
  
  private List<NodeTypeValue> derivedPressureCPNodeTypes = new LinkedList<>();
  public List<NodeTypeValue> getDerivedPressureControlPointNodeTypes() {
    return derivedPressureCPNodeTypes;
  }
  public void addDerivedPressureControlPointNodeTypes(
      List<NodeTypeValue> types) {
    derivedPressureCPNodeTypes.addAll(types);
  }
  
  private NodeTypeValue voltageControlPointNodeType = null;
  public NodeTypeValue getVoltageControlPointNodeType() {
    return voltageControlPointNodeType;
  }
  
  private List<NodeTypeValue> derivedVoltageCPNodeTypes = new LinkedList<>();
  public List<NodeTypeValue> getDerivedVoltageControlPointNodeTypes() {
    return derivedVoltageCPNodeTypes;
  }
  public void addDerivedVoltageControlPointNodeTypes(
      List<NodeTypeValue> types) {
    derivedVoltageCPNodeTypes.addAll(types);
  }
  
  private NodeTypeValue channelCrossingNodeType = null;
  public NodeTypeValue getChannelCrossingNodeType() {
    return channelCrossingNodeType;
  }
  
  private ConnectionType microfluidChannelType = null;
  public ConnectionType getMicrofluidChannelType() {
    return microfluidChannelType;
  }
  
  public void retrieveBaseTypes(Schematic schematic) {
    try {
      microfluidPortType = schematic.getPortType("microfluidPort");
      controlPointNodeType = schematic.getNodeType("controlPoint");
      pressureControlPointNodeType = 
          schematic.getNodeType("pressureControlPoint");
      voltageControlPointNodeType = 
          schematic.getNodeType("voltageControlPoint");
      channelCrossingNodeType =
          schematic.getNodeType("channelCrossing");
      microfluidChannelType =
          schematic.getConnectionType("microfluidChannel");
    } catch (UndeclaredIdentifierException e) {
      throw new CodeGenerationError(
          "could not find required microfluidic schematic type '"
          + e.getIdentifier() + "'; schematic version mismatch or "
          + " not a microfluidic schematic");
    }
  }
  
  public List<NodeTypeValue> retrieveDerivedNodeTypes(Schematic schematic, 
      NodeTypeValue baseType) {
    List<NodeTypeValue> derivedTypes = new LinkedList<>();
    for (NodeTypeValue t : schematic.getNodeTypes().values()) {
      if (t.isSubtypeOf(baseType)) {
        derivedTypes.add(t);
      }
    }
    return derivedTypes;
  }
  
}
