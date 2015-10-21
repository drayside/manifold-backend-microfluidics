package org.manifold.compiler.back.microfluidics.strategies;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.strategies.multiphase.DropletConstraintStrategy;
import org.manifold.compiler.back.microfluidics.strategies.multiphase.TJunctionDeviceStrategy;
import org.manifold.compiler.middle.Schematic;

// Contains strategies for generation of multi-phase circuits
public class MultiPhaseStrategySet extends TranslationStrategy {

  private boolean worstCaseAnalysis = false;
  public void performWorstCastAnalysis(boolean b) {
    this.worstCaseAnalysis = b;
  }
  
  private boolean derivedQuants = true;
  public void calculateDerivedQuantities(boolean b) {
    this.derivedQuants = b;
  }
  
  private DropletConstraintStrategy dropletConstraintStrategy;
  public void useDropletConstraintStrategy(DropletConstraintStrategy strat) {
    this.dropletConstraintStrategy = strat;
  }
  
  private TJunctionDeviceStrategy tjunctionDeviceStrategy;
  public void useTJunctionDeviceStrategy(TJunctionDeviceStrategy strat) {
    this.tjunctionDeviceStrategy = strat;
  }
  
  public MultiPhaseStrategySet() {
    // initialize default strategies
    dropletConstraintStrategy = new DropletConstraintStrategy();
    tjunctionDeviceStrategy = new TJunctionDeviceStrategy(derivedQuants, worstCaseAnalysis);
  }
  
  @Override
  protected List<SExpression> translationStep(Schematic schematic, 
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    exprs.addAll(dropletConstraintStrategy.translate(
        schematic, processParams, typeTable));
    exprs.addAll(tjunctionDeviceStrategy.translate(
        schematic, processParams, typeTable));
    return exprs;
  }
  
}
