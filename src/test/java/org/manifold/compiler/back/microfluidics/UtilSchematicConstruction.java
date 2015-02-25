package org.manifold.compiler.back.microfluidics;

import java.util.HashMap;
import java.util.Map;

import org.manifold.compiler.ConnectionTypeValue;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.ConstraintType;
import org.manifold.compiler.ConstraintValue;
import org.manifold.compiler.InvalidAttributeException;
import org.manifold.compiler.MultipleDefinitionException;
import org.manifold.compiler.NilTypeValue;
import org.manifold.compiler.NodeTypeValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.PortTypeValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.RealTypeValue;
import org.manifold.compiler.RealValue;
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
  // multi-phase
  private static NodeTypeValue fluidEntryNodeType;
  private static NodeTypeValue fluidExitNodeType;
  private static NodeTypeValue tJunctionNodeType;
  // single-phase
  private static NodeTypeValue controlPointNodeType;
  private static NodeTypeValue voltageCPNodeType; 
  private static NodeTypeValue pressureCPNodeType;
  private static NodeTypeValue channelCrossingNodeType;
  private static NodeTypeValue electrophoreticNodeType;
  private static NodeTypeValue electrophoreticCrossType;

  private static ConstraintType controlPointPlacementConstraintType;
  private static ConstraintType channelPlacementConstraintType;
  private static ConstraintType channelDropletVolumeConstraintType;
  
  public static void setupIntermediateTypes() {

    // TODO which signal type do we want here?
    microfluidPortType = new PortTypeValue(NilTypeValue.getInstance(), 
        noTypeAttributes);
    
    // multi-phase
    Map<String, PortTypeValue> fluidEntryPorts = new HashMap<>();
    fluidEntryPorts.put("output", microfluidPortType);
    Map<String, TypeValue> fluidEntryAttributes = new HashMap<>();
    fluidEntryAttributes.put("viscosity", RealTypeValue.getInstance());
    fluidEntryNodeType = new NodeTypeValue(
        fluidEntryAttributes, fluidEntryPorts);
    
    Map<String, PortTypeValue> fluidExitPorts = new HashMap<>();
    fluidExitPorts.put("input", microfluidPortType);
    fluidExitNodeType = new NodeTypeValue(
        noTypeAttributes, fluidExitPorts);
    
    Map<String, PortTypeValue> tJunctionPorts = new HashMap<>();
    tJunctionPorts.put("continuous", microfluidPortType);
    tJunctionPorts.put("dispersed", microfluidPortType);
    tJunctionPorts.put("output", microfluidPortType);
    // TODO attributes
    tJunctionNodeType = new NodeTypeValue(noTypeAttributes, tJunctionPorts);
    
    // single-phase
    controlPointNodeType = new NodeTypeValue(noTypeAttributes, noPorts);
    pressureCPNodeType = new NodeTypeValue(noTypeAttributes, noPorts, 
        controlPointNodeType);
    voltageCPNodeType = new NodeTypeValue(noTypeAttributes, noPorts, 
        controlPointNodeType);
    Map<String, PortTypeValue> channelCrossingPorts = new HashMap<>();
    //       B0
    //        |
    // A0 --- + --- A1
    //        |
    //       B1
    channelCrossingPorts.put("channelA0", microfluidPortType);
    channelCrossingPorts.put("channelA1", microfluidPortType);
    channelCrossingPorts.put("channelB0", microfluidPortType);
    channelCrossingPorts.put("channelB1", microfluidPortType);
    channelCrossingNodeType = new NodeTypeValue(noTypeAttributes, 
        channelCrossingPorts);

    // single-phase electrophoresis
    // TODO: remove all references to this
    Map<String, PortTypeValue> electrophoreticNodePorts = new HashMap<>();
    electrophoreticNodePorts.put("sampleIn", microfluidPortType);
    electrophoreticNodePorts.put("wasteOut", microfluidPortType);
    electrophoreticNodeType = new NodeTypeValue(noTypeAttributes,
        electrophoreticNodePorts);

    // single-phase electrophoretic cross
    Map<String, PortTypeValue> electrophoreticCrossPorts = new HashMap<>();
    electrophoreticCrossType = new NodeTypeValue(noTypeAttributes,
        electrophoreticCrossPorts);
    
    // controlPointPlacementConstraint(ControlPointNode node, Real x, Real y)
    Map<String, TypeValue> cxtCPPlaceAttrs = new HashMap<>();
    cxtCPPlaceAttrs.put("node", controlPointNodeType);
    cxtCPPlaceAttrs.put("x", RealTypeValue.getInstance());
    cxtCPPlaceAttrs.put("y", RealTypeValue.getInstance());
    controlPointPlacementConstraintType = new ConstraintType(cxtCPPlaceAttrs);
    
    // channelPlacementConstraint(Connection channel, Real x, Real y)
    Map<String, TypeValue> cxtChanPlaceAttrs = new HashMap<>();
    cxtChanPlaceAttrs.put("channel", ConnectionTypeValue.getInstance());
    cxtChanPlaceAttrs.put("x", RealTypeValue.getInstance());
    cxtChanPlaceAttrs.put("y", RealTypeValue.getInstance());
    channelPlacementConstraintType = new ConstraintType(cxtChanPlaceAttrs);
    
    // channelDropletVolumeConstraint(Connection channel, Real volume)
    Map<String, TypeValue> cxtChanDropVolAttrs = new HashMap<>();
    cxtChanDropVolAttrs.put("channel", ConnectionTypeValue.getInstance());
    cxtChanDropVolAttrs.put("volume", RealTypeValue.getInstance());
    channelDropletVolumeConstraintType = 
        new ConstraintType(cxtChanDropVolAttrs);
    
    setUp = true;
  }

  public static Schematic instantiateSchematic(String name) 
      throws MultipleDefinitionException {
    if (!setUp) {
      setupIntermediateTypes();
    }
    Schematic s = new Schematic(name);
    
    s.addPortType("microfluidPort", microfluidPortType);
    // multi-phase
    s.addNodeType("fluidEntry", fluidEntryNodeType);
    s.addNodeType("fluidExit", fluidExitNodeType);
    s.addNodeType("tJunction", tJunctionNodeType);
    
    // single-phase
    s.addNodeType("controlPoint", controlPointNodeType);
    s.addNodeType("pressureControlPoint", pressureCPNodeType);
    s.addNodeType("voltageControlPoint", voltageCPNodeType);
    s.addNodeType("channelCrossing", channelCrossingNodeType);
    s.addNodeType("electrophoreticNode", electrophoreticNodeType);
    s.addNodeType("electrophoreticCross", electrophoreticCrossType);
    
    s.addConstraintType(
        "controlPointPlacementConstraint",
        controlPointPlacementConstraintType);
    s.addConstraintType(
        "channelPlacementConstraint",
        channelPlacementConstraintType);
    s.addConstraintType(
        "channelDropletVolumeConstraint",
        channelDropletVolumeConstraintType);

    return s;
  }

  public static ConnectionValue instantiateChannel(PortValue from, PortValue to)
      throws UndeclaredAttributeException, InvalidAttributeException,
      TypeMismatchException {
    ConnectionValue channel = new ConnectionValue(from, to, noAttributes);
    return channel;
  }
  
  public static NodeValue instantiateFluidEntry(Schematic schematic,
      double viscosity)
      throws SchematicException {
    Map<String, Value> attrsMap = new HashMap<>();
    RealValue mu = new RealValue(viscosity);
    attrsMap.put("viscosity", mu);
    Map<String, Map<String, Value>> portAttrsMap = new HashMap<>();
    portAttrsMap.put("output", noAttributes);
    NodeValue exit = new NodeValue(schematic.getNodeType("fluidEntry"),
        attrsMap, portAttrsMap);
    return exit;
  }
  
  public static NodeValue instantiateFluidExit(Schematic schematic)
      throws SchematicException {
    Map<String, Map<String, Value>> portAttrsMap = new HashMap<>();
    portAttrsMap.put("input", noAttributes);
    NodeValue exit = new NodeValue(schematic.getNodeType("fluidExit"),
        noAttributes, portAttrsMap);
    return exit;
  }
  
  public static NodeValue instantiateTJunction(Schematic schematic)
      throws SchematicException {
    // TODO
    return null;
  }

  public static NodeValue instantiateElectrophoreticNode(Schematic schematic)
      throws SchematicException {
    Map<String, Map<String, Value>> portAttrsMap = new HashMap<>();
    portAttrsMap.put("sampleIn", noAttributes);
    portAttrsMap.put("wasteOut", noAttributes);
    NodeValue electrophoreticNode = new NodeValue(
        schematic.getNodeType("electrophoreticNode"), 
        noAttributes, portAttrsMap);
    return electrophoreticNode;
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
  
  public static ConstraintValue instantiateControlPointPlacementConstraint(
      NodeValue node, Double x, Double y) throws UndeclaredAttributeException, 
      InvalidAttributeException, TypeMismatchException {
    Map<String, Value> attrs = new HashMap<>();
    attrs.put("node", node);
    attrs.put("x", new RealValue(x));
    attrs.put("y", new RealValue(y));
    ConstraintValue cxt = new ConstraintValue(
        controlPointPlacementConstraintType, attrs);
    return cxt;
  }
  
  public static ConstraintValue instantiateChannelPlacementConstraint(
      ConnectionValue channel, Double x, Double y) 
      throws UndeclaredAttributeException, InvalidAttributeException, 
        TypeMismatchException {
    Map<String, Value> attrs = new HashMap<>();
    attrs.put("channel", channel);
    attrs.put("x", new RealValue(x));
    attrs.put("y", new RealValue(y));
    ConstraintValue cxt = new ConstraintValue(
        channelPlacementConstraintType, attrs);
    return cxt;
  }
 
  public static ConstraintValue instantiateChannelDropletVolumeConstraint(
      ConnectionValue channel, Double volume) 
      throws UndeclaredAttributeException, InvalidAttributeException, 
        TypeMismatchException {
    Map<String, Value> attrs = new HashMap<>();
    attrs.put("channel", channel);
    attrs.put("volume", new RealValue(volume));
    ConstraintValue cxt = new ConstraintValue(
        channelDropletVolumeConstraintType, attrs);
    return cxt;
  }
  
}
