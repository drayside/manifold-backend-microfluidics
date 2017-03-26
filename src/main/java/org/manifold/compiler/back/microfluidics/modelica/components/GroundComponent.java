package org.manifold.compiler.back.microfluidics.modelica.components;


import org.manifold.compiler.back.microfluidics.modelica.ModelicaComponent;

import java.io.IOException;
import java.io.Writer;

public class GroundComponent extends ModelicaComponent {

  public GroundComponent(String name) {
    super(name);
  }

  protected void writeComponentDeclaration(Writer writer) throws IOException {
    writer.write("Maplesoft.Electrical.Analog.Passive.Ground ");
    writer.write(String.format("%s", this.componentName));
  }

}
