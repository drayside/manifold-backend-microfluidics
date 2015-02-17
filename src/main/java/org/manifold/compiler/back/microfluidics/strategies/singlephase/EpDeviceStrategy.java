package org.manifold.compiler.back.microfluidics.strategies.multiphase;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeTypeValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.UndeclaredIdentifierException;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.Numeral;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

/**
*Constrainti
* Colinearity of the channels
* rc_collection + rc_dispense = length
* Perpendicar(Ch1 and Rc_collection), same for ch2 and rc_collection
* Placement of rc_collection and rc_dispense has to be within chip
*Attributes
*- position of the crossing point
*- width of the node(length of channels)
*- length of the node
* - rc_collection length
* - rc_dispense length
**/



public class EpDeviceStrategy extends TranslationStrategy {

  //get the connection associated with this port
  // TODO this is VERY EXPENSIVE, find an optimization
  protected ConnectionValue getConnection(
      Schematic schematic, PortValue port) {
    for (ConnectionValue conn : schematic.getConnections().values()) {
      if (conn.getFrom().equals(port) || conn.getTo().equals(port)) {
        return conn;
      }
    }
    return null;
  }
  
  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    // look for all electrophoretic nodes
    NodeTypeValue targetNode = typeTable.getElectrophoreticNodeType();
    for (NodeValue node : schematic.getNodes().values()) {
      if (!(node.getType().isSubtypeOf(targetNode))) {
        continue;
      }
      // pull connections out of the node
      try {
        // TODO refactor these into constants
        ConnectionValue entryChannel = getConnection(
            schematic, node.getPort("sampleIn"));
        ConnectionValue exitChannel = getConnection(
            schematic, node.getPort("wasteOut"));
        exprs.addAll(translateElectrophoreticDevice(schematic, node, 
            entryChannel, exitChannel));
      } catch (UndeclaredIdentifierException e) {
        throw new CodeGenerationError("undeclared identifier '" 
            + e.getIdentifier() + "' when inspecting electrophoretic node '"
            + schematic.getNodeName(node) + "'; "
            + "possible schematic version mismatch");
      }
    }
    return exprs;
  }

  /**
   *  Constrain the direction of flow in a channel
   *  given the desired direction of flow and the port into or out of which
   *  the flow passes.
   */
  private SExpression constrainFlowDirection(Schematic schematic,
      PortValue port, ConnectionValue channel, boolean isOutput) {
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
    // if the connection direction and constraint direction are different,
    // the flow rate must be negative; otherwise the flow is in
    // the same direction as the channel and so the flow is positive
    if (connectedIntoJunction ^ isOutput) {
      // negative flow
      return (QFNRA.assertLessThan(flowRate, new Numeral(0)));
    } else {
      // positive flow
      return (QFNRA.assertGreater(flowRate, new Numeral(0)));
    }
  }
  
  private List<SExpression> translateElectrophoreticDevice(Schematic schematic,
      NodeValue electrophoreticNode, ConnectionValue entryChannel, 
      ConnectionValue exitChannel) throws UndeclaredIdentifierException {
    List<SExpression> exprs = new LinkedList<>();

    // channel/node characteristics
    Symbol hEntry = SymbolNameGenerator
        .getsym_ChannelHeight(schematic, entryChannel);
    Symbol wEntry = SymbolNameGenerator
        .getsym_ChannelWidth(schematic, entryChannel);
    Symbol hExit = SymbolNameGenerator
        .getsym_ChannelHeight(schematic, exitChannel);
    Symbol wExit = SymbolNameGenerator
        .getsym_ChannelWidth(schematic, exitChannel);

/*
    // TODO constraint: all channels must be rectangular
    // TODO (?) constraint: continuous and output channel must be parallel
    // TODO (?) constraint: disperse and output channel must be perpendicular
    // constraint: flow rates must be positive into the junction at inputs
    PortValue pContinuous = junction.getPort("continuous");
    exprs.add(constrainFlowDirection(
        schematic, pContinuous, chContinuous, false));
    PortValue pDispersed = junction.getPort("dispersed");
    exprs.add(constrainFlowDirection(
        schematic, pDispersed, chDispersed, false));
    // constraint: flow rate must be positive out of the junction at output
    PortValue pOutput = junction.getPort("output");
    exprs.add(constrainFlowDirection(
        schematic, pOutput, chOutput, true));
    // constraint: channel width must be equal at the continuous medium
    // port and the output port
    exprs.add(QFNRA.assertEqual(w, 
        SymbolNameGenerator.getsym_ChannelWidth(schematic, chOutput)));
    
    // constraint: the height of all connected channels is equal
    exprs.add(QFNRA.assertEqual(SymbolNameGenerator
        .getsym_ChannelHeight(schematic, chContinuous), SymbolNameGenerator
        .getsym_ChannelHeight(schematic, chDispersed)));
    exprs.add(QFNRA.assertEqual(SymbolNameGenerator
        .getsym_ChannelHeight(schematic, chContinuous), SymbolNameGenerator
        .getsym_ChannelHeight(schematic, chOutput)));
    
    // constraint: epsilon is non-negative
    exprs.add(QFNRA.assertGreaterEqual(epsilon, new Numeral(0)));
*/    
    /* There are two expressions given for normalized-Vfill.
     * The (MUCH) simpler expression applies when wIn <= w;
     * the complex expression applies when wIn > w.
     * This requires a conditional expression to do correctly.
     */
 /*   
    // for the case where wIn <= w:
    // normalizedVFill = 3pi/8 - (pi/2)(1 - pi/4)(h/w)
    SExpression vFillSimple = QFNRA.subtract(
        QFNRA.multiply(new Decimal(3.0 / 8.0), pi), 
        QFNRA.multiply(QFNRA.multiply(
                QFNRA.divide(pi, new Numeral(2)), 
                QFNRA.subtract(new Numeral(1), 
                    QFNRA.divide(pi, new Numeral(4)))), 
                QFNRA.divide(h, w)));
    
    
    // for the case where wIn > w:
    // things get a lot more interesting
    SExpression vFillComplex = QFNRA.add(
        QFNRA.add(
            QFNRA.multiply(
                QFNRA.subtract(QFNRA.divide(pi, new Numeral(4)), 
                    QFNRA.multiply(new Decimal(0.5), 
                        QFNRA.arcsin(
                            QFNRA.subtract(new Numeral(1), QFNRA.divide(w, wIn))
              ))), 
                QFNRA.pow(QFNRA.divide(wIn, w), new Numeral(2))), 
            QFNRA.multiply(
                new Decimal(-0.5),
                QFNRA.multiply(
                    QFNRA.subtract(QFNRA.divide(wIn, w), new Numeral(1)),
                    QFNRA.pow(QFNRA.subtract(
                        QFNRA.multiply(new Numeral(2), 
                            QFNRA.divide(wIn, w)), new Numeral(1)), 
                            new Decimal(0.5))
          )
        )),
        QFNRA.add(
            QFNRA.divide(pi, new Numeral(8)),
            QFNRA.multiply(
                QFNRA.multiply(new Decimal(-0.5), 
                    QFNRA.subtract(new Numeral(1), 
                        QFNRA.divide(pi, new Numeral(4)))),
                QFNRA.multiply(
                    QFNRA.add(
                        QFNRA.multiply(QFNRA.subtract(
                            QFNRA.divide(pi, new Numeral(2)), 
                            QFNRA.arcsin(QFNRA.subtract(
                                new Numeral(1), QFNRA.divide(w, wIn)))), 
                            QFNRA.divide(wIn, w)),
                        QFNRA.divide(pi, new Numeral(2))), 
                    QFNRA.divide(h, w))
        )
      )
    );
    
    SExpression normalizedVFill = QFNRA.conditional(
        QFNRA.lessThanEqual(wIn, w),
        vFillSimple,
        vFillComplex);
    
    // alpha depends on these intermediate expressions
    // this first one appears at least three times as a subexpression of rPinch
    SExpression hwParallel = 
        QFNRA.divide(QFNRA.multiply(h, w), QFNRA.add(h, w));
    SExpression rPinch = QFNRA.add(
        w,
        QFNRA.add(
            QFNRA.subtract(wIn, QFNRA.subtract(hwParallel, epsilon)),
            QFNRA.pow(
                QFNRA.multiply(new Numeral(2),
                    QFNRA.multiply(
                        QFNRA.subtract(wIn, hwParallel),
                        QFNRA.subtract(w, hwParallel)
            )), new Decimal(0.5))));
    // rFill = max(w, wIn)
    SExpression rFill = QFNRA.conditional(
        QFNRA.greater(w, wIn), w, wIn);
    SExpression alpha = QFNRA.multiply(
        QFNRA.subtract(new Numeral(1), QFNRA.divide(pi, new Numeral(4))),
        QFNRA.multiply(
            QFNRA.pow(QFNRA.subtract(new Numeral(1), qGutterByQC), 
                new Numeral(-1)),
            QFNRA.add(
                QFNRA.subtract(
                    QFNRA.pow(QFNRA.divide(rPinch, w), new Numeral(2)), 
                    QFNRA.pow(QFNRA.divide(rFill, w), new Numeral(2))),
                QFNRA.multiply(QFNRA.divide(pi, new Numeral(4)), QFNRA.multiply(
                    QFNRA.subtract(
                        QFNRA.divide(rPinch, w), 
                        QFNRA.divide(rFill, w)),
                    QFNRA.divide(h, w))))));
    // the droplet volume at the output (Voutput) is given by
    // Voutput/hw^2 = Vfill/hw^2 + alpha * Qd/Qc
    Symbol vOutput = SymbolNameGenerator
        .getsym_ChannelDropletVolume(schematic, chOutput);
    exprs.add(QFNRA.assertEqual(vOutput, 
        QFNRA.multiply(QFNRA.multiply(h, QFNRA.multiply(w, w)), 
            QFNRA.add(normalizedVFill, 
                QFNRA.multiply(alpha, QFNRA.divide(qD, qC))))));
*/    
    return exprs;
  }
  
}
