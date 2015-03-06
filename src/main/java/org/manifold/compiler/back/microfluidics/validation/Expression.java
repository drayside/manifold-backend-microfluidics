package org.manifold.compiler.back.microfluidics.validation;

import java.util.Set;

public abstract class Expression {

  public abstract Set<Variable> freeVariables();
  
}
