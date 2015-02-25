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

public class ElectrophoreticCrossStrategy extends TranslationStrategy {

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
    NodeTypeValue targetNode = typeTable.getElectrophoreticCrossType();
    for (NodeValue node : schematic.getNodes().values()) {
      if (!(node.getType().isSubtypeOf(targetNode))) {
        continue;
      }
      // pull connections out of the node
      try {
        // TODO refactor these into constants
        exprs.addAll(translateElectrophoreticCross(schematic, node)); 
      } catch (UndeclaredIdentifierException e) {
        throw new CodeGenerationError("undeclared identifier '" 
            + e.getIdentifier() + "' when inspecting electrophoretic node '"
            + schematic.getNodeName(node) + "'; "
            + "possible schematic version mismatch");
      }
    }
    return exprs;
  }
  
  private List<SExpression> translateElectrophoreticCross(Schematic schematic,
      NodeValue nCross) throws UndeclaredIdentifierException {
    List<SExpression> exprs = new LinkedList<>();

    Symbol lenSeparation = SymbolNameGenerator
        .getsym_ElectrophoreticCrossSeparationLength(schematic, nCross);
    Symbol lenTail = SymbolNameGenerator
        .getsym_ElectrophoreticCrossTailLength(schematic, nCross);
    Symbol injectionSampleVoltage = SymbolNameGenerator
        .getsym_ElectrophoreticCrossInjectionSampleVoltage(schematic, nCross);
    Symbol injectionWasteVoltage = SymbolNameGenerator
        .getsym_ElectrophoreticCrossInjectionWasteVoltage(schematic, nCross);
    Symbol injectionCathodeVoltage = SymbolNameGenerator
        .getsym_ElectrophoreticCrossInjectionCathodeVoltage(schematic, nCross);

    // pull-back voltage constraints
    exprs.add(QFNRA.assertEqual(injectionSampleVoltage,
        QFNRA.multiply(injectionCathodeVoltage, 
            QFNRA.divide(lenSeparation,
                QFNRA.add(lenSeparation, lenTail)))));
    exprs.add(QFNRA.assertEqual(
        injectionSampleVoltage, injectionWasteVoltage));

    return exprs;
  }
  
}
