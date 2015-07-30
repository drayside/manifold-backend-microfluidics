package org.manifold.compiler.back.microfluidics.strategies.pressureflow;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

public class SimplePressureFlowStrategy extends PressureFlowStrategy {

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
    
    exprs.add(QFNRA.assertEqual(QFNRA.subtract(p2, p1),
        QFNRA.multiply(chV, chR)));
    
    // now declare a "worst case" flow rate, i.e. with maximum # of droplets
    Symbol chV_WorstCase = SymbolNameGenerator.getsym_ChannelFlowRate_WorstCase(schematic, conn);
    exprs.add(QFNRA.declareRealVariable(chV_WorstCase));
    // the resistance in the worst case is (approximately)
    // equal to the base channel resistance plus
    // the number of droplets times the resistance of each droplet
    Symbol nDroplets = SymbolNameGenerator
        .getsym_ChannelMaxDroplets(schematic, conn);
    Symbol dropletResistance = SymbolNameGenerator
        .getsym_ChannelDropletResistance(schematic, conn);
    SExpression chR_WorstCase = QFNRA.add(chR, 
        QFNRA.multiply(nDroplets, dropletResistance));
    // assume pressures are the same as before,
    // but flow rates can change in the worst case
    // TODO is this right?
    exprs.add(QFNRA.assertEqual(QFNRA.subtract(p1, p2),
        QFNRA.multiply(chV_WorstCase, chR_WorstCase)));
    
    //Channel Velocity constraint
    Symbol channel_velocity = SymbolNameGenerator.getsym_ChannelVelocity(schematic, conn);
    exprs.add(QFNRA.declareRealVariable(channel_velocity));
    
    exprs.add(QFNRA.assertEqual(channel_velocity, 
    		QFNRA.divide(
    				SymbolNameGenerator.getsym_ChannelFlowRate(schematic, conn),
    				QFNRA.multiply(
    						SymbolNameGenerator.getsym_constant_pi(), 
    						QFNRA.pow(SymbolNameGenerator.getsym_ChannelRadius(schematic, conn),
    								new Decimal(2.0))))));
    
    return exprs;
  }
  
}
