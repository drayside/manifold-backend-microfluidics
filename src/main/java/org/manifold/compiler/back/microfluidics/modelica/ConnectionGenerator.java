package org.manifold.compiler.back.microfluidics.modelica;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.UndeclaredIdentifierException;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.middle.Schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConnectionGenerator {

  public List<ModelicaConnection> connectionList(Schematic schematic) {
    List<ModelicaConnection> connections = new ArrayList<>();

    Map<String, ConnectionValue> schematicConnections =
      schematic.getConnections();

    for (String connectionName : schematicConnections.keySet()) {
      ConnectionValue connectionValue =
        schematicConnections.get(connectionName);

      ModelicaConnection modelicaConnection =
        new ModelicaConnection(schematic, connectionValue);
      connections.add(modelicaConnection);
    }

    return connections;
  }

}
