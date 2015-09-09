package org.manifold.compiler.back.microfluidics;

import org.manifold.compiler.back.microfluidics.smt2.*;
import org.manifold.compiler.middle.BackAnnotationBuilder;
import org.manifold.compiler.middle.Schematic;

public class ReverseInference {
	private DRealSolver.Result res;
	private Schematic schematic;
	
	public ReverseInference(DRealSolver.Result res, Schematic schematic){
		this.res = res;
		this.schematic = schematic;
	}
	
}
