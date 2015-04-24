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

  // approximate erfc(x) = 1 - erf(x), assuming x >= 0
  protected SExpression erfc(SExpression exp) {
      SExpression ret = QFNRA.divide(
          new Numeral(1),
          QFNRA.pow(
              QFNRA.add(
                  new Numeral(1),
                  QFNRA.add(
                      QFNRA.multiply(
                          new Decimal(0.278393), 
                          exp
                      ),
                      QFNRA.add(
                          QFNRA.multiply(
                              new Decimal(0.230389),
                              QFNRA.pow(
                                  exp, 
                                  new Numeral(2)
                              )
                          ),
                          QFNRA.add(
                              QFNRA.multiply(
                                  new Decimal(0.000972),
                                  QFNRA.pow(
                                      exp,
                                      new Numeral(3)
                                  )
                              ),
                              QFNRA.multiply(
                                  new Decimal(0.078108),
                                  QFNRA.pow(
                                      exp,
                                      new Numeral(4)
                                  )
                              )
                          )
                      )
                  )
              ),
              new Numeral(4)
          )
      );
      return ret;
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
    Symbol baselineTimeLeakageConcentration = SymbolNameGenerator
        .getsym_EPCrossBaselineTimeLeakageConcentration(schematic, nCross);
    Symbol baselineTimeSampleConcentration = SymbolNameGenerator
        .getsym_EPCrossBaselineTimeSampleConcentration(schematic, nCross);
    Symbol peakTimeSampleConcentration = SymbolNameGenerator
        .getsym_EPCrossPeakTimeSampleConcentration(schematic, nCross);
    Symbol sampleInitialConcentration = SymbolNameGenerator
        .getsym_EPCrossSampleInitialConcentration(schematic, nCross);
    Symbol sampleInitialSpread = SymbolNameGenerator
        .getsym_EPCrossSampleInitialSpread(schematic, nCross);
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
    Symbol peakTime = SymbolNameGenerator
        .getsym_EPCrossPeakTime(schematic, nCross);
    Symbol baselineTime = SymbolNameGenerator
        .getsym_EPCrossBaselineTime(schematic, nCross);
    Symbol sampleChannelRadius = SymbolNameGenerator
        .getsym_EPCrossSampleChannelRadius(schematic, nCross);
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
    exprs.add(QFNRA.declareRealVariable(baselineTimeLeakageConcentration));
    exprs.add(QFNRA.declareRealVariable(baselineTimeSampleConcentration));
    exprs.add(QFNRA.declareRealVariable(peakTimeSampleConcentration));
    exprs.add(QFNRA.declareRealVariable(sampleInitialConcentration));
    exprs.add(QFNRA.declareRealVariable(sampleInitialSpread));
    exprs.add(QFNRA.declareRealVariable(sampleDiffusionConstant));
    exprs.add(QFNRA.declareRealVariable(sampleCharge));
    exprs.add(QFNRA.declareRealVariable(sampleHydrodynamicRadius));
    exprs.add(QFNRA.declareRealVariable(sampleElectrophoreticMobility));
    exprs.add(QFNRA.declareRealVariable(bulkMobility));
    exprs.add(QFNRA.declareRealVariable(bulkViscosity));
    exprs.add(QFNRA.declareRealVariable(separationDistance));
    exprs.add(QFNRA.declareRealVariable(peakTime));
    exprs.add(QFNRA.declareRealVariable(baselineTime));
    exprs.add(QFNRA.declareRealVariable(sampleChannelRadius));
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
        new Decimal(-3.75e-8)
    ));
    exprs.add(QFNRA.assertEqual(
        injectionAnodeNodeVoltage, 
        new Decimal(-1e3)
    ));
    exprs.add(QFNRA.assertEqual(
        injectionCathodeNodeVoltage, 
        new Decimal(0)
    ));
    exprs.add(QFNRA.assertEqual(lenSeparationChannel, new Decimal(0.030)));
    exprs.add(QFNRA.assertEqual(lenTail, new Decimal(0.0045)));
    exprs.add(QFNRA.assertEqual(lenSampleChannel, new Decimal(0.0045)));
    exprs.add(QFNRA.assertEqual(lenWasteChannel, new Decimal(0.0045)));
    exprs.add(QFNRA.assertEqual(separationDistance, new Decimal(0.025)));
    exprs.add(QFNRA.assertEqual(sampleInitialConcentration, new Decimal(1)));
    exprs.add(QFNRA.assertEqual(sampleDiffusionConstant, new Decimal(5.85e-12)));
    exprs.add(QFNRA.assertEqual(sampleChannelRadius, new Decimal(5e-5)));

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
    exprs.add(QFNRA.assertLessThan(
        injectionSampleNodeVoltage, 
        injectionCathodeNodeVoltage
    ));
    exprs.add(QFNRA.assertGreater(
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
    // TODO: Consider using more accurate formula?
    exprs.add(QFNRA.assertEqual(peakTime, 
        QFNRA.divide(separationDistance, 
            injectionSeparationChannelSampleVelocity)));
    exprs.add(QFNRA.assertGreater(baselineTime, peakTime));
    exprs.add(QFNRA.assertEqual(sampleInitialSpread, 
        QFNRA.divide(
            sampleChannelRadius, 
            new Decimal(2.355)
        )
    ));
    exprs.add(QFNRA.assertEqual(baselineTimeLeakageConcentration, 
        QFNRA.divide(
            QFNRA.multiply(
                sampleInitialConcentration,
                erfc(
                    QFNRA.divide(
                        QFNRA.multiply(
                            injectionSampleChannelSampleVelocity,
                            QFNRA.subtract(
                                baselineTime,
                                peakTime
                            )
                        ),
                        QFNRA.multiply(
                            new Numeral(2),
                            QFNRA.sqrt(
                                QFNRA.multiply(
                                    sampleDiffusionConstant,
                                    QFNRA.subtract(
                                        baselineTime,
                                        peakTime
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            QFNRA.multiply(
                QFNRA.sqrt(
                    QFNRA.multiply(
                        new Numeral(2),
                        new Decimal(3.14159) // TODO: refactor out
                    )
                ),
                sampleInitialSpread
            )
        )
    ));
    exprs.add(QFNRA.assertEqual(peakTimeSampleConcentration, 
        QFNRA.divide(
            QFNRA.multiply(
                sampleInitialConcentration,
                QFNRA.exp(
                    QFNRA.divide(
                        QFNRA.pow(
                            QFNRA.subtract(
                                separationDistance,
                                QFNRA.multiply(
                                    injectionSeparationChannelSampleVelocity,
                                    peakTime
                                )
                            ),
                            new Numeral(2)
                        ),
                        QFNRA.multiply(
                            new Numeral(-2),
                            QFNRA.pow(
                                QFNRA.add(
                                    sampleInitialSpread,
                                    QFNRA.sqrt(
                                        QFNRA.multiply(
                                            new Numeral(2),
                                            QFNRA.multiply(
                                                sampleDiffusionConstant,
                                                peakTime
                                            )
                                        )
                                    )
                                ),
                                new Numeral(2)
                            )
                        )
                    )
                )
            ),
            QFNRA.multiply(
                QFNRA.sqrt(
                    QFNRA.multiply(
                        new Numeral(2),
                        new Decimal(3.14159) // TODO: refactor out
                    )
                ),
                QFNRA.add(
                    sampleInitialSpread,
                    QFNRA.sqrt(
                        QFNRA.multiply(
                            new Numeral(2),
                            QFNRA.multiply(
                                sampleDiffusionConstant,
                                peakTime
                            )
                        )
                    )
                )
            )
        )
    ));
    exprs.add(QFNRA.assertEqual(baselineTimeSampleConcentration, 
        QFNRA.divide(
            QFNRA.multiply(
                sampleInitialConcentration,
                QFNRA.exp(
                    QFNRA.divide(
                        QFNRA.pow(
                            QFNRA.subtract(
                                separationDistance,
                                QFNRA.multiply(
                                    injectionSeparationChannelSampleVelocity,
                                    baselineTime
                                )
                            ),
                            new Numeral(2)
                        ),
                        QFNRA.multiply(
                            new Numeral(-2),
                            QFNRA.pow(
                                QFNRA.add(
                                    sampleInitialSpread,
                                    QFNRA.sqrt(
                                        QFNRA.multiply(
                                            new Numeral(2),
                                            QFNRA.multiply(
                                                sampleDiffusionConstant,
                                                baselineTime
                                            )
                                        )
                                    )
                                ),
                                new Numeral(2)
                            )
                        )
                    )
                )
            ),
            QFNRA.multiply(
                QFNRA.sqrt(
                    QFNRA.multiply(
                        new Numeral(2),
                        new Decimal(3.14159) // TODO: refactor out
                    )
                ),
                QFNRA.add(
                    sampleInitialSpread,
                    QFNRA.sqrt(
                        QFNRA.multiply(
                            new Numeral(2),
                            QFNRA.multiply(
                                sampleDiffusionConstant,
                                baselineTime
                            )
                        )
                    )
                )
            )
        )
    ));
    exprs.add(QFNRA.assertEqual(
        baselineTimeSampleConcentration,
        QFNRA.divide(
            peakTimeSampleConcentration,
            new Decimal(10)
        )
    ));
    // TODO: Ideally this should be >=, but doesn't always return optimal 
    // ranges without ability to specify some sort of minimization objective.
    exprs.add(QFNRA.assertEqual(
        QFNRA.divide(
            baselineTimeSampleConcentration,
            baselineTimeLeakageConcentration
        ),
        new Decimal(1)
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
