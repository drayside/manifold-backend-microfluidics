package org.manifold.compiler.back.microfluidics;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.back.microfluidics.matlab.MatlabStatement;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.middle.Schematic;

public abstract class TranslationStrategy { 
  private List<SExpression> cachedExprs = new LinkedList<SExpression>();
  private List<MatlabStatement> cachedMatlabExprs = new LinkedList<MatlabStatement>();

  protected final List<SExpression> getCachedExprs() {
    return cachedExprs;
  }
  
  protected final List<MatlabStatement> getCachedMatlabExprs() {
	  return cachedMatlabExprs;
  }
 
  private boolean cacheValid = false;
  protected final void cacheIsValid() {
    cacheValid = true;
  }
  
  private boolean matlabCacheValid = false;
  protected final void matlabCacheIsValid() {
    matlabCacheValid = true;
  }

  protected final void invalidateCache() {
    cachedExprs = new LinkedList<SExpression>();
    cacheValid = false;
  }
  
  protected final void invalidateMatlabCache() {
    cachedMatlabExprs = new LinkedList<MatlabStatement>();
    matlabCacheValid = false;
  }
  
  public final List<SExpression> translate(Schematic schematic, 
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable) {
    invalidateCache();
    cachedExprs.addAll(translationStep(schematic, processParams, typeTable));
    cacheIsValid();
    return cachedExprs;
  }
  
  public final List<MatlabStatement> translateMatlab(Schematic schematic,
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable) {
    invalidateMatlabCache();
    cachedMatlabExprs.addAll(matlabTranslationStep(schematic, processParams, typeTable));
    cacheIsValid();
    return cachedMatlabExprs;
  }
  
  // Cache-oblivious "real" translation step, overridden by implementors.
  protected abstract List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable);
  
  protected List<MatlabStatement> matlabTranslationStep(Schematic schematic,
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable) {
    return new LinkedList<MatlabStatement>();
  }
  
  public final List<SExpression> getTranslatedExprs() {
    if (cacheValid) {
      return cachedExprs;
    } else {
      throw new CodeGenerationError(
          "cannot retrieve translated exprs before translation is done"
          + " (cache may have been invalidated after a previous run)");
    }
  }
  
  public final List<MatlabStatement> getMatlabTranslatedExprs() {
    if (matlabCacheValid) {
      return cachedMatlabExprs;
    } else {
      throw new CodeGenerationError(
          "cannot retrieve translated matlab exprs before translation is done"
          + " (cache may have been invalidated after a previous run)");
    }
  }
  
  /**
   * @return a connection having any port of n1 and n2 as its endpoints,
   * or null if no such channel exists
   */
  public ConnectionValue getConnectingChannel(Schematic schematic,
      PrimitiveTypeTable typeTable,
      NodeValue n1, NodeValue n2) {
    return getConnectingChannel(schematic, typeTable, n1, n2, false);
  }
  
  /**
   * @return a connection from any port of n1 to any port of n2
   * (if directed is true), or a connection having any port of n1 and n2 
   * as its endpoints (if directed is false),
   * or null if no such channel exists
   */
  public ConnectionValue getConnectingChannel(Schematic schematic,
      PrimitiveTypeTable typeTable,
      NodeValue n1, NodeValue n2, boolean directed) {
    for (ConnectionValue conn : schematic.getConnections().values()) {
      // TODO check whether the port types are correct
      for (PortValue p1 : n1.getPorts().values()) {
        for (PortValue p2 : n2.getPorts().values()) {
          // check if we have n1 --(conn)-> n2 (forward direction)
          if (conn.getFrom().equals(p1) && conn.getTo().equals(p2)) {
            return conn;
          }
          // check if we have n2 --(conn)-> n1 (reverse direction)
          if (!directed && conn.getFrom().equals(p2) 
              && conn.getTo().equals(p1)) {
            return conn;
          }
        }
      }
    }
    // couldn't find anything
    return null;
  }
}
