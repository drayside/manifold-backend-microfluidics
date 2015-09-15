package org.manifold.compiler.back.microfluidics.strategies.pressureflow;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.manifold.compiler.Attributes;
import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.RealValue;
import org.manifold.compiler.UndeclaredAttributeException;
import org.manifold.compiler.Value;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

public class CircularChannelResistanceStrategy extends TranslationStrategy {

  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    for (ConnectionValue conn : schematic.getConnections().values()) {
      // TODO check port types
      // TODO it would be really cool to make the channel type
      // part of the SMT2 equations so we could solve for that too
      // TODO we are just assuming all channels are rectangular right now
      
      // TODO this might not stay here
      Symbol nDroplets = SymbolNameGenerator
          .getsym_ChannelMaxDroplets(schematic, conn);
      exprs.add(QFNRA.declareRealVariable(nDroplets));
      
      Symbol dropletResistance = SymbolNameGenerator
          .getsym_ChannelDropletResistance(schematic, conn);
      exprs.add(QFNRA.declareRealVariable(dropletResistance));
      
      //Need to find a refactored way to do this
      exprs.addAll(translateCircularChannel(schematic, conn));
    }
    return exprs;
  }
  
  private List<SExpression> translateCircularChannel(
	      Schematic schematic, ConnectionValue channel) {
	    List<SExpression> exprs = new LinkedList<>();
	    // R = (8 * mu * L) / (pi * R^4)
	    // for channel radius R
	    // total length L
	    // viscosity of the solvent is mu
	    Symbol chR = SymbolNameGenerator.getsym_ChannelResistance(
	        schematic, channel);
	    Symbol R = SymbolNameGenerator.getsym_ChannelRadius(schematic, channel);
	    Symbol mu = SymbolNameGenerator.getsym_ChannelViscosity(schematic, channel);
	    Symbol chL = SymbolNameGenerator.getsym_ChannelLength(schematic, channel);
	    
	    exprs.add(QFNRA.declareRealVariable(chR));
	    exprs.add(QFNRA.assertGreater(chR, new Decimal(0.0)));
	    exprs.add(QFNRA.assertGreater(R, new Decimal(0.0)));
	    exprs.add(QFNRA.declareRealVariable(R));
	    exprs.add(QFNRA.assertGreater(mu, new Decimal(0.0)));
	    exprs.add(QFNRA.declareRealVariable(chL));
	    exprs.add(QFNRA.assertGreater(chL, new Decimal(0.0)));
	    
	    /*Some typechecking needs to happen here to ensure the attributes are
	     * indeed of type RealValue*/
	    RealValue length;
		try {
			length = (RealValue) channel.getAttribute("length");
			exprs.add(QFNRA.assertEqual(chL, new Decimal(length.toDouble())));
		    RealValue radius = (RealValue) channel.getAttribute("radius");
		    exprs.add(QFNRA.assertEqual(R, new Decimal(radius.toDouble())));
		} catch (UndeclaredAttributeException e) {
			e.printStackTrace();
		}
	    
	    
	    SExpression resistancecircular = QFNRA.assertEqual(chR,
	        QFNRA.divide(QFNRA.multiply(
	            new Decimal(8.0), QFNRA.multiply(mu, chL)), 
	            QFNRA.multiply(new Decimal(Math.PI), QFNRA.pow(R, new Decimal(4.0))
	                )));
	    exprs.add(resistancecircular);
	    return exprs;
	  }
  
}
