package org.manifold.compiler.back.microfluidics.smt2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DRealSolver implements AutoCloseable {

  class RealRange {
    public final double lowerBound;
    public final double upperBound;
    
    public RealRange(double lower, double upper) {
      this.lowerBound = lower;
      this.upperBound = upper;
    }
  }
  
  class Result {
    private final boolean satisfiable;
    public boolean isSatisfiable() {
      return this.satisfiable;
    }
    
    private Map<Symbol, RealRange> ranges;
    public RealRange getRange(Symbol sym) {
      return ranges.get(sym);
    }
    public void addResult(String symbol, String lowerBound, String upperBound) {
      Symbol sym = new Symbol(symbol);
      double lb = Double.parseDouble(lowerBound);
      double ub = Double.parseDouble(upperBound);
      RealRange range = new RealRange(lb, ub);
      ranges.put(sym, range);
    }
    
    public Result(boolean satisfiable) {
      this.satisfiable = satisfiable;
      this.ranges = new HashMap<>();
    }
  }
  
  private static String pathToDReal = null;
  
  private void findDReal() {
    Map<String, String> env = System.getenv();
    if (env.containsKey("PATH")) {
      String[] paths = env.get("PATH").split(":");
      for (String path : paths) {
        File dReal = new File(path + "/dReal");
        if (dReal.exists() && !dReal.isDirectory() && dReal.canExecute()) {
          pathToDReal = path + "/dReal";
          break;
        }
      }
    } else {
      File dReal = new File("./dReal");
      if (dReal.exists() && !dReal.isDirectory() && dReal.canExecute()) {
        pathToDReal = "./dReal";
      }
    }
    if (pathToDReal == null) {
      throw new IllegalStateException("cannot find dReal executable");
    }
  }
  
  public DRealSolver() {
    if (pathToDReal == null) {
      findDReal();
    }
  }
 
  private Process dRealProcess = null;
  private BufferedWriter writer;
  private BufferedReader reader;
  
  public void open() throws IOException {
    List<String> command = new LinkedList<>();
    command.add(pathToDReal);
    ProcessBuilder builder = new ProcessBuilder(command);
    builder.redirectErrorStream(true);
    dRealProcess = builder.start();
    
    OutputStream os = dRealProcess.getOutputStream();
    OutputStreamWriter osw = new OutputStreamWriter(os);
    writer = new BufferedWriter(osw);
    
    InputStream is = dRealProcess.getInputStream();
    InputStreamReader isr = new InputStreamReader(is);
    reader = new BufferedReader(isr);
    
    write("(set-logic QF_NRA)");
  }
  
  public void write(String data) throws IOException {
    if (dRealProcess == null) {
      throw new IllegalStateException("dReal session has not been opened");
    }
    writer.write(data);
    writer.newLine();
  }
  
  protected static final String regexFloatingPointConstant =
      "[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?";
  
  protected static final Pattern regexSolutionLine =
      Pattern.compile("^\\s*(\\S+)\\s*:\\s*\\[.*\\]\\s*=\\s*\\[\\s*("
          + regexFloatingPointConstant + 
          ")\\s*,\\s*("
          + regexFloatingPointConstant +
          ")\\s*\\]\\s*$");
  
  public Result solve() throws IOException {
    write("(check-sat)");
    write("(exit)");
    writer.flush();
    writer.close();
    String result = reader.readLine();
    if(result.equals("unsat")) {
      return new Result(false);
    } else if (result.startsWith("Solution:")) {
      Result model = new Result(true);
      // parse lines until we see "sat"
      result = reader.readLine();
      while (!(result.equals("sat"))) {
        // expect a line of the form
        // x : [ ****** ] = [999.99, 999.99]
        Matcher mResult = regexSolutionLine.matcher(result);
        if (mResult.matches()) {
          String symbolName = mResult.group(1);
          String lowerBound = mResult.group(2);
          String upperBound = mResult.group(3);
          model.addResult(symbolName, lowerBound, upperBound);
        } else {
          throw new RuntimeException("result line has unexpected format: " 
              + result);
        }
      }
      return model;
    } else {
      throw new RuntimeException("dReal encountered an error: " + result);
    }
  }
  
  @Override
  public void close() {
    if (dRealProcess != null) {
      dRealProcess.destroyForcibly();
    }
  }
  
}
