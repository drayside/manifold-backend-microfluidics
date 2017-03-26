package org.manifold.compiler.back.microfluidics;


import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.InferredValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.Value;
import org.manifold.compiler.back.microfluidics.smt2.DRealSolver;
import org.manifold.compiler.middle.Schematic;

import static org.manifold.compiler.back.microfluidics.TestMicrofluidicsBackend.viscosityOfWater;

public class TestInferredAttributeAdder {

  @BeforeClass
  public static void setupClass() {
    UtilSchematicConstruction.setupIntermediateTypes();
  }

  @Test
  public void testPopulationFromDrealResult() throws Exception {

    // Simple schematic with fluidEntry, fluidExit, and channel
    Schematic schematic = UtilSchematicConstruction
      .instantiateSchematic("testSimpleSynthesis");
    NodeValue entry = UtilSchematicConstruction.instantiateFluidEntry(
      schematic, viscosityOfWater);
    schematic.addNode("in0", entry);
    NodeValue exit = UtilSchematicConstruction.instantiateFluidExit(schematic);
    schematic.addNode("out0", exit);
    ConnectionValue entryToExit = UtilSchematicConstruction.instantiateChannel(
      entry.getPort("output"), exit.getPort("input"));
    schematic.addConnection("channel0", entryToExit);

    // Fake dReal result with fake ranges.
    DRealSolver.Result drealResult = new DRealSolver().new Result(true);
    drealResult.addResult("channel0.flowrate", "-0.5", "0.5");
    drealResult.addResult("channel0.viscosity", "0.3", "0.6");
    drealResult.addResult("in0.pos_x", "0.0", "0.04");
    drealResult.addResult("out0.input.pressure", "10000", "20000");

    schematic = InferredAttributeAdder.populateFromDrealResults(
      schematic, drealResult);

    // TODO: We may want convenience methods for unwrapping InferValues. Most
    //       notably, a method that will delegate to the underlying value if
    //       it is assigned
    Value flowRateValue = ((InferredValue) schematic.getConnection("channel0")
            .getAttribute("flowrate")).get();
    double flowRate = Double.parseDouble(flowRateValue.toString());
    Assert.assertTrue(flowRate >= -0.5 || flowRate <= 0.5);

    Value viscosityValue = ((InferredValue) schematic.getConnection("channel0")
            .getAttribute("viscosity")).get();
    double viscosity = Double.parseDouble(viscosityValue.toString());
    Assert.assertTrue(viscosity >= 0.3 && viscosity <= 0.6);

    Value posXValue = ((InferredValue) schematic.getNode("in0")
            .getAttribute("pos_x")).get();
    double posX = Double.parseDouble(posXValue.toString());
    Assert.assertTrue(posX >= 0 && posX <= 0.04);

    Value pressureValue = ((InferredValue) schematic.getNode("out0")
            .getPort("input").getAttribute("pressure")).get();
    double pressure = Double.parseDouble(pressureValue.toString());
    Assert.assertTrue(pressure >= 10000 && pressure <= 20000);
  }

}
