package org.manifold.compiler.back.microfluidics.smt2;

import java.util.Map;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.middle.Schematic;

// Helper class to generate consistent symbol names from
// schematic nodes/connections/etc.
public class SymbolNameGenerator {

  /**
   * Retrieves the symbol whose value is the mathematical constant "pi".
   */
  public static Symbol getsym_constant_pi() {
    return new Symbol("PI");
  }
  
  /**
   * Retrieves the symbol that defines the x-coordinate of a node's position.
   */
  public static Symbol getsym_NodeX(Schematic schematic, NodeValue node) {
    String nodeName = schematic.getNodeName(node);
    return new Symbol(nodeName.concat("_pos_x"));
  }
  
  /**
   * Retrieves the symbol that defines the y-coordinate of a node's position.
   */
  public static Symbol getsym_NodeY(Schematic schematic, NodeValue node) {
    String nodeName = schematic.getNodeName(node);
    return new Symbol(nodeName.concat("_pos_Y"));
  }
  
  /**
   * Retrieves the symbol that defines the pressure at a node
   * (throughout the entire node, i.e. at every port).
   */
  public static Symbol getSym_NodePressure(
      Schematic schematic, NodeValue node) {
    String nodeName = schematic.getNodeName(node);
    return new Symbol(nodeName.concat("_pressure"));
  }
  
  /**
   * Retrieves the symbol that defines the pressure at a single port of a node.
   */
  public static Symbol getSym_PortPressure(
      Schematic schematic, PortValue port) {
    NodeValue node = port.getParent();
    String nodeName = schematic.getNodeName(node);
    // reverse map the port onto its name
    String portName = null;
    for (Map.Entry<String, PortValue> entry : node.getPorts().entrySet()) {
      if (entry.getValue().equals(port)) {
        portName = entry.getKey();
        break;
      }
    }
    if (portName == null) {
      throw new CodeGenerationError("could not map port to name for node '"
          + nodeName + "'");
    }
    return new Symbol(nodeName + "_" + portName + "_pressure");
  }
  
  /**
   * Retrieves the symbol that defines the length of a channel.
   */
  public static Symbol getsym_ChannelLength(Schematic schematic, 
      ConnectionValue ch) {
    String chName = schematic.getConnectionName(ch);
    return new Symbol(chName.concat("_pos_x"));
  }
  
  /**
   * Retrieves the symbol that defines the flow rate through a channel.
   * Positive flow is in the direction into the "from" connection
   * and out of the "to" connection, i.e. (from) --(ch)-> (to).
   */
  public static Symbol getsym_ChannelFlowRate(Schematic schematic, 
      ConnectionValue ch) {
    String chName = schematic.getConnectionName(ch);
    return new Symbol(chName.concat("_flowrate"));
  }
  
  /**
   * Retrieves the symbol that defines the viscosity of fluid
   * present in a channel.
   */
  public static Symbol getsym_ChannelViscosity(Schematic schematic, 
      ConnectionValue ch) {
    String chName = schematic.getConnectionName(ch);
    return new Symbol(chName.concat("_viscosity"));
  }
  
  /**
   * Retrieves the symbol that defines the hydrodynamic resistance
   * of a channel.
   */
  public static Symbol getsym_ChannelResistance(Schematic schematic, 
      ConnectionValue ch) {
    String chName = schematic.getConnectionName(ch);
    return new Symbol(chName.concat("_resistance"));
  }

  /**
   * Retrieves the symbol that defines the volume of a droplet 
   * emitted by a channel.
   */
  public static Symbol getsym_ChannelDropletVolume(Schematic schematic,
      ConnectionValue ch) {
    String chName = schematic.getConnectionName(ch);
    return new Symbol(chName.concat("_droplet_volume"));
  }

  /**
   * Retrieves the symbol that defines the height of a (rectangular) channel.
   */
  public static Symbol getsym_ChannelHeight(Schematic schematic,
      ConnectionValue ch) {
    String chName = schematic.getConnectionName(ch);
    return new Symbol(chName.concat("_height"));
  }

  /**
   * Retrieves the symbol that defines the width of a (rectangular) channel.
   */
  public static Symbol getsym_ChannelWidth(Schematic schematic,
      ConnectionValue ch) {
    String chName = schematic.getConnectionName(ch);
    return new Symbol(chName.concat("_width"));
  }

  /**
   * Retrieves the symbol that defines the "sharpness" of the corners
   * of a T-junction.
   */
  public static Symbol getsym_TJunctionEpsilon(Schematic schematic,
      NodeValue junc) {
    String jName = schematic.getNodeName(junc);
    return new Symbol(jName.concat("_epsilon"));
  }
  
}
