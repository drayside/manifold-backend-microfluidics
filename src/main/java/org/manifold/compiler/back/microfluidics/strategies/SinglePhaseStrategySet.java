package org.manifold.compiler.back.microfluidics.strategies;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.strategies.singlephase.ElectrophoreticCrossStrategy;
import org.manifold.compiler.back.microfluidics.strategies.singlephase.ReservoirDeviceStrategy;
import org.manifold.compiler.middle.Schematic;

public class SinglePhaseStrategySet extends TranslationStrategy {

  private ElectrophoreticCrossStrategy electrophoreticCrossStrategy;
  public void useElectrophoreticCrossStrategy(
      ElectrophoreticCrossStrategy strat) {
    this.electrophoreticCrossStrategy = strat;
  }

  private ReservoirDeviceStrategy reservoirDeviceStrategy;
  public void useReservoirDeviceStrategy(ReservoirDeviceStrategy strat) {
    this.reservoirDeviceStrategy = strat;
  }
  
  public SinglePhaseStrategySet() {
    // initialize default strategies
    this.electrophoreticCrossStrategy = new ElectrophoreticCrossStrategy();
    this.reservoirDeviceStrategy = new ReservoirDeviceStrategy();
  }
  
  @Override
  protected List<SExpression> translationStep(Schematic schematic, 
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    exprs.addAll(electrophoreticCrossStrategy.translate(
        schematic, processParams, typeTable));
    exprs.addAll(reservoirDeviceStrategy.translate(
        schematic, processParams, typeTable));
    return exprs;
  }
  
}
