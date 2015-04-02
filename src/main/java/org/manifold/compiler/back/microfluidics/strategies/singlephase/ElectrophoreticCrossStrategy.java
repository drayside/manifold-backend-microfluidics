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
  /*
   * Terminology:
   *                        __
   *                       (  ) Anode node
   *                       |  |
   *                       |  | <-- Tail channel
   *               ---------  ---------
   * Sample node ( ) --- loading -->  ( ) Waste node
   *               ---------  ---------
   *      Sample channel ^ |  | ^ Waste channel
   *                       |  |
   *                       |  |
   *                       |  |
   *                       |  | <-- Separation channel
   *                       |  |
   *                       |  |
   *                       |  |
   *                       (__) Cathode node
   *
   *   Electrophoretic rocess is split into two phases:
   *     1) Loading phase
   *     2) Separation phase
   */

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

    Symbol pi = SymbolNameGenerator.getsym_constant_pi();

    Symbol lenSeparationChannel = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelLength(schematic, nCross);
    Symbol lenTail = SymbolNameGenerator
        .getsym_EPCrossTailChannelLength(schematic, nCross);
    Symbol lenCross = SymbolNameGenerator
        .getsym_EPCrossLength(schematic, nCross);
    Symbol injectionSampleNodeVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionSampleNodeVoltage(schematic, nCross);
    Symbol injectionWasteNodeVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionWasteNodeVoltage(schematic, nCross);
    Symbol injectionAnodeNodeVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionAnodeNodeVoltage(schematic, nCross);
    Symbol injectionCathodeNodeVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionCathodeNodeVoltage(schematic, nCross);
    Symbol injectionSeparationChannelE = SymbolNameGenerator
        .getsym_EPCrossInjectionSeparationChannelE(schematic, nCross);
    Symbol injectionSeparationChannelSampleVelocity = SymbolNameGenerator
        .getsym_EPCrossInjectionSeparationChannelSampleVelocity(
            schematic, nCross);
    Symbol injectionSampleChannelE = SymbolNameGenerator
        .getsym_EPCrossInjectionSampleChannelE(schematic, nCross);
    Symbol injectionSampleChannelSampleVelocity = SymbolNameGenerator
        .getsym_EPCrossInjectionSampleChannelSampleVelocity(schematic, nCross);
    Symbol separationTimeLeakageConcentration = SymbolNameGenerator
        .getsym_EPCrossSeparationTimeLeakageConcentration(schematic, nCross);
    Symbol sampleInitialConcentration = SymbolNameGenerator
        .getsym_EPCrossSampleInitialConcentration(schematic, nCross);
    Symbol sampleDiffusionConstant = SymbolNameGenerator
        .getsym_EPCrossSampleDiffusionConstant(schematic, nCross);
    Symbol bulkMobility = SymbolNameGenerator
        .getsym_EPCrossBulkMobility(schematic, nCross);
    Symbol separationDistance = SymbolNameGenerator
        .getsym_EPCrossSeparationDistance(schematic, nCross);
    Symbol separationTime = SymbolNameGenerator
        .getsym_EPCrossSeparationTime(schematic, nCross);
    /*Symbol separationChannelOuterRadius = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelOuterRadius(schematic, nCross);
    Symbol separationChannelInnerRadius = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelInnerRadius(schematic, nCross);
    Symbol separationChannelElectricalConductivity = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelElectricalConductivity(
            schematic, nCross);
    Symbol separationChannelThermalConductivity = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelThermalConductivity(schematic, nCross);*/

    // declare variables
    exprs.add(QFNRA.declareRealVariable(lenSeparationChannel));
    exprs.add(QFNRA.declareRealVariable(lenTail));
    exprs.add(QFNRA.declareRealVariable(lenCross));
    exprs.add(QFNRA.declareRealVariable(injectionSampleNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionWasteNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionAnodeNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionCathodeNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionSeparationChannelE));
    exprs.add(QFNRA.declareRealVariable(
        injectionSeparationChannelSampleVelocity));
    exprs.add(QFNRA.declareRealVariable(injectionSampleChannelE));
    exprs.add(QFNRA.declareRealVariable(injectionSampleChannelSampleVelocity));
    exprs.add(QFNRA.declareRealVariable(separationTimeLeakageConcentration));
    exprs.add(QFNRA.declareRealVariable(sampleInitialConcentration));
    exprs.add(QFNRA.declareRealVariable(sampleDiffusionConstant));
    exprs.add(QFNRA.declareRealVariable(separationDistance));
    exprs.add(QFNRA.declareRealVariable(separationTime));
    /*exprs.add(QFNRA.declareRealVariable(separationChannelOuterRadius));
    exprs.add(QFNRA.declareRealVariable(separationChannelInnerRadius));
    exprs.add(QFNRA.declareRealVariable(
        separationChannelElectricalConductivity));
    exprs.add(QFNRA.declareRealVariable(separationChannelThermalConductivity));*/

    // physical constraints
    exprs.add(QFNRA.assertEqual(lenCross, 
        QFNRA.add(lenSeparationChannel, lenTail)));

    // pull-back voltage constraints
    exprs.add(QFNRA.assertEqual(injectionSeparationChannelE,
        QFNRA.divide(
            QFNRA.subtract(
                injectionAnodeNodeVoltage, 
                injectionCathodeNodeVoltage
            ),
            lenCross
        )
    ));
    exprs.add(QFNRA.assertEqual(separationTime, 
        QFNRA.divide(separationDistance, 
            injectionSeparationChannelSampleVelocity)));
    exprs.add(QFNRA.assertGreater(injectionSampleNodeVoltage,
        QFNRA.multiply(injectionAnodeNodeVoltage, 
            QFNRA.divide(lenSeparationChannel, lenCross))));
    exprs.add(QFNRA.assertLessThan(
        injectionSampleNodeVoltage, injectionCathodeNodeVoltage));
    exprs.add(QFNRA.assertEqual(
        injectionSampleNodeVoltage, injectionWasteNodeVoltage));
    exprs.add(QFNRA.assertEqual(separationTimeLeakageConcentration, 
        QFNRA.divide(
            QFNRA.multiply(
                sampleInitialConcentration,
                QFNRA.exp(
                    QFNRA.divide(
                        QFNRA.multiply(
                            new Numeral(-1),
                            QFNRA.pow(
                                QFNRA.multiply(
                                    injectionSampleChannelSampleVelocity,
                                    separationTime
                                ),
                                new Numeral(2)
                            )
                        ),
                        QFNRA.multiply(
                            new Numeral(4),
                            QFNRA.multiply(
                                sampleDiffusionConstant,
                                separationTime
                            )
                        )
                    )
                )
            ),
            QFNRA.multiply(
                new Numeral(2),
                QFNRA.sqrt(
                    QFNRA.multiply(
                        pi,
                        QFNRA.multiply(
                            sampleDiffusionConstant,
                            separationTime
                        )
                    )
                )
            )
        )
    ));

    // Joule heating constraints
    // TODO: verify that sign is correct in equation
    /*exprs.add(QFNRA.assertLessThan(
        QFNRA.multiply(
            QFNRA.divide(
                QFNRA.multiply(
                    QFNRA.pow(
                        QFNRA.multiply(
                            separationChannelInnerRadius, 
                            injectionSeparationChannelE
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
    ));*/

    return exprs;
  }
  
}
