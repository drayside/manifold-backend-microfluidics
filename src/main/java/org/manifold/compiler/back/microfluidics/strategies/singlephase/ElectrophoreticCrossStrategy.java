package org.manifold.compiler.back.microfluidics.strategies.singlephase;

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
        .getsym_EPCrossSeparationLength(schematic, nCross);
    Symbol lenTail = SymbolNameGenerator
        .getsym_EPCrossTailLength(schematic, nCross);
    Symbol lenCross = SymbolNameGenerator
        .getsym_EPCrossLength(schematic, nCross);
    Symbol injectionSampleVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionSampleVoltage(schematic, nCross);
    Symbol injectionWasteVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionWasteVoltage(schematic, nCross);
    Symbol injectionCathodeVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionCathodeVoltage(schematic, nCross);
    Symbol separationChannelE = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelE(schematic, nCross);
    Symbol separationChannelOuterRadius = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelOuterRadius(schematic, nCross);
    Symbol separationChannelInnerRadius = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelInnerRadius(schematic, nCross);
    Symbol separationChannelElectricalConductivity = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelElectricalConductivity(
            schematic, nCross);
    Symbol separationChannelThermalConductivity = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelThermalConductivity(schematic, nCross);
    Symbol sampleDiffusionCoefficient = SymbolNameGenerator
        .getsym_EPCrossSampleDiffusionCoefficient(schematic, nCross);

    // declare variables
    exprs.add(QFNRA.declareRealVariable(lenSeparation));
    exprs.add(QFNRA.declareRealVariable(lenTail));
    exprs.add(QFNRA.declareRealVariable(lenCross));
    exprs.add(QFNRA.declareRealVariable(injectionSampleVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionWasteVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionCathodeVoltage));
    exprs.add(QFNRA.declareRealVariable(separationChannelE));
    exprs.add(QFNRA.declareRealVariable(separationChannelOuterRadius));
    exprs.add(QFNRA.declareRealVariable(separationChannelInnerRadius));
    exprs.add(QFNRA.declareRealVariable(
        separationChannelElectricalConductivity));
    exprs.add(QFNRA.declareRealVariable(separationChannelThermalConductivity));
    exprs.add(QFNRA.declareRealVariable(sampleDiffusionCoefficient));

    // physical constraints
    exprs.add(QFNRA.assertEqual(lenCross, QFNRA.add(lenSeparation, lenTail)));

    // pull-back voltage constraints
    exprs.add(QFNRA.assertEqual(injectionSampleVoltage,
        QFNRA.multiply(injectionCathodeVoltage, 
            QFNRA.divide(lenSeparation, lenCross))));
    exprs.add(QFNRA.assertEqual(
        injectionSampleVoltage, injectionWasteVoltage));

    // Joule heating constraints
    // TODO: verify that sign is correct in equation
    exprs.add(QFNRA.assertEqual(separationChannelE,
        QFNRA.divide(injectionCathodeVoltage, lenCross)));
    exprs.add(QFNRA.assertLessThan(
        QFNRA.multiply(
            QFNRA.divide(
                QFNRA.multiply(
                    QFNRA.pow(
                        QFNRA.multiply(
                            separationChannelInnerRadius, 
                            separationChannelE
                        ),
                        new Numeral(2)
                    ),
                    separationChannelElectricalConductivity
                ),
                QFNRA.multiply(
                    new Numeral(2), 
                    separationChannelThermalConductivity
                )
            ),
            QFNRA.log(
                QFNRA.divide(
                    separationChannelOuterRadius,
                    separationChannelInnerRadius
                )
            )
        ),
        new Numeral(1)
    ));

    return exprs;
  }
  
}
