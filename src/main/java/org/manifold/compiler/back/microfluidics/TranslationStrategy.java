package org.manifold.compiler.back.microfluidics;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.middle.Schematic;

public abstract class TranslationStrategy { 
  private List<SExpression> cachedExprs = new LinkedList<SExpression>();
  protected final List<SExpression> getCachedExprs() {
    return cachedExprs;
  }
  private boolean cacheValid = false;
  protected final void cacheIsValid() {
    cacheValid = true;
  }
  protected final void invalidateCache() {
    cachedExprs = new LinkedList<SExpression>();
    cacheValid = false;
  }
  
  public final List<SExpression> translate(Schematic schematic, 
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable) {
    invalidateCache();
    cachedExprs.addAll(_translate(schematic, processParams, typeTable));
    cacheIsValid();
    return cachedExprs;
  }
  
  // Cache-oblivious "real" translation step, overridden by implementors.
  protected abstract List<SExpression> _translate(Schematic schematic,
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable);
  
  public final List<SExpression> getTranslatedExprs() {
    if (cacheValid) {
      return cachedExprs;
    } else {
      throw new CodeGenerationError(
          "cannot retrieve translated exprs before translation is done"
          + " (cache may have been invalidated after a previous run)");
    }
  }
}
