package org.manifold.compiler.back.microfluidics.modelica.components;

import org.manifold.compiler.back.microfluidics.modelica.ModelicaComponent;

import java.io.IOException;
import java.io.Writer;

// TODO: replace with something microfluidics-related
public class VoltageSourceComponent extends ModelicaComponent {

  private double voltage;

  public VoltageSourceComponent(String name, double voltage) {
    super(name);
    this.voltage = voltage;
  }

  protected void writeComponentDeclaration(Writer writer) throws IOException {
    writer.write(
      "Maplesoft.Electrical.Analog.Sources.Voltage.ConstantVoltage ");
    writer.write(String.format("%s(V=%f)",
      this.componentName, this.voltage));
  }
}
