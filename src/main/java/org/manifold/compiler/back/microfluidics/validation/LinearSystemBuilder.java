package org.manifold.compiler.back.microfluidics.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class LinearSystemBuilder {

  private static Logger log = LogManager.getLogger(LinearSystemBuilder.class);
  
  private List<List<LinearTerm>> equations;
  private List<Expression> rightHandSides;
  
  // get all variables used as part of the constant expression of any term
  public Set<Variable> getConstantVariables() {
    Set<Variable> constantVariables = new HashSet<Variable>();
    for (List<LinearTerm> terms : equations) {
      for (LinearTerm term : terms) {
        constantVariables.addAll(term.getScalarExpression().freeVariables());
      }
    }
    return constantVariables;
  }
  
  // get all variables used as an unknown in any term
  public Set<Variable> getUnknownVariables() {
    Set<Variable> unknownVariables = new HashSet<Variable>();
    for (List<LinearTerm> terms : equations) {
      for (LinearTerm term : terms) {
        unknownVariables.add(term.getVariable());
      }
    }
    return unknownVariables;
  }
  
  //get all variables used as an unknown in any term
  public Set<Variable> getRHSVariables() {
   Set<Variable> rhsVariables = new HashSet<Variable>();
   for (Expression expr : rightHandSides) {
       rhsVariables.addAll(expr.freeVariables());
   }
   return rhsVariables;
  }
  
  public void addEquation(List<LinearTerm> terms, Expression rhs) {
    equations.add(terms);
    rightHandSides.add(rhs);
  }
  
  public void addEquation(LinearTerm[] terms, Expression rhs) {
    equations.add(Arrays.asList(terms));
    rightHandSides.add(rhs);
  }
  
  public LinearSystemBuilder() {
    this.equations = new LinkedList<List<LinearTerm>>();
    this.rightHandSides = new LinkedList<Expression>();
  }
  
  public boolean isWellFormed() {
    // if a variable appears as part of a constant expression,
    // that variable can't be used as the unknown variable in any linear term
    Set<Variable> constantVariables = getConstantVariables();
    Set<Variable> unknownVariables = getUnknownVariables();
    constantVariables.retainAll(unknownVariables);
    if (!(constantVariables.isEmpty())) {
      for (Variable v : constantVariables) {
        log.error("constant variable " + v + 
            " also appears as an unknown in this system");
      }
      return false;
    }
    // similarly, if a variable appears on the right-hand side,
    // it can't be an unknown variable
    Set<Variable> rhsVariables = getRHSVariables();
    rhsVariables.retainAll(unknownVariables);
    if (!(rhsVariables.isEmpty())) {
      for (Variable v : rhsVariables) {
        log.error("right-hand-side variable " + v + 
            " also appears as an unknown in this system");
      }
      return false;
    }
    
    // TODO every term-list must be non-empty
    
    return true;
  }
  
  public String build(Variable x) {
    if (!isWellFormed()) return "";
    StringBuilder sb = new StringBuilder();
    // make at least an expression of the form "x = A^-1 * b"
    // (or preferably left division, i.e. "x = A \ b" is equivalent
    // and keep track of the order in which the unknowns appear in x
    Set<Variable> unknownVariableSet = getUnknownVariables();
    List<Variable> unknownVariables = new ArrayList<Variable>(unknownVariableSet);
    
    // build A matrix
    StringBuilder aStr = new StringBuilder();
    aStr.append("[");
    // build each row
    for (List<LinearTerm> terms : equations) {
      for (int i = 0; i < unknownVariables.size(); ++i) {
        // TODO deal with multiple terms that use the same unknown
        boolean found = false;
        LinearTerm foundTerm = null;
        for (LinearTerm term : terms) {
          // does unknown variable i appear in this term?
          if (term.getVariable().equals(unknownVariables.get(i))) {
            found = true;
            foundTerm = term;
            break;
          }
        }
        if (found) {
          aStr.append(foundTerm.getScalarExpression()).append(", ");
        } else {
          // zero
          aStr.append("0, ");
        }
      }
      // terminate row
      aStr.append(";");
    }
    aStr.append("]");
    
    // build b matrix
    StringBuilder bStr = new StringBuilder();
    bStr.append("[");
    for (Expression expr : rightHandSides) {
      bStr.append(expr.toString() + "; ");
    }
    bStr.append("]");
    
    sb.append(x).append(" = ").append(aStr).append(" \\ ").append(bStr).append(";");
    return sb.toString();
  }
  
}
