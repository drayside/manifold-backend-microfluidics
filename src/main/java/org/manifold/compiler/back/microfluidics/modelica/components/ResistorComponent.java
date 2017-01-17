package org.manifold.compiler.back.microfluidics.modelica.components;

import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.modelica.ModelicaComponent;

import java.io.IOException;
import java.io.Writer;

// TODO: replace with something microfluidics-related
public class ResistorComponent extends ModelicaComponent {

  private double resistance;

  public ResistorComponent(String name, NodeValue node) {
    super(name, node);
    resistance = 1;
  }

  protected void writeComponentDeclaration(Writer writer) throws IOException {
    writer.write("Maplesoft.Electrical.Analog.Passive.Resistors.Resistor ");
    writer.write(String.format(
      "%s(R=%f, T_ref=300.15, alpha=0, useHeatPort=false, T=R1.T_ref)",
      this.componentName, this.resistance));
  }

}
