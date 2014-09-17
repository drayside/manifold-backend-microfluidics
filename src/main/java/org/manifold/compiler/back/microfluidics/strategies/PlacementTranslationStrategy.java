package org.manifold.compiler.back.microfluidics.strategies;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.strategies.placement.LengthRuleStrategy;
import org.manifold.compiler.middle.Schematic;

public class PlacementTranslationStrategy extends TranslationStrategy {
  
  private boolean assumeInfiniteArea = false;
  public boolean getAssumeInfiniteArea() {
    return this.assumeInfiniteArea;
  }
  public void setAssumeInfiniteArea(boolean setting) {
    this.assumeInfiniteArea = setting;
  }
  
  private LengthRuleStrategy lengthRuleStrategy;
  public void useLengthRuleStrategy(LengthRuleStrategy lrs) {
    this.lengthRuleStrategy = lrs;
  }
  
  @Override
  protected List<SExpression> translationStep(Schematic schematic, 
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    exprs.addAll(lengthRuleStrategy.translate(
        schematic, processParams, typeTable));
    return exprs;
  }
  
}
