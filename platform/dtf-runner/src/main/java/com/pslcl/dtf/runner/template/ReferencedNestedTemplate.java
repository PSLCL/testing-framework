package com.pslcl.dtf.runner.template;

public class ReferencedNestedTemplate {
	int nestedStepReference;
	InstancedTemplate instancedTemplate;
	
	public ReferencedNestedTemplate(int nestedStepReference, InstancedTemplate instancedTemplate) {
		this.nestedStepReference = nestedStepReference;
		this.instancedTemplate = instancedTemplate;
	}
}
