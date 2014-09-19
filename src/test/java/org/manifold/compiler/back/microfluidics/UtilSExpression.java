package org.manifold.compiler.back.microfluidics;

import static org.junit.Assert.fail;

import org.manifold.compiler.back.microfluidics.smt2.ParenList;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;

public class UtilSExpression {

  // find e1 matching (assert e1) where e1 matches (= eA eB)
  public static ParenList findAssertEqual(SExpression assertion) {
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
  
}
