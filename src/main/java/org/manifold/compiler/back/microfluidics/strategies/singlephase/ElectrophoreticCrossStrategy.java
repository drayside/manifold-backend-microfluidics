package org.manifold.compiler.back.microfluidics.strategies.singlephase;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ArrayValue;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.IntegerValue;
import org.manifold.compiler.NodeTypeValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.RealValue;
import org.manifold.compiler.UndeclaredAttributeException;
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
      } catch (UndeclaredAttributeException e) {
        // TODO: replace with more informative error message
        throw new CodeGenerationError("undeclared attribute");
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
      NodeValue nCross) throws UndeclaredIdentifierException, 
      UndeclaredAttributeException {
    List<SExpression> exprs = new LinkedList<>();

    ArrayValue analyteElectrophoreticMobilityAttr = 
      (ArrayValue)nCross.getAttribute("analyteElectrophoreticMobility");
    ArrayValue analyteInitialSurfaceConcentrationAttr = 
      (ArrayValue)nCross.getAttribute("analyteInitialSurfaceConcentration");
    ArrayValue analyteDiffusionCoefficientAttr = 
      (ArrayValue)nCross.getAttribute("analyteDiffusionCoefficient");
    int numAnalytes = analyteElectrophoreticMobilityAttr.length();

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
    Symbol separationDistance = SymbolNameGenerator
      .getsym_EPCrossSeparationDistance(schematic, nCross);
    Symbol injectionChannelRadius = SymbolNameGenerator
      .getsym_EPCrossInjectionChannelRadius(schematic, nCross);
    Symbol sampleInitialSpread = SymbolNameGenerator
      .getsym_EPCrossSampleInitialSpread(schematic, nCross);
    Symbol startTime = SymbolNameGenerator
      .getsym_EPCrossStartTime(schematic, nCross);
    Symbol endTime = SymbolNameGenerator
      .getsym_EPCrossEndTime(schematic, nCross);
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
    
    for (int i = 0; i < numAnalytes; ++i) {
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
    for (int i = 0; i < numAnalytes - 1; ++i) {
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
    exprs.add(QFNRA.declareRealVariable(separationDistance));
    exprs.add(QFNRA.declareRealVariable(injectionChannelRadius));
    exprs.add(QFNRA.declareRealVariable(sampleInitialSpread));
    exprs.add(QFNRA.declareRealVariable(startTime));
    exprs.add(QFNRA.declareRealVariable(endTime));
    exprs.add(QFNRA.declareRealVariable(baselineConcentration));
    
    for (int i = 0; i < numAnalytes; ++i) {
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
    for (int i = 0; i < numAnalytes - 1; ++i) {
      exprs.add(QFNRA.declareRealVariable(fadeTimeConcentration[i]));
      exprs.add(QFNRA.declareRealVariable(fadeTime[i]));
    }

    // "constants"
    // Distances are in metres, times are in seconds, voltages are in volts, 
    // masses are in kilograms, and surface concentrations are in moles per 
    // metres squared.
    exprs.add(QFNRA.assertEqual(
      bulkMobility,
      new Decimal(((RealValue)nCross.getAttribute("bulkMobility")).toDouble())
    ));
    exprs.add(QFNRA.assertEqual(
      injectionCathodeNodeVoltage, 
      new Decimal(
        ((RealValue)nCross.getAttribute("injectionCathodeNodeVoltage"))
        .toDouble()
      )
    ));
    exprs.add(QFNRA.assertEqual(
      injectionAnodeNodeVoltage, 
      new Decimal(0)
    ));
    exprs.add(QFNRA.assertEqual(
      lenSeparationChannel, 
      new Decimal(
        ((RealValue)nCross.getAttribute("lenSeparationChannel")).toDouble()
      )
    ));
    exprs.add(QFNRA.assertEqual(
      lenTail, 
      new Decimal(
        ((RealValue)nCross.getAttribute("lenInjectionChannel")).toDouble()
      )
    ));
    exprs.add(QFNRA.assertEqual(
      lenInjectionChannel, 
      new Decimal(
        ((RealValue)nCross.getAttribute("lenInjectionChannel")).toDouble()
      )
    ));
    exprs.add(QFNRA.assertEqual(
      lenWasteChannel, 
      new Decimal(
        ((RealValue)nCross.getAttribute("lenInjectionChannel")).toDouble()
      )
    ));
    exprs.add(QFNRA.assertEqual(
      injectionChannelRadius, 
      new Decimal(((RealValue)nCross.getAttribute("channelRadius")).toDouble())
    ));
    exprs.add(QFNRA.assertEqual(
      baselineConcentration, 
      new Decimal(
        ((RealValue)nCross.getAttribute("baselineConcentration")).toDouble()
      )
    ));

    for (int i = 0; i < numAnalytes; ++i) {
      exprs.add(QFNRA.assertEqual(
        analyteElectrophoreticMobility[i], 
        new Decimal(
          ((RealValue)analyteElectrophoreticMobilityAttr.get(i)).toDouble()
        )
      ));
      exprs.add(QFNRA.assertEqual(
        analyteInitialSurfaceConcentration[i], 
        new Decimal(
          ((RealValue)analyteInitialSurfaceConcentrationAttr.get(i)).toDouble()
        )
      ));
      exprs.add(QFNRA.assertEqual(
        analyteDiffusionCoefficient[i], 
        new Decimal(
          ((RealValue)analyteDiffusionCoefficientAttr.get(i)).toDouble()
        )
      ));
    }

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
    for (int i = 0; i < numAnalytes; ++i) {
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
    if (numAnalytes > 1) {
      exprs.add(QFNRA.assertGreater(
        peakTime[0],
        startTime
      ));
      for (int i = 0; i < numAnalytes - 1; ++i) {
        exprs.add(QFNRA.assertGreater(
          fadeTime[i],
          QFNRA.add(
            peakTime[i],
            new Numeral(1)
          )
        ));
        exprs.add(QFNRA.assertGreater(
          peakTime[i + 1],
          QFNRA.add(
            fadeTime[i],
            new Numeral(1)
          )
        ));
      }
      exprs.add(QFNRA.assertGreater(
        endTime,
        peakTime[numAnalytes - 1]
      ));

      for (int i = 0; i < numAnalytes; ++i) {
        SExpression[] peakTimeAnalyteSpread = new SExpression[numAnalytes];
        SExpression[] peakTimeAnalyteConcentration = 
          new SExpression[numAnalytes];

        for (int j = 0; j < numAnalytes; ++j) {
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
              new Decimal(2.506628), // sqrt(2*pi)
              peakTimeAnalyteSpread[j]
            )
          );
        }
        SExpression peakTimeConcentrationExpr = QFNRA.add(
          peakTimeAnalyteConcentration[0],
          peakTimeAnalyteConcentration[1]
        );
        for (int j = 2; j < numAnalytes; ++j) {
          peakTimeConcentrationExpr = QFNRA.add(
            peakTimeConcentrationExpr,
            peakTimeAnalyteConcentration[j]
          );
        }
        exprs.add(QFNRA.assertEqual(
          peakTimeConcentration[i],
          peakTimeConcentrationExpr
        ));

        SExpression peakTimeAnalyteConcentrationDerivative = QFNRA.multiply(
          QFNRA.divide(
            peakTimeAnalyteConcentration[i],
            peakTimeAnalyteSpread[i]
          ),
          QFNRA.add(
            QFNRA.divide(
              QFNRA.multiply(
                injectionSeparationChannelAnalyteVelocity[i],
                QFNRA.subtract(
                  separationDistance,
                  QFNRA.multiply(
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
                      QFNRA.multiply(
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
        );

        exprs.add(QFNRA.assertEqual(
          peakTimeAnalyteConcentrationDerivative,
          new Numeral(0)
        ));
        exprs.add(QFNRA.assertGreaterEqual(
          QFNRA.divide(
            peakTimeAnalyteConcentration[i],
            baselineConcentration
          ),
          new Numeral(10)
        ));
        exprs.add(QFNRA.assertGreaterEqual(
          QFNRA.divide(
            peakTimeAnalyteConcentration[i],
            peakTimeConcentration[i]
          ),
          new Decimal(0.95)
        ));
        if (i == 0) {
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
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[i],
                          startTime
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
                                analyteDiffusionCoefficient[i],
                                startTime
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
                new Decimal(2.506628), // sqrt(2*pi)
                QFNRA.add(
                  sampleInitialSpread,
                  QFNRA.sqrt(
                    QFNRA.multiply(
                      new Numeral(2),
                      QFNRA.multiply(
                        analyteDiffusionCoefficient[i],
                        startTime
                      )
                    )
                  )
                )
              )
            )
          ));
        } else if (i == numAnalytes - 1) {
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
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[i],
                          endTime
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
                                analyteDiffusionCoefficient[i],
                                endTime
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
                new Decimal(2.506628), // sqrt(2*pi)
                QFNRA.add(
                  sampleInitialSpread,
                  QFNRA.sqrt(
                    QFNRA.multiply(
                      new Numeral(2),
                      QFNRA.multiply(
                        analyteDiffusionCoefficient[i],
                        endTime
                      )
                    )
                  )
                )
              )
            )
          ));
        }
      }
      for (int i = 0; i < numAnalytes - 1; ++i) {
        SExpression[] fadeTimeAnalyteSpread = new SExpression[numAnalytes];
        SExpression[] fadeTimeAnalyteConcentration = 
          new SExpression[numAnalytes];

        for (int j = 0; j < numAnalytes; ++j) {
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
              new Decimal(2.506628), // sqrt(2*pi)
              fadeTimeAnalyteSpread[j]
            )
          );
        }
        SExpression fadeTimeConcentrationExpr = QFNRA.add(
          fadeTimeAnalyteConcentration[0],
          fadeTimeAnalyteConcentration[1]
        );
        for (int j = 2; j < numAnalytes; ++j) {
          fadeTimeConcentrationExpr = QFNRA.add(
            fadeTimeConcentrationExpr,
            fadeTimeAnalyteConcentration[j]
          );
        }
        exprs.add(QFNRA.assertEqual(
          fadeTimeConcentration[i],
          fadeTimeConcentrationExpr
        ));

        exprs.add(QFNRA.assertEqual(
          fadeTimeAnalyteConcentration[i],
          fadeTimeAnalyteConcentration[i + 1]
        ));
        exprs.add(QFNRA.assertGreaterEqual(
          QFNRA.divide(
            QFNRA.multiply(
              new Numeral(2),
              fadeTimeAnalyteConcentration[i]
            ),
            /*QFNRA.add(
              fadeTimeAnalyteConcentration[i],
              fadeTimeAnalyteConcentration[i + 1]
            ),*/
            fadeTimeConcentration[i]
          ),
          new Decimal(0.95)
        ));
      }
     
      SExpression threshold = new Decimal(0.85);
      exprs.add(QFNRA.assertLessThanEqual(
        QFNRA.divide(
          fadeTimeConcentration[0],
          peakTimeConcentration[0]
        ),
        threshold
      ));
      for (int i = 1; i < numAnalytes - 1; ++i) {
        exprs.add(QFNRA.assertLessThanEqual(
          QFNRA.divide(
            fadeTimeConcentration[i - 1],
            peakTimeConcentration[i]
          ),
          threshold
        ));
        exprs.add(QFNRA.assertLessThanEqual(
          QFNRA.divide(
            fadeTimeConcentration[i],
            peakTimeConcentration[i]
          ),
          threshold
        ));
      }
      exprs.add(QFNRA.assertLessThanEqual(
        QFNRA.divide(
          fadeTimeConcentration[numAnalytes - 2],
          peakTimeConcentration[numAnalytes - 1]
        ),
        threshold
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
