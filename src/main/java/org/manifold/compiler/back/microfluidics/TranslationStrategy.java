package org.manifold.compiler.back.microfluidics;

import java.io.Writer;

import org.manifold.compiler.middle.Schematic;

public interface TranslationStrategy {
  public void translate(Schematic schematic, 
      ProcessParameters processParams,
      PrimitiveTypeTable typeTable);
  public void write(Writer writer);
}
