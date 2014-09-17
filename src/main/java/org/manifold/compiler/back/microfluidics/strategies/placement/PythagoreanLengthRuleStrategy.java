package org.manifold.compiler.back.microfluidics.strategies.placement;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.smt2.Numeral;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

public class PythagoreanLengthRuleStrategy extends LengthRuleStrategy {
  
  @Override
  protected List<SExpression> translationStep(Schematic schematic, 
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable) {
    return translationStep(schematic, typeTable);
  }

  protected List<SExpression> translationStep(Schematic schematic, 
      PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    // consider all pairs of distinct nodes
    for (NodeValue n1 : schematic.getNodes().values()) {
      for (NodeValue n2 : schematic.getNodes().values()) {
        if (n1 == n2) {
          continue;
        }
        // see if there is a channel connecting these nodes
        // (use directed search as we want to avoid duplicates)
        ConnectionValue channel = getConnectingChannel(schematic, typeTable,
            n1, n2, true);
        if (channel != null) {
          exprs.add(generateLengthAssertion(schematic, n1, n2, channel));
        }
      }
    }
    return exprs;
  }

  private SExpression generateLengthAssertion(Schematic schematic,
      NodeValue n1, NodeValue n2, 
      ConnectionValue channel) {
    // The formula we represent here is:
    // (n1.x - n2.x)^2 + (n1.y - n2.y)^2 = channel.length^2
    Symbol n1x = SymbolNameGenerator.getsym_NodeX(schematic, n1);
    Symbol n1y = SymbolNameGenerator.getsym_NodeY(schematic, n1);
    Symbol n2x = SymbolNameGenerator.getsym_NodeX(schematic, n2);
    Symbol n2y = SymbolNameGenerator.getsym_NodeY(schematic, n2);
    Symbol chLen = SymbolNameGenerator.getsym_ChannelLength(schematic, channel);
    
    SExpression sideA = QFNRA.subtract(n1x, n2x);
    SExpression aSquared = QFNRA.pow(sideA, new Numeral(2));
    SExpression sideB = QFNRA.subtract(n1y, n2y);
    SExpression bSquared = QFNRA.pow(sideB, new Numeral(2));
    SExpression aSquaredPlusBSquared = QFNRA.add(aSquared, bSquared);
    SExpression cSquared = QFNRA.pow(chLen, new Numeral(2));
    return QFNRA.assertEqual(aSquaredPlusBSquared, cSquared);
  }
  
}
