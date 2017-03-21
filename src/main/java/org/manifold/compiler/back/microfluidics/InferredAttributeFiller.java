package org.manifold.compiler.back.microfluidics;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.Value;
import org.manifold.compiler.back.microfluidics.smt2.DRealSolver;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

import java.util.Map;


public class InferredAttributeFiller {

  public static void populateFromDrealResults(Schematic schematic,
                                       DRealSolver.Result drealResult) {
    if (!drealResult.isSatisfiable()) {
      return;
    }

    Map<Symbol, DRealSolver.RealRange> ranges = drealResult.getRanges();

    for (Symbol symbol : ranges.keySet()) {
      Value schematicRef = SymbolNameGenerator.getSchematicSymbolLocation(
        symbol, schematic);
      if (schematicRef == null) {
        continue;
      }

      String[] splitSymbol = symbol.toString().split("\\.");
      String symbolName = splitSymbol[splitSymbol.length - 1];

      if (schematicRef instanceof NodeValue) {

      } else if (schematicRef instanceof ConnectionValue) {

      } else if (schematicRef instanceof PortValue) {

      }


    }
  }
}
