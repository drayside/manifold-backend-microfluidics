package org.manifold.compiler.back.microfluidics;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConstraintType;
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

  // multi-phase stuff
  
  private NodeTypeValue fluidEntryNodeType = null;
  public NodeTypeValue getFluidEntryNodeType() {
    return fluidEntryNodeType;
  }
  
  private NodeTypeValue fluidExitNodeType = null;
  public NodeTypeValue getFluidExitNodeType() {
    return fluidExitNodeType;
  }
  
  private NodeTypeValue tJunctionNodeType = null;
  public NodeTypeValue getTJunctionNodeType() {
    return tJunctionNodeType;
  }
  
  // single-phase stuff, probably out of date
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
 
  private NodeTypeValue electrophoreticCrossType = null;
  public NodeTypeValue getElectrophoreticCrossType(){
    return electrophoreticCrossType;
  }  
 
  private NodeTypeValue reservoirType = null;
  public NodeTypeValue getReservoirType(){
    return reservoirType;
  }  
 
  public void retrieveBaseTypes(Schematic schematic) {
    try {
      microfluidPortType = schematic.getPortType("microfluidPort");
      
      // multi-phase
      fluidEntryNodeType = 
          schematic.getNodeType("fluidEntry");
      fluidExitNodeType = 
          schematic.getNodeType("fluidExit");
      tJunctionNodeType = 
          schematic.getNodeType("tJunction");
      
      // single-phase
      controlPointNodeType = schematic.getNodeType("controlPoint");
      pressureControlPointNodeType = 
          schematic.getNodeType("pressureControlPoint");
      voltageControlPointNodeType = 
          schematic.getNodeType("voltageControlPoint");
      channelCrossingNodeType =
          schematic.getNodeType("channelCrossing");
      electrophoreticCrossType = 
          schematic.getNodeType("electrophoreticCross");
      reservoirType = schematic.getNodeType("reservoir");
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
  
  private ConstraintType controlPointPlacementConstraintType = null;
  public ConstraintType getControlPointPlacementConstraintType() {
    return controlPointPlacementConstraintType;
  }
  
  private ConstraintType channelPlacementConstraintType = null;
  public ConstraintType getChannelPlacementConstraintType() {
    return channelPlacementConstraintType;
  }
  
  private ConstraintType channelDropletVolumeConstraintType = null;
  public ConstraintType getchannelDropletVolumeConstraintType() {
    return channelDropletVolumeConstraintType;
  }
  
  public void retrieveConstraintTypes(Schematic schematic) {
    try {
      controlPointPlacementConstraintType =
          schematic.getConstraintType("controlPointPlacementConstraint");
      channelPlacementConstraintType =
          schematic.getConstraintType("channelPlacementConstraint");
      channelDropletVolumeConstraintType =
          schematic.getConstraintType("channelDropletVolumeConstraint");
    } catch (UndeclaredIdentifierException e) {
      throw new CodeGenerationError(
          "could not find required microfluidic schematic constraint type '"
          + e.getIdentifier() + "'; schematic version mismatch or "
          + " not a microfluidic schematic");
    }
  }
  
}
