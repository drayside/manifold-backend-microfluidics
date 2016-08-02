package org.manifold.compiler.back.microfluidics.smt2;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.back.microfluidics.SchematicUtil;
import org.manifold.compiler.middle.Schematic;


public class Macros {
  
  // generate an expression that describes the constraint
  // "total flow in = total flow out"
  // this is difficult because the actual flow direction may be backwards
  // with respect to the expected direction
  public static List<SExpression> generateConservationOfFlow(
      Schematic schematic, List<PortValue> connectedPorts) {
    List<SExpression> flowRatesIn = new LinkedList<SExpression>();
    List<SExpression> flowRatesOut = new LinkedList<SExpression>();
    
    List<SExpression> flowRatesInWorstCase = new LinkedList<SExpression>();
    List<SExpression> flowRatesOutWorstCase = new LinkedList<SExpression>();
    
    for (PortValue port : connectedPorts) {
      ConnectionValue channel = SchematicUtil.getConnection(schematic, port);
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
      Symbol flowRateWorstCase = SymbolNameGenerator
          .getsym_ChannelFlowRate_WorstCase(schematic, channel);
      if (connectedIntoJunction) {
        // flow rate is positive into the junction
        flowRatesIn.add(flowRate);
        flowRatesInWorstCase.add(flowRateWorstCase);
      } else {
        // flow rate is positive out of the junction
        flowRatesOut.add(flowRate);
        flowRatesOutWorstCase.add(flowRateWorstCase);
      }
    }
    List<SExpression> exprs = new LinkedList<>();
    exprs.add(QFNRA.assertEqual(QFNRA.add(flowRatesIn),
        QFNRA.add(flowRatesOut)));
    exprs.add(QFNRA.assertEqual(QFNRA.add(flowRatesInWorstCase),
        QFNRA.add(flowRatesOutWorstCase)));
    return exprs;
  }
  
}
