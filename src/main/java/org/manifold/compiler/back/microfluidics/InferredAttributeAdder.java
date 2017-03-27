package org.manifold.compiler.back.microfluidics;

import org.manifold.compiler.*;
import org.manifold.compiler.back.microfluidics.smt2.DRealSolver;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.middle.BackAnnotationBuilder;
import org.manifold.compiler.middle.Schematic;
import org.manifold.compiler.middle.SchematicException;

import java.util.Map;


public class InferredAttributeAdder {

  private enum ComponentType { INVALID, NODE, CONNECTION, PORT, CONSTRAINT };

  private static InferredValue constructInferredRealValue(double val) {
    InferredTypeValue inferredRealType =
            new InferredTypeValue(RealTypeValue.getInstance());
    InferredValue inferredReal = null;
    try {
      inferredReal = new InferredValue(inferredRealType, new RealValue(val));
    } catch (TypeMismatchException e) { }

    return inferredReal;
  }

  public static Schematic populateFromDrealResults(Schematic schematic,
    DRealSolver.Result drealResult) throws SchematicException {

    if (!drealResult.isSatisfiable()) {
      return schematic;
    }

    Map<Symbol, DRealSolver.RealRange> ranges = drealResult.getRanges();

    BackAnnotationBuilder annotationBuilder =
      new BackAnnotationBuilder(schematic);

    for (Symbol symbol : ranges.keySet()) {

      // For now, arbitrarily picking the median value in the range.
      // TODO: choose a value from the range in a systematic way
      double lowerBound = ranges.get(symbol).lowerBound;
      double upperBound = ranges.get(symbol).upperBound;
      double attributeValue = (lowerBound + upperBound) / 2;

      ComponentType componentType = symbolType(schematic, symbol);
      if (componentType == ComponentType.INVALID) {
        continue;
      }

      String[] splitSymbol = symbol.toString().split("\\.");
      String attributeName = splitSymbol[splitSymbol.length - 1];

      switch (componentType) {
          case NODE:
            String nodeName = splitSymbol[0];
            annotationBuilder.annotateNodeAttribute(nodeName, attributeName,
              constructInferredRealValue(attributeValue));
            break;
          case CONNECTION:
            String connectionName = splitSymbol[0];
            //annotationBuilder.annotateConnectionAttribute(connectionName,
              //attributeName, constructInferredRealValue(attributeValue));
            break;
          case PORT:
            nodeName = splitSymbol[0];
            String portName = splitSymbol[1];
            //annotationBuilder.annotatePortAttribute(nodeName, portName,
              //attributeName, constructInferredRealValue(attributeValue));
            break;
          case CONSTRAINT:
            String constraintName = splitSymbol[0];
            annotationBuilder.annotateConstraintAttribute(constraintName,
              attributeName, constructInferredRealValue(attributeValue));
            break;
      }
    }

    return annotationBuilder.build();
  }

  private static ComponentType symbolType(Schematic schematic, Symbol symbol) {
    String[] splitSymbol = symbol.toString().split("\\.");

    if (splitSymbol.length == 2) {
      String componentName = splitSymbol[0];
      try {
        NodeValue node = schematic.getNode(componentName);
        return ComponentType.NODE;
      } catch (UndeclaredIdentifierException e) { }

      try {
        ConnectionValue connection = schematic.getConnection(componentName);
        return ComponentType.CONNECTION;
      } catch (UndeclaredIdentifierException e) { }

      try {
        ConstraintValue constraint = schematic.getConstraint(componentName);
        return ComponentType.CONSTRAINT;
      } catch (UndeclaredIdentifierException e) { }

      return ComponentType.INVALID;

    } else if (splitSymbol.length == 3) {
      // A node.port.property combination.
      try {
        NodeValue node = schematic.getNode(splitSymbol[0]);
        PortValue port = node.getPort(splitSymbol[1]);
        return ComponentType.PORT;
      } catch (UndeclaredIdentifierException e) {
        return ComponentType.INVALID;
      }

    }

    return ComponentType.INVALID;
  }
}
