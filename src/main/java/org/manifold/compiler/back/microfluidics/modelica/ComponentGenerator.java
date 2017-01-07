package org.manifold.compiler.back.microfluidics.modelica;

import org.manifold.compiler.NodeValue;
import org.manifold.compiler.UndeclaredAttributeException;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.back.microfluidics.modelica.components.CapacitorComponent;
import org.manifold.compiler.middle.Schematic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ComponentGenerator {

  public List<ModelicaComponent> componentList(Schematic schematic) {
    List<ModelicaComponent> components = new ArrayList<>();
    Map<String, NodeValue> nodes = schematic.getNodes();

    for (String nodeName : nodes.keySet()) {
      NodeValue node = nodes.get(nodeName);

      // TODO: discriminate based on attributes
      CapacitorComponent capacitorComponent =
        new CapacitorComponent(nodeName, node);
      components.add(capacitorComponent);
    }

    return components;
  }

}
