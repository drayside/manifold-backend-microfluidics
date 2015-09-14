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
        //exprs.addAll(translateElectrophoreticCrossApproximate(schematic, node));
        exprs.addAll(translateElectrophoreticCrossConstrained(schematic, node)); 
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

  private List<SExpression> translateElectrophoreticCrossConstrained(
      Schematic schematic, NodeValue nCross) throws 
      UndeclaredIdentifierException, UndeclaredAttributeException {
    List<SExpression> exprs = new LinkedList<>();

    ArrayValue analyteElectrophoreticMobilityAttr = 
      (ArrayValue) nCross.getAttribute("analyteElectrophoreticMobility");
    ArrayValue analyteInitialSurfaceConcentrationAttr = 
      (ArrayValue) nCross.getAttribute("analyteInitialSurfaceConcentration");
    ArrayValue analyteDiffusionCoefficientAttr = 
      (ArrayValue) nCross.getAttribute("analyteDiffusionCoefficient");
    int numAnalytes = analyteElectrophoreticMobilityAttr.length();

    Symbol lenSeparationChannel = SymbolNameGenerator
      .getsym_EPCrossSeparationChannelLength(schematic, nCross);
    Symbol lenTail = SymbolNameGenerator
      .getsym_EPCrossTailChannelLength(schematic, nCross);
    Symbol lenCross = SymbolNameGenerator
      .getsym_EPCrossLength(schematic, nCross);
    Symbol injectionCathodeNodeVoltage = SymbolNameGenerator
      .getsym_EPCrossInjectionCathodeNodeVoltage(schematic, nCross);
    Symbol injectionAnodeNodeVoltage = SymbolNameGenerator
      .getsym_EPCrossInjectionAnodeNodeVoltage(schematic, nCross);
    Symbol injectionSeparationChannelE = SymbolNameGenerator
      .getsym_EPCrossInjectionSeparationChannelE(schematic, nCross);
    Symbol bulkMobility = SymbolNameGenerator
      .getsym_EPCrossBulkMobility(schematic, nCross);
    Symbol separationDistance = SymbolNameGenerator
      .getsym_EPCrossSeparationDistance(schematic, nCross);
    Symbol injectionChannelRadius = SymbolNameGenerator
      .getsym_EPCrossInjectionChannelRadius(schematic, nCross);
    Symbol sampleInitialSpread = SymbolNameGenerator
      .getsym_EPCrossSampleInitialSpread(schematic, nCross);
    Symbol detectableConcentration = SymbolNameGenerator
      .getsym_EPCrossDetectableConcentration(schematic, nCross);
    Symbol negligibleConcentration = SymbolNameGenerator
      .getsym_EPCrossNegligibleConcentration(schematic, nCross);
        
    Symbol[] injectionSeparationChannelAnalyteVelocity = 
      new Symbol[numAnalytes];
    Symbol[] injectionInjectionChannelAnalyteVelocity = new Symbol[numAnalytes];
    Symbol[] analyteInitialSurfaceConcentration = new Symbol[numAnalytes];
    Symbol[] analyteDiffusionCoefficient = new Symbol[numAnalytes];
    Symbol[] analyteElectrophoreticMobility = new Symbol[numAnalytes];
    Symbol[] maxTimeConcentration = new Symbol[numAnalytes];
    Symbol[] minTimeConcentration = new Symbol[numAnalytes - 1];
    Symbol[] startTime = new Symbol[numAnalytes];
    Symbol[] focusTime = new Symbol[numAnalytes];
    Symbol[] maxTime = new Symbol[numAnalytes];
    Symbol[] fadeTime = new Symbol[numAnalytes];
    Symbol[] endTime = new Symbol[numAnalytes];
    Symbol[] minTime = new Symbol[numAnalytes - 1];
    
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
      maxTimeConcentration[i] = SymbolNameGenerator
        .getsym_EPCrossMaxTimeConcentration(schematic, nCross, i);
      startTime[i] = SymbolNameGenerator
        .getsym_EPCrossStartTime(schematic, nCross, i);
      focusTime[i] = SymbolNameGenerator
        .getsym_EPCrossFocusTime(schematic, nCross, i);
      maxTime[i] = SymbolNameGenerator
        .getsym_EPCrossMaxTime(schematic, nCross, i);
      fadeTime[i] = SymbolNameGenerator
        .getsym_EPCrossFadeTime(schematic, nCross, i);
      endTime[i] = SymbolNameGenerator
        .getsym_EPCrossEndTime(schematic, nCross, i);
    }
    for (int i = 0; i < numAnalytes - 1; ++i) {
      minTimeConcentration[i] = SymbolNameGenerator
        .getsym_EPCrossMinTimeConcentration(schematic, nCross, i);
      minTime[i] = SymbolNameGenerator
        .getsym_EPCrossMinTime(schematic, nCross, i);
    }

    // declare variables
    exprs.add(QFNRA.declareRealVariable(lenSeparationChannel));
    exprs.add(QFNRA.declareRealVariable(lenTail));
    exprs.add(QFNRA.declareRealVariable(lenCross));
    exprs.add(QFNRA.declareRealVariable(injectionCathodeNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionAnodeNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionSeparationChannelE));
    exprs.add(QFNRA.declareRealVariable(bulkMobility));
    exprs.add(QFNRA.declareRealVariable(separationDistance));
    exprs.add(QFNRA.declareRealVariable(injectionChannelRadius));
    exprs.add(QFNRA.declareRealVariable(sampleInitialSpread));
    exprs.add(QFNRA.declareRealVariable(detectableConcentration));
    exprs.add(QFNRA.declareRealVariable(negligibleConcentration));
    
    for (int i = 0; i < numAnalytes; ++i) {
      exprs.add(QFNRA.declareRealVariable(
        injectionSeparationChannelAnalyteVelocity[i]));
      exprs.add(QFNRA.declareRealVariable(
        injectionInjectionChannelAnalyteVelocity[i]));
      exprs.add(QFNRA.declareRealVariable(
        analyteInitialSurfaceConcentration[i]));
      exprs.add(QFNRA.declareRealVariable(analyteDiffusionCoefficient[i]));
      exprs.add(QFNRA.declareRealVariable(analyteElectrophoreticMobility[i]));
      exprs.add(QFNRA.declareRealVariable(maxTimeConcentration[i]));
      exprs.add(QFNRA.declareRealVariable(startTime[i]));
      exprs.add(QFNRA.declareRealVariable(focusTime[i]));
      exprs.add(QFNRA.declareRealVariable(maxTime[i]));
      exprs.add(QFNRA.declareRealVariable(fadeTime[i]));
      exprs.add(QFNRA.declareRealVariable(endTime[i]));
    }
    for (int i = 0; i < numAnalytes - 1; ++i) {
      exprs.add(QFNRA.declareRealVariable(minTimeConcentration[i]));
      exprs.add(QFNRA.declareRealVariable(minTime[i]));
    }

    // "constants"
    // Distances are in metres, times are in seconds, voltages are in volts, 
    // masses are in kilograms, and surface concentrations are in moles per 
    // metres squared.
    exprs.add(QFNRA.assertEqual(
      bulkMobility,
      new Decimal(((RealValue) nCross.getAttribute("bulkMobility")).toDouble())
    ));
    exprs.add(QFNRA.assertEqual(
      injectionCathodeNodeVoltage, 
      new Decimal(
        ((RealValue) nCross.getAttribute("injectionCathodeNodeVoltage"))
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
        ((RealValue) nCross.getAttribute("lenSeparationChannel")).toDouble()
      )
    ));
    exprs.add(QFNRA.assertEqual(
      lenTail, 
      new Decimal(
        ((RealValue) nCross.getAttribute("lenInjectionChannel")).toDouble()
      )
    ));
    exprs.add(QFNRA.assertEqual(
      injectionChannelRadius, 
      new Decimal(((RealValue) nCross.getAttribute("channelRadius")).toDouble())
    ));
    exprs.add(QFNRA.assertEqual(
      detectableConcentration, 
      new Decimal(
        ((RealValue) nCross.getAttribute("detectableConcentration")).toDouble()
      )
    ));

    for (int i = 0; i < numAnalytes; ++i) {
      exprs.add(QFNRA.assertEqual(
        analyteElectrophoreticMobility[i], 
        new Decimal(
          ((RealValue) analyteElectrophoreticMobilityAttr.get(i)).toDouble()
        )
      ));
      exprs.add(QFNRA.assertEqual(
        analyteInitialSurfaceConcentration[i], 
        new Decimal(
          ((RealValue) analyteInitialSurfaceConcentrationAttr.get(i)).toDouble()
        )
      ));
      exprs.add(QFNRA.assertEqual(
        analyteDiffusionCoefficient[i], 
        new Decimal(
          ((RealValue) analyteDiffusionCoefficientAttr.get(i)).toDouble()
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
        new Decimal(1.17741)
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
      // for analytes to be considered sufficiently separated, we require that 
      // adjacent analyte peaks and minimums be at least one second apart in 
      // the output electropherogram
      for (int i = 0; i < numAnalytes - 1; ++i) {
        exprs.add(QFNRA.assertGreater(
          minTime[i],
          QFNRA.add(
            maxTime[i],
            new Numeral(1)
          )
        ));
        exprs.add(QFNRA.assertGreater(
          maxTime[i + 1],
          QFNRA.add(
            minTime[i],
            new Numeral(1)
          )
        ));
      }

      if (numAnalytes > 3) {
        for (int i = 0; i < numAnalytes - 1; ++i) {
          exprs.add(QFNRA.assertGreater(
            startTime[i + 1],
            startTime[i]
          ));
          exprs.add(QFNRA.assertGreater(
            endTime[i + 1],
            endTime[i]
          ));
        }
      }
      for (int i = 0; i < numAnalytes; ++i) {
        if (numAnalytes > 3 || numAnalytes == 3 && i != 1) {
          exprs.add(QFNRA.assertGreater(
            endTime[i],
            fadeTime[i]
          ));
        }
        exprs.add(QFNRA.assertGreater(
          fadeTime[i],
          maxTime[i]
        ));
        exprs.add(QFNRA.assertGreater(
          maxTime[i],
          focusTime[i]
        ));
        if (numAnalytes > 3 || numAnalytes == 3 && i != 1) {
          exprs.add(QFNRA.assertGreater(
            focusTime[i],
            startTime[i]
          ));
        }

        // we also prohibit non-adjacent peaks (Gaussian curves) from 
        // overlapping in any "significant" manner
        if (i - 1 >= 0) {
          exprs.add(QFNRA.assertGreater(
            maxTime[i],
            fadeTime[i - 1]
          ));
        }
        if (i - 2 >= 0) {
          exprs.add(QFNRA.assertGreater(
            maxTime[i],
            endTime[i - 2]
          ));
        }
        if (i + 1 < numAnalytes) {
          exprs.add(QFNRA.assertGreater(
            focusTime[i + 1],
            maxTime[i]
          ));
        }
        if (i + 2 < numAnalytes) {
          exprs.add(QFNRA.assertGreater(
            startTime[i + 2],
            maxTime[i]
          ));
        }
      }
      for (int i = 0; i < numAnalytes - 1; ++i) {
        if (i + 2 < numAnalytes) {
          exprs.add(QFNRA.assertGreater(
            startTime[i + 2],
            minTime[i]
          ));
        }
        if (i - 1 >= 0) {
          exprs.add(QFNRA.assertGreater(
            minTime[i],
            endTime[i - 1]
          ));
        }
      }

      // requried to ensure that electropherogram is initially increasing
      SExpression initialSurfaceConcentrationExpr = QFNRA.add( 
        analyteInitialSurfaceConcentration[0],
        analyteInitialSurfaceConcentration[1]
      );
      for (int j = 2; j < numAnalytes; ++j) {
        initialSurfaceConcentrationExpr = QFNRA.add(
          initialSurfaceConcentrationExpr,
          analyteInitialSurfaceConcentration[j]
        );
      }
      exprs.add(QFNRA.assertLessThan(
        QFNRA.divide(
          QFNRA.multiply(
            initialSurfaceConcentrationExpr,
            QFNRA.exp(
              QFNRA.divide(
                QFNRA.pow(
                  QFNRA.divide(
                    separationDistance,
                    sampleInitialSpread
                  ),
                  new Numeral(2)
                ),
                new Numeral(-2)
              )
            )
          ),
          QFNRA.multiply(
            new Decimal(2.506628), // sqrt(2*pi)
            sampleInitialSpread
          )
        ),
        detectableConcentration
      ));

      for (int i = 0; i < numAnalytes; ++i) {
        SExpression startTimeAnalyteSpread = QFNRA.add(
          sampleInitialSpread,
          QFNRA.sqrt(
            QFNRA.multiply(
              new Numeral(2),
              QFNRA.multiply(
                analyteDiffusionCoefficient[i],
                startTime[i]
              )
            )
          )
        );
        SExpression startTimeAnalyteConcentration = QFNRA.divide(
          QFNRA.multiply(
            analyteInitialSurfaceConcentration[i],
            QFNRA.exp(
              QFNRA.divide(
                QFNRA.pow(
                  QFNRA.divide(
                    QFNRA.subtract(
                      separationDistance,
                      QFNRA.multiply(
                        injectionSeparationChannelAnalyteVelocity[i],
                        startTime[i]
                      )
                    ),
                    startTimeAnalyteSpread
                  ),
                  new Numeral(2)
                ),
                new Numeral(-2)
              )
            )
          ),
          QFNRA.multiply(
            new Decimal(2.506628), // sqrt(2*pi)
            startTimeAnalyteSpread
          )
        );
        SExpression focusTimeAnalyteSpread = QFNRA.add(
          sampleInitialSpread,
          QFNRA.sqrt(
            QFNRA.multiply(
              new Numeral(2),
              QFNRA.multiply(
                analyteDiffusionCoefficient[i],
                focusTime[i]
              )
            )
          )
        );
        SExpression focusTimeAnalyteConcentration = QFNRA.divide(
          QFNRA.multiply(
            analyteInitialSurfaceConcentration[i],
            QFNRA.exp(
              QFNRA.divide(
                QFNRA.pow(
                  QFNRA.divide(
                    QFNRA.subtract(
                      separationDistance,
                      QFNRA.multiply(
                        injectionSeparationChannelAnalyteVelocity[i],
                        focusTime[i]
                      )
                    ),
                    focusTimeAnalyteSpread
                  ),
                  new Numeral(2)
                ),
                new Numeral(-2)
              )
            )
          ),
          QFNRA.multiply(
            new Decimal(2.506628), // sqrt(2*pi)
            focusTimeAnalyteSpread
          )
        );
        SExpression maxTimeAnalyteSpread = QFNRA.add(
          sampleInitialSpread,
          QFNRA.sqrt(
            QFNRA.multiply(
              new Numeral(2),
              QFNRA.multiply(
                analyteDiffusionCoefficient[i],
                maxTime[i]
              )
            )
          )
        );
        SExpression maxTimeAnalyteConcentration = QFNRA.divide(
          QFNRA.multiply(
            analyteInitialSurfaceConcentration[i],
            QFNRA.exp(
              QFNRA.divide(
                QFNRA.pow(
                  QFNRA.divide(
                    QFNRA.subtract(
                      separationDistance,
                      QFNRA.multiply(
                        injectionSeparationChannelAnalyteVelocity[i],
                        maxTime[i]
                      )
                    ),
                    maxTimeAnalyteSpread
                  ),
                  new Numeral(2)
                ),
                new Numeral(-2)
              )
            )
          ),
          QFNRA.multiply(
            new Decimal(2.506628), // sqrt(2*pi)
            maxTimeAnalyteSpread
          )
        );
        SExpression fadeTimeAnalyteSpread = QFNRA.add(
          sampleInitialSpread,
          QFNRA.sqrt(
            QFNRA.multiply(
              new Numeral(2),
              QFNRA.multiply(
                analyteDiffusionCoefficient[i],
                fadeTime[i]
              )
            )
          )
        );
        SExpression fadeTimeAnalyteConcentration = QFNRA.divide(
          QFNRA.multiply(
            analyteInitialSurfaceConcentration[i],
            QFNRA.exp(
              QFNRA.divide(
                QFNRA.pow(
                  QFNRA.divide(
                    QFNRA.subtract(
                      separationDistance,
                      QFNRA.multiply(
                        injectionSeparationChannelAnalyteVelocity[i],
                        fadeTime[i]
                      )
                    ),
                    fadeTimeAnalyteSpread
                  ),
                  new Numeral(2)
                ),
                new Numeral(-2)
              )
            )
          ),
          QFNRA.multiply(
            new Decimal(2.506628), // sqrt(2*pi)
            fadeTimeAnalyteSpread
          )
        );
        SExpression endTimeAnalyteSpread = QFNRA.add(
          sampleInitialSpread,
          QFNRA.sqrt(
            QFNRA.multiply(
              new Numeral(2),
              QFNRA.multiply(
                analyteDiffusionCoefficient[i],
                endTime[i]
              )
            )
          )
        );
        SExpression endTimeAnalyteConcentration = QFNRA.divide(
          QFNRA.multiply(
            analyteInitialSurfaceConcentration[i],
            QFNRA.exp(
              QFNRA.divide(
                QFNRA.pow(
                  QFNRA.divide(
                    QFNRA.subtract(
                      separationDistance,
                      QFNRA.multiply(
                        injectionSeparationChannelAnalyteVelocity[i],
                        endTime[i]
                      )
                    ),
                    endTimeAnalyteSpread
                  ),
                  new Numeral(2)
                ),
                new Numeral(-2)
              )
            )
          ),
          QFNRA.multiply(
            new Decimal(2.506628), // sqrt(2*pi)
            endTimeAnalyteSpread
          )
        );

        exprs.add(QFNRA.assertEqual(
          maxTime[i],
          QFNRA.divide(
            separationDistance,
            injectionSeparationChannelAnalyteVelocity[i]
          )
        ));
        exprs.add(QFNRA.assertEqual(
          maxTimeConcentration[i],
          maxTimeAnalyteConcentration
        ));
        exprs.add(QFNRA.assertEqual(
          focusTimeAnalyteConcentration,
          QFNRA.multiply(
            negligibleConcentration,
            new Decimal(0.45)
          )
        ));
        exprs.add(QFNRA.assertEqual(
          fadeTimeAnalyteConcentration,
          QFNRA.multiply(
            negligibleConcentration,
            new Decimal(0.45)
          )
        ));
        if (numAnalytes > 3 || numAnalytes == 3 && i != 1) {
          exprs.add(QFNRA.assertEqual(
            startTimeAnalyteConcentration,
            QFNRA.multiply(
              negligibleConcentration,
              new Decimal((numAnalytes == 3) ? 0.1 : (0.1 / (numAnalytes - 3)))
            )
          ));
          exprs.add(QFNRA.assertEqual(
            endTimeAnalyteConcentration,
            QFNRA.multiply(
              negligibleConcentration,
              new Decimal((numAnalytes == 3) ? 0.1 : (0.1 / (numAnalytes - 3)))
            )
          ));
        }
        exprs.add(QFNRA.assertGreaterEqual(
          QFNRA.divide(
            maxTimeConcentration[i],
            detectableConcentration
          ),
          new Numeral(10)
        ));
      }

      for (int i = 0; i < numAnalytes - 1; ++i) {
        SExpression[] minTimeAnalyteSpread = new SExpression[numAnalytes];
        SExpression[] minTimeAnalyteConcentration = 
          new SExpression[numAnalytes];

        for (int j = i; j < i + 2; ++j) {
          minTimeAnalyteSpread[j] = QFNRA.add(
            sampleInitialSpread,
            QFNRA.sqrt(
              QFNRA.multiply(
                new Numeral(2),
                QFNRA.multiply(
                  analyteDiffusionCoefficient[j],
                  minTime[i]
                )
              )
            )
          );
          minTimeAnalyteConcentration[j] = QFNRA.divide(
            QFNRA.multiply(
              analyteInitialSurfaceConcentration[j],
              QFNRA.exp(
                QFNRA.divide(
                  QFNRA.pow(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[j],
                          minTime[i]
                        )
                      ),
                      minTimeAnalyteSpread[j]
                    ),
                    new Numeral(2)
                  ),
                  new Numeral(-2)
                )
              )
            ),
            QFNRA.multiply(
              new Decimal(2.506628), // sqrt(2*pi)
              minTimeAnalyteSpread[j]
            )
          );
        }

        final double C1 = 
            ((RealValue) analyteInitialSurfaceConcentrationAttr.get(i))
            .toDouble();
        final double C2 = 
            ((RealValue) analyteInitialSurfaceConcentrationAttr.get(i + 1))
            .toDouble();
        final double D1 = 
            ((RealValue) analyteDiffusionCoefficientAttr.get(i)).toDouble();
        final double D2 = 
            ((RealValue) analyteDiffusionCoefficientAttr.get(i + 1)).toDouble();
        final double u1 = 
            ((RealValue) nCross.getAttribute("bulkMobility")).toDouble() + 
            ((RealValue) analyteElectrophoreticMobilityAttr.get(i)).toDouble();
        final double u2 = 
            ((RealValue) nCross.getAttribute("bulkMobility")).toDouble() + 
            ((RealValue) analyteElectrophoreticMobilityAttr.get(i + 1))
            .toDouble();
        final double diff = (C1 / C2) * Math.sqrt((D2 * u1) / (D1 * u2));
        if (diff >= 10 || diff <= 0.1) {
          SExpression minTimeAnalyteConcentrationDerivative1 = QFNRA.multiply(
            QFNRA.divide(
              minTimeAnalyteConcentration[i],
              minTimeAnalyteSpread[i]
            ),
            QFNRA.add(
              QFNRA.divide(
                QFNRA.multiply(
                  injectionSeparationChannelAnalyteVelocity[i],
                  QFNRA.subtract(
                    separationDistance,
                    QFNRA.multiply(
                      injectionSeparationChannelAnalyteVelocity[i],
                      minTime[i]
                    )
                  )
                ),
                minTimeAnalyteSpread[i]
              ),
              QFNRA.multiply(
                QFNRA.subtract(
                  QFNRA.pow(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[i],
                          minTime[i]
                        )
                      ),
                      minTimeAnalyteSpread[i]
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
                      minTime[i]
                    )
                  )
                )
              )
            )
          );
          SExpression minTimeAnalyteConcentrationDerivative2 = QFNRA.multiply(
            QFNRA.divide(
              minTimeAnalyteConcentration[i + 1],
              minTimeAnalyteSpread[i + 1]
            ),
            QFNRA.add(
              QFNRA.divide(
                QFNRA.multiply(
                  injectionSeparationChannelAnalyteVelocity[i + 1],
                  QFNRA.subtract(
                    separationDistance,
                    QFNRA.multiply(
                      injectionSeparationChannelAnalyteVelocity[i + 1],
                      minTime[i]
                    )
                  )
                ),
                minTimeAnalyteSpread[i + 1]
              ),
              QFNRA.multiply(
                QFNRA.subtract(
                  QFNRA.pow(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[i + 1],
                          minTime[i]
                        )
                      ),
                      minTimeAnalyteSpread[i + 1]
                    ),
                    new Numeral(2)
                  ),
                  new Numeral(1)
                ),
                QFNRA.sqrt(
                  QFNRA.divide(
                    analyteDiffusionCoefficient[i + 1],
                    QFNRA.multiply(
                      new Numeral(2),
                      minTime[i]
                    )
                  )
                )
              )
            )
          );
          exprs.add(QFNRA.assertEqual(
            new Numeral(0),
            QFNRA.add(
              minTimeAnalyteConcentrationDerivative1, 
              minTimeAnalyteConcentrationDerivative2 
            )
          ));
          exprs.add(QFNRA.assertEqual(
            minTimeConcentration[i],
            QFNRA.add(
              QFNRA.add(
                minTimeAnalyteConcentration[i],
                minTimeAnalyteConcentration[i + 1]
              ),
              QFNRA.multiply(
                negligibleConcentration,
                new Decimal(
                  (numAnalytes == 3) ? 0.1 : 
                    0.1 * (numAnalytes - 2) / (numAnalytes - 3)
                )
              )
            )
          ));
        } else {
          exprs.add(QFNRA.assertEqual(
            minTimeAnalyteConcentration[i],
            minTimeAnalyteConcentration[i + 1]
          ));
          exprs.add(QFNRA.assertEqual(
            minTimeConcentration[i],
            QFNRA.add(
              QFNRA.multiply(
                new Numeral(2),
                minTimeAnalyteConcentration[i]
              ),
              QFNRA.multiply(
                negligibleConcentration,
                new Decimal(
                  (numAnalytes == 3) ? 0.1 : 
                    0.1 * (numAnalytes - 2) / (numAnalytes - 3)
                )
              )
            )
          ));
        }
      }
    
      // TODO: determining the minimum initial surface concentration and 
      // maximum diffusion coefficient can be done with SMT2 constraints 
      // (note that min/max operators would require dReal3)
      double minInitialSurfaceConcentration = Double.POSITIVE_INFINITY;
      double maxDiffusionCoefficient = 0;
      for (int i = 0; i < numAnalytes; ++i) {
        minInitialSurfaceConcentration = Math.min(
            minInitialSurfaceConcentration, 
            ((RealValue) analyteInitialSurfaceConcentrationAttr.get(i))
            .toDouble());
        maxDiffusionCoefficient = Math.max(maxDiffusionCoefficient, 
            ((RealValue) analyteDiffusionCoefficientAttr.get(i))
            .toDouble());
      }
      exprs.add(QFNRA.assertEqual(
        negligibleConcentration,
        QFNRA.divide(
          new Decimal(minInitialSurfaceConcentration / 5),
          QFNRA.add(
            sampleInitialSpread,
            QFNRA.sqrt(
              QFNRA.multiply(
                new Numeral(2),
                QFNRA.multiply(
                  new Decimal(maxDiffusionCoefficient),
                  maxTime[numAnalytes - 1]
                )
              )
            )
          )
        )
      ));
      /*SExpression minPeakConcentration = QFNRA.min(
        maxTimeConcentration[0],
        maxTimeConcentration[1]
      );
      for (int i = 2; i < numAnalytes; ++i) {
        minPeakConcentration = QFNRA.min(
          minPeakConcentration,
          maxTimeConcentration[i]
        );
      }
      exprs.add(QFNRA.assertEqual(
        negligibleConcentration,
        QFNRA.divide(
          minPeakConcentration,
          new Numeral(20)
        )
      ));*/

      SExpression threshold = new Decimal(0.85);
      for (int i = 0; i < numAnalytes - 1; ++i) {
        exprs.add(QFNRA.assertLessThanEqual(
          QFNRA.divide(
            minTimeConcentration[i],
            maxTimeConcentration[i]
          ),
          threshold
        ));
        exprs.add(QFNRA.assertLessThanEqual(
          QFNRA.divide(
            minTimeConcentration[i],
            maxTimeConcentration[i + 1]
          ),
          threshold
        ));
      }
    }

    return exprs;
  }

  private List<SExpression> translateElectrophoreticCrossApproximate(
      Schematic schematic, NodeValue nCross) throws 
      UndeclaredIdentifierException, UndeclaredAttributeException {
    List<SExpression> exprs = new LinkedList<>();

    ArrayValue analyteElectrophoreticMobilityAttr = 
      (ArrayValue) nCross.getAttribute("analyteElectrophoreticMobility");
    ArrayValue analyteInitialSurfaceConcentrationAttr = 
      (ArrayValue) nCross.getAttribute("analyteInitialSurfaceConcentration");
    ArrayValue analyteDiffusionCoefficientAttr = 
      (ArrayValue) nCross.getAttribute("analyteDiffusionCoefficient");
    int numAnalytes = analyteElectrophoreticMobilityAttr.length();

    Symbol lenSeparationChannel = SymbolNameGenerator
      .getsym_EPCrossSeparationChannelLength(schematic, nCross);
    Symbol lenTail = SymbolNameGenerator
      .getsym_EPCrossTailChannelLength(schematic, nCross);
    Symbol lenCross = SymbolNameGenerator
      .getsym_EPCrossLength(schematic, nCross);
    Symbol injectionCathodeNodeVoltage = SymbolNameGenerator
      .getsym_EPCrossInjectionCathodeNodeVoltage(schematic, nCross);
    Symbol injectionAnodeNodeVoltage = SymbolNameGenerator
      .getsym_EPCrossInjectionAnodeNodeVoltage(schematic, nCross);
    Symbol injectionSeparationChannelE = SymbolNameGenerator
      .getsym_EPCrossInjectionSeparationChannelE(schematic, nCross);
    Symbol bulkMobility = SymbolNameGenerator
      .getsym_EPCrossBulkMobility(schematic, nCross);
    Symbol separationDistance = SymbolNameGenerator
      .getsym_EPCrossSeparationDistance(schematic, nCross);
    Symbol injectionChannelRadius = SymbolNameGenerator
      .getsym_EPCrossInjectionChannelRadius(schematic, nCross);
    Symbol sampleInitialSpread = SymbolNameGenerator
      .getsym_EPCrossSampleInitialSpread(schematic, nCross);
    Symbol detectableConcentration = SymbolNameGenerator
      .getsym_EPCrossDetectableConcentration(schematic, nCross);
        
    Symbol[] injectionSeparationChannelAnalyteVelocity = 
      new Symbol[numAnalytes];
    Symbol[] injectionInjectionChannelAnalyteVelocity = new Symbol[numAnalytes];
    Symbol[] analyteInitialSurfaceConcentration = new Symbol[numAnalytes];
    Symbol[] analyteDiffusionCoefficient = new Symbol[numAnalytes];
    Symbol[] analyteElectrophoreticMobility = new Symbol[numAnalytes];
    Symbol[] maxTimeConcentration = new Symbol[numAnalytes];
    Symbol[] minTimeConcentration = new Symbol[numAnalytes - 1];
    Symbol[] maxTime = new Symbol[numAnalytes];
    Symbol[] minTime = new Symbol[numAnalytes - 1];
    
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
      maxTimeConcentration[i] = SymbolNameGenerator
        .getsym_EPCrossMaxTimeConcentration(schematic, nCross, i);
      maxTime[i] = SymbolNameGenerator
        .getsym_EPCrossMaxTime(schematic, nCross, i);
    }
    for (int i = 0; i < numAnalytes - 1; ++i) {
      minTimeConcentration[i] = SymbolNameGenerator
        .getsym_EPCrossMinTimeConcentration(schematic, nCross, i);
      minTime[i] = SymbolNameGenerator
        .getsym_EPCrossMinTime(schematic, nCross, i);
    }

    // declare variables
    exprs.add(QFNRA.declareRealVariable(lenSeparationChannel));
    exprs.add(QFNRA.declareRealVariable(lenTail));
    exprs.add(QFNRA.declareRealVariable(lenCross));
    exprs.add(QFNRA.declareRealVariable(injectionCathodeNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionAnodeNodeVoltage));
    exprs.add(QFNRA.declareRealVariable(injectionSeparationChannelE));
    exprs.add(QFNRA.declareRealVariable(bulkMobility));
    exprs.add(QFNRA.declareRealVariable(separationDistance));
    exprs.add(QFNRA.declareRealVariable(injectionChannelRadius));
    exprs.add(QFNRA.declareRealVariable(sampleInitialSpread));
    exprs.add(QFNRA.declareRealVariable(detectableConcentration));
    
    for (int i = 0; i < numAnalytes; ++i) {
      exprs.add(QFNRA.declareRealVariable(
        injectionSeparationChannelAnalyteVelocity[i]));
      exprs.add(QFNRA.declareRealVariable(
        injectionInjectionChannelAnalyteVelocity[i]));
      exprs.add(QFNRA.declareRealVariable(
        analyteInitialSurfaceConcentration[i]));
      exprs.add(QFNRA.declareRealVariable(analyteDiffusionCoefficient[i]));
      exprs.add(QFNRA.declareRealVariable(analyteElectrophoreticMobility[i]));
      exprs.add(QFNRA.declareRealVariable(maxTimeConcentration[i]));
      exprs.add(QFNRA.declareRealVariable(maxTime[i]));
    }
    for (int i = 0; i < numAnalytes - 1; ++i) {
      exprs.add(QFNRA.declareRealVariable(minTimeConcentration[i]));
      exprs.add(QFNRA.declareRealVariable(minTime[i]));
    }

    // "constants"
    // Distances are in metres, times are in seconds, voltages are in volts, 
    // masses are in kilograms, and surface concentrations are in moles per 
    // metres squared.
    exprs.add(QFNRA.assertEqual(
      bulkMobility,
      new Decimal(((RealValue) nCross.getAttribute("bulkMobility")).toDouble())
    ));
    exprs.add(QFNRA.assertEqual(
      injectionCathodeNodeVoltage, 
      new Decimal(
        ((RealValue) nCross.getAttribute("injectionCathodeNodeVoltage"))
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
        ((RealValue) nCross.getAttribute("lenSeparationChannel")).toDouble()
      )
    ));
    exprs.add(QFNRA.assertEqual(
      lenTail, 
      new Decimal(
        ((RealValue) nCross.getAttribute("lenInjectionChannel")).toDouble()
      )
    ));
    exprs.add(QFNRA.assertEqual(
      injectionChannelRadius, 
      new Decimal(((RealValue) nCross.getAttribute("channelRadius")).toDouble())
    ));
    exprs.add(QFNRA.assertEqual(
      detectableConcentration, 
      new Decimal(
        ((RealValue) nCross.getAttribute("detectableConcentration")).toDouble()
      )
    ));

    for (int i = 0; i < numAnalytes; ++i) {
      exprs.add(QFNRA.assertEqual(
        analyteElectrophoreticMobility[i], 
        new Decimal(
          ((RealValue) analyteElectrophoreticMobilityAttr.get(i)).toDouble()
        )
      ));
      exprs.add(QFNRA.assertEqual(
        analyteInitialSurfaceConcentration[i], 
        new Decimal(
          ((RealValue) analyteInitialSurfaceConcentrationAttr.get(i)).toDouble()
        )
      ));
      exprs.add(QFNRA.assertEqual(
        analyteDiffusionCoefficient[i], 
        new Decimal(
          ((RealValue) analyteDiffusionCoefficientAttr.get(i)).toDouble()
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
        new Decimal(1.17741)
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
      for (int i = 0; i < numAnalytes - 1; ++i) {
        exprs.add(QFNRA.assertGreater(
          minTime[i],
          QFNRA.add(
            maxTime[i],
            new Numeral(1)
          )
        ));
        exprs.add(QFNRA.assertGreater(
          maxTime[i + 1],
          QFNRA.add(
            minTime[i],
            new Numeral(1)
          )
        ));
      }
      
      // requried to ensure that electropherogram is initially increasing
      SExpression initialSurfaceConcentrationExpr = QFNRA.add( 
        analyteInitialSurfaceConcentration[0],
        analyteInitialSurfaceConcentration[1]
      );
      for (int j = 2; j < numAnalytes; ++j) {
        initialSurfaceConcentrationExpr = QFNRA.add(
          initialSurfaceConcentrationExpr,
          analyteInitialSurfaceConcentration[j]
        );
      }
      exprs.add(QFNRA.assertLessThan(
        QFNRA.divide(
          QFNRA.multiply(
            initialSurfaceConcentrationExpr,
            QFNRA.exp(
              QFNRA.divide(
                QFNRA.pow(
                  QFNRA.divide(
                    separationDistance,
                    sampleInitialSpread
                  ),
                  new Numeral(2)
                ),
                new Numeral(-2)
              )
            )
          ),
          QFNRA.multiply(
            new Decimal(2.506628), // sqrt(2*pi)
            sampleInitialSpread
          )
        ),
        detectableConcentration
      ));

      for (int i = 0; i < numAnalytes; ++i) {
        SExpression[] maxTimeAnalyteSpread = new SExpression[numAnalytes];
        SExpression[] maxTimeAnalyteConcentration = 
          new SExpression[numAnalytes];

        for (int j = 0; j < numAnalytes; ++j) {
          maxTimeAnalyteSpread[j] = QFNRA.add(
            sampleInitialSpread,
            QFNRA.sqrt(
              QFNRA.multiply(
                new Numeral(2),
                QFNRA.multiply(
                  analyteDiffusionCoefficient[j],
                  maxTime[i]
                )
              )
            )
          );
          maxTimeAnalyteConcentration[j] = QFNRA.divide(
            QFNRA.multiply(
              analyteInitialSurfaceConcentration[j],
              QFNRA.exp(
                QFNRA.divide(
                  QFNRA.pow(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[j],
                          maxTime[i]
                        )
                      ),
                      maxTimeAnalyteSpread[j]
                    ),
                    new Numeral(2)
                  ),
                  new Numeral(-2)
                )
              )
            ),
            QFNRA.multiply(
              new Decimal(2.506628), // sqrt(2*pi)
              maxTimeAnalyteSpread[j]
            )
          );
        }
        SExpression maxTimeConcentrationExpr = QFNRA.add(
          maxTimeAnalyteConcentration[0],
          maxTimeAnalyteConcentration[1]
        );
        for (int j = 2; j < numAnalytes; ++j) {
          maxTimeConcentrationExpr = QFNRA.add(
            maxTimeConcentrationExpr,
            maxTimeAnalyteConcentration[j]
          );
        }
        exprs.add(QFNRA.assertEqual(
          maxTimeConcentration[i],
          maxTimeConcentrationExpr
        ));

        exprs.add(QFNRA.assertEqual(
          maxTime[i],
          QFNRA.divide(
            separationDistance,
            injectionSeparationChannelAnalyteVelocity[i]
          )
        ));
        exprs.add(QFNRA.assertGreaterEqual(
          QFNRA.divide(
            maxTimeConcentration[i],
            detectableConcentration
          ),
          new Numeral(10)
        ));
        exprs.add(QFNRA.assertGreaterEqual(
          QFNRA.divide(
            maxTimeAnalyteConcentration[i],
            maxTimeConcentration[i]
          ),
          new Decimal(5.0 / 6)
        ));
      }
      for (int i = 0; i < numAnalytes - 1; ++i) {
        SExpression[] minTimeAnalyteSpread = new SExpression[numAnalytes];
        SExpression[] minTimeAnalyteConcentration = 
          new SExpression[numAnalytes];

        for (int j = 0; j < numAnalytes; ++j) {
          minTimeAnalyteSpread[j] = QFNRA.add(
            sampleInitialSpread,
            QFNRA.sqrt(
              QFNRA.multiply(
                new Numeral(2),
                QFNRA.multiply(
                  analyteDiffusionCoefficient[j],
                  minTime[i]
                )
              )
            )
          );
          minTimeAnalyteConcentration[j] = QFNRA.divide(
            QFNRA.multiply(
              analyteInitialSurfaceConcentration[j],
              QFNRA.exp(
                QFNRA.divide(
                  QFNRA.pow(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[j],
                          minTime[i]
                        )
                      ),
                      minTimeAnalyteSpread[j]
                    ),
                    new Numeral(2)
                  ),
                  new Numeral(-2)
                )
              )
            ),
            QFNRA.multiply(
              new Decimal(2.506628), // sqrt(2*pi)
              minTimeAnalyteSpread[j]
            )
          );
        }
        SExpression minTimeConcentrationExpr = QFNRA.add(
          minTimeAnalyteConcentration[0],
          minTimeAnalyteConcentration[1]
        );
        for (int j = 2; j < numAnalytes; ++j) {
          minTimeConcentrationExpr = QFNRA.add(
            minTimeConcentrationExpr,
            minTimeAnalyteConcentration[j]
          );
        }
        exprs.add(QFNRA.assertEqual(
          minTimeConcentration[i],
          minTimeConcentrationExpr
        ));

        final double C1 = 
            ((RealValue) analyteInitialSurfaceConcentrationAttr.get(i))
            .toDouble();
        final double C2 = 
            ((RealValue) analyteInitialSurfaceConcentrationAttr.get(i + 1))
            .toDouble();
        final double D1 = 
            ((RealValue) analyteDiffusionCoefficientAttr.get(i)).toDouble();
        final double D2 = 
            ((RealValue) analyteDiffusionCoefficientAttr.get(i + 1)).toDouble();
        final double u1 = 
            ((RealValue) nCross.getAttribute("bulkMobility")).toDouble() + 
            ((RealValue) analyteElectrophoreticMobilityAttr.get(i)).toDouble();
        final double u2 = 
            ((RealValue) nCross.getAttribute("bulkMobility")).toDouble() + 
            ((RealValue) analyteElectrophoreticMobilityAttr.get(i + 1))
            .toDouble();
        final double diff = (C1 / C2) * Math.sqrt((D2 * u1) / (D1 * u2));
        if (diff >= 10 || diff <= 0.1) {
          SExpression minTimeAnalyteConcentrationDerivative1 = QFNRA.multiply(
            QFNRA.divide(
              minTimeAnalyteConcentration[i],
              minTimeAnalyteSpread[i]
            ),
            QFNRA.add(
              QFNRA.divide(
                QFNRA.multiply(
                  injectionSeparationChannelAnalyteVelocity[i],
                  QFNRA.subtract(
                    separationDistance,
                    QFNRA.multiply(
                      injectionSeparationChannelAnalyteVelocity[i],
                      minTime[i]
                    )
                  )
                ),
                minTimeAnalyteSpread[i]
              ),
              QFNRA.multiply(
                QFNRA.subtract(
                  QFNRA.pow(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[i],
                          minTime[i]
                        )
                      ),
                      minTimeAnalyteSpread[i]
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
                      minTime[i]
                    )
                  )
                )
              )
            )
          );
          SExpression minTimeAnalyteConcentrationDerivative2 = QFNRA.multiply(
            QFNRA.divide(
              minTimeAnalyteConcentration[i + 1],
              minTimeAnalyteSpread[i + 1]
            ),
            QFNRA.add(
              QFNRA.divide(
                QFNRA.multiply(
                  injectionSeparationChannelAnalyteVelocity[i + 1],
                  QFNRA.subtract(
                    separationDistance,
                    QFNRA.multiply(
                      injectionSeparationChannelAnalyteVelocity[i + 1],
                      minTime[i]
                    )
                  )
                ),
                minTimeAnalyteSpread[i + 1]
              ),
              QFNRA.multiply(
                QFNRA.subtract(
                  QFNRA.pow(
                    QFNRA.divide(
                      QFNRA.subtract(
                        separationDistance,
                        QFNRA.multiply(
                          injectionSeparationChannelAnalyteVelocity[i + 1],
                          minTime[i]
                        )
                      ),
                      minTimeAnalyteSpread[i + 1]
                    ),
                    new Numeral(2)
                  ),
                  new Numeral(1)
                ),
                QFNRA.sqrt(
                  QFNRA.divide(
                    analyteDiffusionCoefficient[i + 1],
                    QFNRA.multiply(
                      new Numeral(2),
                      minTime[i]
                    )
                  )
                )
              )
            )
          );
          exprs.add(QFNRA.assertEqual(
            new Numeral(0),
            QFNRA.add(
              minTimeAnalyteConcentrationDerivative1, 
              minTimeAnalyteConcentrationDerivative2 
            )
          ));
        } else {
          exprs.add(QFNRA.assertEqual(
            minTimeAnalyteConcentration[i],
            minTimeAnalyteConcentration[i + 1]
          ));
        }
      }
     
      SExpression threshold = new Decimal(0.85);
      for (int i = 0; i < numAnalytes - 1; ++i) {
        exprs.add(QFNRA.assertLessThanEqual(
          QFNRA.divide(
            minTimeConcentration[i],
            maxTimeConcentration[i]
          ),
          threshold
        ));
        exprs.add(QFNRA.assertLessThanEqual(
          QFNRA.divide(
            minTimeConcentration[i],
            maxTimeConcentration[i + 1]
          ),
          threshold
        ));
      }
    }

    return exprs;
  }
}
