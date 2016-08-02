package org.manifold.compiler.back.microfluidics;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.manifold.compiler.Backend;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.ParenList;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.back.microfluidics.strategies.MultiPhaseStrategySet;
import org.manifold.compiler.back.microfluidics.strategies.PlacementTranslationStrategySet;
import org.manifold.compiler.back.microfluidics.strategies.PressureFlowStrategySet;
import org.manifold.compiler.middle.Schematic;

public class MicrofluidicsBackend implements Backend {

  private static final Logger log =
      LogManager.getLogger("MicrofluidicsBackend");

  private void err(String message) {
    log.error(message);
    log.error("stopping code generation due to error");
    throw new CodeGenerationError(message);
  }
  
  @Override
  public String getBackendName() {
    return "microfluidics";
  }

  @SuppressWarnings("static-access")
  private void createOptionProcessParameters(Options options) {
    Option processFile = OptionBuilder.withArgName("file")
        .hasArg()
        .withDescription("load process parameters from given JSON file")
        .create("bProcessFile");
    options.addOption(processFile);
    ProcessParameters.createOptions(options);
  }
  
  private ProcessParameters processParams;
  public ProcessParameters getProcessParameters() {
    return processParams;
  }
  
  private void collectOptionProcessParameters(CommandLine cmd) 
      throws IOException {
    try {
      // first look for this particular option if it is present
      // and use the value as filename
      String filename = cmd.getOptionValue("bProcessFile");
      if (filename != null) {
        // load process parameters from this path
        processParams = ProcessParameters.loadFromFile(filename);
      } else {
        log.debug("no process file specified,"
            + "reading process parameters from command line");
        // assume parameters have been specified on the command line
        processParams = ProcessParameters.loadFromCommandLine(cmd);
      }
    } catch (Exception e) {
      log.error(
          "process parameters could not be loaded;"
          + " specify them with either '-bProcessFile <filename>'"
          + "or '-bProcess<param> <value>'");
      throw e;
    }
  }
  
  @Override
  public void registerArguments(Options options) {
    createOptionProcessParameters(options);
  }
  
  private void collectOptions(CommandLine cmd) throws IOException {
    collectOptionProcessParameters(cmd);
  }

  @Override
  public void invokeBackend(Schematic schematic, CommandLine cmd)
      throws Exception {
    collectOptions(cmd);
    run(schematic);
  }

  private PrimitiveTypeTable primitiveTypes = new PrimitiveTypeTable();
  public PrimitiveTypeTable getPrimitiveTypes() {
    return primitiveTypes;
  }
  public static PrimitiveTypeTable constructTypeTable(Schematic schematic) {
    PrimitiveTypeTable typeTable = new PrimitiveTypeTable();
    typeTable.retrieveBaseTypes(schematic);
    checkTypeHierarchy(typeTable);
    typeTable.addDerivedPressureControlPointNodeTypes(
        typeTable.retrieveDerivedNodeTypes(schematic, 
            typeTable.getPressureControlPointNodeType()));
    typeTable.addDerivedVoltageControlPointNodeTypes(
        typeTable.retrieveDerivedNodeTypes(schematic, 
            typeTable.getVoltageControlPointNodeType()));
    typeTable.retrieveConstraintTypes(schematic);
    return typeTable;
  }
  
  private static void checkTypeHierarchy(PrimitiveTypeTable typeTable) {
    // Look for subtypes of controlPointNode
    if (!typeTable.getPressureControlPointNodeType()
        .isSubtypeOf(typeTable.getControlPointNodeType())) {
      throw new CodeGenerationError("schematic type incompatibility:"
          + "pressureControlPoint must be a subtype of controlPoint");
    }
    if (!typeTable.getVoltageControlPointNodeType()
        .isSubtypeOf(typeTable.getControlPointNodeType())) {
      throw new CodeGenerationError("schematic type incompatibility:"
          + "voltageControlPoint must be a subtype of controlPoint");
    }
  }
  
  // Sort a list of unsorted expressions so that all declarations (declare-fun)
  // come before all assertions (assert).
  public List<SExpression> sortExprs(List<SExpression> unsorted) {
    List<SExpression> retval = new LinkedList<>();
    List<SExpression> decls = new LinkedList<>();
    List<SExpression> asserts = new LinkedList<>();
    List<SExpression> others = new LinkedList<>();
    
    for (SExpression expr : unsorted) {
      if (expr instanceof ParenList) {
        ParenList list = (ParenList) expr;
        SExpression head = list.getExprs().get(0);
        if (head instanceof Symbol) {
          Symbol s = (Symbol) head;
          if (s.getName().equals("declare-fun")) {
            decls.add(expr);
          } else if (s.getName().equals("assert")) {
            asserts.add(expr);
          } else {
            others.add(expr);
          }
        } else {
          others.add(expr);
        }
      } else {
        others.add(expr);
      }
    }
    
    retval.addAll(decls);
    retval.addAll(asserts);
    retval.addAll(others);
    return retval;
  }
  
  public void run(Schematic schematic) throws IOException {
    primitiveTypes = constructTypeTable(schematic);
    // translation step
    // for now: one pass
    List<SExpression> exprs = new LinkedList<>();
    exprs.add(QFNRA.useQFNRA());
    
    List<SExpression> unsortedExprs = new LinkedList<>();
    // define constant pi
    unsortedExprs.add(QFNRA.declareRealVariable(
        SymbolNameGenerator.getsym_constant_pi()));
    unsortedExprs.add(QFNRA.assertEqual(
        SymbolNameGenerator.getsym_constant_pi(), 
        new Decimal(Math.PI)));
    
    PlacementTranslationStrategySet placeSet = 
        new PlacementTranslationStrategySet();
    unsortedExprs.addAll(placeSet.translate(
        schematic, processParams, primitiveTypes));
    MultiPhaseStrategySet multiPhase = new MultiPhaseStrategySet();
    unsortedExprs.addAll(multiPhase.translate(
        schematic, processParams, primitiveTypes));
    PressureFlowStrategySet pressureFlow = new PressureFlowStrategySet();
    unsortedExprs.addAll(pressureFlow.translate(
        schematic, processParams, primitiveTypes));
    
    exprs.addAll(sortExprs(unsortedExprs));
    
    // (check-sat) (exit)
    exprs.add(new ParenList(new SExpression[] {
      new Symbol("check-sat")
    }));
    exprs.add(new ParenList(new SExpression[] {
      new Symbol("exit")
    }));
    // write to "schematic-name.smt2"
    String filename = schematic.getName() + ".smt2";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
      for (SExpression expr : exprs) {
        expr.write(writer);
        writer.newLine();
      }
    }
  }
  
}
