package org.manifold.compiler.back.microfluidics.strategies.pressureflow;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

/**
 * Creates SMT2 equations for bounding pressure at each node in the microfluidic
 * circuit outlined in schematic, no expansion is performed on each port to
 * search ports down the channel. Use AnalyticalPressureFlowStrategy for this
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class SimplePressureFlowStrategy extends PressureFlowStrategy {

  private final boolean performWorstCaseAnalysis;
  
  /**
   * Toggle adding a worst case assertion to the expression that forces a
   * minimum flow rate 
   * 
   * @param performWorstCaseAnalysis True if this assertion is to be added
   */
  public SimplePressureFlowStrategy(boolean performWorstCaseAnalysis) {
    this.performWorstCaseAnalysis = performWorstCaseAnalysis;
  }
  
  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();   
    for (ConnectionValue conn : schematic.getConnections().values()) {
      exprs.addAll(translate(conn, schematic));
    }
    return exprs;
  }

  /** 
   * Assert that the difference in pressure in a channel from beginning to end
   * must be the same as the flow rate * resistance
   * 
   * @param conn  Channel within the circuit to calculate resistance of
   * @param schematic  Microfluidic circuit to analyze
   * @return SMT2 expression asserting the resistance   */
  private List<SExpression> translate(ConnectionValue conn, 
      Schematic schematic) {
    List<SExpression> exprs = new LinkedList<>();
    
    // for each channel, generate an expression of the form dP = V*R
    Symbol p1 = SymbolNameGenerator.getSym_PortPressure(schematic, 
        conn.getFrom());
    Symbol p2 = SymbolNameGenerator.getSym_PortPressure(schematic,
        conn.getTo());
    Symbol chV = SymbolNameGenerator.getsym_ChannelFlowRate(schematic, conn);
    Symbol chR = SymbolNameGenerator.getsym_ChannelResistance(schematic, conn);
    // assume the port pressures and resistance are declared elsewhere;
    // we still need to declare the flow rate
    exprs.add(QFNRA.declareRealVariable(chV));
    
    exprs.add(QFNRA.assertEqual(QFNRA.subtract(p1, p2),
        QFNRA.multiply(chV, chR)));
    
    if (performWorstCaseAnalysis) {
      // now declare a "worst case" flow rate, i.e. with maximum # of droplets
      Symbol chVWorstCase = SymbolNameGenerator
          .getsym_ChannelFlowRate_WorstCase(schematic, conn);
      exprs.add(QFNRA.declareRealVariable(chVWorstCase));
      // the resistance in the worst case is (approximately)
      // equal to the base channel resistance plus
      // the number of droplets times the resistance of each droplet
      Symbol nDroplets = SymbolNameGenerator
          .getsym_ChannelMaxDroplets(schematic, conn);
      Symbol dropletResistance = SymbolNameGenerator
          .getsym_ChannelDropletResistance(schematic, conn);
      SExpression chRWorstCase = QFNRA.add(chR,
          QFNRA.multiply(nDroplets, dropletResistance));
      // assume pressures are the same as before,
      // but flow rates can change in the worst case
      // TODO is this right?
      exprs.add(QFNRA.assertEqual(QFNRA.subtract(p1, p2),
          QFNRA.multiply(chVWorstCase, chRWorstCase)));
    }
    
    return exprs;
  }
  
}
