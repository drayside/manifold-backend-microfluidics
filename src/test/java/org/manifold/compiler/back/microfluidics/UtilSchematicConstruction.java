package org.manifold.compiler.back.microfluidics;

import java.util.HashMap;
import java.util.Map;

import org.manifold.compiler.ConnectionType;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.InvalidAttributeException;
import org.manifold.compiler.MultipleDefinitionException;
import org.manifold.compiler.NodeTypeValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.PortTypeValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.TypeMismatchException;
import org.manifold.compiler.TypeValue;
import org.manifold.compiler.UndeclaredAttributeException;
import org.manifold.compiler.UndeclaredIdentifierException;
import org.manifold.compiler.Value;
import org.manifold.compiler.middle.Schematic;
import org.manifold.compiler.middle.SchematicException;

// Utility class for quickly setting up Schematics in test cases.
public class UtilSchematicConstruction {

  private static boolean setUp = false;

  private static final Map<String, TypeValue> noTypeAttributes 
    = new HashMap<>();
  private static final Map<String, Value> noAttributes = new HashMap<>();
  private static Map<String, PortTypeValue> noPorts = new HashMap<>();
  
  private static PortTypeValue microfluidPortType;
  private static NodeTypeValue controlPointNodeType;
  private static NodeTypeValue voltageCPNodeType; 
  private static NodeTypeValue pressureCPNodeType;
  private static NodeTypeValue channelCrossingNodeType;
  private static ConnectionType microfluidChannelType;

  public static void setupIntermediateTypes() {

    microfluidPortType = new PortTypeValue(noTypeAttributes);
    controlPointNodeType = new NodeTypeValue(noTypeAttributes, noPorts);
    pressureCPNodeType = new NodeTypeValue(noTypeAttributes, noPorts, 
        controlPointNodeType);
    voltageCPNodeType = new NodeTypeValue(noTypeAttributes, noPorts, 
        controlPointNodeType);
    Map<String, PortTypeValue> channelCrossingPorts = new HashMap<>();
    channelCrossingPorts.put("channelA0", microfluidPortType);
    channelCrossingPorts.put("channelA1", microfluidPortType);
    channelCrossingPorts.put("channelB0", microfluidPortType);
    channelCrossingPorts.put("channelB1", microfluidPortType);
    channelCrossingNodeType = new NodeTypeValue(noTypeAttributes, 
        channelCrossingPorts);

    // TODO channel geometry enum -- once we have enums in the intermediate
    microfluidChannelType = new ConnectionType(noTypeAttributes);
    
    setUp = true;
  }

  public static Schematic instantiateSchematic(String name) 
      throws MultipleDefinitionException {
    if (!setUp) {
      setupIntermediateTypes();
    }
    Schematic s = new Schematic(name);
    
    s.addPortType("microfluidPort", microfluidPortType);

    s.addNodeType("controlPoint", controlPointNodeType);
    s.addNodeType("pressureControlPoint", pressureCPNodeType);
    s.addNodeType("voltageControlPoint", voltageCPNodeType);
    s.addNodeType("channelCrossing", channelCrossingNodeType);

    s.addConnectionType("microfluidChannel", microfluidChannelType);

    return s;
  }

  /**
   * Instantiate a pressure control point with the given number of ports.
   * The control point's typename will be "pressureControlPointN",
   * where N is the number of ports. This type will be created if it does
   * not exist in the schematic. Each port will be named 
   * "channel0" through "channel(n-1)".
   */
  public static NodeValue instantiatePressureControlPoint(Schematic schematic,
      int nPorts) 
      throws SchematicException {
    // first check whether we already know about this node type
    String typename = "pressureControlPoint" + Integer.toString(nPorts);
    NodeTypeValue cpType;
    Map<String, Map<String, Value>> portAttrsMap = new HashMap<>();
    try {
      cpType = schematic.getNodeType(typename);
    } catch (UndeclaredIdentifierException e) {
      // create a new type and add it to the schematic
      Map<String, PortTypeValue> portMap = new HashMap<>();
      for (int i = 0; i < nPorts; ++i) {
        String portname = "channel" + Integer.toString(i);
        portMap.put(portname, microfluidPortType);
      }
      cpType = new NodeTypeValue(noTypeAttributes, portMap, pressureCPNodeType);
      schematic.addNodeType(typename, cpType);
    }
    for (String portName : cpType.getPorts().keySet()) {
      portAttrsMap.put(portName, noAttributes);
    }
    NodeValue cp = new NodeValue(cpType, noAttributes, portAttrsMap);
    return cp;
  }
  
  /**
   * Instantiate a voltage control point with the given number of ports.
   * The control point's typename will be "pressureControlPointN",
   * where N is the number of ports. This type will be created if it does
   * not exist in the schematic. Each port will be named 
   * "channel0" through "channel(n-1)".
   */
  public static NodeValue instantiateVoltageControlPoint(Schematic schematic,
      int nPorts) 
      throws SchematicException {
    // first check whether we already know about this node type
    String typename = "voltageControlPoint" + Integer.toString(nPorts);
    NodeTypeValue cpType;
    Map<String, Map<String, Value>> portAttrsMap = new HashMap<>();
    try {
      cpType = schematic.getNodeType(typename);
    } catch (UndeclaredIdentifierException e) {
      // create a new type and add it to the schematic
      Map<String, PortTypeValue> portMap = new HashMap<>();
      for (int i = 0; i < nPorts; ++i) {
        String portname = "channel" + Integer.toString(i);
        portMap.put(portname, microfluidPortType);
      }
      cpType = new NodeTypeValue(noTypeAttributes, portMap, voltageCPNodeType);
      schematic.addNodeType(typename, cpType);
    }
    for (String portName : cpType.getPorts().keySet()) {
      portAttrsMap.put(portName, noAttributes);
    }
    NodeValue cp = new NodeValue(cpType, noAttributes, portAttrsMap);
    return cp;
  }

  public static ConnectionValue instantiateChannel(PortValue from, PortValue to)
      throws UndeclaredAttributeException, InvalidAttributeException,
      TypeMismatchException {
    ConnectionValue channel = new ConnectionValue(microfluidChannelType, 
        from, to, noAttributes);
    return channel;
  }
  
}
