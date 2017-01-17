package org.manifold.compiler.back.microfluidics;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.UndeclaredIdentifierException;
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

  // Get the schematic's name for a NodeValue it contains
  public static String getNodeName(Schematic schematic, NodeValue nodeValue) {
    for (String nodeName : schematic.getNodes().keySet()) {
      try {
        if (schematic.getNode(nodeName).equals(nodeValue)) {
          return nodeName;
        }
      } catch (UndeclaredIdentifierException e) {
        continue;
      }
    }
    return null;
  }

  // Get a NodeValue's name for a PortValue it contains
  public static String getPortName(NodeValue nodeValue, PortValue portValue) {
    for (String portName : nodeValue.getPorts().keySet()) {
      try {
        if (nodeValue.getPort(portName).equals(portValue)) {
          return portName;
        }
      } catch (UndeclaredIdentifierException e) {
        continue;
      }
    }
    return null;
  }

}
