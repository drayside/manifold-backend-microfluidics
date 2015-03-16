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
    
    exprs.add(QFNRA.assertEqual(QFNRA.subtract(p1, p2),
        QFNRA.multiply(chV, chR)));
    
    return exprs;
  }
  
}
