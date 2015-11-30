package com.pslcl.dtf.runner.template;

import java.util.concurrent.Callable;

import com.pslcl.dtf.runner.process.DBTemplate;
import com.pslcl.dtf.runner.process.RunEntryCore;
import com.pslcl.dtf.runner.process.RunnerMachine;

/**
 * 
 * 
 */
class NestedTemplateTask implements Callable<InstancedTemplate> {
	int nestedStepReference;
	RunEntryCore reCore;
	DBTemplate nestedDBTemplate;
	RunnerMachine runnerMachine;
	
	NestedTemplateTask(int nestedStepReference, RunEntryCore reCore, DBTemplate nestedDBTemplate, RunnerMachine runnerMachine) {
		this.nestedStepReference = nestedStepReference;
		this.reCore = reCore;
		this.nestedDBTemplate = nestedDBTemplate;
		this.runnerMachine = runnerMachine;
	}
	
	/**
	 * 
	 */
	@Override
	public InstancedTemplate call() {
		InstancedTemplate iT = runnerMachine.getTemplateProvider().getInstancedTemplate(nestedStepReference, reCore, nestedDBTemplate, runnerMachine);
		return iT;
	}
	
}