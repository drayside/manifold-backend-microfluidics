package org.manifold.compiler.back.microfluidics.strategies.placement;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.middle.Schematic;

// Strategy to constrain the coordinates of nodes based on
// the length of the channel connecting them.
public abstract class LengthRuleStrategy extends TranslationStrategy {
  /**
   * @return a connecting having any port of n1 and n2 as its endpoints,
   * or null if no such channel exists
   */
  public ConnectionValue getConnectingChannel(Schematic schematic,
      NodeValue n1, NodeValue n2) {
    for (ConnectionValue conn : schematic.getConnections().values()) {
      for (PortValue p1 : n1.getPorts().values()) {
        for (PortValue p2 : n2.getPorts().values()) {
          // check if we have n1 --(conn)-> n2
          if (conn.getFrom().equals(p1) && conn.getTo().equals(p2)) {
            return conn;
          }
          // check if we have n2 --(conn)-> n1
          if (conn.getFrom().equals(p2) && conn.getTo().equals(p1)) {
            return conn;
          }
        }
      }
    }
    // couldn't find anything
    return null;
  }
}
