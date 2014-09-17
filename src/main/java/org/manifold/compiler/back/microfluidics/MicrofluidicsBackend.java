package org.manifold.compiler.back.microfluidics;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.manifold.compiler.Backend;
import org.manifold.compiler.middle.Schematic;

public class MicrofluidicsBackend implements Backend {

  private static Logger log = LogManager.getLogger("MicrofluidicsBackend");

  private void err(String message) {
    log.error(message);
    log.error("stopping code generation due to error");
    throw new CodeGenerationError(message);
  }
  
  private Options options;
  
  @Override
  public String getBackendName() {
    return "microfluidics";
  }

  @SuppressWarnings("static-access")
  private void createOptionProcessParameters() {
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
  
  private void createOptionDefinitions() {
    options = new Options();
    createOptionProcessParameters();
  }
  
  private void collectOptions(CommandLine cmd) throws IOException {
    collectOptionProcessParameters(cmd);
  }
  
  public void readArguments(String[] args) throws ParseException, IOException {
    // set up options for command-line parsing
    createOptionDefinitions();
    // parse command line
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, args);
    // retrieve command-line options
    collectOptions(cmd);
  }
  
  @Override
  public void invokeBackend(Schematic schematic, String[] args)
      throws Exception {
    readArguments(args);
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
  
  public void run(Schematic schematic) {
    primitiveTypes = constructTypeTable(schematic);
  }
  
}
