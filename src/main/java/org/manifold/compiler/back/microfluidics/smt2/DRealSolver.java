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

/**
 * Sends valid QF_NRA expression to dReal through the command line for it 
 * to return us if that expression has a solution or not
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class DRealSolver implements AutoCloseable {
  //TODO: Consider switching to a library instead of command line  to reduce
  // having dReal installed as a dependency https://github.com/sosy-lab/java-smt

  /**
   * Provides range of values (lower and upper) for a Symbol object contained
   * within a RealRange method 
   * 
   * @author Murphy? Comments by Josh
   *
   */
  class RealRange {
    public final double lowerBound;
    public final double upperBound;
    
    /**
     * Stores the range of values a Symbol can take
     * 
     * @param lower  Lower value in the range
     * @param upper  Upper value in the range
     */
    public RealRange(double lower, double upper) {
      this.lowerBound = lower;
      this.upperBound = upper;
    }
  }
  
  /**
   * Stores the result (satisfiable or not satisfiable)returned from dReal
   * @author Murphy? Comments by Josh
   *
   */
  class Result {
    private final boolean satisfiable;
    /**
     * If this expression is satisfiable this returns true, this is 
     * @return
     */
    public boolean isSatisfiable() {
      return this.satisfiable;
    }
    
    /**
     * Provides the range of values (lower, upper) allowed for any Symbol object
     * as a RealRange object
     */
    private Map<Symbol, RealRange> ranges;
    
    /**
     * Returns the range of a Symbol object
     * 
     * @param sym  Symbol object
     * @return RealRange object for that Symbol with lower and upper attributes
     */
    public RealRange getRange(Symbol sym) {
      return ranges.get(sym);
    }
    
    /**
     * Add a new Symbol with a range of values to be bounded by lowerBound and
     * upperBound, used to contain the result returned by dReal
     * 
     * @param symbolName  Name of the symbol tested by dReal
     * @param lowerBound  Lower bound of the range of values that the Symbol can
     * take to be true
     * @param upperBound  Upper bound of the range of values that the Symbol can
     * take to be true
     */
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
  
  /**
   * Finds the path to the dReal solver on your computer to send expressions to
   * solve for, if found it saves the path to pathToDReal
   */
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
  
  /**
   * Gets the path to dReal solver by calling pathToDReal
   */
  public DRealSolver() {
    if (pathToDReal == null) {
      findDReal();
    }
  }
 
  private Process dRealProcess = null;
  private BufferedWriter writer;
  private BufferedReader reader;
  
  /**
   * Creates writer and reader objects that send a receive data from dReal
   * installed on your computer
   * @throws IOException  If input or output exception occurs when communicating
   * with dRealProcess
   */
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
    
    // TODO: If an SExpression is written (which already adds this line to
    // expression head when created) then it is sent twice which is unnecessary
    write("(set-logic QF_NRA)");
  }
  
  /**
   * Sends an expression to dReal for it to solve
   * 
   * @param data - The expression to be sent to dReal
   * @throws IOException  If input or output exception occurs when communicating
   * with dRealProcess
   */
  public void write(String data) throws IOException {
    if (dRealProcess == null) {
      throw new IllegalStateException("dReal session has not been opened");
    }
    writer.write(data);
    writer.newLine();
  }
  
  /**
   * Sends an SExpression to dReal to solve, already in correct QF_NRA form
   * 
   * @param data - The SEpression to be sent to dReal
   * @throws IOException  If input or output exception occurs when communicating
   * with dRealProcess
   */
  public void write(SExpression expr) throws IOException {
    if (dRealProcess == null) {
      throw new IllegalStateException("dReal session has not been opened");
    }
    write(expr.toString());
  }
  
  /**
   * Parses the response from dReal to extract the upper and lower bounds on
   * that Symbol determined by dReal
   * 
   * @param model  Result object that contains the Symbol name and bounds
   * returned by dReal
   * @param line  The response from dReal
   */
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
  
  /**
   * Runs the dReal solver on the given expression written to writer by calling
   * the write method
   * 
   * @return model - A Result object with attribute satisfiable = True if dReal
   * determines the expression to be satisfiable, False otherwise
   * @throws IOException - If input or output exception occurs when
   * communicating with dRealProcess or if it returns an unexpected result
   * (besides 'unsat' or 'Solution:')
   */
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
