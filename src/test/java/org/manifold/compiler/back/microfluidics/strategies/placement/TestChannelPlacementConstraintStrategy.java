package org.manifold.compiler.back.microfluidics.strategies.placement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.ConstraintValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.MicrofluidicsBackend;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.UtilSExpression;
import org.manifold.compiler.back.microfluidics.UtilSchematicConstruction;
import org.manifold.compiler.back.microfluidics.smt2.ExprEvalVisitor;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;
import org.manifold.compiler.middle.SchematicException;

public class TestChannelPlacementConstraintStrategy {
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
    
    ChannelPlacementConstraintStrategy strat = 
        new ChannelPlacementConstraintStrategy();
    List<SExpression> exprs = strat.translationStep(sch, typeTable);
    if (exprs.size() == 0) {
      fail("no expressions generated in translation");
    } else if (exprs.size() > 1) {
      for (SExpression expr : exprs) {
        System.err.println(expr.toString());
      }
      fail("too many expressions generated in translation");
    }
    
    SExpression exprLeft = UtilSExpression
        .findAssertEqual(exprs.get(0)).getExprs().get(1);
    SExpression exprRight = UtilSExpression
        .findAssertEqual(exprs.get(0)).getExprs().get(2);
    
    // now we need to find symbols corresponding to the node x and y position
    Symbol n1x = SymbolNameGenerator.getsym_NodeX(sch, n1);
    Symbol n1y = SymbolNameGenerator.getsym_NodeY(sch, n1);
    Symbol n2x = SymbolNameGenerator.getsym_NodeX(sch, n2);
    Symbol n2y = SymbolNameGenerator.getsym_NodeY(sch, n2);
    
    // bind these symbols to specific coordinates such that
    // the constrained point falls on the line between them
    
    double x1 = -5.0;
    double y1 = 2.0;
    double x2 = 3.0;
    double y2 = -2.0;
    ExprEvalVisitor eval = new ExprEvalVisitor();
    eval.addBinding(n1x, x1);
    eval.addBinding(n1y, y1);
    eval.addBinding(n2x, x2);
    eval.addBinding(n2y, y2);
    
    exprLeft.accept(eval);
    double lhs = eval.getValue();
    exprRight.accept(eval);
    double rhs = eval.getValue();
    assertEquals(rhs, lhs, 0.000001);
  }
}
