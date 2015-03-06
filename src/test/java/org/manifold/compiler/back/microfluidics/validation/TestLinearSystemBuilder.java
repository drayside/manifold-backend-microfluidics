package org.manifold.compiler.back.microfluidics.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class TestLinearSystemBuilder {
  
  @Test
  public void testTwoVariableSystemGeneration() {
    // 2x - 3y = -2
    // 4x + y = 24
    // has the solution x=5, y=4
    LinearSystemBuilder builder = new LinearSystemBuilder();
    Variable x = new Variable("x");
    Variable y = new Variable("y");
    builder.addEquation(new LinearTerm[]{
        new LinearTerm(new Constant(2.0), x),
        new LinearTerm(new Constant(-3.0), y),
    }, new Constant(-2.0));
    builder.addEquation(new LinearTerm[]{
        new LinearTerm(new Constant(4.0), x),
        new LinearTerm(new Constant(1.0), y),
    }, new Constant(24.0));
    Variable X = new Variable("X");
    String eqn = builder.build(X);
    assertNotNull(eqn);
    assertFalse(eqn.isEmpty());
    System.err.println(eqn);
  }
  
}
