package org.manifold.compiler.back.microfluidics.strategies.placement;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
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

public class TestFiniteChipAreaRuleStrategy {

  @Test
  public void testViolation_OutOfBounds() throws SchematicException {
    // create a single node and bind it outside the device area
    Schematic sch = UtilSchematicConstruction.instantiateSchematic("test");
    NodeValue n1 = UtilSchematicConstruction.instantiatePressureControlPoint(
        sch, 1);
    sch.addNode("n1", n1);
    
    PrimitiveTypeTable typeTable = MicrofluidicsBackend.constructTypeTable(sch);
    ProcessParameters params = ProcessParameters.loadTestData();
    
    ChipAreaRuleStrategy strat = new FiniteChipAreaRuleStrategy();
    List<SExpression> exprs = strat.translate(sch, params, typeTable);
    
    AssertionChecker check = new AssertionChecker();
    check.addBinding(SymbolNameGenerator.getsym_NodeX(
        sch, n1), params.getMaximumChipSizeX() + 1.0);
    check.addBinding(SymbolNameGenerator.getsym_NodeY(
        sch, n1), -1.0);
    if (check.verify(exprs)) {
      fail("rule violation not detected");
    }
  }
  
}
