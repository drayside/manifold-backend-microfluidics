package org.manifold.compiler.back.microfluidics.modelica.components;

import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.modelica.ModelicaComponent;

import java.io.IOException;
import java.io.Writer;

// TODO: replace with something microfluidics-related
public class CapacitorComponent extends ModelicaComponent {

  private double capacitance;

  public CapacitorComponent(String name, NodeValue node) {
    super(name, node);
    capacitance = 1;
  }

  protected void writeComponentDeclaration(Writer writer) throws IOException {
    writer.write("Maplesoft.Electrical.Analog.Passive.Capacitors.Capacitor ");
    writer.write(String.format("%s(C=%f)",
      this.componentName, this.capacitance));
  }
}
