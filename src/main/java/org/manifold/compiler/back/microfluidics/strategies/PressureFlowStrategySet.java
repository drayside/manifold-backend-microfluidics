package org.manifold.compiler.back.microfluidics.strategies;

import java.sql.Types;
import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.strategies.pressureflow.ChannelResistanceStrategy;
import org.manifold.compiler.back.microfluidics.strategies.pressureflow.FluidEntryExitDeviceStrategy;
import org.manifold.compiler.back.microfluidics.strategies.pressureflow.PressureFlowStrategy;
import org.manifold.compiler.back.microfluidics.strategies.pressureflow.SimplePressureFlowStrategy;
import org.manifold.compiler.middle.Schematic;
/**
 * Contains strategies for calculation of the pressures within
 * microfluidic circuits, uses the translation methods for 
 * each of the constituent pressure strategies used
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class PressureFlowStrategySet extends TranslationStrategy {

  private boolean worstCaseAnalysis = false;
  /**
   * Toggle whether worst case analysis should be performed by the constituent
   * strategies
   * 
   * @param b  True if worst case analysis should be performed
   */
  public void performWorstCastAnalysis(boolean b) {
    this.worstCaseAnalysis = b;
  }
  
  private ChannelResistanceStrategy channelResistanceStrategy;
  /**
   * Provide a non-default dropletConstraintStrategy, must also provide a
   * FluidEntryExitDeviceStrategy and pressureFlow to create a complete
   * PressureFlowStrategySet
   * 
   * @param strat  The ChannelResistanceStrategy for use in the
   * PressureFlowStrategySet
   */
  public void useChannelResistanceStrategy(
      ChannelResistanceStrategy strat) {
    this.channelResistanceStrategy = strat;
  }
  
  private FluidEntryExitDeviceStrategy entryExitStrategy;
  /**
   * Provide a non-default FluidEntryExitDeviceStrategy, must also provide a
   * dropletConstraintStrategy and pressureFlow to create a complete
   * PressureFlowStrategySet
   * 
   * @param strat  The FluidEntryExitDeviceStrategy for use in the
   * PressureFlowStrategySet
   */
  public void useFluidEntryExitDeviceStrategy(
      FluidEntryExitDeviceStrategy strat) {
    this.entryExitStrategy = strat;
  }
  
  private PressureFlowStrategy pressureFlow;
  /**
   * Provide a non-default pressureFlow, must also provide a
   * FluidEntryExitDeviceStrategy and dropletConstraintStrategy to create a 
   * complete PressureFlowStrategySet
   * 
   * @param strat  The PressureFlowStrategy for use in the
   * PressureFlowStrategySet
   */
  public void usePressureFlowStrategy(PressureFlowStrategy strat) {
    this.pressureFlow = strat;
  }
  
  /**
   * Constructs a default ChannelTesistanceStrategy, FluidExitDeviceStrategy and
   * pressureFlow to make a default PressureFlowStrategySet 
   */
  public PressureFlowStrategySet() {
    channelResistanceStrategy = new ChannelResistanceStrategy();
    entryExitStrategy = new FluidEntryExitDeviceStrategy();
    pressureFlow = new SimplePressureFlowStrategy(worstCaseAnalysis);
  }
  
  /**
   * Translation of PressureFlowStrategySet uses the translation methods of
   * ChannelTesistanceStrategy, FluidExitDeviceStrategy and pressureFlow on
   * their respective types within the schematic
   */
  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    exprs.addAll(channelResistanceStrategy.translate(
        schematic, processParams, typeTable));
    exprs.addAll(entryExitStrategy.translate(
        schematic, processParams, typeTable));
    exprs.addAll(pressureFlow.translate(
        schematic, processParams, typeTable));
    return exprs;
  }

}