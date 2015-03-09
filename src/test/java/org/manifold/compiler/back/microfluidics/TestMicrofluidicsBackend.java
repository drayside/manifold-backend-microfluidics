package org.manifold.compiler.back.microfluidics;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PatternLayout;
import org.junit.BeforeClass;
import org.junit.Test;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.middle.Schematic;

public class TestMicrofluidicsBackend {
  
  public static double viscosityOfWater = 0.001002; // Pa*s
  
  @BeforeClass
  public static void setUpClass() {
    LogManager.getRootLogger().setLevel(Level.ALL);
    PatternLayout layout = new PatternLayout(
        "%-5p [%t]: %m%n");
    LogManager.getRootLogger().addAppender(
        new ConsoleAppender(layout, ConsoleAppender.SYSTEM_ERR));
    
    UtilSchematicConstruction.setupIntermediateTypes();
  }
  
  @Test
  public void testTJunctionSynthesis() throws Exception {
    String[] args = {
      "-bProcessMinimumNodeDistance", "0.0001",
      "-bProcessMinimumChannelLength", "0.0001",
      "-bProcessMaximumChipSizeX", "0.04",
      "-bProcessMaximumChipSizeY", "0.04",
      "-bProcessCriticalCrossingAngle", "0.0872664626"
    };
    
    Schematic schematic = UtilSchematicConstruction
        .instantiateSchematic("testTJunctionSynthesis");
    
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
    // TODO constrain the pressure in the channel to be 0.001 Pa
    
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    Options options = new Options();
    backend.registerArguments(options);
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, args);
    backend.invokeBackend(schematic, cmd);
  }
  
  // TODO update test for new interface
  /*
  @Test
  public void testOptionProcessParameters_FromCLI() 
      throws ParseException, IOException {
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    String[] args = {
      "-bProcessMinimumNodeDistance", "0.0001",
      "-bProcessMinimumChannelLength", "0.0001",
      "-bProcessMaximumChipSizeX", "0.04",
      "-bProcessMaximumChipSizeY", "0.04",
      // both single and double dash are correct
      "--bProcessCriticalCrossingAngle", "0.0872664626"
    };
    backend.readArguments(args);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testOptionProcessParameters_FromCLI_MissingOptions() 
      throws ParseException, IOException {
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    String[] args = {
      "-bProcessMinimumNodeDistance", "0.0001",
      "-bProcessMinimumChannelLength", "0.0001",
      "-bProcessMaximumChipSizeX", "0.04",
      "-bProcessMaximumChipSizeY", "0.04",
      //"-bProcessCriticalCrossingAngle", "0.0872664626" // leave out
    };
    backend.readArguments(args);
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void testOptionProcessParameters_FromCLI_NotANumber() 
      throws ParseException, IOException {
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    String[] args = {
      "-bProcessMinimumNodeDistance", "foo",
      "-bProcessMinimumChannelLength", "bar",
      "-bProcessMaximumChipSizeX", "baz",
      "-bProcessMaximumChipSizeY", "doge",
      "-bProcessCriticalCrossingAngle", "wow"
    };
    backend.readArguments(args);
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
      "-bProcessCriticalCrossingAngle", "0.0872664626"
    };
    Schematic schematic = UtilSchematicConstruction
        .instantiateSchematic("test");
    backend.invokeBackend(schematic, args); 
  }
  */
}
