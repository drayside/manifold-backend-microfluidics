package org.manifold.compiler.back.microfluidics.modelica.maple;

import com.maplesoft.openmaple.Algebraic;
import org.junit.Assert;
import org.junit.Test;

public class TestOpenMapleExecutor {
  @Test
  public void testSimpleExecute() {
    OpenMapleExecutor executor = new OpenMapleExecutor();
    executor.writeLine("x + x;");
    Algebraic a = executor.execute();
    Assert.assertEquals("2*x", a.toString());
  }
}
