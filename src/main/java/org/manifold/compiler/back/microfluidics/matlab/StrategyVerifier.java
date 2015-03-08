package org.manifold.compiler.back.microfluidics.matlab;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class StrategyVerifier {
  private Set<ImportStatement> importStatements;
  private List<InstantiationStatement> instantiationStatements;
  private List<VerificationStatement> verificationStatements;
  
  public StrategyVerifier() {
    this.importStatements = new HashSet<ImportStatement>();
    this.instantiationStatements = new LinkedList<InstantiationStatement>();
    this.verificationStatements = new LinkedList<VerificationStatement>();
  }
  
  public void addImport(String pkg) {
    this.importStatements.add(new ImportStatement(pkg));
  }
  
  public void addInstantiation(InstantiationStatement stmt) {
    this.instantiationStatements.add(stmt);
  }
}
