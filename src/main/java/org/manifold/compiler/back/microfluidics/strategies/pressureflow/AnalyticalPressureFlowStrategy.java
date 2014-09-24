package org.manifold.compiler.back.microfluidics.strategies.pressureflow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.PortValue;
import org.manifold.compiler.UndeclaredIdentifierException;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;
import org.manifold.compiler.back.microfluidics.PrimitiveTypeTable;
import org.manifold.compiler.back.microfluidics.ProcessParameters;
import org.manifold.compiler.back.microfluidics.TranslationStrategy;
import org.manifold.compiler.back.microfluidics.smt2.Decimal;
import org.manifold.compiler.back.microfluidics.smt2.QFNRA;
import org.manifold.compiler.back.microfluidics.smt2.SExpression;
import org.manifold.compiler.back.microfluidics.smt2.Symbol;
import org.manifold.compiler.back.microfluidics.smt2.SymbolNameGenerator;
import org.manifold.compiler.middle.Schematic;

public class AnalyticalPressureFlowStrategy extends TranslationStrategy {

  // get the connection associated with this port
  // TODO this is VERY EXPENSIVE, find an optimization
  protected ConnectionValue getConnection(Schematic schematic, PortValue port) {
    for (ConnectionValue conn : schematic.getConnections().values()) {
      if (conn.getFrom().equals(port) || conn.getTo().equals(port)) {
        return conn;
      }
    }
    return null;
  }
  
  @Override
  protected List<SExpression> translationStep(Schematic schematic,
      ProcessParameters processParams, PrimitiveTypeTable typeTable) {
    List<SExpression> exprs = new LinkedList<>();
    Set<NodeValue> closedNodes = new HashSet<NodeValue>();
    // TODO solve for the pressures/flows at ports instead of at nodes (?)
    // this may be more useful for dual-phase systems
    for (NodeValue startNode : schematic.getNodes().values()) {
      closedNodes.add(startNode);
      // we only care about pressure CPs
      if (!(startNode.getType().isSubtypeOf(
          typeTable.getPressureControlPointNodeType()))) {
        continue;
      }
      for (PortValue startPort : startNode.getPorts().values()) {
        // find all channels between this port and the nearest port at a 
        // pressure-affected node/port
        ExpandedPath expansion = expandThrough(
            startPort, schematic, typeTable);
        // Check to make sure that the last port we expanded into
        // does not correspond to a closed node.
        NodeValue lastNode = expansion.ports.get(expansion.ports.size() - 1)
            .getParent();
        if (closedNodes.contains(lastNode)) {
          continue;
        }
        // The equation we want is P_i = P_j + (-1)^phi Q_ij R_ij
        // We can simplify this a bit:
        // assuming the connection goes from P_i to P_j
        // (in the sense of the from() and to() values of the ConnectionValue)
        // we take the conventional direction of channel current to be
        // from P_i and to P_j, and so we get (P_i - P_j) = Q_ij * R_ij
        // We loop over our expanded path and generate this constraint
        // for each connection we expanded through.
        
        for (int i = 0; i < expansion.connections.size(); ++i) {
          ConnectionValue channel = expansion.connections.get(i);
          PortValue portFrom = channel.getFrom();
          PortValue portTo = channel.getTo();
          Symbol portPressureFrom = SymbolNameGenerator.
              getSym_PortPressure(schematic, portFrom);
          Symbol portPressureTo = SymbolNameGenerator.
              getSym_PortPressure(schematic, portTo);
          Symbol channelFlow = SymbolNameGenerator.
              getsym_ChannelFlowRate(schematic, channel);
          Symbol channelResistance = SymbolNameGenerator.
              getsym_ChannelResistance(schematic, channel);
          exprs.add(QFNRA.assertEqual(
              QFNRA.subtract(portPressureFrom, portPressureTo),
              QFNRA.multiply(channelFlow, channelResistance)));
        }
        // There's a second expression we have to generate here:
        // any node that we are allowed to expand through has to have the
        // property that the total flow into that node is zero.
        // (In other words, for two channels connected to the same node,
        // the flow in one channel is equal to the flow in the other,
        // modulo conventional flow direction.)
        for (int i = 1; i < expansion.connections.size(); ++i) {
          ConnectionValue channelPost = expansion.connections.get(i);
          Symbol flowPost = SymbolNameGenerator.getsym_ChannelFlowRate(
              schematic, channelPost);
          ConnectionValue channelPre = expansion.connections.get(i - 1);
          Symbol flowPre = SymbolNameGenerator.getsym_ChannelFlowRate(
              schematic, channelPre);
          // get all four ports
          PortValue channelPreFrom = channelPre.getFrom();
          PortValue channelPreTo = channelPre.getTo();
          PortValue channelPostFrom = channelPost.getFrom();
          PortValue channelPostTo = channelPost.getTo();
          // find which ports match on these channels
          if (channelPreTo.equals(channelPostFrom) 
              || channelPostTo.equals(channelPreFrom)) {
            // the channels have the same conventional flow direction
            exprs.add(QFNRA.assertEqual(flowPre, flowPost));
          } else if (channelPreTo.equals(channelPostTo)
              || channelPreFrom.equals(channelPostFrom)) {
            // the channels have opposite conventional flow direction
            exprs.add(QFNRA.assertEqual(new Decimal(0.0), 
                QFNRA.add(flowPre, flowPost)));
          } else {
            throw new CodeGenerationError(
                "inconsistent port matching when expanding channels");
          }
        }
      }
    }
    return exprs;
  }

