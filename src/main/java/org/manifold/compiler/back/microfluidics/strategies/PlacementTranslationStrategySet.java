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

public class PlacementTranslationStrategySet extends TranslationStrategy {
  
  private boolean assumeInfiniteArea = false;
  public boolean getAssumeInfiniteArea() {
    return this.assumeInfiniteArea;
  }
  
  private ChannelPlacementConstraintStrategy channelPlacementStrategy;
  private ChipAreaRuleStrategy chipAreaRuleStrategy;
  private ControlPointPlacementConstraintStrategy controlPointPlacementStrategy;
  private CriticalAngleStrategy criticalAngleStrategy;
  private LengthRuleStrategy lengthRuleStrategy;
  private MinimumChannelLengthStrategy minimumChannelLengthStrategy;
  
  public void useChannelPlacementStrategy(
      ChannelPlacementConstraintStrategy cps) {
    this.channelPlacementStrategy = cps;
  }
   
  public void setAssumeInfiniteArea(boolean setting) {
    this.assumeInfiniteArea = setting;
    if (this.assumeInfiniteArea) {
       // TODO infinite chip area rule strategy
    } else {
      this.chipAreaRuleStrategy = new FiniteChipAreaRuleStrategy();
    }
  }
  
  public void useControlPointPlacementStrategy(
      ControlPointPlacementConstraintStrategy cps) {
    this.controlPointPlacementStrategy = cps;
  }
  
  public void useCriticalAngleStrategy(CriticalAngleStrategy cas) {
    this.criticalAngleStrategy = cas;
  }
  
  public void useLengthRuleStrategy(LengthRuleStrategy lrs) {
    this.lengthRuleStrategy = lrs;
  }
  
  public void useMinimumChannelLengthStrategy(
      MinimumChannelLengthStrategy mls) {
    this.minimumChannelLengthStrategy = mls;
  }
  
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
