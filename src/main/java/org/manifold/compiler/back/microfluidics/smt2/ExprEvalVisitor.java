package org.manifold.compiler.back.microfluidics.smt2;

import java.util.HashMap;
import java.util.Map;

public class ExprEvalVisitor implements SExpressionVisitor {

  private double value = 0.0;
  public double getValue() {
    return value;
  }
  
  // variable bindings
  private Map<Symbol, Double> bindings = new HashMap<>();
  public void addBinding(Symbol var, Double value) {
    bindings.put(var, value);
  }
  
  @Override
  public void visit(Symbol s) {
    if (bindings.containsKey(s)) {
      value = bindings.get(s);
    } else {
      throw new ArithmeticException(
          "no binding for variable '" + s.getName() + "'");
    }
  }

  @Override
  public void visit(Numeral n) {
    value = n.getValue();
  }

  @Override
  public void visit(Decimal d) {
    value = d.getValue();
  }

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
