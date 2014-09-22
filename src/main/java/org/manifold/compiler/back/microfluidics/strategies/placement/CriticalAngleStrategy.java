package org.manifold.compiler.back.microfluidics.strategies.placement;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.middle.Schematic;

public abstract class CriticalAngleStrategy extends TranslationStrategy {

  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    // iterate over all combinations of 3 nodes
    List<NodeValue> nodes = new ArrayList<>(schematic.getNodes().values());
    for (int i = 0; i < nodes.size(); ++i) {
      for (int j = i + 1; j < nodes.size(); ++j) {
        for (int k = j + 1; k < nodes.size(); ++k) {
          NodeValue n1 = nodes.get(i);
          NodeValue n2 = nodes.get(j);
          NodeValue n3 = nodes.get(k);
          // we need to see n1 <--> n2 <--> n3
          ConnectionValue ch12 = getConnectingChannel(schematic, 
              typeTable, n1, n2);
          ConnectionValue ch23 = getConnectingChannel(schematic, 
              typeTable, n2, n3);
          if (ch12 == null || ch23 == null) {
            continue;
          }
          // otherwise, call virtual method to generate constraint
          exprs.add(generateCriticalAngleConstraint(
              schematic, processParams, typeTable,
              n1, ch12, n2, ch23, n3));
        }
      }
    }
    return exprs;
  }

  public abstract SExpression generateCriticalAngleConstraint(
      Schematic schematic, ProcessParameters processParams,
      PrimitiveTypeTable typeTable,
      NodeValue node1, ConnectionValue channel1to2, NodeValue node2, 
      ConnectionValue channel2to3, NodeValue node3);
  
}
