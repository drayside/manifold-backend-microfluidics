package org.manifold.compiler.back.microfluidics.smt2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

public class TestDRealSolver {

  @Test
  public void testFindDReal() {
    try (DRealSolver dReal = new DRealSolver()) { }
    catch (IllegalStateException e) {
      fail("dReal not found. Make sure dReal is installed and accessible.");
    }
  }
  
  @Test
  public void testResultUnsat() throws IOException {
    try (DRealSolver dReal = new DRealSolver()) {
      dReal.open();
      dReal.write("(set-logic QF_NRA)");
      dReal.write("(declare-fun x () Real)");
      dReal.write("(assert (= x 1.0))");
      dReal.write("(assert (= x 2.0))");
      dReal.write("(check-sat)");
      dReal.write("(exit)");
      DRealSolver.Result res = dReal.solve();
      assertFalse(res.isSatisfiable());
    } catch (IllegalStateException e) {
      fail("dReal not found. Make sure dReal is installed and accessible.");
    }
  }
  
  @Test
  public void testResultSat() throws IOException {
    try (DRealSolver dReal = new DRealSolver()) {
      dReal.open();
      dReal.write("(set-logic QF_NRA)");
      dReal.write("(declare-fun x () Real)");
      dReal.write("(assert (= x 1.0))");
      dReal.write("(check-sat)");
      dReal.write("(exit)");
      DRealSolver.Result res = dReal.solve();
      assertTrue(res.isSatisfiable());
    } catch (IllegalStateException e) {
      fail("dReal not found. Make sure dReal is installed and accessible.");
    }
  }
  
  @Test
  public void testResultRange() throws IOException {
    try (DRealSolver dReal = new DRealSolver()) {
      dReal.open();
      dReal.write("(set-logic QF_NRA)");
      dReal.write("(declare-fun x () Real)");
      dReal.write("(assert (= x 1.0))");
      dReal.write("(check-sat)");
      dReal.write("(exit)");
      DRealSolver.Result res = dReal.solve();
      assertTrue(res.isSatisfiable());
      // now actually get the range of x
      // should be [1, 1]
      DRealSolver.RealRange range = res.getRange(new Symbol("x"));
      assertNotNull(range);
      assertEquals(1.0, range.lowerBound, 0.001);
      assertEquals(1.0, range.upperBound, 0.001);
    } catch (IllegalStateException e) {
      fail("dReal not found. Make sure dReal is installed and accessible.");
    }
  }
  
}
