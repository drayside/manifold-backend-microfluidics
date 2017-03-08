package org.manifold.compiler.back.microfluidics;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.middle.Schematic;

import java.io.FileWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestMicrofluidicsBackend {
  
  public static double viscosityOfWater = 0.001002; // Pa*s

  private static final Logger log = LogManager.getRootLogger();
  
  @BeforeClass
  public static void setUpClass() {
    LogManager.getRootLogger().setLevel(Level.ALL);
    PatternLayout layout = new PatternLayout(
        "%-5p [%t]: %m%n");
    LogManager.getRootLogger().addAppender(
        new ConsoleAppender(layout, ConsoleAppender.SYSTEM_ERR));
    
    UtilSchematicConstruction.setupIntermediateTypes();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOptionProcessParameters_FromCLI_MissingOptions()
      throws Exception {
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    String[] args = {
        "-bProcessMinimumNodeDistance", "0.0001",
        "-bProcessMinimumChannelLength", "0.0001",
        "-bProcessMaximumChipSizeX", "0.04",
        "-bProcessMaximumChipSizeY", "0.04",
        //"-bProcessCriticalCrossingAngle", "0.0872664626" // leave out
    };
    Schematic schematic = UtilSchematicConstruction
        .instantiateSchematic("empty");

    Options options = new Options();
    backend.registerArguments(options);
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, args);
    backend.invokeBackend(schematic, cmd);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testOptionProcessParameters_FromCLI_NotANumber()
          throws Exception {
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    String[] args = {
        "-bProcessMinimumNodeDistance", "foo",
        "-bProcessMinimumChannelLength", "bar",
        "-bProcessMaximumChipSizeX", "baz",
        "-bProcessMaximumChipSizeY", "doge",
        "-bProcessCriticalCrossingAngle", "wow"
    };
    Schematic schematic = UtilSchematicConstruction
        .instantiateSchematic("empty");

    Options options = new Options();
    backend.registerArguments(options);
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, args);
    backend.invokeBackend(schematic, cmd);
  }

  @Test
  public void testInvokeBackend_EmptySchematic()
      throws Exception {
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    String[] args = {
        "-bProcessMinimumNodeDistance", "0.0001",
        "-bProcessMinimumChannelLength", "0.0001",
        "-bProcessMaximumChipSizeX", "0.04",
        "-bProcessMaximumChipSizeY", "0.04",
        // both single and double dash are correct
        "--bProcessCriticalCrossingAngle", "0.0872664626"
    };
    Schematic schematic = UtilSchematicConstruction
            .instantiateSchematic("empty");

    runAcceptanceTest(schematic, args);
  }
  
  @Test
  public void testSimpleSynthesis() throws Exception {
    String[] args = {
      "-bProcessMinimumNodeDistance", "0.0001",
      "-bProcessMinimumChannelLength", "0.0001",
      "-bProcessMaximumChipSizeX", "0.04",
      "-bProcessMaximumChipSizeY", "0.04",
      "-bProcessCriticalCrossingAngle", "0.0872664626"
    };
    
    Schematic schematic = UtilSchematicConstruction
        .instantiateSchematic("testSimpleSynthesis");
    
    // Make a very simple schematic:
    // (fluidEntry) ---> (fluidExit)
    NodeValue entry = UtilSchematicConstruction.instantiateFluidEntry(
        schematic, viscosityOfWater);
    schematic.addNode("in0", entry);
    NodeValue exit = UtilSchematicConstruction.instantiateFluidExit(schematic);
    schematic.addNode("out0", exit);
    ConnectionValue entryToExit = UtilSchematicConstruction.instantiateChannel(
        entry.getPort("output"), exit.getPort("input"));
    schematic.addConnection("channel0", entryToExit);
    
    runAcceptanceTest(schematic, args);
  }
  
  @Test
  public void testTJunctionSynthesis() throws Exception {
    String[] args = {
      "-bProcessMinimumNodeDistance", "0.0001",
      "-bProcessMinimumChannelLength", "0.0001",
      "-bProcessMaximumChipSizeX", "0.10",
      "-bProcessMaximumChipSizeY", "0.10",
      "-bProcessCriticalCrossingAngle", "0.0872664626"
    };
    
    Schematic schematic = UtilSchematicConstruction
        .instantiateSchematic("testTJunctionSynthesis");
    
    // Make a schematic with two inputs, one output, and a T-junction
    NodeValue entry = UtilSchematicConstruction.instantiateFluidEntry(
        schematic, 0.01);
    schematic.addNode("in0", entry);
    NodeValue disperse = UtilSchematicConstruction.instantiateFluidEntry(
        schematic, 0.001);
    schematic.addNode("in1", disperse);
    NodeValue exit = UtilSchematicConstruction.instantiateFluidExit(schematic);
    schematic.addNode("out0", exit);
    
    NodeValue junction = UtilSchematicConstruction
        .instantiateTJunction(schematic);
    schematic.addNode("junction0", junction);
    
    
    ConnectionValue entryToJunction = UtilSchematicConstruction
        .instantiateChannel(entry.getPort("output"),
        junction.getPort("continuous"));
    schematic.addConnection("channelC", entryToJunction);
    ConnectionValue disperseToJunction = UtilSchematicConstruction
        .instantiateChannel(disperse.getPort("output"),
        junction.getPort("dispersed"));
    schematic.addConnection("channelD", disperseToJunction);
    ConnectionValue junctionToExit = UtilSchematicConstruction
        .instantiateChannel(junction.getPort("output"),
        exit.getPort("input"));
    schematic.addConnection("channelE", junctionToExit);
    
    runAcceptanceTest(schematic, args);
  }

  private void runAcceptanceTest(Schematic schematic, String[] args)
      throws Exception {

    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    Options options = new Options();
    backend.registerArguments(options);
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, args);
    backend.invokeBackend(schematic, cmd);

    verifyOutputFile(schematic.getName() + ".smt2");
    verifyOutputFile(schematic.getName() + ".mo");
  }

  private void verifyOutputFile(String fileName)
      throws Exception {

    // Backend originally outputs its files at the project root.
    Path actualOutputPath = Paths.get(fileName);
    Path expectedOutputPath = Paths.get(
      "src/test/java/org/manifold/compiler/back/microfluidics/acceptance/" +
      fileName);
    Path errorPath = Paths.get(
      "src/test/java/org/manifold/compiler/back/microfluidics/acceptance/" +
      fileName + ".actual");

    String expectedFileContents = null;
    if (Files.exists(expectedOutputPath)) {
      byte[] encoded = Files.readAllBytes(expectedOutputPath);
      expectedFileContents = normalizeLineEndings(
        new String(encoded, Charset.defaultCharset()));
    }

    String actualFileContents = null;
    if (Files.exists(actualOutputPath)) {
      byte[] encoded = Files.readAllBytes(actualOutputPath);
      actualFileContents = normalizeLineEndings(
        new String(encoded, Charset.defaultCharset()));
    }

    if (expectedFileContents != null) {
      if (!expectedFileContents.equals(actualFileContents)) {
        FileWriter fileWriter = new FileWriter(errorPath.toFile());
        try {
          fileWriter.write(actualFileContents);
        } finally {
          fileWriter.close();
        }

        String explanation = new StringBuilder()
          .append("ERROR: This output does not match the expected output.\n")
          .append("The new output has been saved as '")
          .append(errorPath.getFileName())
          .append("'.\n")
          .append("If the changes are valid, regenerate it by deleting '")
          .append(expectedOutputPath.getFileName())
          .append("' and running tests again.\n")
          .toString();

        log.error(explanation);
        throw new AssertionError(explanation);
      }
    } else {
      log.warn("Generating new expected output: " + expectedOutputPath);
      FileWriter fileWriter = new FileWriter(expectedOutputPath.toFile());
      try {
        fileWriter.write(actualFileContents);
      } finally {
        fileWriter.close();
      }
    }

    if (Files.exists(errorPath)) {
      Files.delete(errorPath);
    }
  }

  private String normalizeLineEndings(String fileContents) {
    return fileContents.replaceAll("\\r\\n?", "\n");
  }

}
