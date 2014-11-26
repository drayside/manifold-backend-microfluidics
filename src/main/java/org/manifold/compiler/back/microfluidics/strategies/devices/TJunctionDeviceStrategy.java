package org.manifold.compiler.back.microfluidics.strategies.devices;

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
    NodeTypeValue targetNode = typeTable.getTJunctionNodetype();
    for (NodeValue node : schematic.getNodes().values()) {
      if (!(node.getType().isSubtypeOf(targetNode))) {
        continue;
      }
      // pull connections out of the node
      try {
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
    }
    return exprs;
  }

  private List<SExpression> translateTJunction(Schematic schematic,
      NodeValue junction,
      ConnectionValue chContinuous, ConnectionValue chDispersed,
      ConnectionValue chOutput) {
    /* Predictive model for the size of bubbles and droplets 
     * created in microfluidic T-junctions.
     * van Steijn, Kleijn, and Kreutzer.
     * Lab Chip, 2010, 10, 2513.
     * doi:10.1039/c002625e
     */
    List<SExpression> exprs = new LinkedList<>();

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
    Symbol pi = SymbolNameGenerator.getsym_constant_pi();
    
    // TODO physical constraints (e.g. channel dimension equality)
    
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
    SExpression vFillComplex = null; // TODO
    SExpression normalizedVFill = QFNRA.conditional(
        QFNRA.lessThanEqual(wIn, w),
        vFillSimple,
        vFillComplex);
    
    SExpression alpha = null; // TODO
    // the droplet volume at the output (Voutput) is given by
    // Voutput/hw^2 = Vfill/hw^2 + alpha * Qd/Qc
    Symbol vOutput = SymbolNameGenerator
        .getsym_ChannelDropletVolume(schematic, chOutput);
    exprs.add(QFNRA.assertEqual(vOutput, 
        QFNRA.multiply(QFNRA.multiply(h, QFNRA.multiply(w, w)), 
            QFNRA.add(normalizedVFill, 
                QFNRA.multiply(alpha, QFNRA.divide(qD, qC))))));
    
    return exprs;
  }
  
}
