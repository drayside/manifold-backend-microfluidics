package org.manifold.compiler.back.microfluidics.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class TestLinearSystemBuilder {
  
  @Test
  public void testTwoVariableSystemGeneration() {
    // 2x - 3y = -2
    // 4x + y = 24
    // has the solution x=5, y=4
    LinearSystemBuilder builder = new LinearSystemBuilder();
    Variable x = new Variable("x");
    Variable y = new Variable("y");
    builder.addEquation(new LinearTerm[]{
        new LinearTerm(new Constant(2.0), x),
        new LinearTerm(new Constant(-3.0), y),
    }, new Constant(-2.0));
    builder.addEquation(new LinearTerm[]{
        new LinearTerm(new Constant(4.0), x),
        new LinearTerm(new Constant(1.0), y),
    }, new Constant(24.0));
    Variable X = new Variable("X");
    String eqn = builder.build(X);
    assertNotNull(eqn);
    assertFalse(eqn.isEmpty());
    System.err.println(eqn);
    System.err.println(builder.getOrderedUnknowns());
    // TODO call out to MATLAB/Octave and verify solution
  }
  
  @Test
  public void testRegulationedTJunctions4() {
    // only generate the linear system here
    LinearSystemBuilder builder = new LinearSystemBuilder();
    Variable flow_Cin = new Variable("flow_Cin");
    Variable pressure_Cin = new Variable("pressure_Cin");
    Variable[] flow_C = new Variable[]{
      new Variable("flow_C0"),
      new Variable("flow_C1"),
      new Variable("flow_C2"),
      new Variable("flow_C3")
    };
    Variable[] res_C = new Variable[]{
      new Variable("res_C0"),
      new Variable("res_C1"),
      new Variable("res_C2"),
      new Variable("res_C3")
    };
    Variable[] pressure_J = new Variable[]{
      new Variable("pressure_J0"),
      new Variable("pressure_J1"),
      new Variable("pressure_J2"),
      new Variable("pressure_J3")
    };
    Variable[] flow_D = new Variable[]{
      new Variable("flow_D0"),
      new Variable("flow_D1"),
      new Variable("flow_D2"),
      new Variable("flow_D3")
    };
    Variable[] flow_Ef = new Variable[]{
      new Variable("flow_E0f"),
      new Variable("flow_E1f"),
      new Variable("flow_E2f"),
      new Variable("flow_E3f")
    };
    Variable[] res_Ef = new Variable[]{
      new Variable("res_E0f"),
      new Variable("res_E1f"),
      new Variable("res_E2f"),
      new Variable("res_E3f")
    };
    Variable[] flow_G = new Variable[]{
      new Variable("flow_G0"),
      new Variable("flow_G1"),
      new Variable("flow_G2"),
      new Variable("flow_G3")
    };
    Variable[] res_G = new Variable[]{
      new Variable("res_G0"),
      new Variable("res_G1"),
      new Variable("res_G2"),
      new Variable("res_G3")
    };
    Variable[] pressure_G = new Variable[]{
      new Variable("pressure_G0"),
      new Variable("pressure_G1"),
      new Variable("pressure_G2"),
      new Variable("pressure_G3")
    };
    Variable[] flow_Eb = new Variable[]{
      new Variable("flow_E0b"),
      new Variable("flow_E1b"),
      new Variable("flow_E2b"),
      new Variable("flow_E3b")
    };
    Variable[] res_Eb = new Variable[]{
      new Variable("res_E0b"),
      new Variable("res_E1b"),
      new Variable("res_E2b"),
      new Variable("res_E3b")
    };
    Variable[] pressure_W = new Variable[]{
      new Variable("pressure_W0"),
      new Variable("pressure_W1"),
      new Variable("pressure_W2"),
      new Variable("pressure_W3")
    };
    // flow_Cin = flow_C0 + flow_C1 + flow_C2 + flow_C3
    builder.addEquation(new LinearTerm[]{
      new LinearTerm(flow_C[0]),
      new LinearTerm(flow_C[1]),
      new LinearTerm(flow_C[2]),
      new LinearTerm(flow_C[3])
    }, flow_Cin);
    // pressure_Cin = 2.65 atmospheres
    builder.addEquation(
        new LinearTerm[]{new LinearTerm(pressure_Cin)}, 
        new Constant(268511.25));
    for (int i = 0; i < 4; ++i) {
      // pressure_Cin - pressure_Ji - res_Ci * flow_Ci = 0
      builder.addEquation(new LinearTerm[]{
        new LinearTerm(pressure_Cin),
        new LinearTerm(new Constant(-1.0), pressure_J[i]),
        new LinearTerm(new NegationExpression(res_C[i]), flow_C[i])
      }, new Constant(0.0));
      // flow_Ci + flow_Di - flow_Ei,f = 0
      builder.addEquation(new LinearTerm[]{
        new LinearTerm(flow_C[i]),
        new LinearTerm(flow_D[i]),
        new LinearTerm(new Constant(-1.0), flow_Ef[i])
      }, new Constant(0.0));
      // flow_Di = 10 uL/min (current injection)
      builder.addEquation(new LinearTerm[]{
        new LinearTerm(flow_D[i])
      }, new Constant(10.0 * 1.667 * Math.pow(10.0, -8.0)));
      // flow_Ei,f + flow_Gi-1,i - flow_Ei,b - flow_Gi,i+1 = 0
      builder.addEquation(new LinearTerm[]{
        new LinearTerm(flow_Ef[i]),
        new LinearTerm(flow_G[(i+3) % 4]), // effectively (i-1)%4
        new LinearTerm(new Constant(-1.0), flow_Eb[i]),
        new LinearTerm(new Constant(-1.0), flow_G[i])
      }, new Constant(0.0));
      // pressure_Gi - pressure_Gi+1 - res_Gi,i+1 * flow_Gi,i+1 = 0
      builder.addEquation(new LinearTerm[]{
        new LinearTerm(pressure_G[i]),
        new LinearTerm(new Constant(-1.0), pressure_G[(i+1)%4]),
        new LinearTerm(new NegationExpression(res_G[i]), flow_G[i])
      }, new Constant(0.0));
      // pressure_Ji - pressure_Gi - res_Ei,f * flow_Ei,f = 0 
      builder.addEquation(new LinearTerm[]{
        new LinearTerm(pressure_J[i]),
        new LinearTerm(new Constant(-1.0), pressure_G[i]),
        new LinearTerm(new NegationExpression(res_Ef[i]), flow_Ef[i])
      }, new Constant(0.0));
      // pressure_Gi - pressure_Wi - res_Ei,b * flow_Ei,b = 0 
      builder.addEquation(new LinearTerm[]{
        new LinearTerm(pressure_G[i]),
        new LinearTerm(new Constant(-1.0), pressure_W[i]),
        new LinearTerm(new NegationExpression(res_Eb[i]), flow_Eb[i])
      }, new Constant(0.0));
      // pressure_Wi = 1 atmosphere
      builder.addEquation(new LinearTerm[]{
        new LinearTerm(pressure_W[i])
      }, new Constant(101325.0));
    }
    
    Variable X = new Variable("X");
    String eqn = builder.build(X);
    assertNotNull(eqn);
    assertFalse(eqn.isEmpty());
    // print constant terms and RHS terms
    for (Variable cst : builder.getConstantVariables()) {
      System.err.println(cst + " = 0.0;");
    }
    for (Variable rhs : builder.getRHSVariables()) {
      System.err.println(rhs + " = 0.0;");
    }
    System.err.println(eqn);
    System.err.println(builder.getOrderedUnknowns());
  }
  
}
