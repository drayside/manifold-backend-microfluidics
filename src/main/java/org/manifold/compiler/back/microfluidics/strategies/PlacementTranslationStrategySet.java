package org.manifold.compiler.back.microfluidics.strategies;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.strategies.placement.*;
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
  private ChannelLengthStrategy channelLengthStrategy;
  
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
      ChannelLengthStrategy mls) {
    this.channelLengthStrategy = mls;
  }
  
  public PlacementTranslationStrategySet() {
    // initialize default strategies
    channelPlacementStrategy = new ChannelPlacementConstraintStrategy();
    setAssumeInfiniteArea(false);
    controlPointPlacementStrategy = 
        new ControlPointPlacementConstraintStrategy();
    criticalAngleStrategy = new CosineLawCriticalAngleStrategy();
    lengthRuleStrategy = new PythagoreanLengthRuleStrategy();
    channelLengthStrategy = new ChannelLengthStrategy();
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
    exprs.addAll(channelLengthStrategy.translate(
        schematic, processParams, typeTable));
    return exprs;
  }
  
}
