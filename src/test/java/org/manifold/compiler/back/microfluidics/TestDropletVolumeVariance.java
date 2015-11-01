package org.manifold.compiler.back.microfluidics;

import java.util.LinkedList;
import java.util.List;

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
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.strategies.multiphase.TJunctionDeviceStrategy;
import org.manifold.compiler.back.microfluidics.strategies.pressureflow.ChannelResistanceStrategy;
import org.manifold.compiler.back.microfluidics.strategies.pressureflow.FluidEntryExitDeviceStrategy;
import org.manifold.compiler.back.microfluidics.strategies.pressureflow.SimplePressureFlowStrategy;
import org.manifold.compiler.middle.Schematic;

public class TestDropletVolumeVariance {

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
  public void testVolumeVariance_defaults() throws Exception {
    String[] args = {
      "-bProcessMinimumNodeDistance", "0.0001",
      "-bProcessMinimumChannelLength", "0.0001",
      "-bProcessMaximumChipSizeX", "0.10",
      "-bProcessMaximumChipSizeY", "0.10",
      "-bProcessCriticalCrossingAngle", "0.0872664626"
    };
    
    Schematic schematic = UtilSchematicConstruction
        .instantiateSchematic("testDropletVolumeVariance");
    
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
    
    // doing this by hand temporarily
    MicrofluidicsBackend backend = new MicrofluidicsBackend();
    Options options = new Options();
    backend.registerArguments(options);
    CommandLineParser parser = new org.apache.commons.cli.BasicParser();
    CommandLine cmd = parser.parse(options, args);
    ProcessParameters processParams = ProcessParameters.loadFromCommandLine(cmd);
    PrimitiveTypeTable typeTable = MicrofluidicsBackend.constructTypeTable(schematic);
    
    List<SExpression> exprs = new LinkedList<>();
    
    // do calculate derived quantities, but skip worst-case analysis as we have something else in mind
    ChannelResistanceStrategy chanResStrat = new ChannelResistanceStrategy();
    FluidEntryExitDeviceStrategy entryExitStrat = new FluidEntryExitDeviceStrategy();
    TJunctionDeviceStrategy tJunctionStrat = new TJunctionDeviceStrategy(true, false);
    SimplePressureFlowStrategy flowStrat = new SimplePressureFlowStrategy(false);
    
    exprs.addAll(chanResStrat.translate(schematic, processParams, typeTable));
    exprs.addAll(entryExitStrat.translate(schematic, processParams, typeTable));
    exprs.addAll(flowStrat.translate(schematic, processParams, typeTable));
    exprs.addAll(tJunctionStrat.translate(schematic, processParams, typeTable));
    
    for (SExpression expr : backend.sortExprs(exprs)) {
      System.err.println(expr.toString());
    }
    
  }
  
}
