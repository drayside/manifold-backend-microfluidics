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

/**
 * Contains strategies for generation of multi-phase circuits, uses the
 * translation methods for each of the respective strategies contained within
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class MultiPhaseStrategySet extends TranslationStrategy {

  private boolean worstCaseAnalysis = false;
  /**
   * Toggle whether worst case analysis should be performed by different
   * strategies
   * 
   * @param b  True if worst case analysis should be performed
   */
  public void performWorstCastAnalysis(boolean b) {
    this.worstCaseAnalysis = b;
  }
  
  // TODO need test cases for constructing multiPhaseStrategySet from each
  // of these separate strategies, currently only the default constructor
  // (MultiPhaseStrategySet) has a test case
  private DropletConstraintStrategy dropletConstraintStrategy;
  /**
   * Provide the multiPhaseStrategySet with a non-default
   * dropletConstraintStrategy
   * 
   * @param strat  The DropletConstraintStrategy for use in the
   * multiPhaseStrategy
   */
  public void useDropletConstraintStrategy(DropletConstraintStrategy strat) {
    this.dropletConstraintStrategy = strat;
  }
  
  private TJunctionDeviceStrategy tjunctionDeviceStrategy;
  /**
   * Provide the multiPhaseStrategySet with a non-default
   * TJunctionDeviceStrategy
   * 
   * @param strat  The DropletConstraintStrategy for use in the
   * multiPhaseStrategy
   */
  public void useTJunctionDeviceStrategy(TJunctionDeviceStrategy strat) {
    this.tjunctionDeviceStrategy = strat;
  }
  
  /**
   * Constructs default DropletConstraintStrategy and TJunctionDeviceStrategy
   * to make a default MultiPhaseStrategySet
   */
  public MultiPhaseStrategySet() {
    // initialize default strategies
    dropletConstraintStrategy = new DropletConstraintStrategy();
    tjunctionDeviceStrategy = new TJunctionDeviceStrategy(worstCaseAnalysis);
  }
  
  /**
   * Translation of multiPhaseStrategySet uses the translationStep methods for
   * dropletConstraintStrategy and tjunctionDeviceStrategy on their
   * respective types within the schematic
   */
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
