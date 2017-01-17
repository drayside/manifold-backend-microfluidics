package org.manifold.compiler.back.microfluidics.modelica;

import org.manifold.compiler.NodeValue;

import java.io.IOException;
import java.io.Writer;

public abstract class ModelicaComponent {

  private static final String COMPONENT_ANNOTATION =
    "annotation(Placement(transformation(origin={0,0}," +
      "extent={{0,0},{0,0}},rotation=0)));";

  protected String componentName;
  protected NodeValue schematicNode;


  protected abstract void writeComponentDeclaration(Writer writer)
      throws IOException;

  public ModelicaComponent(String name, NodeValue node) {
    this.componentName = name;
    this.schematicNode = node;
  }

  public void write(Writer writer) throws IOException {
    writer.write("public ");
    writeComponentDeclaration(writer);
    writer.write(" ");
    writer.write(COMPONENT_ANNOTATION);
  }
}
