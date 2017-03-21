package org.manifold.compiler.back.microfluidics.strategies.pressureflow;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.smt2.*;
import org.manifold.compiler.middle.Schematic;

public class SimplePressureFlowStrategy extends PressureFlowStrategy {

  private final boolean performWorstCaseAnalysis;
  
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

    // Set lenient bounds on flowRate to make sure it is not infinity
    exprs.add(QFNRA.assertGreaterEqual(chV, new Decimal(-1000.0)));
    exprs.add(QFNRA.assertLessThanEqual(chV, new Decimal(1000.0)));
    
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
