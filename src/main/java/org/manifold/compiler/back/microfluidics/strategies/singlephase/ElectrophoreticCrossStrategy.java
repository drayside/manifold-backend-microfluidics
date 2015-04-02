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

    Symbol lenSeparationChannel = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelLength(schematic, nCross);
    Symbol lenTail = SymbolNameGenerator
        .getsym_EPCrossTailChannelLength(schematic, nCross);
    Symbol lenSampleChannel = SymbolNameGenerator
        .getsym_EPCrossSampleChannelLength(schematic, nCross);
    Symbol lenWasteChannel = SymbolNameGenerator
        .getsym_EPCrossWasteChannelLength(schematic, nCross);
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
    Symbol injectionIntersectionVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionIntersectionVoltage(schematic, nCross);
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
    Symbol separationTimePeakSampleConcentration = SymbolNameGenerator
        .getsym_EPCrossSeparationTimePeakSampleConcentration(
            schematic, nCross);
    Symbol sampleInitialConcentration = SymbolNameGenerator
        .getsym_EPCrossSampleInitialConcentration(schematic, nCross);
    Symbol sampleDiffusionConstant = SymbolNameGenerator
        .getsym_EPCrossSampleDiffusionConstant(schematic, nCross);
    Symbol sampleCharge = SymbolNameGenerator
        .getsym_EPCrossSampleCharge(schematic, nCross);
    Symbol sampleHydrodynamicRadius = SymbolNameGenerator
        .getsym_EPCrossSampleHydrodynamicRadius(schematic, nCross);
    Symbol sampleElectrophoreticMobility = SymbolNameGenerator
        .getsym_EPCrossSampleElectrophoreticMobility(schematic, nCross);
    Symbol bulkMobility = SymbolNameGenerator
        .getsym_EPCrossBulkMobility(schematic, nCross);
    Symbol bulkViscosity = SymbolNameGenerator
        .getsym_EPCrossBulkViscosity(schematic, nCross);
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
    exprs.add(QFNRA.declareRealVariable(lenSampleChannel));
    exprs.add(QFNRA.declareRealVariable(lenWasteChannel));
    exprs.add(QFNRA.declareRealVariable(lenCross));
    exprs.add(QFNRA.declareRealVariable(injectionSampleNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionWasteNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionAnodeNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionCathodeNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionIntersectionVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionSeparationChannelE));
    exprs.add(QFNRA.declareRealVariable(
        injectionSeparationChannelSampleVelocity));
    exprs.add(QFNRA.declareRealVariable(injectionSampleChannelE));
    exprs.add(QFNRA.declareRealVariable(injectionSampleChannelSampleVelocity));
    exprs.add(QFNRA.declareRealVariable(separationTimeLeakageConcentration));
    exprs.add(QFNRA.declareRealVariable(separationTimePeakSampleConcentration));
    exprs.add(QFNRA.declareRealVariable(sampleInitialConcentration));
    exprs.add(QFNRA.declareRealVariable(sampleDiffusionConstant));
    exprs.add(QFNRA.declareRealVariable(sampleCharge));
    exprs.add(QFNRA.declareRealVariable(sampleHydrodynamicRadius));
    exprs.add(QFNRA.declareRealVariable(sampleElectrophoreticMobility));
    exprs.add(QFNRA.declareRealVariable(bulkMobility));
    exprs.add(QFNRA.declareRealVariable(bulkViscosity));
    exprs.add(QFNRA.declareRealVariable(separationDistance));
    exprs.add(QFNRA.declareRealVariable(separationTime));
    /*exprs.add(QFNRA.declareRealVariable(separationChannelOuterRadius));
    exprs.add(QFNRA.declareRealVariable(separationChannelInnerRadius));
    exprs.add(QFNRA.declareRealVariable(
        separationChannelElectricalConductivity));
    exprs.add(QFNRA.declareRealVariable(separationChannelThermalConductivity));*/

    // "constants"
    // Distances are in metres, times are in seconds, voltages are in volts, 
    // masses are in kilograms
    exprs.add(QFNRA.assertEqual(bulkViscosity, new Decimal(0.001002)));
    exprs.add(QFNRA.assertEqual(bulkMobility, new Decimal(1e-8)));
    exprs.add(QFNRA.assertEqual(
        sampleElectrophoreticMobility, 
        new Decimal(1e-7)
    ));
    exprs.add(QFNRA.assertEqual(injectionAnodeNodeVoltage, new Decimal(0)));
    exprs.add(QFNRA.assertEqual(
        injectionCathodeNodeVoltage, 
        new Decimal(-1e3)
    ));
    exprs.add(QFNRA.assertEqual(lenSeparationChannel, new Decimal(0.030)));
    exprs.add(QFNRA.assertEqual(lenTail, new Decimal(0.0045)));
    exprs.add(QFNRA.assertEqual(lenSampleChannel, new Decimal(0.0045)));
    exprs.add(QFNRA.assertEqual(lenWasteChannel, new Decimal(0.0045)));
    exprs.add(QFNRA.assertEqual(separationDistance, new Decimal(0.025)));
    exprs.add(QFNRA.assertEqual(sampleInitialConcentration, new Decimal(1)));
    exprs.add(QFNRA.assertEqual(sampleDiffusionConstant, new Decimal(1e-13)));

    // physical constraints
    exprs.add(QFNRA.assertEqual(lenCross, 
        QFNRA.add(lenSeparationChannel, lenTail)));

    // pull-back voltage constraints
    exprs.add(QFNRA.assertEqual(injectionIntersectionVoltage,
        QFNRA.add(
            injectionCathodeNodeVoltage,
            QFNRA.multiply(
                QFNRA.subtract(
                    injectionAnodeNodeVoltage, 
                    injectionCathodeNodeVoltage
                ),
                QFNRA.divide(
                    lenSeparationChannel,
                    lenCross
                )
            )
        )
    ));
    exprs.add(QFNRA.assertEqual(
        injectionSampleNodeVoltage, 
        injectionWasteNodeVoltage
    ));
    exprs.add(QFNRA.assertGreater(
        injectionSampleNodeVoltage, 
        injectionCathodeNodeVoltage
    ));
    exprs.add(QFNRA.assertLessThan(
        injectionSampleNodeVoltage,
        injectionIntersectionVoltage
    ));
    exprs.add(QFNRA.assertEqual(injectionSeparationChannelE,
        QFNRA.divide(
            QFNRA.subtract(
                injectionAnodeNodeVoltage,
                injectionCathodeNodeVoltage
            ),
            lenCross
        )
    ));
    exprs.add(QFNRA.assertEqual(injectionSampleChannelE,
        QFNRA.divide(
            QFNRA.subtract(
                injectionIntersectionVoltage,
                injectionSampleNodeVoltage
            ),
            lenSampleChannel
        )
    ));
    /*exprs.add(QFNRA.assertEqual(sampleElectrophoreticMobility,
        QFNRA.divide(
            sampleCharge,
            QFNRA.multiply(
                new Numeral(6),
                QFNRA.multiply(
                    new Decimal(3.14159),
                    QFNRA.multiply(
                        bulkViscosity,
                        sampleHydrodynamicRadius
                    )
                )
            )
        )
    ));*/
    exprs.add(QFNRA.assertEqual(injectionSeparationChannelSampleVelocity,
        QFNRA.multiply(
            QFNRA.add(
                bulkMobility,
                sampleElectrophoreticMobility
            ),
            injectionSeparationChannelE
        )
    ));
    exprs.add(QFNRA.assertEqual(injectionSampleChannelSampleVelocity,
        QFNRA.multiply(
            QFNRA.add(
                bulkMobility,
                sampleElectrophoreticMobility
            ),
            injectionSampleChannelE
        )
    ));
    exprs.add(QFNRA.assertEqual(separationTime, 
        QFNRA.divide(separationDistance, 
            injectionSeparationChannelSampleVelocity)));
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
                        new Decimal(3.14159), // TODO: refactor out
                        QFNRA.multiply(
                            sampleDiffusionConstant,
                            separationTime
                        )
                    )
                )
            )
        )
    ));
    exprs.add(QFNRA.assertEqual(separationTimePeakSampleConcentration, 
        QFNRA.divide(
            sampleInitialConcentration,
            QFNRA.multiply(
                new Numeral(2),
                QFNRA.sqrt(
                    QFNRA.multiply(
                        new Decimal(3.14159), // TODO: refactor out
                        QFNRA.multiply(
                            sampleDiffusionConstant,
                            separationTime
                        )
                    )
                )
            )
        )
    ));
    exprs.add(QFNRA.assertGreaterEqual(
        QFNRA.divide(
            QFNRA.add(
                separationTimePeakSampleConcentration,
                separationTimeLeakageConcentration
            ),
            separationTimeLeakageConcentration
        ),
        new Decimal(2)
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
