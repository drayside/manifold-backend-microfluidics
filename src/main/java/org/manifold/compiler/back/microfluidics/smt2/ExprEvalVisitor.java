package org.manifold.compiler.back.microfluidics.smt2;

import java.util.HashMap;
import java.util.Map;

/**
 * Gets the values of each term within an SExpression, will be either a Symbol,
 * Numeral or operator from a function
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class ExprEvalVisitor implements SExpressionVisitor {

  private double value = 0.0;
  public double getValue() {
    return value;
  }
  
  /**
   * Maps each Symbol to its numerical value
   */
  private Map<Symbol, Double> bindings = new HashMap<>();
  public void addBinding(Symbol var, Double value) {
    bindings.put(var, value);
  }
  
  /**
   * Checks to see if this Symbol is in the map connecting them to their value
   */
  @Override
  public void visit(Symbol s) {
    if (bindings.containsKey(s)) {
      value = bindings.get(s);
    } else {
      throw new ArithmeticException(
          "no binding for variable '" + s.getName() + "'");
    }
  }

  /**
   * Get the value of a Numeral object
   */
  @Override
  public void visit(Numeral n) {
    value = n.getValue();
  }

  /**
   * Get the value of a Decimal object
   */
  @Override
  public void visit(Decimal d) {
    value = d.getValue();
  }

  /**
   * Get the left and right values of a function inside a ParenList
   */
  @Override
  public void visit(ParenList l) {
    if (l.getExprs().isEmpty()) {
      throw new ArithmeticException ("cannot eval empty expression");
    }
    if (l.getExprs().size() == 1) {
      l.getExprs().get(0).accept(this);
    } else {
      // get first symbol to decide what function to run
      SExpression symExpr = l.getExprs().get(0);
      if (!(symExpr instanceof Symbol)) {
        throw new ArithmeticException ("first term of function "
            + l.toString()
            + " must be a symbol");
      } else {
        Symbol func = (Symbol) symExpr;
        // try binary expressions first
        if (evalBinaryExpr(func, l)) {
          return;
        } else {
          throw new ArithmeticException ("cannot evaluate unknown function '"
              + func + "' in expression " + l.toString());
        }
      }
    }
  }
  
  /**
   * Extract the left and right values from a valid function and perform the
   * correct operation on it
   * 
   * @param func  The operator in the expression
   * @param l  List of the two values from the expression
   * @return True if successful and sets value to be the result of the function,
   * False if the operator isn't found
   */
  private boolean evalBinaryExpr(Symbol func, ParenList l) {
    if (l.getExprs().size() != 3) {
      return false;
    }
    l.getExprs().get(1).accept(this);
    double vLeft = value;
    l.getExprs().get(2).accept(this);
    double vRight = value;
    if (func.equals(new Symbol("+"))) {
      value = vLeft + vRight;
      return true;
    } else if (func.equals(new Symbol("-"))) {
      value = vLeft - vRight;
      return true;
    } else if (func.equals(new Symbol("*"))) {
      value = vLeft * vRight;
      return true;
    } else if (func.equals(new Symbol("^"))) {
      value = Math.pow(vLeft, vRight);
      return true;
    } else {
      return false;
    }
  }
    
    
}
