package org.manifold.compiler.back.microfluidics.modelica;

import org.manifold.compiler.*;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.modelica.components.GroundComponent;
import org.manifold.compiler.back.microfluidics.modelica.components.ResistorComponent;
import org.manifold.compiler.back.microfluidics.modelica.components.VoltageSourceComponent;
import org.manifold.compiler.middle.Schematic;

import java.util.HashMap;
import java.util.Map;

public class ComponentGenerator {

  public Map<String, ModelicaComponent> componentList(Schematic schematic,
        PrimitiveTypeTable typeTable) {
    Map<String, ModelicaComponent> components = new HashMap<>();
    Map<String, NodeValue> nodes = schematic.getNodes();
    Map<String, ConnectionValue> connections = schematic.getConnections();

    for (String nodeName : nodes.keySet()) {
      NodeValue node = nodes.get(nodeName);

      if (node.getType().isSubtypeOf(typeTable.getFluidEntryNodeType())) {
        try {
          PortValue outputPort = node.getPort("output");
          double pressure = 1.0;

          VoltageSourceComponent voltageSource = new VoltageSourceComponent(
            nodeName, pressure);
          components.put(nodeName, voltageSource);

        } catch (UndeclaredIdentifierException e) {}

      } else if (node.getType().isSubtypeOf(typeTable.getFluidExitNodeType())) {
        GroundComponent groundComponent = new GroundComponent(nodeName);
        components.put(nodeName, groundComponent);
      }
    }

    // All connections are treated as resistors, though resistance may vary
    for (String connectionName : connections.keySet()) {
      ConnectionValue connection = connections.get(connectionName);

      double resistance = 1.0;

      ResistorComponent resistorComponent = new ResistorComponent(
        connectionName, resistance);
      components.put(connectionName, resistorComponent);
    }

    return components;
  }

}
