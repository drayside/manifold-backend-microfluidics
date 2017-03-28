package org.manifold.compiler.back.microfluidics.modelica;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.UndeclaredIdentifierException;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.SchematicUtil;
import org.manifold.compiler.middle.Schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConnectionGenerator {

  public List<ModelicaConnection> connectionList(Schematic schematic,
      PrimitiveTypeTable typeTable) {
    List<ModelicaConnection> connections = new ArrayList<>();

    Map<String, ConnectionValue> schematicConnections =
      schematic.getConnections();

    // Each connection is modeled as a resistor component.
    // These resistors must connect to the nodes involved in the connection.
    for (String connectionName : schematicConnections.keySet()) {
      ConnectionValue connection =
        schematicConnections.get(connectionName);

      NodeValue fromNode = connection.getFrom().getParent();
      String fromNodeName = SchematicUtil.getNodeName(schematic, fromNode);

      NodeValue toNode = connection.getTo().getParent();
      String toNodeName = SchematicUtil.getNodeName(schematic, toNode);

      ModelicaConnection connectionBeforeResistor = new ModelicaConnection(
        fromNodeName, "p", connectionName, "p");
      connections.add(connectionBeforeResistor);

      ModelicaConnection connectionAfterResistor = new ModelicaConnection(
        connectionName, "n", toNodeName, "p");
      connections.add(connectionAfterResistor);
    }

    // Cycle the ground (fluidExit) at the end of the circuit
    // to the voltageSource (fluidEntry) at the start.
    Map<String, NodeValue> nodes = schematic.getNodes();
    String entryNodeName = null;
    String exitNodeName = null;
    for (String nodeName : nodes.keySet()) {
      NodeValue node = nodes.get(nodeName);
      if (node.getType().isSubtypeOf(typeTable.getFluidEntryNodeType())) {
        entryNodeName = nodeName;
      } else if (node.getType().isSubtypeOf(typeTable.getFluidExitNodeType())) {
        exitNodeName = nodeName;
      }
    }
    if (entryNodeName != null && exitNodeName != null) {
      ModelicaConnection loopConnection = new ModelicaConnection(
        exitNodeName, "p", entryNodeName, "n");
      connections.add(loopConnection);
    }

    return connections;
  }

}