  class ExpandedPath {
    public List<ConnectionValue> connections;
    public List<PortValue> ports;
    
    public ExpandedPath() {
      connections = new ArrayList<>();
      ports = new ArrayList<>();
    }
  }
  
  private ExpandedPath expandThrough(
      PortValue port, Schematic schematic, PrimitiveTypeTable typeTable) {
    ExpandedPath expansion = new ExpandedPath();
    PortValue nextPort = port;
    while (true) {
      ConnectionValue nextConn = getConnection(schematic, nextPort);
      if (nextConn == null) {
        // TODO throw exception
      }
      expansion.connections.add(nextConn);
      PortValue destPort = null;
      if (nextConn.getFrom().equals(nextPort)) {
        destPort = nextConn.getTo();
      } else if (nextConn.getTo().equals(nextPort)) {
        destPort = nextConn.getFrom();
      }
      if (destPort == null) {
        // could not expand: port not connected
        // TODO throw exception
      }
      expansion.ports.add(destPort);
      NodeValue dest = destPort.getParent();
      if (dest.getType().isSubtypeOf(
          typeTable.getPressureControlPointNodeType())) {
        // if this node is a pressure CP, we're done
        return expansion;
      } else if (dest.getType().isSubtypeOf(
          typeTable.getChannelCrossingNodeType())) {
        // if this is a channel crossing, expand through the crossing
        // in the direction in which we came
        // check each port to see where we came in
        try {
          if (dest.getPort("channelA0").equals(destPort)) {
            nextPort = dest.getPort("channelA1");
          } else if (dest.getPort("channelA1").equals(destPort)) {
            nextPort = dest.getPort("channelA0");
          } else if (dest.getPort("channelB0").equals(destPort)) {
            nextPort = dest.getPort("channelB1");
          } else if (dest.getPort("channelB1").equals(destPort)) {
            nextPort = dest.getPort("channelB0");
          } else {
            throw new CodeGenerationError("expanded into a channel crossing"
                + " that does not contain the expanded destination port");
          }
        } catch (UndeclaredIdentifierException e) {
          throw new CodeGenerationError(
              "could not find required port on channel crossing");
        }
      } else {
        // TODO we need more information about how to expand through other nodes
        throw new CodeGenerationError("don't know how to expand through node '"
            + schematic.getNodeName(dest) + "'");
      } 
    }
  }
  
}
