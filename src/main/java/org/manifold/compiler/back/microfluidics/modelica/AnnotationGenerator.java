package org.manifold.compiler.back.microfluidics.modelica;

public class AnnotationGenerator {

  public static String globalAnnotations() {
    String annotations = "annotation(\n" +
      "Diagram(coordinateSystem(preserveAspectRatio=true, " +
      "extent={{0,0},{500.0,500.0}}),graphics),\n" +
      "Icon(coordinateSystem(preserveAspectRatio=true, " +
      "extent={{0,0},{200.0,200.0}})," +
      "graphics={Rectangle(extent={{0,0},{200.0,200.0}}, lineColor={0,0,0})," +
      "Rectangle(extent={{0.0,0.0},{200.0,200.0}})}),\n" +
      "uses(Modelica(version=\"3.2.1\")),\n" +
      "experiment( StartTime = 0,\n" +
      "StopTime = 10.0,\n" +
      "__Maplesoft_solver = \"ck45\",\n" +
      "__Maplesoft_adaptive = true,\n" +
      "__Maplesoft_engine = 2,\n" +
      "Tolerance = 0.10e-4,\n" +
      "__Maplesoft_tolerance_abs = 0.10e-4,\n" +
      "__Maplesoft_step_size = 0.10e-2,\n" +
      "__Maplesoft_plot_points = 200,\n" +
      "__Maplesoft_numeric_jacobian = false,\n" +
      "__Maplesoft_constraint_iterations = 50,\n" +
      "__Maplesoft_event_iterations = 100,\n" +
      "__Maplesoft_algebraic_error_control = false,\n" +
      "__Maplesoft_algebraic_error_relaxation_factor = 1.0,\n" +
      "__Maplesoft_rate_hysteresis = 0.10e-9,\n" +
      "__Maplesoft_scale_method = \"none\",\n" +
      "__Maplesoft_reduce_events = false,\n" +
      "__Maplesoft_integration_diagnostics = false,\n" +
      "__Maplesoft_plot_event_points = true,\n" +
      "__Maplesoft_compiler = true,\n" +
      "__Maplesoft_compiler_optimize = true\n" +
      "));";

    return annotations;
  }
}
