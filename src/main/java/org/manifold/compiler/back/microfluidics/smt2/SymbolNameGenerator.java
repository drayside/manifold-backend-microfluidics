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
    return new Symbol(nodeName.concat("_pos_y"));
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
    return new Symbol(chName.concat("_length"));
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

  /**
   * Retrieves the symbol that defines the length of an electrophoretic cross.
   **/
  public static Symbol getsym_EPCrossLength(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_length"));
  }
  
  /**
   * Retrieves the symbol that defines the length of an electrophoretic 
   * cross's tail channel.
   **/
  public static Symbol getsym_EPCrossTailChannelLength(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_tailChannelLength"));
  }
  
  /**
   * Retrieves the symbol that defines the length of an electrophoretic 
   * cross's separation channel.
   **/
  public static Symbol getsym_EPCrossSeparationChannelLength(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_separationChannelLength"));
  }

  /**
   * Retrieves the symbol that defines the length of an electrophoretic 
   * cross's sample channel.
   **/
  public static Symbol getsym_EPCrossInjectionChannelLength(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_injectionChannelLength"));
  }
  
  /**
   * Retrieves the symbol that defines the length of an electrophoretic 
   * cross's waste channel.
   **/
  public static Symbol getsym_EPCrossWasteChannelLength(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_wasteChannelLength"));
  }
  
  /**
   * Retrieves the symbol that defines the voltage applied at the cathode 
   * reservoir of an electrophoretic cross during injection.
   * TODO: Refactor out to separate "reservoir" node?
   **/
  public static Symbol getsym_EPCrossInjectionCathodeNodeVoltage(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_injectionCathodeNodeVoltage"));
  }
  
  /**
   * Retrieves the symbol that defines the voltage applied at the anode 
   * reservoir of an electrophoretic cross during injection.
   * TODO: Refactor out to separate "reservoir" node?
   **/
  public static Symbol getsym_EPCrossInjectionAnodeNodeVoltage(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_injectionAnodeNodeVoltage"));
  }
  
  /**
   * Retrieves the symbol that defines the electric field strength within  
   * the separation channel of an electrophoretic cross during injection.
   **/
  public static Symbol getsym_EPCrossInjectionSeparationChannelE(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_injectionSeparationChannelE"));
  }

  /**
   * Retrieves the symbol that defines the initial spread of sample plug along 
   * separation channel.
   **/
  public static Symbol getsym_EPCrossSampleInitialSpread(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_sampleInitialSpread"));
  }
  
  /**
   * Retrieves the symbol that defines the bulk mobility (u_EOF).
   **/
  public static Symbol getsym_EPCrossBulkMobility(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_bulkMobility"));
  }
  
  /**
   * Retrieves the symbol that defines the bulk viscosity.
   **/
  public static Symbol getsym_EPCrossBulkViscosity(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_bulkViscosity"));
  }
  
  /**
   * Retrieves the symbol that defines the distance that the sample has to 
   * travel along the separation channel of an electrophoretic cross before 
   * it can be measured.
   **/
  public static Symbol getsym_EPCrossSeparationDistance(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_separationDistance"));
  }
  
  /**
   * Retrieves the symbol that defines the radius of the sample and waste  
   * channels of an electrophoretic cross.
   **/
  public static Symbol getsym_EPCrossInjectionChannelRadius(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_injectionChannelRadius"));
  }
  
  /**
   * Retrieves the symbol that defines the velocity of an analyte within  
   * the separation channel of an electrophoretic cross during injection.
   **/
  public static Symbol getsym_EPCrossInjectionSeparationChannelAnalyteVelocity(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat(
      "_injectionSeparationChannelAnalyteVelocity" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the velocity of an analyte within  
   * the injection channel of an electrophoretic cross during injection.
   **/
  public static Symbol getsym_EPCrossInjectionInjectionChannelAnalyteVelocity(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat(
      "_injectionInjectionChannelAnalyteVelocity" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the concentration of the 
   * electropherogram output peak corresponding to a specific analyte.
   **/
  public static Symbol getsym_EPCrossPeakTimeConcentration(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat(
      "_peakTimeConcentration" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the minimum concentration in the 
   * electropherogram output immediately following a specific peak.
   **/
  public static Symbol getsym_EPCrossVoidTimeConcentration(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat(
      "_voidTimeConcentration" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the peak concentration of an analyte 
   * in the electropherogram output.
   **/
  public static Symbol getsym_EPCrossPeakTimeAnalyteConcentration(
      Schematic schematic, NodeValue electrophoreticCross, int timestepId, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat(
      "_peakTimeAnalyteConcentration" + timestepId + "_" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the spread of an analyte along the 
   * separation channel when its concentration in the electropherogram output 
   * has peaked. 
   **/
  public static Symbol getsym_EPCrossPeakTimeAnalyteSpread(
      Schematic schematic, NodeValue electrophoreticCross, int timestepId, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat(
      "_peakTimeAnalyteSpread" + timestepId + "_" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the initial surface concentration of an 
   * analyte within the separation channel (i.e. C_0).
   **/
  public static Symbol getsym_EPCrossAnalyteInitialSurfaceConcentration(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat(
      "_analyteInitialSurfaceConcentration" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the diffusion coefficient of an analyte.
   **/
  public static Symbol getsym_EPCrossAnalyteDiffusionCoefficient(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat(
      "_analyteDiffusionCoefficient" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the electrophoretic mobility (u_EP) of 
   * an analyte.
   **/
  public static Symbol getsym_EPCrossAnalyteElectrophoreticMobility(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat(
      "_analyteElectrophoreticMobility" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the time at which the concentration of 
   * an analyte peaks in the electropherogram output.
   **/
  public static Symbol getsym_EPCrossPeakTime(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_peakTime" + analyteId));
  }
  
  public static Symbol getsym_EPCrossStartFocusTime(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_startFocusTime" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the time at which the concentration of 
   * an analyte exceeds the baseline level in the electropherogram output.
   **/
  public static Symbol getsym_EPCrossFocusTime(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_focusTime" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the time at which the concentration of 
   * an analyte drops below the baseline level in the electropherogram output.
   **/
  public static Symbol getsym_EPCrossFadeTime(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_fadeTime" + analyteId));
  }
  
  public static Symbol getsym_EPCrossEndFadeTime(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_endFadeTime" + analyteId));
  }
  
  public static Symbol getsym_EPCrossVoidTime(
      Schematic schematic, NodeValue electrophoreticCross, int analyteId) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_voidTime" + analyteId));
  }
  
  /**
   * Retrieves the symbol that defines the time at which the electropherogram
   * output first exceeds the baseline level.
   **/
  public static Symbol getsym_EPCrossStartTime(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_startTime"));
  }
  
  /**
   * Retrieves the symbol that defines the time at which the electropherogram
   * output returns to the baseline level for the final time.
   **/
  public static Symbol getsym_EPCrossEndTime(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_endTime"));
  }
  
  /**
   * Retrieves the symbol that defines the baseline concentration in the 
   * electropherogram output.
   **/
  public static Symbol getsym_EPCrossBaselineConcentration(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_baselineConcentration"));
  }
  
  public static Symbol getsym_EPCrossNegligibleConcentration(
      Schematic schematic, NodeValue electrophoreticCross) {
    String nodeName = schematic.getNodeName(electrophoreticCross);
    return new Symbol(nodeName.concat("_negligibleConcentration"));
  }
}
