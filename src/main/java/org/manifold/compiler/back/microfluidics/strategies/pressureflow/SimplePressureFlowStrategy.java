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
      exprs.add(translate(conn, schematic));
    }
    return exprs;
  }

  private SExpression translate(ConnectionValue conn, 
      Schematic schematic) {
    // for each channel, generate an expression of the form dP = V*R
    Symbol P1 = SymbolNameGenerator.getSym_PortPressure(schematic, 
        conn.getFrom());
    Symbol P2 = SymbolNameGenerator.getSym_PortPressure(schematic,
        conn.getTo());
    Symbol V = SymbolNameGenerator.getsym_ChannelFlowRate(schematic, conn);
    Symbol R = SymbolNameGenerator.getsym_ChannelResistance(schematic, conn);
    return QFNRA.assertEqual(QFNRA.subtract(P1, P2),
        QFNRA.multiply(V, R));
  }
  
}
