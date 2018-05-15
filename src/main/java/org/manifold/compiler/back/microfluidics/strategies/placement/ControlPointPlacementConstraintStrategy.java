package org.manifold.compiler.back.microfluidics.strategies.placement;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConstraintType;
import org.manifold.compiler.ConstraintValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.RealValue;
import org.manifold.compiler.UndeclaredAttributeException;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

/**
 * Get the NodeValue, x and y positions for the control point from the input
 * PrimitiveTypeTable under getControlPointPlacementConstraintType and assert
 * that its equal to the x and y position outlined in schematic for that node 
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class ControlPointPlacementConstraintStrategy 
  extends TranslationStrategy {

  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    // TODO Auto-generated method stub
    return translationStep(schematic, typeTable);
  }

  protected List<SExpression> translationStep(Schematic schematic,
      PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    ConstraintType cxtTarget = typeTable
        .getControlPointPlacementConstraintType();
    for (ConstraintValue cxt : schematic.getConstraints().values()) {
      // look for the target constraint
      if (!(cxt.getType().isSubtypeOf(cxtTarget))) {
        continue;
      }
      // pull attributes out of the constraint
      try {
        NodeValue node = (NodeValue) cxt.getAttribute("node");
        RealValue x = (RealValue) cxt.getAttribute("x");
        RealValue y = (RealValue) cxt.getAttribute("y");
        exprs.add(QFNRA.assertEqual(
            SymbolNameGenerator.getsym_NodeX(schematic, node), 
            new Decimal(x.toDouble())));
        exprs.add(QFNRA.assertEqual(
            SymbolNameGenerator.getsym_NodeY(schematic, node), 
            new Decimal(y.toDouble())));
      } catch (ClassCastException|UndeclaredAttributeException e) {
        throw new CodeGenerationError(
            "instance of controlPointPlacementConstraint"
            + " has values with wrong types");
      }
    }
    return exprs;
  }
  
}
