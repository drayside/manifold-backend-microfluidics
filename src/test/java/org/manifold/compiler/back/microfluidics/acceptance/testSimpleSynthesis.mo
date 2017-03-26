model Main
	import Modelica.Constants.inf;
	import Pi=Modelica.Constants.pi;
	import Modelica.Constants.pi;
	import Maplesoft.Constants.I;

	public Maplesoft.Electrical.Analog.Passive.Ground out0 annotation(Placement(transformation(origin={0,0},extent={{0,0},{0,0}},rotation=0)));
	public Maplesoft.Electrical.Analog.Passive.Resistors.Resistor channel0(R=1.000000, T_ref=300.15, alpha=0, useHeatPort=false, T=channel0.T_ref) annotation(Placement(transformation(origin={0,0},extent={{0,0},{0,0}},rotation=0)));
	public Maplesoft.Electrical.Analog.Sources.Voltage.ConstantVoltage in0(V=(start=1.000000, fixed=true)) annotation(Placement(transformation(origin={0,0},extent={{0,0},{0,0}},rotation=0)));
equation
	connect(in0.n, channel0.p) annotation(Line(points={{0,0},{0,0},{0,0},{0,0}},color={0,0,255},smooth=Smooth.None));
	connect(channel0.n, out0.p) annotation(Line(points={{0,0},{0,0},{0,0},{0,0}},color={0,0,255},smooth=Smooth.None));
	connect(out0.p, in0.n) annotation(Line(points={{0,0},{0,0},{0,0},{0,0}},color={0,0,255},smooth=Smooth.None));
annotation(
	Diagram(coordinateSystem(preserveAspectRatio=true, extent={{0,0},{500.0,500.0}}),graphics),
	Icon(coordinateSystem(preserveAspectRatio=true, extent={{0,0},{200.0,200.0}}),graphics={Rectangle(extent={{0,0},{200.0,200.0}}, lineColor={0,0,0}),Rectangle(extent={{0.0,0.0},{200.0,200.0}})}),
	uses(Modelica(version="3.2.1")),
	experiment( StartTime = 0,
	StopTime = 10.0,
	__Maplesoft_solver = "ck45",
	__Maplesoft_adaptive = true,
	__Maplesoft_engine = 2,
	Tolerance = 0.10e-4,
	__Maplesoft_tolerance_abs = 0.10e-4,
	__Maplesoft_step_size = 0.10e-2,
	__Maplesoft_plot_points = 200,
	__Maplesoft_numeric_jacobian = false,
	__Maplesoft_constraint_iterations = 50,
	__Maplesoft_event_iterations = 100,
	__Maplesoft_algebraic_error_control = false,
	__Maplesoft_algebraic_error_relaxation_factor = 1.0,
	__Maplesoft_rate_hysteresis = 0.10e-9,
	__Maplesoft_scale_method = "none",
	__Maplesoft_reduce_events = false,
	__Maplesoft_integration_diagnostics = false,
	__Maplesoft_plot_event_points = true,
	__Maplesoft_compiler = true,
	__Maplesoft_compiler_optimize = true
	));
end Main;
