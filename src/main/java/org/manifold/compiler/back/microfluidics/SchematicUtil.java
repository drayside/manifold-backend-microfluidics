package org.manifold.compiler.back.microfluidics;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.middle.Schematic;

/**
 * Misc functions for simulating the microfluidics in a given schematic
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class SchematicUtil {

  // Get the connection in schematic associated with this port.
  // TODO this is VERY EXPENSIVE, find an optimization. 
  // Josh: make schematic connections into hashset to make search constant time?
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
