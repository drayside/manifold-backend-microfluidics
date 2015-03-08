package org.manifold.compiler.back.microfluidics.matlab;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.MicrofluidicsBackend;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.UtilSchematicConstruction;
import org.manifold.compiler.middle.Schematic;
import org.manifold.compiler.middle.SchematicException;

public class TestManifoldValueHelper {

  private void showStatements(List<MatlabStatement> stmts) {
    for (MatlabStatement stmt: stmts) {
      System.out.println(stmt.toString());
    }
  }
  
  @Test
  public void test() throws SchematicException {
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
    
    List<MatlabStatement> stmts1 = ManifoldValueHelper.statementsForValue(n1);
    List<MatlabStatement> stmts2 = ManifoldValueHelper.statementsForValue(n2);
    List<MatlabStatement> stmts3 = ManifoldValueHelper.statementsForValue(ch0);
    
    showStatements(stmts1);
    showStatements(stmts2);
    showStatements(stmts3);

    StrategyVerifier verifier = new StrategyVerifier();
    verifier.addStatements(stmts1);
    verifier.addStatements(stmts2);
    verifier.addStatements(stmts3);
    
    System.out.println("\n");
    System.out.println(verifier.writeStatements());
  }

}
