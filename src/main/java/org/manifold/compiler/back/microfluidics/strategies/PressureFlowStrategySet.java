package org.manifold.compiler.back.microfluidics.strategies;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.strategies.pressureflow.ChannelResistanceStrategy;
import org.manifold.compiler.back.microfluidics.strategies.pressureflow.FluidEntryExitDeviceStrategy;
import org.manifold.compiler.middle.Schematic;

public class PressureFlowStrategySet extends TranslationStrategy {

  private ChannelResistanceStrategy channelResistanceStrategy;
  public void useChannelResistanceStrategy(
      ChannelResistanceStrategy strat) {
    this.channelResistanceStrategy = strat;
  }
  
  private FluidEntryExitDeviceStrategy entryExitStrategy;
  public void useFluidEntryExitDeviceStrategy(
      FluidEntryExitDeviceStrategy strat) {
    this.entryExitStrategy = strat;
  }
  
  public PressureFlowStrategySet() {
    channelResistanceStrategy = new ChannelResistanceStrategy();
    entryExitStrategy = new FluidEntryExitDeviceStrategy();
  }
  
  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    exprs.addAll(channelResistanceStrategy.translate(
        schematic, processParams, typeTable));
    exprs.addAll(entryExitStrategy.translate(
        schematic, processParams, typeTable));
    return exprs;
  }

}
