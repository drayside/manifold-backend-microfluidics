Manifold Microfluidics Backend -- Requirements
==============================================

Functional Requirements
-----------------------

* multiple translation strategies to target SMT2 (dReal)
* multiple sub-strategies for different parts of the formula
* dynamic selection of strategies and sub-strategies
* verification of SMT2 in MATLAB, etc.
* re-translation / solution iteration
* CEGAR (counterexample-guided abstraction refinement)
* reduced order models -- change of assumptions
* "which math", "which physics", "which translation strategy"

Circuit Description Requirements
--------------------------------

* A "control point" is a type of node
* A control point can control a pressure or a voltage (but not both)
* Voltage control points are "closed" -- there is no flow in or out of the circuit
* Fluid enters and leaves the circuit through pressure control points
* A control point can have an arbitrary number of channels connected to it 
(for now assume this number can be at most two)

* The following constraints can be placed on control points:
  - Fixed pressure
  - Placement, i.e. (x,y) coordinates

* The following "design parameters" are specified to the backend independent of the schematic:
  - minimum distance between nodes
  - minimum channel length
  - maximum chip area (width, height)
  - critical angle theta_c (constrains channel crossings)

* A "channel" is a type of connection
* Channels can have different shapes:
  - cylinder (radius, length)
  - rectangle (width, height, length)
  - custom (specify geometry of channel cross-section, length)
* All channel attributes are optional and have corresponding constraints
* Channels can have a "purpose" (simple fluid, electrophoresis, etc.) -- delay on this point until we know more

* The following additional constraints can be placed on channels:
  - fixed pressure
  - fixed flow
  - placement (must pass through this (x,y) point)

* A "channel crossing" is a type of node
* Assume for now that channel crossings only join two channels (i.e. four ports)

Initial Target
--------------

* SMT2 pressure/flow constraints for an analytical solution
* ignore channel crossings for the purposes of finding pressure differentials across channels
(i.e. look "through" the crossing to find the pressure)
* Voltage control points use V=IR where the fluid acts like a resistor -- need more information here
* SMT2 placement constraints
