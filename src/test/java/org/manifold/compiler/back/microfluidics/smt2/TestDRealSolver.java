package org.manifold.compiler.back.microfluidics.smt2;

import static org.junit.Assert.assertFalse;

import java.io.IOException;

import org.junit.Test;

public class TestDRealSolver {

  @Test
  public void testFindDReal() {
    try(DRealSolver dReal = new DRealSolver()) {}
  }
  
  @Test
  public void testResultUnsat() throws IOException {
    try(DRealSolver dReal = new DRealSolver()) {
      dReal.open();
      dReal.write("(declare-fun x () Real)");
      dReal.write("(assert (= x 1.0))");
      dReal.write("(assert (= x 2.0))");
      DRealSolver.Result res = dReal.solve();
      assertFalse(res.isSatisfiable());
    }
  }
  
}
