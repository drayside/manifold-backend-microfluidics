package org.manifold.compiler.back.microfluidics.smt2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class TestDRealSolver {

  @Test
  public void testFindDReal() {
    try (DRealSolver dReal = new DRealSolver()) { }
  }
  
  @Test
  public void testResultUnsat() throws IOException {
    try (DRealSolver dReal = new DRealSolver()) {
      dReal.open();
      dReal.write("(declare-fun x () Real)");
      dReal.write("(assert (= x 1.0))");
      dReal.write("(assert (= x 2.0))");
      DRealSolver.Result res = dReal.solve();
      assertFalse(res.isSatisfiable());
    }
  }
  
  @Test
  public void testResultSat() throws IOException {
    try (DRealSolver dReal = new DRealSolver()) {
      dReal.open();
      dReal.write("(declare-fun x () Real)");
      dReal.write("(assert (= x 1.0))");
      DRealSolver.Result res = dReal.solve();
      assertTrue(res.isSatisfiable());
    }
  }
  
  @Test
  public void testResultRange() throws IOException {
    try (DRealSolver dReal = new DRealSolver()) {
      dReal.open();
      dReal.write("(declare-fun x () Real)");
      dReal.write("(assert (= x 1.0))");
      DRealSolver.Result res = dReal.solve();
      assertTrue(res.isSatisfiable());
      // now actually get the range of x
      // should be [1, 1]
      DRealSolver.RealRange range = res.getRange(new Symbol("x"));
      assertNotNull(range);
      assertEquals(1.0, range.lowerBound, 0.001);
      assertEquals(1.0, range.upperBound, 0.001);
    }
  }
  
}
