package org.manifold.compiler.back.microfluidics;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.middle.Schematic;

public class SchematicUtil {

  // Get the connection associated with this port.
  // TODO this is VERY EXPENSIVE, find an optimization.
  public static ConnectionValue getConnection(
    Schematic schematic, PortValue port) {
    for (ConnectionValue conn : schematic.getConnections().values()) {
      if (conn.getFrom().equals(port) || conn.getTo().equals(port)) {
        return conn;
      }
    }
    return null;
  }

}
