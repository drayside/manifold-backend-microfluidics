package org.manifold.compiler.back.microfluidics.strategies.placement;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

public class CosineLawCriticalAngleStrategy extends CriticalAngleStrategy {

  @Override
  public SExpression generateCriticalAngleConstraint(
      Schematic schematic, ProcessParameters processParams,
      PrimitiveTypeTable typeTable,
      NodeValue node1, ConnectionValue channel1to2, NodeValue node2, 
      ConnectionValue channel2to3, NodeValue node3) {
    /* cos(theta) = (A dot B) / (||A|| ||B||)
     * Let A be the vector from n2 to n1,
     * and let B be the vector from n2 to n3.
     * Let theta be the angle between A and B.
     * Call the projections of A onto the x-axis and y-axis
     * Ax and Ay respectively, and similar for B.
     * Then Ax = n1.x - n2.x, and Ay = n1.y - n2.y.
     * Similarly, Bx = n3.x - n2.x, and By = n3.y - n2.y.
     * From this, (A dot B) = (Ax * Bx + Ay * By).
     * We could also write ||A|| = sqrt(Ax * Ax + Ay * Ay),
     * but to avoid the expensive square root
     * we instead write in terms of ||A||^2 = (Ax * Ax + Ay * Ay).
     * This requires squaring the other terms in the expression, to yield
     * cos^2(theta) = (A dot B)^2 / (||A||^2 ||B||^2)
     * Finally, we calculate cos^2(thetaC) for the critical angle thetaC
     * and enforce cos^2(thetaC) <= cos^2(theta).
     */
    Symbol n1x = SymbolNameGenerator.getsym_NodeX(schematic, node1);
    Symbol n1y = SymbolNameGenerator.getsym_NodeY(schematic, node1);
    Symbol n2x = SymbolNameGenerator.getsym_NodeX(schematic, node2);
    Symbol n2y = SymbolNameGenerator.getsym_NodeY(schematic, node2);
    Symbol n3x = SymbolNameGenerator.getsym_NodeX(schematic, node3);
    Symbol n3y = SymbolNameGenerator.getsym_NodeY(schematic, node3);
    SExpression aX = QFNRA.subtract(n1x, n2x);
    SExpression aY = QFNRA.subtract(n1y, n2y);
    SExpression bX = QFNRA.subtract(n3x, n2x);
    SExpression bY = QFNRA.subtract(n3y, n2y);
    SExpression aDotBSquared = QFNRA.pow(
        QFNRA.add(QFNRA.multiply(aX, bX), QFNRA.multiply(aY, bY)),
        new Decimal(2.0));
    SExpression aSquaredBSquared = QFNRA.multiply(
        QFNRA.add(QFNRA.multiply(aX, aX), QFNRA.multiply(aY, aY)),
        QFNRA.add(QFNRA.multiply(bX, bX), QFNRA.multiply(bY, bY)));
    SExpression cosineSquaredTheta = QFNRA.divide(
        aDotBSquared, aSquaredBSquared);
    SExpression cosineSquaredThetaCritical = new Decimal(Math.pow(
        Math.cos(processParams.getCriticalCrossingAngle()), 2.0));
    return QFNRA.assertLessThanEqual(
        cosineSquaredThetaCritical, cosineSquaredTheta);
  }

}
