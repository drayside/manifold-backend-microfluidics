package org.manifold.compiler.back.microfluidics.strategies.placement;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.matlab.ManifoldValueHelper;
import org.manifold.compiler.back.microfluidics.matlab.StrategyVerifier;
import org.manifold.compiler.back.microfluidics.matlab.VerificationStatement;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

// Enforce a minimum channel length as defined by process parameters.
public class MinimumChannelLengthStrategy extends TranslationStrategy {

  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    // Iterate over all channels
    for (ConnectionValue c : schematic.getConnections().values()) {
      // TODO check port types
      Symbol channelLengthSym = SymbolNameGenerator.
          getsym_ChannelLength(schematic, c);
      exprs.add(QFNRA.assertGreaterEqual(
          channelLengthSym, 
          new Decimal(processParams.getMinimumChannelLength())));
    }
    return exprs;
  }
  
  @Override
  protected List<StrategyVerifier> matlabTranslationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    
    List<StrategyVerifier> verifiers = new LinkedList<StrategyVerifier>();
    StrategyVerifier verifier;
    
    for (ConnectionValue c: schematic.getConnections().values()) {
      verifier = new StrategyVerifier();
      verifier.addImport(
          "strategies.placement.verifyMinimumChannelLength");

      verifier.addStatements(
          ManifoldValueHelper.statementsForProcessParameters(processParams));
      verifier.addStatements(ManifoldValueHelper.statementsForValue(c));
      
      List<String> functionArgs = new LinkedList<String>();
      functionArgs.add("chip");
      functionArgs.add(ManifoldValueHelper.variableNameFromValue(c));
      
      verifier.addVerification(new VerificationStatement(
          "verifyMinimumChannelLength", functionArgs));
      
      verifiers.add(verifier);
    }
    
    return verifiers;
  }

}
