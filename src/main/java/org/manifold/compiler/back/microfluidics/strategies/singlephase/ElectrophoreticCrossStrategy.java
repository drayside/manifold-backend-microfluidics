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
        
    Symbol[] injectionSeparationChannelAnalyteVelocity = 
      new Symbol[numAnalytes];
    Symbol[] injectionInjectionChannelAnalyteVelocity = new Symbol[numAnalytes];
    Symbol[] analyteInitialSurfaceConcentration = new Symbol[numAnalytes];
    Symbol[] analyteDiffusionCoefficient = new Symbol[numAnalytes];
    Symbol[] analyteElectrophoreticMobility = new Symbol[numAnalytes];
    Symbol[] peakTimeConcentration = new Symbol[numAnalytes];
    Symbol[] fadeTimeConcentration = new Symbol[numAnalytes - 1];
    Symbol[] peakTime = new Symbol[numAnalytes];
    Symbol[] fadeTime = new Symbol[numAnalytes - 1];
    
    for(int i = 0; i < numAnalytes; ++i) {
      injectionSeparationChannelAnalyteVelocity[i] = SymbolNameGenerator
        .getsym_EPCrossInjectionSeparationChannelAnalyteVelocity(
          schematic, nCross, i);
      injectionInjectionChannelAnalyteVelocity[i] = SymbolNameGenerator
        .getsym_EPCrossInjectionInjectionChannelAnalyteVelocity(
          schematic, nCross, i);
      analyteInitialSurfaceConcentration[i] = SymbolNameGenerator
        .getsym_EPCrossAnalyteInitialSurfaceConcentration(schematic, nCross, i);
      analyteDiffusionCoefficient[i] = SymbolNameGenerator
        .getsym_EPCrossAnalyteDiffusionCoefficient(schematic, nCross, i);
      analyteElectrophoreticMobility[i] = SymbolNameGenerator
        .getsym_EPCrossAnalyteElectrophoreticMobility(schematic, nCross, i);
      peakTimeConcentration[i] = SymbolNameGenerator
        .getsym_EPCrossPeakTimeConcentration(schematic, nCross, i);
      peakTime[i] = SymbolNameGenerator
        .getsym_EPCrossPeakTime(schematic, nCross, i);
    }
    for(int i = 0; i < numAnalytes - 1; ++i) {
      fadeTimeConcentration[i] = SymbolNameGenerator
        .getsym_EPCrossFadeTimeConcentration(schematic, nCross, i);
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
      exprs.add(QFNRA.declareRealVariable(analyteElectrophoreticMobility[i]));
      exprs.add(QFNRA.declareRealVariable(peakTimeConcentration[i]));
      exprs.add(QFNRA.declareRealVariable(peakTime[i]));
    }
    for(int i = 0; i < numAnalytes - 1; ++i) {
      exprs.add(QFNRA.declareRealVariable(fadeTimeConcentration[i]));
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
      new Decimal(-3.75e-8) // N_bp = 1000
    ));
    exprs.add(QFNRA.assertEqual(
      analyteElectrophoreticMobility[1], 
      new Decimal(-3.70e-8) // N_bp = 100
    ));
    exprs.add(QFNRA.assertEqual(
      analyteElectrophoreticMobility[2], 
      new Decimal(-3.60e-8) // N_bp = 50
    ));
    exprs.add(QFNRA.assertEqual(
      analyteInitialSurfaceConcentration[0], 
      new Decimal(2.66e-5) // N_bp = 1000
    ));
    exprs.add(QFNRA.assertEqual(
      analyteInitialSurfaceConcentration[1], 
      new Decimal(1.06e-4) // N_bp = 100
    ));
    exprs.add(QFNRA.assertEqual(
      analyteInitialSurfaceConcentration[2], 
      new Decimal(5.32e-5) // N_bp = 50
    ));
    exprs.add(QFNRA.assertEqual(
      analyteDiffusionCoefficient[0], 
      new Decimal(5.85e-12) // N_bp = 1000
    ));
    exprs.add(QFNRA.assertEqual(
      analyteDiffusionCoefficient[1], 
      new Decimal(2.17e-11) // N_bp = 100
    ));
    exprs.add(QFNRA.assertEqual(
      analyteDiffusionCoefficient[2], 
      new Decimal(3.23e-11) // N_bp = 50
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
    }
 
    // if sample only contains one analyte, then separation is not a concern
    if(numAnalytes > 1) {
      for(int i = 0; i < numAnalytes - 1; ++i) {
        exprs.add(QFNRA.assertGreater(
          peakTime[i + 1],
          peakTime[i]
        ));
        exprs.add(QFNRA.assertGreater(
          fadeTime[i],
          peakTime[i]
        ));
        exprs.add(QFNRA.assertGreater(
          peakTime[i + 1],
          fadeTime[i]
        ));
      }

      for(int i = 0; i < numAnalytes; ++i) {
        SExpression[] peakTimeAnalyteSpread = new SExpression[numAnalytes];
        SExpression[] peakTimeAnalyteConcentration = 
          new SExpression[numAnalytes];
        SExpression[] peakTimeAnalyteConcentrationDerivative = 
          new SExpression[numAnalytes];
        SExpression[] peakTimeAnalyteConcentrationSecondDerivative = 
          new SExpression[numAnalytes];

        for(int j = 0; j < numAnalytes; ++j) {
          peakTimeAnalyteSpread[j] = QFNRA.add(
            sampleInitialSpread,
            QFNRA.sqrt(
              QFNRA.multiply(
                new Numeral(2),
                QFNRA.multiply(
                  analyteDiffusionCoefficient[j],
                  peakTime[i]
                )
              )
            )
          );
          peakTimeAnalyteConcentration[j] = QFNRA.divide(
            QFNRA.multiply(
              analyteInitialSurfaceConcentration[j],
              QFNRA.exp(
                QFNRA.divide(
                  QFNRA.pow(
                    QFNRA.subtract(
                      separationDistance,
                      QFNRA.multiply(
                        injectionSeparationChannelAnalyteVelocity[j],
                        peakTime[i]
                      )
                    ),
                    new Numeral(2)
                  ),
                  QFNRA.multiply(
                    new Numeral(-2),
                    QFNRA.pow(
                      peakTimeAnalyteSpread[j],
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
              peakTimeAnalyteSpread[j]
            )
          );
          peakTimeAnalyteConcentrationDerivative[j] = QFNRA.multiply(
            QFNRA.divide(
              peakTimeAnalyteConcentration[j],
              peakTimeAnalyteSpread[j]
            ),
            QFNRA.add(
              QFNRA.divide(
                QFNRA.multiply(
                  injectionSeparationChannelAnalyteVelocity[j],
                  QFNRA.subtract(
                    separationDistance,
                    QFNRA.multiply(
                      injectionSeparationChannelAnalyteVelocity[j],
                      peakTime[i]
                    )
                  )
                ),
                peakTimeAnalyteSpread[j]
              ),
              QFNRA.multiply(
                QFNRA.subtract(
                  QFNRA.pow(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[j],
                          peakTime[i]
                        )
                      ),
                      peakTimeAnalyteSpread[j]
                    ),
                    new Numeral(2)
                  ),
                  new Numeral(1)
                ),
                QFNRA.sqrt(
                  QFNRA.divide(
                    analyteDiffusionCoefficient[j],
                    QFNRA.multiply(
                      new Numeral(2),
                      peakTime[i]
                    )
                  )
                )
              )
            )
          );
          peakTimeAnalyteConcentrationSecondDerivative[j] = QFNRA.multiply(
            QFNRA.divide(
              peakTimeAnalyteConcentration[j],
              QFNRA.pow(
                peakTimeAnalyteSpread[j],
                new Numeral(2)
              )
            ),
            QFNRA.subtract(
              QFNRA.add(
                QFNRA.add(
                  QFNRA.multiply(
                    QFNRA.subtract(
                      peakTimeAnalyteSpread[j],
                      QFNRA.divide(
                        QFNRA.pow(
                          QFNRA.subtract(
                            separationDistance,
                            QFNRA.multiply(
                              injectionSeparationChannelAnalyteVelocity[j],
                              peakTime[i]
                            )
                          ),
                          new Numeral(2)
                        ),
                        peakTimeAnalyteSpread[j]
                      )
                    ),
                    QFNRA.sqrt(
                      QFNRA.divide(
                        analyteDiffusionCoefficient[j],
                        QFNRA.multiply(
                          new Numeral(8),
                          QFNRA.pow(
                            peakTime[i],
                            new Numeral(3)
                          )
                        )
                      )
                    )
                  ),
                  QFNRA.multiply(
                    QFNRA.divide(
                      analyteDiffusionCoefficient[j],
                      peakTime[i]
                    ),
                    QFNRA.subtract(
                      new Numeral(1),
                      QFNRA.multiply(
                        new Decimal(2.5),
                        QFNRA.pow(
                          QFNRA.divide(
                            QFNRA.subtract(
                              separationDistance,
                              QFNRA.multiply(
                                injectionSeparationChannelAnalyteVelocity[j],
                                peakTime[i]
                              )
                            ),
                            peakTimeAnalyteSpread[j]
                          ),
                          new Numeral(2)
                        )
                      )
                    )
                  )
                ),
                QFNRA.pow(
                  QFNRA.add(
                    QFNRA.multiply(
                      QFNRA.sqrt(
                        QFNRA.divide(
                          analyteDiffusionCoefficient[j],
                          QFNRA.multiply(
                            new Numeral(2),
                            peakTime[i]
                          )
                        )
                      ),
                      QFNRA.pow(
                        QFNRA.divide(
                          QFNRA.subtract(
                            separationDistance,
                            QFNRA.multiply(
                              injectionSeparationChannelAnalyteVelocity[j],
                              peakTime[i]
                            )
                          ),
                          peakTimeAnalyteSpread[j]
                        ),
                        new Numeral(2)
                      )
                    ),
                    QFNRA.divide(
                      QFNRA.multiply(
                        injectionSeparationChannelAnalyteVelocity[j],
                        QFNRA.subtract(
                          separationDistance,
                          QFNRA.multiply(
                            injectionSeparationChannelAnalyteVelocity[j],
                            peakTime[i]
                          )
                        )
                      ),
                      peakTimeAnalyteSpread[j]
                    )
                  ),
                  new Numeral(2)
                )
              ),
              QFNRA.multiply(
                injectionSeparationChannelAnalyteVelocity[j],
                QFNRA.add(
                  injectionSeparationChannelAnalyteVelocity[j],
                  QFNRA.multiply(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[j],
                          peakTime[i]
                        )
                      ),
                      peakTimeAnalyteSpread[j]
                    ),
                    QFNRA.sqrt(
                      QFNRA.divide(
                        QFNRA.multiply(
                          new Numeral(18),
                          analyteDiffusionCoefficient[j]
                        ),
                        peakTime[i]
                      )
                    )
                  )
                )
              )
            )
          );
        }

        SExpression peakTimeConcentrationExpr = QFNRA.add(
          peakTimeAnalyteConcentration[0],
          peakTimeAnalyteConcentration[1]
        );
        SExpression peakTimeConcentrationDerivative = QFNRA.add(
          peakTimeAnalyteConcentrationDerivative[0],
          peakTimeAnalyteConcentrationDerivative[1]
        );
        SExpression peakTimeConcentrationSecondDerivative = QFNRA.add(
          peakTimeAnalyteConcentrationSecondDerivative[0],
          peakTimeAnalyteConcentrationSecondDerivative[1]
        );
        for(int j = 2; j < numAnalytes; ++j) {
          peakTimeConcentrationExpr = QFNRA.add(
            peakTimeConcentrationExpr,
            peakTimeAnalyteConcentration[j]
          );
          peakTimeConcentrationDerivative = QFNRA.add(
            peakTimeConcentrationDerivative,
            peakTimeAnalyteConcentrationDerivative[j]
          );
          peakTimeConcentrationSecondDerivative = QFNRA.add(
            peakTimeConcentrationSecondDerivative,
            peakTimeAnalyteConcentrationSecondDerivative[j]
          );
        }
        exprs.add(QFNRA.assertEqual(
          peakTimeConcentration[i],
          peakTimeConcentrationExpr
        ));
        exprs.add(QFNRA.assertEqual(
          peakTimeConcentrationDerivative,
          new Numeral(0)
        ));
        exprs.add(QFNRA.assertLessThan(
          peakTimeConcentrationSecondDerivative,
          new Numeral(0)
        ));
        exprs.add(QFNRA.assertGreater(
          QFNRA.divide(
            peakTimeAnalyteConcentration[i],
            peakTimeConcentration[i]
          ),
          new Decimal(0.9)
        ));
      }
      for(int i = 0; i < numAnalytes - 1; ++i) {
        SExpression[] fadeTimeAnalyteSpread = new SExpression[numAnalytes];
        SExpression[] fadeTimeAnalyteConcentration = 
          new SExpression[numAnalytes];
        SExpression[] fadeTimeAnalyteConcentrationDerivative = 
          new SExpression[numAnalytes];
        SExpression[] fadeTimeAnalyteConcentrationSecondDerivative = 
          new SExpression[numAnalytes];

        for(int j = 0; j < numAnalytes; ++j) {
          fadeTimeAnalyteSpread[j] = QFNRA.add(
            sampleInitialSpread,
            QFNRA.sqrt(
              QFNRA.multiply(
                new Numeral(2),
                QFNRA.multiply(
                  analyteDiffusionCoefficient[j],
                  fadeTime[i]
                )
              )
            )
          );
          fadeTimeAnalyteConcentration[j] = QFNRA.divide(
            QFNRA.multiply(
              analyteInitialSurfaceConcentration[j],
              QFNRA.exp(
                QFNRA.divide(
                  QFNRA.pow(
                    QFNRA.subtract(
                      separationDistance,
                      QFNRA.multiply(
                        injectionSeparationChannelAnalyteVelocity[j],
                        fadeTime[i]
                      )
                    ),
                    new Numeral(2)
                  ),
                  QFNRA.multiply(
                    new Numeral(-2),
                    QFNRA.pow(
                      fadeTimeAnalyteSpread[j],
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
              fadeTimeAnalyteSpread[j]
            )
          );
          fadeTimeAnalyteConcentrationDerivative[j] = QFNRA.multiply(
            QFNRA.divide(
              fadeTimeAnalyteConcentration[j],
              fadeTimeAnalyteSpread[j]
            ),
            QFNRA.add(
              QFNRA.divide(
                QFNRA.multiply(
                  injectionSeparationChannelAnalyteVelocity[j],
                  QFNRA.subtract(
                    separationDistance,
                    QFNRA.multiply(
                      injectionSeparationChannelAnalyteVelocity[j],
                      fadeTime[i]
                    )
                  )
                ),
                fadeTimeAnalyteSpread[j]
              ),
              QFNRA.multiply(
                QFNRA.subtract(
                  QFNRA.pow(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[j],
                          fadeTime[i]
                        )
                      ),
                      fadeTimeAnalyteSpread[j]
                    ),
                    new Numeral(2)
                  ),
                  new Numeral(1)
                ),
                QFNRA.sqrt(
                  QFNRA.divide(
                    analyteDiffusionCoefficient[j],
                    QFNRA.multiply(
                      new Numeral(2),
                      fadeTime[i]
                    )
                  )
                )
              )
            )
          );
          fadeTimeAnalyteConcentrationSecondDerivative[j] = QFNRA.multiply(
            QFNRA.divide(
              fadeTimeAnalyteConcentration[j],
              QFNRA.pow(
                fadeTimeAnalyteSpread[j],
                new Numeral(2)
              )
            ),
            QFNRA.subtract(
              QFNRA.add(
                QFNRA.add(
                  QFNRA.multiply(
                    QFNRA.subtract(
                      fadeTimeAnalyteSpread[j],
                      QFNRA.divide(
                        QFNRA.pow(
                          QFNRA.subtract(
                            separationDistance,
                            QFNRA.multiply(
                              injectionSeparationChannelAnalyteVelocity[j],
                              fadeTime[i]
                            )
                          ),
                          new Numeral(2)
                        ),
                        fadeTimeAnalyteSpread[j]
                      )
                    ),
                    QFNRA.sqrt(
                      QFNRA.divide(
                        analyteDiffusionCoefficient[j],
                        QFNRA.multiply(
                          new Numeral(8),
                          QFNRA.pow(
                            fadeTime[i],
                            new Numeral(3)
                          )
                        )
                      )
                    )
                  ),
                  QFNRA.multiply(
                    QFNRA.divide(
                      analyteDiffusionCoefficient[j],
                      fadeTime[i]
                    ),
                    QFNRA.subtract(
                      new Numeral(1),
                      QFNRA.multiply(
                        new Decimal(2.5),
                        QFNRA.pow(
                          QFNRA.divide(
                            QFNRA.subtract(
                              separationDistance,
                              QFNRA.multiply(
                                injectionSeparationChannelAnalyteVelocity[j],
                                fadeTime[i]
                              )
                            ),
                            fadeTimeAnalyteSpread[j]
                          ),
                          new Numeral(2)
                        )
                      )
                    )
                  )
                ),
                QFNRA.pow(
                  QFNRA.add(
                    QFNRA.multiply(
                      QFNRA.sqrt(
                        QFNRA.divide(
                          analyteDiffusionCoefficient[j],
                          QFNRA.multiply(
                            new Numeral(2),
                            fadeTime[i]
                          )
                        )
                      ),
                      QFNRA.pow(
                        QFNRA.divide(
                          QFNRA.subtract(
                            separationDistance,
                            QFNRA.multiply(
                              injectionSeparationChannelAnalyteVelocity[j],
                              fadeTime[i]
                            )
                          ),
                          fadeTimeAnalyteSpread[j]
                        ),
                        new Numeral(2)
                      )
                    ),
                    QFNRA.divide(
                      QFNRA.multiply(
                        injectionSeparationChannelAnalyteVelocity[j],
                        QFNRA.subtract(
                          separationDistance,
                          QFNRA.multiply(
                            injectionSeparationChannelAnalyteVelocity[j],
                            fadeTime[i]
                          )
                        )
                      ),
                      fadeTimeAnalyteSpread[j]
                    )
                  ),
                  new Numeral(2)
                )
              ),
              QFNRA.multiply(
                injectionSeparationChannelAnalyteVelocity[j],
                QFNRA.add(
                  injectionSeparationChannelAnalyteVelocity[j],
                  QFNRA.multiply(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[j],
                          fadeTime[i]
                        )
                      ),
                      fadeTimeAnalyteSpread[j]
                    ),
                    QFNRA.sqrt(
                      QFNRA.divide(
                        QFNRA.multiply(
                          new Numeral(18),
                          analyteDiffusionCoefficient[j]
                        ),
                        fadeTime[i]
                      )
                    )
                  )
                )
              )
            )
          );
        }

        SExpression fadeTimeConcentrationExpr = QFNRA.add(
          fadeTimeAnalyteConcentration[0],
          fadeTimeAnalyteConcentration[1]
        );
        SExpression fadeTimeConcentrationDerivative = QFNRA.add(
          fadeTimeAnalyteConcentrationDerivative[0],
          fadeTimeAnalyteConcentrationDerivative[1]
        );
        SExpression fadeTimeConcentrationSecondDerivative = QFNRA.add(
          fadeTimeAnalyteConcentrationSecondDerivative[0],
          fadeTimeAnalyteConcentrationSecondDerivative[1]
        );
        for(int j = 2; j < numAnalytes; ++j) {
          fadeTimeConcentrationExpr = QFNRA.add(
            fadeTimeConcentrationExpr,
            fadeTimeAnalyteConcentration[j]
          );
          fadeTimeConcentrationDerivative = QFNRA.add(
            fadeTimeConcentrationDerivative,
            fadeTimeAnalyteConcentrationDerivative[j]
          );
          fadeTimeConcentrationSecondDerivative = QFNRA.add(
            fadeTimeConcentrationSecondDerivative,
            fadeTimeAnalyteConcentrationSecondDerivative[j]
          );
        }
        exprs.add(QFNRA.assertEqual(
          fadeTimeConcentration[i],
          fadeTimeConcentrationExpr
        ));
        exprs.add(QFNRA.assertEqual(
          fadeTimeConcentrationDerivative,
          new Numeral(0)
        ));
        exprs.add(QFNRA.assertGreater(
          fadeTimeConcentrationSecondDerivative,
          new Numeral(0)
        ));
      }
      
      exprs.add(QFNRA.assertLessThan(
        QFNRA.divide(
          fadeTimeConcentration[0],
          peakTimeConcentration[0]
        ),
        new Decimal(0.9)
      ));
      for(int i = 1; i < numAnalytes - 1; ++i) {
        exprs.add(QFNRA.assertLessThan(
          QFNRA.divide(
            fadeTimeConcentration[i - 1],
            peakTimeConcentration[i]
          ),
          new Decimal(0.9)
        ));
        exprs.add(QFNRA.assertLessThan(
          QFNRA.divide(
            fadeTimeConcentration[i],
            peakTimeConcentration[i]
          ),
          new Decimal(0.9)
        ));
      }
      exprs.add(QFNRA.assertLessThan(
        QFNRA.divide(
          fadeTimeConcentration[numAnalytes - 2],
          peakTimeConcentration[numAnalytes - 1]
        ),
        new Decimal(0.9)
      ));
    }
    
    // pull-back voltage constraints
    /*exprs.add(QFNRA.assertEqual(
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
    ));*/

    return exprs;
  }
}
