package org.manifold.compiler.back.microfluidics.matlab;

import java.util.LinkedList;
import java.util.List;

import org.manifold.compiler.ConnectionValue;
import org.manifold.compiler.NodeValue;
import org.manifold.compiler.Value;
import org.manifold.compiler.back.microfluidics.CodeGenerationError;


/* Collection of helpers to generate the appropriate statements
 * to interact with Manifold values in Matlab
 */
public class ManifoldValueHelper {
  public List<MatlabStatement> statementsForValue(Value value) {
    List<MatlabStatement> stmts;
    
    if (value instanceof NodeValue) {
      stmts = statementsForNodeValue((NodeValue) value);
    } else if (value instanceof ConnectionValue) {
      stmts = statementsForConnectionValue((ConnectionValue) value);
    } else {
      throw new CodeGenerationError(
          "Could not generate matlab statements for type " +
          value.getType().toString());
    }
    
    return stmts;
  }
  
  public List<MatlabStatement> statementsForNodeValue(NodeValue value) {
    List<MatlabStatement> stmts = new LinkedList<MatlabStatement>();
    
    stmts.add(new ImportStatement("types.Node"));
    // TODO extract x,y co-ordinates from the node and instantiate

    return stmts;
  }
  
  public List<MatlabStatement> statementsForConnectionValue(
      ConnectionValue value) {
    List <MatlabStatement> stmts = new LinkedList<MatlabStatement>();
    
    stmts.add(new ImportStatement("types.Channel"));
    // TODO extract channel length from the channel and instantiate
    
    return stmts;
  }
}
