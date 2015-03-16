package org.manifold.compiler.back.microfluidics.smt2;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.middle.Schematic;

public class Macros {

  //get the connection associated with this port
  // TODO this is VERY EXPENSIVE, find an optimization
  protected static ConnectionValue getConnection(
     Schematic schematic, PortValue port) {
    for (ConnectionValue conn : schematic.getConnections().values()) {
      if (conn.getFrom().equals(port) || conn.getTo().equals(port)) {
        return conn;
      }
    }
    return null;
  }
  
  // generate an expression that describes the constraint
  // "total flow in = total flow out"
  // this is difficult because the actual flow direction may be backwards
  // with respect to the expected direction
  public static SExpression generateConservationOfFlow(Schematic schematic,
      List<PortValue> connectedPorts) {
    List<SExpression> flowRatesIn = new LinkedList<SExpression>();
    List<SExpression> flowRatesOut = new LinkedList<SExpression>();
    for (PortValue port : connectedPorts) {
      ConnectionValue channel = getConnection(schematic, port);
      boolean connectedIntoJunction;
      // check which way the channel is connected
      if (channel.getFrom().equals(port)) {
        connectedIntoJunction = false;
      } else if (channel.getTo().equals(port)) {
        connectedIntoJunction = true;
      } else {
        throw new CodeGenerationError("attempt to generate flow direction "
            + "constraint for a channel that is disconnected from the "
            + "target port");
      }
      Symbol flowRate = SymbolNameGenerator
          .getsym_ChannelFlowRate(schematic, channel);
      if (connectedIntoJunction) {
        // flow rate is positive into the junction
        flowRatesIn.add(flowRate);
      } else {
        // flow rate is positive out of the junction
        flowRatesOut.add(flowRate);
      }
    }
    return QFNRA.assertEqual(QFNRA.add(flowRatesIn), QFNRA.add(flowRatesOut));
  }
  
}