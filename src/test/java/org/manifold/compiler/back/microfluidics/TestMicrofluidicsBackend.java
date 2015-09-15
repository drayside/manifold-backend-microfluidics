package org.manifold.compiler.back.microfluidics;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

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
import org.manifold.compiler.RealValue;
import org.manifold.compiler.Value;
import org.manifold.compiler.back.microfluidics.smt2.DRealSolver;
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
  
 //COMMENTING THESE OUT UNTIL CIRCULAR CHANNELS IS REFACTORED CORRECTLY 
 
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
    
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    Options options = new Options();
    backend.registerArguments(options);
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, args);
    backend.invokeBackend(schematic, cmd);
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
    
    
    ConnectionValue entryToJunction = UtilSchematicConstruction.instantiateChannel(
        entry.getPort("output"), junction.getPort("continuous"));
    schematic.addConnection("channelC", entryToJunction);
    ConnectionValue disperseToJunction = UtilSchematicConstruction.instantiateChannel(
        disperse.getPort("output"), junction.getPort("dispersed"));
    schematic.addConnection("channelD", disperseToJunction);
    ConnectionValue junctionToExit = UtilSchematicConstruction.instantiateChannel(
        junction.getPort("output"), exit.getPort("input"));
    schematic.addConnection("channelE", junctionToExit);
    
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    Options options = new Options();
    backend.registerArguments(options);
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, args);
    backend.invokeBackend(schematic, cmd);
  }


//  @Test
//  public void TestReverseInference() throws Exception{
//	    String[] args = {
//	    	      "-bProcessMinimumNodeDistance", "0.000001",
//	    	      "-bProcessMinimumChannelLength", "0.000001",
//	    	      "-bProcessMaximumChipSizeX", "0.04",
//	    	      "-bProcessMaximumChipSizeY", "0.04",
//	    	      "-bProcessCriticalCrossingAngle", "0.0872664626"
//	    	    };
//	    		// TODO Refactor as inputs somehow
//	    	    double inputpressure = 94.0;
//	    	    double outputpressure = 7899.0;
//	    		
//	    	    Schematic schematic = UtilSchematicConstruction
//	    	        .instantiateSchematic("testSimpleSynthesis");
//	    	    
//	    	    // Make a very simple schematic:
//	    	    // (fluidEntry) ---> (fluidExit)
//	    	    NodeValue entry = UtilSchematicConstruction.instantiateFluidEntry(
//	    	        schematic, viscosityOfWater,inputpressure);
//	    	    schematic.addNode("in0", entry);
//	    	    NodeValue exit = UtilSchematicConstruction.instantiateFluidExit(schematic,outputpressure);
//	    	    schematic.addNode("out0", exit);
//	    	    /*attributes for channel of pipe model*/
//	    	    Map<String, Value> attrsMap=new HashMap<>();
//	    	    RealValue length = new RealValue(0.000020);
//	    	    RealValue radius = new RealValue(0.000001);
//	    	    //RealValue inputpressure = new RealValue(94.0);
//	    	    //RealValue outputpressure = new RealValue(7899.0);
//	    	    attrsMap.put("length", length);
//	    	    attrsMap.put("radius", radius);
//	    	    //attrsMap.put("inputpressure", inputpressure);
//	    	    //attrsMap.put("outputpressure", outputpressure);
//	    	    
//	    	    ConnectionValue entryToExit = UtilSchematicConstruction.instantiateChannel(
//	    	        entry.getPort("output"), exit.getPort("input"),attrsMap);
//	    	    schematic.addConnection("channel0", entryToExit);
//	    	    
//	    	    MicrofluidicsBackend backend = new MicrofluidicsBackend();
//	    	    Options options = new Options();
//	    	    backend.registerArguments(options);
//	    	    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
//	    	    CommandLine cmd = parser.parse(options, args);
//	    	    backend.invokeBackend(schematic, cmd);
//	    	    //DRealSolver.Result res = backend.invokeBackend(schematic, cmd, "stdin");
//		    //assertTrue(res.isSatisfiable());
//  }
  
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
