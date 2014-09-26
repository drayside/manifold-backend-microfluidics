package org.manifold.compiler.back.microfluidics.strategies.pressureflow;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.ConstraintValue;
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

public class TestAnalyticalPressureFlowStrategy {

  @Test
  public void testTwoNodes() throws SchematicException {
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
    double chanInterceptX = 1.0;
    double chanInterceptY = -1.0;
    ConstraintValue cxtChanPlace = UtilSchematicConstruction
        .instantiateChannelPlacementConstraint(
            ch0, chanInterceptX, chanInterceptY);
    sch.addConstraint("cxt0", cxtChanPlace);
    
    PrimitiveTypeTable typeTable = MicrofluidicsBackend.constructTypeTable(sch);
    ProcessParameters processParams = ProcessParameters.loadTestData();
    
    AnalyticalPressureFlowStrategy strat = new AnalyticalPressureFlowStrategy();
    List<SExpression> exprs = strat.translationStep(
        sch, processParams, typeTable);
    
    AssertionChecker check = new AssertionChecker();
    // we bind the pressure at the channel0 port of both nodes,
    // the channel flow, and the channel resistivity
    // eventually we want to verify (7.2 - 4.6) = 52 * 0.05
    check.addBinding(SymbolNameGenerator.getSym_PortPressure(
        sch, n1.getPort("channel0")), 7.2);
    check.addBinding(SymbolNameGenerator.getSym_PortPressure(
        sch, n2.getPort("channel0")), 4.6);
    check.addBinding(SymbolNameGenerator.getsym_ChannelFlowRate(
        sch, ch0), 52.0);
    check.addBinding(SymbolNameGenerator.getsym_ChannelResistance(
        sch, ch0), 0.05);
    
    if (!check.verify(exprs)) {
      fail("assertion failed: " + check.getLastExpression().toString() +
          " LHS = " + Double.toString(check.getLastLHS()) + " RHS = " +
          Double.toString(check.getLastRHS()));
    }
  }
  
}
