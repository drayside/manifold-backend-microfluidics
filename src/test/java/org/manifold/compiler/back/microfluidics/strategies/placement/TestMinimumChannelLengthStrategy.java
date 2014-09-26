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
import org.manifold.compiler.back.microfluidics.smt2.AssertionChecker;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;
import org.manifold.compiler.middle.SchematicException;

public class TestMinimumChannelLengthStrategy {
  @Test
  public void testViolation_ZeroLength() throws SchematicException {
 // create this schematic:
    // (n1) --- (n2)
    Schematic sch = UtilSchematicConstruction.instantiateSchematic("test");
    NodeValue n1 = UtilSchematicConstruction.instantiatePressureControlPoint(
        sch, 1);
    sch.addNode("n1", n1);
    NodeValue n2 = UtilSchematicConstruction.instantiatePressureControlPoint(
        sch, 1);
    sch.addNode("n2", n2);
    ConnectionValue ch0 = UtilSchematicConstruction.instantiateChannel(
        n1.getPort("channel0"), n2.getPort("channel0"));
    sch.addConnection("ch0", ch0);
    
    ProcessParameters params = ProcessParameters.loadTestData();
    PrimitiveTypeTable typeTable = MicrofluidicsBackend.constructTypeTable(sch);
    
    MinimumChannelLengthStrategy strat = new MinimumChannelLengthStrategy();
    List<SExpression> exprs = strat.translationStep(sch, params, typeTable);
    
    AssertionChecker check = new AssertionChecker();
    check.addBinding(SymbolNameGenerator.getsym_ChannelLength(sch, ch0), 0.0);
    
    if (check.verify(exprs)) {
      fail("failed to detect rule violation");
    }
  }
}
