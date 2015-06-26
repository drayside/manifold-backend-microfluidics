package org.manifold.compiler.back.microfluidics.strategies.singlephase;

import java.util.LinkedList;
import java.util.ArrayList;
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
   *                       (  ) Cathode node
   *                       |  |
   *                       |  | <-- Tail channel
   *               ---------  ---------
   * Sample node ( ) --- loading -->  ( ) Waste node
   *               ---------  ---------
   *   Injection channel ^ |  | ^ Waste channel
   *                       |  |
   *                       |  |
   *                       |  |
   *                       |  | <-- Separation channel
   *                       |  |
   *                       |  |
   *                       |  |
   *                       (__) Anode node
   *
   *   Electrophoretic process is split into two phases:
   *     1) Loading phase
   *     2) Separation phase
   *   The two aforementioned phases are separated by an injection step.
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
    final int numAnalytes = 3;

    Symbol lenSeparationChannel = SymbolNameGenerator
        .getsym_EPCrossSeparationChannelLength(schematic, nCross);
    Symbol lenTail = SymbolNameGenerator
        .getsym_EPCrossTailChannelLength(schematic, nCross);
    Symbol lenInjectionChannel = SymbolNameGenerator
        .getsym_EPCrossInjectionChannelLength(schematic, nCross);
    Symbol lenWasteChannel = SymbolNameGenerator
        .getsym_EPCrossWasteChannelLength(schematic, nCross);
    Symbol lenCross = SymbolNameGenerator
        .getsym_EPCrossLength(schematic, nCross);
    Symbol injectionSampleNodeVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionSampleNodeVoltage(schematic, nCross);
    Symbol injectionWasteNodeVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionWasteNodeVoltage(schematic, nCross);
    Symbol injectionCathodeNodeVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionCathodeNodeVoltage(schematic, nCross);
    Symbol injectionAnodeNodeVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionAnodeNodeVoltage(schematic, nCross);
    Symbol injectionIntersectionVoltage = SymbolNameGenerator
        .getsym_EPCrossInjectionIntersectionVoltage(schematic, nCross);
    Symbol injectionSeparationChannelE = SymbolNameGenerator
        .getsym_EPCrossInjectionSeparationChannelE(schematic, nCross);
    Symbol injectionInjectionChannelE = SymbolNameGenerator
        .getsym_EPCrossInjectionInjectionChannelE(schematic, nCross);    
    Symbol bulkMobility = SymbolNameGenerator
        .getsym_EPCrossBulkMobility(schematic, nCross);
    Symbol bulkViscosity = SymbolNameGenerator
        .getsym_EPCrossBulkViscosity(schematic, nCross);
    Symbol separationDistance = SymbolNameGenerator
        .getsym_EPCrossSeparationDistance(schematic, nCross);
    Symbol injectionChannelRadius = SymbolNameGenerator
        .getsym_EPCrossInjectionChannelRadius(schematic, nCross);
    Symbol sampleInitialSpread = SymbolNameGenerator
        .getsym_EPCrossSampleInitialSpread(schematic, nCross);
    Symbol baselineConcentration = SymbolNameGenerator
        .getsym_EPCrossBaselineConcentration(schematic, nCross);
        
    ArrayList<Symbol> injectionSeparationChannelAnalyteVelocity = 
        new ArrayList<Symbol>(numAnalytes);
    ArrayList<Symbol> injectionInjectionChannelAnalyteVelocity = 
        new ArrayList<Symbol>(numAnalytes);
    ArrayList<Symbol> analyteInitialSurfaceConcentration = 
        new ArrayList<Symbol>(numAnalytes);
    ArrayList<Symbol> analyteDiffusionCoefficient = 
        new ArrayList<Symbol>(numAnalytes);
    ArrayList<Symbol> analyteElectrophoreticMobility = 
        new ArrayList<Symbol>(numAnalytes);
    ArrayList<Symbol> peakTimeAnalyteConcentration = 
        new ArrayList<Symbol>(numAnalytes);
    ArrayList<Symbol> peakTimeAnalyteSpread = 
        new ArrayList<Symbol>(numAnalytes);
    ArrayList<Symbol> peakTime = new ArrayList<Symbol>(numAnalytes);
    ArrayList<Symbol> focusTime = new ArrayList<Symbol>(numAnalytes);
    ArrayList<Symbol> fadeTime = new ArrayList<Symbol>(numAnalytes);
    
    for(int i = 0; i < numAnalytes; ++i) {
        injectionSeparationChannelAnalyteVelocity[i] = SymbolNameGenerator
            .getsym_EPCrossInjectionSeparationChannelAnalyteVelocity(
                schematic, nCross, i);
        injectionInjectionChannelAnalyteVelocity[i] = SymbolNameGenerator
            .getsym_EPCrossInjectionInjectionChannelAnalyteVelocity(
                schematic, nCross, i);
        analyteInitialSurfaceConcentration[i] = SymbolNameGenerator
            .getsym_EPCrossAnalyteInitialSurfaceConcentration(
                schematic, nCross, i);
        analyteDiffusionCoefficient[i] = SymbolNameGenerator
            .getsym_EPCrossAnalyteDiffusionCoefficient(schematic, nCross, i);
        analyteElectrophoreticMobility[i] = SymbolNameGenerator
            .getsym_EPCrossAnalyteElectrophoreticMobility(
                schematic, nCross, i);
        peakTimeAnalyteConcentration[i] = SymbolNameGenerator
            .getsym_EPCrossPeakTimeAnalyteConcentration(
                schematic, nCross, i);
        peakTimeAnalyteSpread[i] = SymbolNameGenerator
            .getsym_EPCrossAnalyteElectrophoreticMobility(
                schematic, nCross, i);
        peakTime[i] = SymbolNameGenerator
            .getsym_EPCrossPeakTime(schematic, nCross, i);
        focusTime[i] = SymbolNameGenerator
            .getsym_EPCrossFocusTime(schematic, nCross, i);
        fadeTime[i] = SymbolNameGenerator
            .getsym_EPCrossFadeTime(schematic, nCross, i);
    }

    // declare variables
    exprs.add(QFNRA.declareRealVariable(lenSeparationChannel));
    exprs.add(QFNRA.declareRealVariable(lenTail));
    exprs.add(QFNRA.declareRealVariable(lenInjectionChannel));
    exprs.add(QFNRA.declareRealVariable(lenWasteChannel));
    exprs.add(QFNRA.declareRealVariable(lenCross));
    exprs.add(QFNRA.declareRealVariable(injectionSampleNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionWasteNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionCathodeNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionAnodeNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionIntersectionVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionSeparationChannelE));
    exprs.add(QFNRA.declareRealVariable(injectionInjectionChannelE));
    exprs.add(QFNRA.declareRealVariable(bulkMobility));
    exprs.add(QFNRA.declareRealVariable(bulkViscosity));
    exprs.add(QFNRA.declareRealVariable(separationDistance));
    exprs.add(QFNRA.declareRealVariable(injectionChannelRadius));
    exprs.add(QFNRA.declareRealVariable(sampleInitialSpread));
    exprs.add(QFNRA.declareRealVariable(baselineConcentration));
    
    for(int i = 0; i < numAnalytes; ++i) {
        exprs.add(QFNRA.declareRealVariable(
            injectionSeparationChannelAnalyteVelocity[i]));
        exprs.add(QFNRA.declareRealVariable(
            injectionInjectionChannelAnalyteVelocity[i]));
        exprs.add(QFNRA.declareRealVariable(
            analyteInitialSurfaceConcentration[i]));
        exprs.add(QFNRA.declareRealVariable(analyteDiffusionCoefficient[i]));
        exprs.add(QFNRA.declareRealVariable(
            analyteElectrophoreticMobility[i]));
        exprs.add(QFNRA.declareRealVariable(
            peakTimeAnalyteConcentration[i]));
        exprs.add(QFNRA.declareRealVariable(
            peakTimeAnalyteSpread[i]));
        exprs.add(QFNRA.declareRealVariable(peakTime[i]));
        exprs.add(QFNRA.declareRealVariable(focusTime[i]));
        exprs.add(QFNRA.declareRealVariable(fadeTime[i]));
    }

    // "constants"
    // Distances are in metres, times are in seconds, voltages are in volts, 
    // masses are in kilograms, and surface concentrations are in moles per 
    // metres squared.
    exprs.add(QFNRA.assertEqual(
        bulkViscosity, 
        new Decimal(0.001002)
    ));
    exprs.add(QFNRA.assertEqual(
        bulkMobility, 
        new Decimal(1e-8)
    ));
    exprs.add(QFNRA.assertEqual(
        analyteElectrophoreticMobility[0], 
        new Decimal(-3.70e-8) // N_bp = 100
    ));
    exprs.add(QFNRA.assertEqual(
        analyteElectrophoreticMobility[1], 
        new Decimal(-3.60e-8) // N_bp = 50
    ));
    exprs.add(QFNRA.assertEqual(
        analyteElectrophoreticMobility[2], 
        new Decimal(-3.75e-8) // N_bp = 1000
    ));
    exprs.add(QFNRA.assertEqual(
        analyteInitialSurfaceConcentration[0], 
        new Decimal(2.0) // N_bp = 100
    ));
    exprs.add(QFNRA.assertEqual(
        analyteInitialSurfaceConcentration[1], 
        new Decimal(1.0) // N_bp = 50
    ));
    exprs.add(QFNRA.assertEqual(
        analyteInitialSurfaceConcentration[2], 
        new Decimal(0.5) // N_bp = 1000
    ));
    exprs.add(QFNRA.assertEqual(
        analyteDiffusionCoefficient[0], 
        new Decimal(2.17e-11) // N_bp = 100
    ));
    exprs.add(QFNRA.assertEqual(
        analyteDiffusionCoefficient[1], 
        new Decimal(3.23e-11) // N_bp = 50
    ));
    exprs.add(QFNRA.assertEqual(
        analyteDiffusionCoefficient[2], 
        new Decimal(5.85e-12) // N_bp = 1000
    ));
    exprs.add(QFNRA.assertEqual(
        injectionCathodeNodeVoltage, 
        new Decimal(-1e2)
    ));
    exprs.add(QFNRA.assertEqual(
        injectionAnodeNodeVoltage, 
        new Decimal(0)
    ));
    exprs.add(QFNRA.assertEqual(
        lenSeparationChannel, 
        new Decimal(0.030)
    ));
    exprs.add(QFNRA.assertEqual(
        lenTail, 
        new Decimal(0.0045)
    ));
    exprs.add(QFNRA.assertEqual(
        lenInjectionChannel, 
        new Decimal(0.0045)
    ));
    exprs.add(QFNRA.assertEqual(
        lenWasteChannel, 
        new Decimal(0.0045)
    ));
    //exprs.add(QFNRA.assertEqual(
    //    separationDistance, 
    //    new Decimal(0.025)
    //));
    exprs.add(QFNRA.assertEqual(
        injectionChannelRadius, 
        new Decimal(5e-5)
    ));
    exprs.add(QFNRA.assertEqual(
        baselineConcentration, 
        new Decimal(0.01)
    ));

    // physical constraints
    exprs.add(QFNRA.assertEqual(
        lenCross, 
        QFNRA.add(
            lenSeparationChannel, 
            lenTail
        )
    ));
    exprs.add(QFNRA.assertGreater(
        separationDistance,
        new Numeral(0)
    ));
    exprs.add(QFNRA.assertGreater(
        lenSeparationChannel,
        separationDistance
    ));

    // separation resolution constraints
    exprs.add(QFNRA.assertEqual(
        injectionSeparationChannelE,
        QFNRA.divide(
            QFNRA.subtract(
                injectionCathodeNodeVoltage,
                injectionAnodeNodeVoltage
            ),
            lenCross
        )
    ));
    exprs.add(QFNRA.assertEqual(sampleInitialSpread, 
        QFNRA.divide(
            injectionChannelRadius, 
            new Decimal(2.355)
        )
    ));
    
    for(int i = 0; i < numAnalytes; ++i) {
        exprs.add(QFNRA.assertEqual(
            injectionSeparationChannelAnalyteVelocity[i],
            QFNRA.multiply(
                QFNRA.add(
                    bulkMobility,
                    analyteElectrophoreticMobility[i]
                ),
                injectionSeparationChannelE
            )
        ));
        exprs.add(QFNRA.assertEqual(
            peakTimeAnalyteSpread[i],
            QFNRA.add(
                sampleInitialSpread,
                QFNRA.sqrt(
                    QFNRA.multiply(
                        new Numeral(2),
                        QNFRA.multiply(
                            analyteDiffusionCoefficient[i],
                            peakTime[i]
                        )
                    )
                )
            )
        ));
        exprs.add(QFNRA.assertEqual(
            peakTimeAnalyteConcentration[i],
            QFNRA.divide(
                QFNRA.multiply(
                    analyteInitialSurfaceConcentration[i],
                    QFNRA.exp(
                        QFNRA.divide(
                            QFNRA.pow(
                                QFNRA.subtract(
                                    separationDistance,
                                    QNFRA.multiply(
                                        injectionSeparationChannelAnalyteVelocity[i],
                                        peakTime[i]
                                    )
                                ),
                                new Numeral(2)
                            ),
                            QFNRA.multiply(
                                new Numeral(-2),
                                QFNRA.pow(
                                    peakTimeAnalyteSpread[i],
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
                    peakTimeAnalyteSpread[i]
                )
            )
        ));
        exprs.add(QFNRA.assertEqual(
            new Numeral(0),
            QFNRA.multiply(
                QFNRA.divide(
                    QFNRA.multiply(
                        analyteInitialSurfaceConcentration[i],
                        QFNRA.exp(
                            QFNRA.divide(
                                QFNRA.pow(
                                    QFNRA.subtract(
                                        separationDistance,
                                        QNFRA.multiply(
                                            injectionSeparationChannelAnalyteVelocity[i],
                                            peakTime[i]
                                        )
                                    ),
                                    new Numeral(2)
                                ),
                                QFNRA.multiply(
                                    new Numeral(-2),
                                    QFNRA.pow(
                                        peakTimeAnalyteSpread[i],
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
                        QFNRA.pow(
                            peakTimeAnalyteSpread[i],
                            new Numeral(2)
                        )
                    )
                ),
                QFNRA.add(
                    QFNRA.divide(
                        QFNRA.multiply(
                            injectionSeparationChannelAnalyteVelocity[i],
                            QFNRA.subtract(
                                separationDistance,
                                QNFRA.multiply(
                                    injectionSeparationChannelAnalyteVelocity[i],
                                    peakTime[i]
                                )
                            )
                        ),
                        peakTimeAnalyteSpread[i]
                    ),
                    QFNRA.multiply(
                        QFNRA.subtract(
                            QFNRA.pow(
                                QFNRA.divide(
                                    QFNRA.subtract(
                                        separationDistance,
                                        QNFRA.multiply(
                                            injectionSeparationChannelAnalyteVelocity[i],
                                            peakTime[i]
                                        )
                                    ),
                                    peakTimeAnalyteSpread[i]
                                ),
                                new Numeral(2)
                            ),
                            new Numeral(1)
                        ),
                        QFNRA.sqrt(
                            QFNRA.divide(
                                analyteDiffusionCoefficient[i],
                                QFNRA.multiply(
                                    new Numeral(2),
                                    peakTime[i]
                                )
                            )
                        )
                    )
                )
            )
        ));
        exprs.add(QFNRA.assertEqual(
            baselineConcentration,
            QFNRA.divide(
                QFNRA.multiply(
                    analyteInitialSurfaceConcentration[i],
                    QFNRA.exp(
                        QFNRA.divide(
                            QFNRA.pow(
                                QFNRA.subtract(
                                    separationDistance,
                                    QNFRA.multiply(
                                        injectionSeparationChannelAnalyteVelocity[i],
                                        focusTime[i]
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
                                                QNFRA.multiply(
                                                    analyteDiffusionCoefficient[i],
                                                    focusTime[i]
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
                                QNFRA.multiply(
                                    analyteDiffusionCoefficient[i],
                                    focusTime[i]
                                )
                            )
                        )
                    )
                )
            )
        ));
        exprs.add(QFNRA.assertGreater(
            peakTime[i],
            focusTime[i]
        ));
        exprs.add(QFNRA.assertEqual(
            baselineConcentration,
            QFNRA.divide(
                QFNRA.multiply(
                    analyteInitialSurfaceConcentration[i],
                    QFNRA.exp(
                        QFNRA.divide(
                            QFNRA.pow(
                                QFNRA.subtract(
                                    separationDistance,
                                    QNFRA.multiply(
                                        injectionSeparationChannelAnalyteVelocity[i],
                                        fadeTime[i]
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
                                                QNFRA.multiply(
                                                    analyteDiffusionCoefficient[i],
                                                    fadeTime[i]
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
                                QNFRA.multiply(
                                    analyteDiffusionCoefficient[i],
                                    fadeTime[i]
                                )
                            )
                        )
                    )
                )
            )
        ));
        exprs.add(QFNRA.assertGreater(
            fadeTime[i],
            peakTime[i]
        ));
        exprs.add(QFNRA.assertGreater(
            QFNRA.divide(
                peakTimeAnalyteConcentration[i],
                baselineConcentration
            ),
            new Numeral(10)
        ));
    }
    
    for(int i = 0; i < numAnalytes; ++i) {
        for(int j = 0; j < numAnalytes; ++j) {
            if (i == j) {
                continue;
            }
            exprs.add(QFNRA.assertThat(
                QFNRA.or(
                    QFNRA.greater(
                        focusTime[i],
                        fadeTime[j]
                    ),
                    QFNRA.greater(
                        peakTime[j],
                        peakTime[i]
                    )
                )
            ));
        }
    }
    
    // pull-back voltage constraints
    exprs.add(QFNRA.assertEqual(
        injectionIntersectionVoltage,
        QFNRA.add(
            injectionAnodeNodeVoltage,
            QFNRA.multiply(
                QFNRA.subtract(
                    injectionCathodeNodeVoltage, 
                    injectionAnodeNodeVoltage
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
        injectionAnodeNodeVoltage
    ));
    exprs.add(QFNRA.assertGreater(
        injectionSampleNodeVoltage,
        injectionIntersectionVoltage
    ));
    exprs.add(QFNRA.assertEqual(
        injectionInjectionChannelE,
        QFNRA.divide(
            QFNRA.subtract(
                injectionIntersectionVoltage,
                injectionSampleNodeVoltage
            ),
            lenInjectionChannel
        )
    ));

    return exprs;
  }
}
