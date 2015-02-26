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
        .instantiateSchematic("test");
    
    // Make a very simple schematic:
    // (fluidEntry) ---> (fluidExit)
    // Water has a viscosity of 0.001002 Pa*s
    NodeValue entry = UtilSchematicConstruction.instantiateFluidEntry(
        schematic, 0.001002);
    schematic.addNode("n_entry", entry);
    NodeValue exit = UtilSchematicConstruction.instantiateFluidExit(schematic);
    schematic.addNode("n_exit", exit);
    ConnectionValue entryToExit = UtilSchematicConstruction.instantiateChannel(
        entry.getPort("output"), exit.getPort("input"));
    schematic.addConnection("c_entry_to_exit", entryToExit);
    // TODO constrain the pressure in the channel to be 0.001 Pa
    
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    Options options = new Options();
    backend.registerArguments(options);
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, args);
    backend.invokeBackend(schematic, cmd);
  }

  @Test
  public void testElectrophoerticCrossSynthesis() throws Exception {
    String[] args = {
        "-bProcessMinimumNodeDistance", "0.0001",
        "-bProcessMinimumChannelLength", "0.0001",
        "-bProcessMaximumChipSizeX", "0.04",
        "-bProcessMaximumChipSizeY", "0.04",
        "-bProcessCriticalCrossingAngle", "0.0872664626"
      };
    
    Schematic schematic = UtilSchematicConstruction
        .instantiateSchematic("electrophoreticCrossTest");
    
    NodeValue cross = UtilSchematicConstruction
        .instantiateElectrophoreticCross(schematic);
    NodeValue sampleReservoir = UtilSchematicConstruction
        .instantiateReservoir(schematic);
    NodeValue wasteReservoir = UtilSchematicConstruction
        .instantiateReservoir(schematic);
    NodeValue anodeReservoir = UtilSchematicConstruction
        .instantiateReservoir(schematic);
    NodeValue cathodeReservoir = UtilSchematicConstruction
        .instantiateReservoir(schematic);

    ConnectionValue sampleToCross = UtilSchematicConstruction
        .instantiateChannel(sampleReservoir.getPort("opening"), 
        cross.getPort("sample"));
    ConnectionValue wasteToCross = UtilSchematicConstruction
        .instantiateChannel(sampleReservoir.getPort("opening"), 
        cross.getPort("waste"));
    ConnectionValue anodeToCross = UtilSchematicConstruction
        .instantiateChannel(sampleReservoir.getPort("opening"), 
        cross.getPort("anode"));
    ConnectionValue cathodeToCross = UtilSchematicConstruction
        .instantiateChannel(sampleReservoir.getPort("opening"), 
        cross.getPort("cathode"));

    schematic.addNode("n_cross", cross);
    schematic.addNode("n_sample", sampleReservoir);
    schematic.addNode("n_waste", wasteReservoir);
    schematic.addNode("n_anode", anodeReservoir);
    schematic.addNode("n_cathode", cathodeReservoir);
    schematic.addConnection("c_sample_to_cross", sampleToCross);
    schematic.addConnection("c_waste_to_cross", wasteToCross);
    schematic.addConnection("c_anode_to_cross", anodeToCross);
    schematic.addConnection("c_cathode_to_cross", cathodeToCross);
    
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    Options options = new Options();
    backend.registerArguments(options);
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, args);
    backend.invokeBackend(schematic, cmd);
  }
 
 /*
  @Test
  public void testElectrophoreticSynthesis() throws Exception {
    Schematic schematic = UtilSchematicConstruction.
      instantiateSchematic("test");

    NodeValue electrophoreticNode = UtilSchematicConstruction.
        instantiateElectrophoreticNode(schematic);
    schematic.addNode("n_electrophoretic", electrophoreticNode);
    NodeValue entry = UtilSchematicConstruction.instantiateFluidEntry(
        schematic, 0.001002);
    schematic.addNode("n_entry", entry);
    NodeValue exit = UtilSchematicConstruction.instantiateFluidExit(schematic);
    schematic.addNode("n_exit", exit);
    ConnectionValue entryChannel = UtilSchematicConstruction.
        instantiateChannel(entry.getPort("output"), 
        electrophoreticNode.getPort("sampleIn"));
    schematic.addConnection("c_entry", entryChannel);
    ConnectionValue exitChannel = UtilSchematicConstruction.instantiateChannel(
        electrophoreticNode.getPort("wasteOut"), exit.getPort("input"));
    schematic.addConnection("c_exit", exitChannel);
  }
*/

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
