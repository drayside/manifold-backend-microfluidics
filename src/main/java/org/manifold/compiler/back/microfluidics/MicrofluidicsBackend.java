package org.manifold.compiler.back.microfluidics;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.manifold.compiler.Backend;
import org.manifold.compiler.ConnectionType;
import org.manifold.compiler.NodeTypeValue;
import org.manifold.compiler.PortTypeValue;
import org.manifold.compiler.TypeValue;
import org.manifold.compiler.UndeclaredIdentifierException;
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

  private PortTypeValue microfluidPortType = null;
  private NodeTypeValue controlPointNodeType = null;
  private NodeTypeValue pressureControlPointNodeType = null;
  private List<NodeTypeValue> derivedPressureCPNodeTypes;
  private NodeTypeValue voltageControlPointNodeType = null;
  private List<NodeTypeValue> derivedVoltageCPNodeTypes;
  private NodeTypeValue channelCrossingNodeType = null;
  private ConnectionType microfluidChannelType = null;
  
  private void retrieveBaseTypes(Schematic schematic) {
    try {
      microfluidPortType = schematic.getPortType("microfluidPort");
      controlPointNodeType = schematic.getNodeType("controlPoint");
      pressureControlPointNodeType = 
          schematic.getNodeType("pressureControlPoint");
      voltageControlPointNodeType = 
          schematic.getNodeType("voltageControlPoint");
      channelCrossingNodeType =
          schematic.getNodeType("channelCrossing");
      microfluidChannelType =
          schematic.getConnectionType("microfluidChannel");
    } catch (UndeclaredIdentifierException e) {
      err("could not find required microfluidic schematic type '"
          + e.getIdentifier() + "'; schematic version mismatch or "
          + " not a microfluidic schematic");
    }
  }
  
  private List<NodeTypeValue> retrieveDerivedNodeTypes(Schematic schematic, 
      NodeTypeValue baseType) {
    List<NodeTypeValue> derivedTypes = new LinkedList<>();
    for (NodeTypeValue t : schematic.getNodeTypes().values()) {
      if (t.isSubtypeOf(baseType)) {
        derivedTypes.add(t);
      }
    }
    return derivedTypes;
  }
  
  private void checkTypeHierarchy() {
    // Look for subtypes of controlPointNode
    if (!pressureControlPointNodeType.isSubtypeOf(controlPointNodeType)) {
      err("schematic type incompatibility:"
          + "pressureControlPoint must be a subtype of controlPoint");
    }
    if (!voltageControlPointNodeType.isSubtypeOf(controlPointNodeType)) {
      err("schematic type incompatibility:"
          + "voltageControlPoint must be a subtype of controlPoint");
    }
  }
  
  public void run(Schematic schematic) {
    retrieveBaseTypes(schematic);
    checkTypeHierarchy();
    derivedPressureCPNodeTypes = 
        retrieveDerivedNodeTypes(schematic, pressureControlPointNodeType);
    derivedPressureCPNodeTypes = 
        retrieveDerivedNodeTypes(schematic, voltageControlPointNodeType);
  }
  
}
