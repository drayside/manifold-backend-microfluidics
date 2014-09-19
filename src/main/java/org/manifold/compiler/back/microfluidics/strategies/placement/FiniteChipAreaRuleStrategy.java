package org.manifold.compiler.back.microfluidics.strategies.placement;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

public class FiniteChipAreaRuleStrategy extends ChipAreaRuleStrategy {

  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    // loop through all control points
    for (NodeValue n : schematic.getNodes().values()) {
      if (!(n.getType().isSubtypeOf(typeTable.getControlPointNodeType()))) {
        continue;
      }
      Symbol nodeX = SymbolNameGenerator.getsym_NodeX(schematic, n);
      Symbol nodeY = SymbolNameGenerator.getsym_NodeY(schematic, n);
      exprs.add(QFNRA.assertGreater(nodeX, new Decimal(0.0)));
      exprs.add(QFNRA.assertGreater(nodeY, new Decimal(0.0)));
      exprs.add(QFNRA.assertLessThan(nodeX, 
          new Decimal(processParams.getMaximumChipSizeX())));
      exprs.add(QFNRA.assertLessThan(nodeY, 
          new Decimal(processParams.getMaximumChipSizeY())));
    }
    return exprs;
  }

}
