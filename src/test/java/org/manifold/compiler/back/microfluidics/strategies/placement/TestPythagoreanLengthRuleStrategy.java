package org.manifold.compiler.back.microfluidics.strategies.placement;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.MicrofluidicsBackend;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.UtilSchematicConstruction;
import org.manifold.compiler.back.microfluidics.matlab.CompoundStrategyVerifier;
import org.manifold.compiler.back.microfluidics.matlab.StrategyVerifier;
import org.manifold.compiler.back.microfluidics.smt2.AssertionChecker;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;
import org.manifold.compiler.middle.SchematicException;

public class TestPythagoreanLengthRuleStrategy {
  
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
    AssertionChecker check = new AssertionChecker();
    check.addBinding(n1x, originX);
    check.addBinding(n1y, originY);
    check.addBinding(n2x, originX + deltaX);
    check.addBinding(n2y, originY + deltaY);
    check.addBinding(ch0Len, lengthHypotenuse);
    
    if (!check.verify(exprs)) {
      fail("assertion failed: " + check.getLastExpression().toString());
    }

    sch = UtilSchematicConstruction.instantiateSchematic("test_matlab");
    NodeValue n3 = UtilSchematicConstruction.instantiatePressureControlPoint(
        sch, 2);
    sch.addNode("n3", n3);
    NodeValue n4 = UtilSchematicConstruction.instantiatePressureControlPoint(
        sch, 1);
    sch.addNode("n4", n4);
    NodeValue n5 = UtilSchematicConstruction.instantiatePressureControlPoint(
        sch, 1);
    sch.addNode("n5", n5);
    ConnectionValue ch1 = UtilSchematicConstruction.instantiateChannel(
        n3.getPort("channel0"), n4.getPort("channel0"));
    sch.addConnection("ch1", ch1);
    ConnectionValue ch2 = UtilSchematicConstruction.instantiateChannel(
        n3.getPort("channel1"), n5.getPort("channel0"));
    sch.addConnection("ch2", ch2);
    
    List<StrategyVerifier> verifiers = strat.matlabTranslationStep(sch, null, typeTable);
    CompoundStrategyVerifier compounded = new CompoundStrategyVerifier();
    compounded.addVerifiers(verifiers);

    System.out.println(compounded.writeStatements());
  }

}
