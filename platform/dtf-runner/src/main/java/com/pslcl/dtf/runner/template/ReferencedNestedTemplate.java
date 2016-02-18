package com.pslcl.dtf.runner.template;

public class ReferencedNestedTemplate {
	int nestedStepReference;
	InstancedTemplate iT;
	
	public ReferencedNestedTemplate(int nestedStepReference, InstancedTemplate instancedTemplate) {
		this.nestedStepReference = nestedStepReference;
		this.iT = instancedTemplate;
	}
}
