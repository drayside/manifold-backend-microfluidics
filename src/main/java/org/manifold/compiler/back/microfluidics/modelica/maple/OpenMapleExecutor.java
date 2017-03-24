package org.manifold.compiler.back.microfluidics.modelica.maple;

import com.maplesoft.openmaple.*;
import com.maplesoft.externalcall.MapleException;

public class OpenMapleExecutor {
  static private Engine mapleEngine;
  private StringBuilder sb;

  static {
    String a[];
    a = new String[1];
    a[0] = "java";
    try {
      mapleEngine = new Engine(a, new EngineCallBacksDefault(), null, null);
    } catch (MapleException e) {
      System.err.println(e.toString());
      throw new RuntimeException(e);
    }
  }

  public OpenMapleExecutor() throws RuntimeException {
    sb = new StringBuilder();
  }

  public void writeLine(String code) {
    sb.append(code);
    sb.append('\n');
  }

  public Algebraic execute() throws RuntimeException {
    Algebraic result;
    try {
      result = mapleEngine.evaluate(sb.toString());
      sb.setLength(0);
    } catch (MapleException e) {
      System.err.println(e.toString());
      throw new RuntimeException(e);
    }
    return result;
  }
}
