package org.manifold.compiler.back.microfluidics.modelica;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.back.microfluidics.SchematicUtil;
import org.manifold.compiler.middle.Schematic;

import java.io.IOException;
import java.io.Writer;

public class ModelicaConnection {

  private static final String CONNECTION_ANNOTATION =
    "annotation(Line(points={{0,0},{0,0},{0,0},{0,0}},color={0,0,255}," +
      "smooth=Smooth.None));";

  private Schematic schematic;
  private ConnectionValue connection;

  public ModelicaConnection(Schematic schematic, ConnectionValue connection) {
    this.schematic = schematic;
    this.connection = connection;
  }

  public void write(Writer writer) throws IOException {
    NodeValue fromNode = connection.getFrom().getParent();
    String fromNodeName = SchematicUtil.getNodeName(schematic, fromNode);

    NodeValue toNode = connection.getTo().getParent();
    String toNodeName = SchematicUtil.getNodeName(schematic, toNode);

    String fromPortName = SchematicUtil.getPortName(
      fromNode, connection.getFrom());
    String toPortName = SchematicUtil.getPortName(toNode, connection.getTo());

    //writer.write(String.format(
    //  "connect(%s.%s, %s.%s) ",
    //  fromNodeName, fromPortName, toNodeName, toPortName));

    // TODO: replace with above once we're no longer using electrical library
    writer.write(String.format(
      "connect(%s.n, %s.p) ",
      fromNodeName, toNodeName));

    writer.write(CONNECTION_ANNOTATION);
  }
}
