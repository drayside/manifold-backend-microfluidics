package org.manifold.compiler.back.microfluidics.strategies.placement;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
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
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

/**
 * Constrain a channel to pass through a given point P.
 * The way this is done is by looking at the endpoints of the channel
 * (call them I and J) and constraining the triangle IPJ to have zero area:
 * x_i (y_p − y_j ) + x_p (y_j − y_i ) + x_j (y_i − y_p ) = 0
 * 
 * @author Murphy?
 * 
 */
public class ChannelPlacementConstraintStrategy extends TranslationStrategy {

  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    return translationStep(schematic, typeTable);
  }
  
  protected List<SExpression> translationStep(Schematic schematic,
      PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<SExpression>();
    // start by looking for channel placement constraints
    ConstraintType targetConstraint = 
        typeTable.getChannelPlacementConstraintType();
    for (ConstraintValue cxt : schematic.getConstraints().values()) {
      if (!(cxt.getType().isSubtypeOf(targetConstraint))) {
        continue;
      }
      // pull attributes out of the constraint
      try {
        ConnectionValue channel = (ConnectionValue) cxt.getAttribute("channel");
        NodeValue nodeI = channel.getFrom().getParent();
        Symbol xI = SymbolNameGenerator.getsym_NodeX(schematic, nodeI);
        Symbol yI = SymbolNameGenerator.getsym_NodeY(schematic, nodeI);
        NodeValue nodeJ = channel.getTo().getParent();
        Symbol xJ = SymbolNameGenerator.getsym_NodeX(schematic, nodeJ);
        Symbol yJ = SymbolNameGenerator.getsym_NodeY(schematic, nodeJ);
        Decimal xP = new Decimal(((RealValue) cxt.getAttribute("x"))
            .toDouble());
        Decimal yP = new Decimal(((RealValue) cxt.getAttribute("y"))
            .toDouble());
        exprs.add(QFNRA.assertEqual(new Decimal(0.0),
            QFNRA.add(
                QFNRA.multiply(
                    xI, QFNRA.subtract(yP, yJ)
            ),
                QFNRA.add(
                    QFNRA.multiply(
                        xP, QFNRA.subtract(yJ, yI)
              ),
                    QFNRA.multiply(
                        xJ, QFNRA.subtract(yI, yP)
              )))));
      } catch (ClassCastException|UndeclaredAttributeException e) {
        throw new CodeGenerationError(
            "instance of controlPointPlacementConstraint"
            + " has values with wrong types");
      }
    }
    return exprs;
  }

}
