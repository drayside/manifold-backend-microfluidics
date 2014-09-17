package org.manifold.compiler.back.microfluidics.strategies.placement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.MicrofluidicsBackend;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.UtilSchematicConstruction;
import org.manifold.compiler.back.microfluidics.smt2.ExprEvalVisitor;
import org.manifold.compiler.back.microfluidics.smt2.ParenList;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;
import org.manifold.compiler.middle.SchematicException;

public class TestPythagoreanLengthRuleStrategy {
  
  // find e1 matching (assert e1) where e1 matches (= eA eB)
  private ParenList findAssertEqual(SExpression assertion) {
    if (!(assertion instanceof ParenList)) {
      fail("expression not an assertion (not a list): " + assertion.toString());
    }
    if (((ParenList) assertion).getExprs().size() != 2) {
      fail("expression not an assertion (wrong # of args): " 
          + assertion.toString());
    }
    SExpression assertSym = ((ParenList) assertion).getExprs().get(0);
    if (!(assertSym instanceof Symbol)) {
      fail("expression not an assertion "
          + " (first argument not a symbol): " 
          + assertion.toString());
    }
    if (!((Symbol) assertSym).equals(new Symbol("assert"))) {
      fail("expression not an assertion "
          + " (first argument bad symbol): " 
          + assertion.toString());
    }
    SExpression e1 = ((ParenList) assertion).getExprs().get(1);
    if (!(e1 instanceof ParenList)) {
      fail("expression not an assertion (second argument not a list): " 
          + e1.toString());
    }
    if (((ParenList) e1).getExprs().size() != 3) {
      fail("expression not an equality statement (wrong # of args): " 
          + e1.toString());
    }
    SExpression equalSym = ((ParenList) e1).getExprs().get(0);
    if (!(equalSym instanceof Symbol)) {
      fail("expression not an equality statement"
          + " (first argument not a symbol): " + e1.toString());
    }
    if (!((Symbol) equalSym).equals(new Symbol("="))) {
      fail("expression not an equality statement"
          + " (first argument bad symbol): " + e1.toString());
    }
    return (ParenList) e1;
  }
  
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
    
    PrimitiveTypeTable typeTable = MicrofluidicsBackend.constructTypeTable(sch);
    
    PythagoreanLengthRuleStrategy strat = new PythagoreanLengthRuleStrategy();
    List<SExpression> exprs = strat.translationStep(sch, typeTable);
    if (exprs.size() == 0) {
      fail("no expressions generated in translation");
    } else if (exprs.size() > 1) {
      for (SExpression expr : exprs) {
        System.err.println(expr.toString());
      }
      fail("too many expressions generated in translation");
    }
    
    SExpression exprLeft = findAssertEqual(exprs.get(0)).getExprs().get(1);
    SExpression exprRight = findAssertEqual(exprs.get(0)).getExprs().get(2);
    
    // now we need to find symbols corresponding to the node x and y position
    // as well as the connection length
    Symbol n1x = SymbolNameGenerator.getsym_NodeX(sch, n1);
    Symbol n1y = SymbolNameGenerator.getsym_NodeY(sch, n1);
    Symbol n2x = SymbolNameGenerator.getsym_NodeX(sch, n2);
    Symbol n2y = SymbolNameGenerator.getsym_NodeY(sch, n2);
    Symbol ch0Len = SymbolNameGenerator.getsym_ChannelLength(sch, ch0);
    
    // bind these symbols so as to make a right triangle
    
    double originX = 12.3;
    double originY = -4.56;
    double deltaX = -3.0;
    double deltaY = 4.0;
    double lengthHypotenuse = 5.0;
    ExprEvalVisitor eval = new ExprEvalVisitor();
    eval.addBinding(n1x, originX);
    eval.addBinding(n1y, originY);
    eval.addBinding(n2x, originX + deltaX);
    eval.addBinding(n2y, originY + deltaY);
    eval.addBinding(ch0Len, lengthHypotenuse);
    
    exprLeft.accept(eval);
    double lhs = eval.getValue();
    exprRight.accept(eval);
    double rhs = eval.getValue();
    assertEquals(rhs, lhs, 0.000001);
    
  }

}
