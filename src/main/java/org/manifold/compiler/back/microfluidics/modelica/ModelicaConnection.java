package org.manifold.compiler.back.microfluidics.modelica;

import java.io.IOException;
import java.io.Writer;

public class ModelicaConnection {

  private static final String CONNECTION_ANNOTATION =
    "annotation(Line(points={{0,0},{0,0},{0,0},{0,0}},color={0,0,255}," +
      "smooth=Smooth.None));";

  private String fromNodeName;
  private String fromPortName;
  private String toNodeName;
  private String toPortName;

  public ModelicaConnection(String fromNodeName,
      String fromPortName, String toNodeName, String toPortName) {
    this.fromNodeName = fromNodeName;
    this.fromPortName = fromPortName;
    this.toNodeName = toNodeName;
    this.toPortName = toPortName;
  }

  public void write(Writer writer) throws IOException {
    writer.write(String.format(
      "connect(%s.%s, %s.%s) ",
      fromNodeName, fromPortName, toNodeName, toPortName));

    writer.write(CONNECTION_ANNOTATION);
  }
}
