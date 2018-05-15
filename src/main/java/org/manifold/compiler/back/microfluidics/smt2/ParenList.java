package org.manifold.compiler.back.microfluidics.smt2;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * List of SExpressions that contain expression in valid QF_NRA form. When write
 * is called, parentheses are put around each expression
 * 
 * @author Murphy? Comments by Josh
 *
 */
public class ParenList extends SExpression {

  private List<SExpression> exprs = new ArrayList<>();
  public List<SExpression> getExprs() {
    return ImmutableList.copyOf(exprs);
  }
  
  /**
   * Empty ParenList constructor to allow for void ParenLists to be created 
   */
  public ParenList() { }
  

  /**
   * Constructs a ParenList from an SExpression containing a single expression
   * 
   * @param expr  SExpression containing a single expr
   */
  public ParenList(SExpression expr) {
    this.exprs.add(expr);
  }
  
  /**
   * Constructs a ParenList from an SExpression containing multiple expressions,
   * adding each of them to the ParenList.exprs
   * 
   * @param exprs  SExpression containing a multiple exprs
   */
  public ParenList(SExpression exprs[]) {
    for (SExpression expr : exprs) {
      this.exprs.add(expr);
    }
  }
  
  /**
   * Constructs a ParenList from a list of SExpressions, adding all of them to
   * the ParenList.exprs
   * 
   * @param exprs  List of multiple SExpressions
   */
  public ParenList(List<SExpression> exprs) {
    this.exprs.addAll(exprs);
  }
  
  @Override
  public void write(Writer writer) throws IOException {
    writer.write('(');
    for (SExpression expr : exprs) {
      writer.write(' ');
      expr.write(writer);
    }
    writer.write(' ');
    writer.write(')');
  }

  @Override
  public void accept(SExpressionVisitor visitor) {
    visitor.visit(this);
  }

}
