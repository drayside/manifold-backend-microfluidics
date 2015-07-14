package org.manifold.compiler.back.microfluidics.strategies.multiphase;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.ConstraintValue;
import org.manifold.compiler.RealValue;
import org.manifold.compiler.UndeclaredAttributeException;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

public class DropletConstraintStrategy extends TranslationStrategy {

  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    // get all droplet-related constraints
    for (ConstraintValue cxt : schematic.getConstraints().values()) {
      if (cxt.getType().isSubtypeOf(
          typeTable.getchannelDropletVolumeConstraintType())) {
        // droplet volume constraint
        exprs.addAll(translateDropletVolumeConstraint(
            schematic, typeTable, cxt));
      }
    }
    return exprs;
  }

  private List<SExpression> translateDropletVolumeConstraint(
      Schematic schematic, PrimitiveTypeTable typeTable,
      ConstraintValue cxt) {
    List<SExpression> exprs = new LinkedList<>();
    try {
      ConnectionValue channel = (ConnectionValue) cxt.getAttribute("channel");
      Decimal volume = new Decimal(((RealValue) cxt.getAttribute("volume"))
          .toDouble());
      Symbol vChannel = 
          SymbolNameGenerator.getsym_ChannelDropletVolume(schematic, channel);
      exprs.add(QFNRA.assertEqual(vChannel, volume));
    } catch (ClassCastException|UndeclaredAttributeException e) {
      throw new CodeGenerationError(
          "instance of channelDropletVolumeConstraint"
          + " has values with wrong types");
    }
    return exprs;
  }
  
}
