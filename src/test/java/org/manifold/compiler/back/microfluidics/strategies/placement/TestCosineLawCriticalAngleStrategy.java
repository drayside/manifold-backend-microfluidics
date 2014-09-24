package org.manifold.compiler.back.microfluidics.strategies.placement;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.MicrofluidicsBackend;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.UtilSchematicConstruction;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.middle.Schematic;
import org.manifold.compiler.middle.SchematicException;

public class TestCosineLawCriticalAngleStrategy {

  @Test
  public void testThreeNodes() throws SchematicException {
    // create this schematic:
    // (n1) -- (n2) -- (n3)
    Schematic sch = UtilSchematicConstruction.instantiateSchematic("test");
    NodeValue n1 = UtilSchematicConstruction.instantiatePressureControlPoint(
        sch, 1);
    sch.addNode("n1", n1);
    NodeValue n2 = UtilSchematicConstruction.instantiatePressureControlPoint(
        sch, 2);
    sch.addNode("n2", n2);
    NodeValue n3 = UtilSchematicConstruction.instantiatePressureControlPoint(
        sch, 1);
    sch.addNode("n3", n3);
    ConnectionValue ch0 = UtilSchematicConstruction.instantiateChannel(
        n1.getPort("channel0"), n2.getPort("channel0"));
    sch.addConnection("ch0", ch0);
    ConnectionValue ch1 = UtilSchematicConstruction.instantiateChannel(
        n2.getPort("channel1"), n3.getPort("channel0"));
    sch.addConnection("ch1", ch1);
    
    PrimitiveTypeTable typeTable = MicrofluidicsBackend.constructTypeTable(sch);
    ProcessParameters params = ProcessParameters.loadTestData();
    
    CriticalAngleStrategy strat = new CosineLawCriticalAngleStrategy();
    List<SExpression> exprs = strat.translationStep(sch, params, typeTable);
    if (exprs.size() == 0) {
      fail("no expressions generated in translation");
    } else if (exprs.size() > 1) {
      for (SExpression expr : exprs) {
        System.err.println(expr.toString());
      }
      fail("too many expressions generated in translation");
    }
    // TODO verify expression
  }
  
}
