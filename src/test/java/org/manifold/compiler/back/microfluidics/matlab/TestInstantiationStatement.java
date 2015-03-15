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
    List<String> paramOrder = new LinkedList<String>();
    paramOrder.add("x");
    paramOrder.add("y");

    InstantiationStatement stmt =
        new InstantiationStatement("n1", "Node", paramOrder);
    
    assertEquals("n1", stmt.getVariableName());
    assertTrue(stmt.getIsTemplate());
    assertEquals("n1 = Node({x},{y});", stmt.toString());
    
    Map<String, String> paramValues = new HashMap<String, String>();
    paramValues.put("x", "5");
    paramValues.put("y", "15");
    
    stmt.fillTemplate(paramValues);
    
    assertFalse(stmt.getIsTemplate());
    assertEquals("n1 = Node(5,15);", stmt.toString());
    
    AssignmentStatement assignment = new AssignmentStatement("n1", "Node(1, 2)");
    assertEquals("Node(1, 2);", assignment.getRhsString());
    assertEquals("n1 = Node(1, 2);", assignment.toString());
  }

}
