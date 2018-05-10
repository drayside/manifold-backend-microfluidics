package org.manifold.compiler.back.microfluidics.strategies;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.strategies.placement.ChannelPlacementConstraintStrategy;
import org.manifold.compiler.back.microfluidics.strategies.placement.ChipAreaRuleStrategy;
import org.manifold.compiler.back.microfluidics.strategies.placement.ControlPointPlacementConstraintStrategy;
import org.manifold.compiler.back.microfluidics.strategies.placement.CosineLawCriticalAngleStrategy;
import org.manifold.compiler.back.microfluidics.strategies.placement.CriticalAngleStrategy;
import org.manifold.compiler.back.microfluidics.strategies.placement.FiniteChipAreaRuleStrategy;
import org.manifold.compiler.back.microfluidics.strategies.placement.LengthRuleStrategy;
import org.manifold.compiler.back.microfluidics.strategies.placement.MinimumChannelLengthStrategy;
import org.manifold.compiler.back.microfluidics.strategies.placement.PythagoreanLengthRuleStrategy;
import org.manifold.compiler.middle.Schematic;

/**
 * Contains strategies for generation of the positions of each component in
 * microfluidic circuits, uses the translation methods for 
 * each of the respective position strategies contained within
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class PlacementTranslationStrategySet extends TranslationStrategy {
  
  private ChannelPlacementConstraintStrategy channelPlacementStrategy;
  private ChipAreaRuleStrategy chipAreaRuleStrategy;
  private ControlPointPlacementConstraintStrategy controlPointPlacementStrategy;
  private CriticalAngleStrategy criticalAngleStrategy;
  private LengthRuleStrategy lengthRuleStrategy;
  private MinimumChannelLengthStrategy minimumChannelLengthStrategy;
  
  private boolean assumeInfiniteArea = false;
  /**
   * @return True if PlacementTranslationStrategySet will assume area of the
   * chip is infinite
   */
  public boolean getAssumeInfiniteArea() {
    return this.assumeInfiniteArea;
  }
  /**
   * Activate infinite chip area strategy for use when translation
   * TODO: an infinite chip area needs to be implemented still, so this should
   * always be set to False until that is created
   * 
   * @param setting  True then it uses the built-in infinite chip area strategy,
   * False uses FiniteChipAreaRuleStrategy
   */
  public void setAssumeInfiniteArea(boolean setting) {
    this.assumeInfiniteArea = setting;
    if (this.assumeInfiniteArea) {
       // TODO infinite chip area rule strategy
    } else {
      this.chipAreaRuleStrategy = new FiniteChipAreaRuleStrategy();
    }
  }
  
  /**
   * Set the channel placement strategy to use for translation
   * @param cps  The ChannelPlacementConstraintStrategy to use  
   */
  public void useChannelPlacementStrategy(
      ChannelPlacementConstraintStrategy cps) {
    this.channelPlacementStrategy = cps;
  }
   
  /**
   * Set the control point placement strategy to use for translation
   * @param cpps  The ControlPointPlacementConstraintStrategy to use  
   */
  public void useControlPointPlacementStrategy(
      ControlPointPlacementConstraintStrategy cpps) {
    this.controlPointPlacementStrategy = cpps;
  }
  
  /**
   * Set the critical angle strategy to use for translation
   * @param cas  The CriticalAngleStrategy to use  
   */
  public void useCriticalAngleStrategy(CriticalAngleStrategy cas) {
    this.criticalAngleStrategy = cas;
  }
  
  /**
   * Set the length rule strategy to use for translation
   * @param lrs The LengthRuleStrategy to use  
   */
  public void useLengthRuleStrategy(LengthRuleStrategy lrs) {
    this.lengthRuleStrategy = lrs;
  }
  
  /**
   * Set the minimum channel length strategy to use for translation
   * @param mls The MinimumChannelLengthStrategy to use  
   */
  public void useMinimumChannelLengthStrategy(
      MinimumChannelLengthStrategy mls) {
    this.minimumChannelLengthStrategy = mls;
  }
  
  /**
   * Constructs the placement translation strategy using default values for the
   * channelPlacementStrategy, controlPointPlacementStrategy,  
   * criticalAngleStrategy, lengthRuleStrategy and minimumChannelLengthStrategy 
   * which are used to determine the translation strategy for the placement
   * of each component
   */
  public PlacementTranslationStrategySet() {
    // initialize default strategies
    channelPlacementStrategy = new ChannelPlacementConstraintStrategy();
    setAssumeInfiniteArea(false);
    controlPointPlacementStrategy = 
        new ControlPointPlacementConstraintStrategy();
    criticalAngleStrategy = new CosineLawCriticalAngleStrategy();
    lengthRuleStrategy = new PythagoreanLengthRuleStrategy();
    minimumChannelLengthStrategy = new MinimumChannelLengthStrategy();
  }
  
  /**
   * Apply the translation strategies of each of the constituents of 
   * PlacementTranslationStrategySet and add all of their components to the list
   * of expressions exprs
   */
  @Override
  protected List<SExpression> translationStep(Schematic schematic, 
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    exprs.addAll(channelPlacementStrategy.translate(
        schematic, processParams, typeTable));
    exprs.addAll(chipAreaRuleStrategy.translate(
        schematic, processParams, typeTable));
    exprs.addAll(controlPointPlacementStrategy.translate(
        schematic, processParams, typeTable));
    exprs.addAll(criticalAngleStrategy.translate(
        schematic, processParams, typeTable));
    exprs.addAll(lengthRuleStrategy.translate(
        schematic, processParams, typeTable));
    exprs.addAll(minimumChannelLengthStrategy.translate(
        schematic, processParams, typeTable));
    return exprs;
  }
  
}