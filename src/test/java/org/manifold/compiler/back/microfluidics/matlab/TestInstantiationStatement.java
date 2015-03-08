package org.manifold.compiler.back.microfluidics.matlab;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TestInstantiationStatement {

  @Test
  public void test() {
    List<String> params = new LinkedList<String>();
    params.add("x");
    params.add("y");

    InstantiationStatement stmt =
        new InstantiationStatement("n1", "Node", params);
    
    assertEquals("n1", stmt.getVariableName());
    assertTrue(stmt.getIsTemplate());
    assertEquals("Node({x},{y});", stmt.getExpression());
    
    Map<String, String> paramValues = new HashMap<String, String>();
    paramValues.put("x", "5");
    paramValues.put("y", "15");
    
    stmt.fillTemplate(paramValues);
    
    assertFalse(stmt.getIsTemplate());
    assertEquals("Node(5,15);", stmt.getExpression());
    
    stmt = new InstantiationStatement("n1", "Node(1, 2);");
    assertEquals("Node(1, 2);", stmt.getExpression());
    
    stmt = new InstantiationStatement("sum", "15");
    assertEquals("15", stmt.getExpression());
    assertEquals("sum = 15;", stmt.writeStatement());
  }

}
