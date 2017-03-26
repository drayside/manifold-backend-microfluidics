package org.manifold.compiler.back.microfluidics.modelica;

public class AnnotationGenerator {

  public static String globalAnnotations() {
    String annotations = "annotation(\n" +
      "\tDiagram(coordinateSystem(preserveAspectRatio=true, " +
      "extent={{0,0},{500.0,500.0}}),graphics),\n" +
      "\tIcon(coordinateSystem(preserveAspectRatio=true, " +
      "extent={{0,0},{200.0,200.0}})," +
      "graphics={Rectangle(extent={{0,0},{200.0,200.0}}, lineColor={0,0,0})," +
      "Rectangle(extent={{0.0,0.0},{200.0,200.0}})}),\n" +
      "\tuses(Modelica(version=\"3.2.1\")),\n" +
      "\texperiment( StartTime = 0,\n" +
      "\tStopTime = 10.0,\n" +
      "\t__Maplesoft_solver = \"ck45\",\n" +
      "\t__Maplesoft_adaptive = true,\n" +
      "\t__Maplesoft_engine = 2,\n" +
      "\tTolerance = 0.10e-4,\n" +
      "\t__Maplesoft_tolerance_abs = 0.10e-4,\n" +
      "\t__Maplesoft_step_size = 0.10e-2,\n" +
      "\t__Maplesoft_plot_points = 200,\n" +
      "\t__Maplesoft_numeric_jacobian = false,\n" +
      "\t__Maplesoft_constraint_iterations = 50,\n" +
      "\t__Maplesoft_event_iterations = 100,\n" +
      "\t__Maplesoft_algebraic_error_control = false,\n" +
      "\t__Maplesoft_algebraic_error_relaxation_factor = 1.0,\n" +
      "\t__Maplesoft_rate_hysteresis = 0.10e-9,\n" +
      "\t__Maplesoft_scale_method = \"none\",\n" +
      "\t__Maplesoft_reduce_events = false,\n" +
      "\t__Maplesoft_integration_diagnostics = false,\n" +
      "\t__Maplesoft_plot_event_points = true,\n" +
      "\t__Maplesoft_compiler = true,\n" +
      "\t__Maplesoft_compiler_optimize = true\n" +
      "\t));";

    return annotations;
  }
}
