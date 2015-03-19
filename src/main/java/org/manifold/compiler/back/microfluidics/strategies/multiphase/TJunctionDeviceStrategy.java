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
import org.manifold.compiler.back.microfluidics.smt2.Macros;
import org.manifold.compiler.back.microfluidics.smt2.Numeral;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

public class TJunctionDeviceStrategy extends TranslationStrategy {

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
    // look for all T-junctions
    NodeTypeValue targetNode = typeTable.getTJunctionNodeType();
    for (NodeValue node : schematic.getNodes().values()) {
      if (!(node.getType().isSubtypeOf(targetNode))) {
        continue;
      }
      // pull connections out of the node
      try {
        // TODO refactor these into constants
        ConnectionValue chContinuous = getConnection(
            schematic, node.getPort("continuous"));
        ConnectionValue chDispersed = getConnection(
            schematic, node.getPort("dispersed"));
        ConnectionValue chOutput = getConnection(
            schematic, node.getPort("output"));
        exprs.addAll(translateTJunction(schematic, node, 
            chContinuous, chDispersed, chOutput));
      } catch (UndeclaredIdentifierException e) {
        throw new CodeGenerationError("undeclared identifier '" 
            + e.getIdentifier() + "' when inspecting T-junction node '"
            + schematic.getNodeName(node) + "'; "
            + "possible schematic version mismatch");
      }
      // TODO: look for all constraints relating to this T-junction
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
    if (!(connectedIntoJunction ^ isOutput)) {
      // negative flow
      return (QFNRA.assertLessThan(flowRate, new Numeral(0)));
    } else {
      // positive flow
      return (QFNRA.assertGreater(flowRate, new Numeral(0)));
    }
  }
  
  private SExpression calculatedDropletVolume(SExpression h, SExpression w,
      SExpression wIn, SExpression epsilon, SExpression qD, SExpression qC) {
    /* Predictive model for the size of bubbles and droplets 
     * created in microfluidic T-junctions.
     * van Steijn, Kleijn, and Kreutzer.
     * Lab Chip, 2010, 10, 2513.
     * doi:10.1039/c002625e
     */
    
    SExpression qGutterByQC = new Decimal(0.1);
    Symbol pi = SymbolNameGenerator.getsym_constant_pi();
    
    /* There are two expressions given for normalized-Vfill.
     * The (MUCH) simpler expression applies when wIn <= w;
     * the complex expression applies when wIn > w.
     * This requires a conditional expression to do correctly.
     */
    
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
    return QFNRA.multiply(QFNRA.multiply(h, QFNRA.multiply(w, w)), 
        QFNRA.add(normalizedVFill, 
            QFNRA.multiply(alpha, QFNRA.divide(qD, qC))));
  }
  
  private List<SExpression> translateTJunction(Schematic schematic,
      NodeValue junction,
      ConnectionValue chContinuous, ConnectionValue chDispersed,
      ConnectionValue chOutput) throws UndeclaredIdentifierException {
    List<SExpression> exprs = new LinkedList<>();

    Symbol nodeX = SymbolNameGenerator.getsym_NodeX(schematic, junction);
    Symbol nodeY = SymbolNameGenerator.getsym_NodeY(schematic, junction);
    exprs.add(QFNRA.declareRealVariable(nodeX));
    exprs.add(QFNRA.declareRealVariable(nodeY));
    
    // channel/junction characteristics
    Symbol h = SymbolNameGenerator
        .getsym_ChannelHeight(schematic, chContinuous);
    Symbol w = SymbolNameGenerator
        .getsym_ChannelWidth(schematic, chContinuous);
    Symbol wIn = SymbolNameGenerator
        .getsym_ChannelWidth(schematic, chDispersed);
    Symbol qC = SymbolNameGenerator
        .getsym_ChannelFlowRate(schematic, chContinuous);
    Symbol qD = SymbolNameGenerator
        .getsym_ChannelFlowRate(schematic, chDispersed);
    Symbol qOut = SymbolNameGenerator
        .getsym_ChannelFlowRate(schematic, chOutput);
    Symbol epsilon = SymbolNameGenerator
        .getsym_TJunctionEpsilon(schematic, junction);
    Symbol pi = SymbolNameGenerator.getsym_constant_pi();
    
    // declare epsilon
    exprs.add(QFNRA.declareRealVariable(epsilon));
    
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
    
    // constraint: epsilon is zero (sharp-edged t-junction)
    exprs.add(QFNRA.assertEqual(epsilon, new Numeral(0)));
    
    // constraint: all port pressures are equalized
    Symbol nodePressure = SymbolNameGenerator.getSym_NodePressure(
        schematic, junction);
    Symbol continuousPressure = SymbolNameGenerator.getSym_PortPressure(
        schematic, pContinuous);
    Symbol dispersePressure = SymbolNameGenerator.getSym_PortPressure(
        schematic, pDispersed);
    Symbol outputPressure = SymbolNameGenerator.getSym_PortPressure(
        schematic, pOutput);
    // declare these too
    exprs.add(QFNRA.declareRealVariable(nodePressure));
    exprs.add(QFNRA.declareRealVariable(continuousPressure));
    exprs.add(QFNRA.declareRealVariable(dispersePressure));
    exprs.add(QFNRA.declareRealVariable(outputPressure));
    exprs.add(QFNRA.assertEqual(nodePressure, continuousPressure));
    exprs.add(QFNRA.assertEqual(nodePressure, dispersePressure));
    exprs.add(QFNRA.assertEqual(nodePressure, outputPressure));
    
    // constraint: flow in = flow out
    List<PortValue> connectedPorts = new LinkedList<PortValue>();
    connectedPorts.add(pContinuous);
    connectedPorts.add(pDispersed);
    connectedPorts.add(pOutput);
    exprs.add(Macros.generateConservationOfFlow(schematic, connectedPorts));
    
    // constraint: viscosity of output = viscosity of continuous
    Symbol continuousViscosity = SymbolNameGenerator.getsym_ChannelViscosity(
        schematic, chContinuous);
    Symbol outputViscosity = SymbolNameGenerator.getsym_ChannelViscosity(
        schematic, chOutput);
    exprs.add(QFNRA.assertEqual(continuousViscosity, outputViscosity));
    
    
    Symbol vOutput = SymbolNameGenerator
        .getsym_ChannelDropletVolume(schematic, chOutput);
    exprs.add(QFNRA.declareRealVariable(vOutput));
    exprs.add(QFNRA.assertEqual(vOutput, calculatedDropletVolume(
        h, w, wIn, epsilon, qD, qC)));
    
    return exprs;
  }
  
}
