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

public class DRealSolver implements AutoCloseable {

  class RealRange {
    public final double lowerBound;
    public final double upperBound;
    
    public RealRange(double lower, double upper) {
      this.lowerBound = lower;
      this.upperBound = upper;
    }
  }
  
  public class Result {
    private final boolean satisfiable;
    public boolean isSatisfiable() {
      return this.satisfiable;
    }
    
    private Map<Symbol, RealRange> ranges;
    public RealRange getRange(Symbol sym) {
      return ranges.get(sym);
    }
    public void addResult(String symbolName, 
        String lowerBound, String upperBound) {
      Symbol sym = new Symbol(symbolName);
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
  
  public BufferedWriter getWriter(){
	  return writer;
  }
  
  public void open() throws IOException {
    List<String> command = new LinkedList<>();
    command.add(pathToDReal);
    command.add("--in");
    command.add("--model");
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
  
  public void write(SExpression expr) throws IOException {
    write(expr.toString());
  }
  
  public String read() throws IOException {
	  return reader.readLine();
  }
  
  protected void interpretResultLine(Result model, String line) {
    // expect a line of the form
    // x : [ ****** ] = [999.99, 999.99]
    int symbolColonIdx = line.indexOf(':');
    if (symbolColonIdx == -1) {
      throw new IllegalArgumentException("invalid input '" + line 
          + "', no separator colon found");
    }
    // everything up to the colon is the symbol name
    String symbolName = line.substring(0, symbolColonIdx).trim();
    // look for the equals sign
    int equalsIdx = line.indexOf('=', symbolColonIdx + 1);
    if (equalsIdx == -1) {
      throw new IllegalArgumentException("invalid input '" + line 
          + "', no equals sign found");
    }
    // look for [ , ] in that order
    int beginRangeIdx = line.indexOf('[', equalsIdx + 1);
    if (beginRangeIdx == -1) {
      throw new IllegalArgumentException("invalid input '" + line 
          + "', no range [ found");
    }
    
    int infinityIdx = line.indexOf("INFTY");
    //if the value has INFTY skipping looking for range.
    //Need to consider for positive and negative INFTY
    if (infinityIdx== -1){
	    int commaRangeIdx = line.indexOf(',', beginRangeIdx + 1);
	    if (commaRangeIdx == -1) {
	      throw new IllegalArgumentException("invalid input '" + line 
	          + "', no comma found in range");
	    }
	    int endRangeIdx = line.indexOf(']', commaRangeIdx + 1);
	    if (endRangeIdx == -1) {
	      throw new IllegalArgumentException("invalid input '" + line 
	          + "', no range ] found");
	    }
	 // everything between [ and , is the lower bound;
	    String lowerBound = line.substring(beginRangeIdx + 1, commaRangeIdx).trim();
	    // everything between , and ] is the upper bound
	    String upperBound = line.substring(commaRangeIdx + 1, endRangeIdx).trim();
	    model.addResult(symbolName, lowerBound, upperBound);	    
    } 
//    else {
//    	String infinityOperator = String.valueOf(line.charAt(infinityIdx));
//    	model.addResult(symbolName, infinityOperator+"IN");
//    }
    
    
    
  }
  
  public Result solve() throws IOException {
    write("(check-sat)");
    write("(exit)");
    writer.flush();
    writer.close();
    String result = reader.readLine();
    if (result.startsWith("unsat")) {
      return new Result(false);
    } else if (result.startsWith("Solution:")) {
      Result model = new Result(true);
      // parse lines until we see the final one
      while (!((result = reader.readLine()).startsWith("delta-sat"))) {
        // skip blank lines
        if (result.trim().equals("")) {
          continue;
        }
        interpretResultLine(model, result);
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
