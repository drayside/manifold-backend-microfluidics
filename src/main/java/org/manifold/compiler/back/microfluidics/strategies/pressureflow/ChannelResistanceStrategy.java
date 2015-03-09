package org.manifold.compiler.back.microfluidics.strategies.pressureflow;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

public class ChannelResistanceStrategy extends TranslationStrategy {

  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    for (ConnectionValue conn : schematic.getConnections().values()) {
      // TODO check port types
      // TODO it would be really cool to make the channel type
      // part of the SMT2 equations so we could solve for that too
      // TODO we are just assuming all channels are rectangular right now
      exprs.addAll(translateRectangularChannel(schematic, conn));
    }
    return exprs;
  }

  private List<SExpression> translateRectangularChannel(
      Schematic schematic, ConnectionValue channel) {
    List<SExpression> exprs = new LinkedList<>();
    // R = (12 * mu * L) / (w * h^3 * (1 - 0.630 (h/w)) )
    // for channel width w, height h, h < w
    // total length L
    // viscosity of the solvent is mu
    Symbol chR = SymbolNameGenerator.getsym_ChannelResistance(
        schematic, channel);
    Symbol w = SymbolNameGenerator.getsym_ChannelWidth(schematic, channel);
    Symbol h = SymbolNameGenerator.getsym_ChannelHeight(schematic, channel);
    Symbol mu = SymbolNameGenerator.getsym_ChannelViscosity(schematic, channel);
    Symbol chL = SymbolNameGenerator.getsym_ChannelLength(schematic, channel);
    
    exprs.add(QFNRA.declareRealVariable(chR));
    exprs.add(QFNRA.assertGreater(chR, new Decimal(0.0)));
    exprs.add(QFNRA.declareRealVariable(w));
    exprs.add(QFNRA.assertGreater(w, new Decimal(0.0)));
    exprs.add(QFNRA.declareRealVariable(h));
    exprs.add(QFNRA.assertGreater(h, new Decimal(0.0)));
    exprs.add(QFNRA.declareRealVariable(mu));
    exprs.add(QFNRA.assertGreater(mu, new Decimal(0.0)));
    exprs.add(QFNRA.declareRealVariable(chL));
    exprs.add(QFNRA.assertGreater(chL, new Decimal(0.0)));
    
    SExpression resistanceRectangular = QFNRA.assertEqual(chR,
        QFNRA.divide(QFNRA.multiply(
            new Decimal(12.0), QFNRA.multiply(mu, chL)), 
            QFNRA.multiply(w, QFNRA.multiply(QFNRA.pow(h, new Decimal(3.0)),
                QFNRA.subtract(new Decimal(1.0), 
                    QFNRA.multiply(new Decimal(0.630), QFNRA.divide(h, w)))))));
    exprs.add(resistanceRectangular);
    SExpression heightLessThanWidth = QFNRA.assertLessThan(h, w);
    exprs.add(heightLessThanWidth);
    return exprs;
  }
  
}
