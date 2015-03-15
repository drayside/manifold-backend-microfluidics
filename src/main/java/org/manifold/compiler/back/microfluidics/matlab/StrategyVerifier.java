package org.manifold.compiler.back.microfluidics.matlab;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.manifold.compiler.back.microfluidics.CodeGenerationError;

public class StrategyVerifier {
  private Set<ImportStatement> importStatements;
  private List<AssignmentStatement> assignmentStatements;
  private List<VerificationStatement> verificationStatements;
  
  public StrategyVerifier() {
    this.importStatements = new HashSet<ImportStatement>();
    this.assignmentStatements = new LinkedList<AssignmentStatement>();
    this.verificationStatements = new LinkedList<VerificationStatement>();
  }
  
  public Set<ImportStatement> getImportStatements() {
    return this.importStatements;
  }
  
  public void addImport(String pkg) {
    this.importStatements.add(new ImportStatement(pkg));
  }
  
  public void addImport(ImportStatement stmt) {
    this.importStatements.add(stmt);
  }
  
  public void addInstantiation(InstantiationStatement stmt) {
    this.assignmentStatements.add(stmt);
  }
  
  public void addVerification(VerificationStatement stmt) {
    this.verificationStatements.add(stmt);
  }
  
  public void addStatements(List<MatlabStatement> stmts) {
    for (MatlabStatement stmt: stmts) {
      if (stmt instanceof ImportStatement) {
        addImport((ImportStatement) stmt);
      } else if (stmt instanceof InstantiationStatement) {
        addInstantiation((InstantiationStatement) stmt);
      } else if (stmt instanceof VerificationStatement) {
        addVerification((VerificationStatement) stmt);
      } else {
        throw new CodeGenerationError("Could not add statement to StrategyVerifier");
      }
    }
  }
  
  public String writeInstantiationStatements() {
    StringBuilder builder = new StringBuilder();
    for (AssignmentStatement stmt: this.assignmentStatements) {
      builder.append(stmt.toString());
      builder.append("\n");
    }
    
    return builder.toString();
  }
  
  public String writeVerificationStatements() {
    StringBuilder builder = new StringBuilder();
    for (VerificationStatement stmt: this.verificationStatements) {
      builder.append(stmt.toString());
      builder.append("\n");
    }
    
    return builder.toString();
  }

  public String writeStatements() {
    StringBuilder builder = new StringBuilder();
    
    for (ImportStatement stmt: importStatements) {
      builder.append(stmt.toString());
      builder.append("\n");
    }
    
    builder.append("\n");
    builder.append("res = 1");
    builder.append("\n");

    builder.append(writeInstantiationStatements());
    builder.append("\n");
    
    builder.append(writeVerificationStatements());
    return builder.toString();
  }
}
