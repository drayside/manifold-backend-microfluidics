package org.manifold.compiler.back.microfluidics.matlab;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class CompoundStrategyVerifier {
  private Set<ImportStatement> importStatements;
  private List<StrategyVerifier> verifiers;
  
  public CompoundStrategyVerifier() {
    this.importStatements = new HashSet<ImportStatement>();
    this.verifiers = new LinkedList<StrategyVerifier>();
  }
  
  public void addVerifier(StrategyVerifier verifier) {
    this.importStatements.addAll(verifier.getImportStatements());
    this.verifiers.add(verifier);
  }
  
  public void addVerifiers(List<StrategyVerifier> verifiers) {
    for (StrategyVerifier verifier: verifiers) {
      addVerifier(verifier);
    }
  }
  
  public String writeStatements() {
    StringBuilder builder = new StringBuilder();
    for (ImportStatement stmt: this.importStatements) {
      builder.append(stmt.toString());
      builder.append("\n");
    }
    
    builder.append("\n");
    builder.append("res = 1;");
    builder.append("\n");
    
    for (StrategyVerifier verifier: this.verifiers) {
      builder.append(verifier.writeInstantiationStatements());
      builder.append("\n");
      builder.append(verifier.writeVerificationStatements());
      builder.append("\n");
    }
    
    return builder.toString();
  }
}
