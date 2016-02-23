package com.pslcl.dtf.runner.template;

import java.util.concurrent.Callable;

import com.pslcl.dtf.runner.process.DBTemplate;
import com.pslcl.dtf.runner.process.RunEntryCore;
import com.pslcl.dtf.runner.process.RunnerMachine;

/**
 * 
 * 
 */
class NestedTemplateTask implements Callable<ReferencedNestedTemplate> {
	int stepReference;
	RunEntryCore reCore;
	DBTemplate nestedDBTemplate;
	RunnerMachine runnerMachine;
	
	NestedTemplateTask(int stepReference, RunEntryCore reCore, DBTemplate nestedDBTemplate, RunnerMachine runnerMachine) {
		this.stepReference = stepReference;
		this.reCore = reCore;
		this.nestedDBTemplate = nestedDBTemplate;
		this.runnerMachine = runnerMachine;
	}
	
	/**
	 * 
	 */
	@Override
	public ReferencedNestedTemplate call() throws Exception {
		InstancedTemplate iT = runnerMachine.getTemplateProvider().getInstancedTemplate(this.reCore, this.nestedDBTemplate, this.runnerMachine);
		ReferencedNestedTemplate rnt = new ReferencedNestedTemplate(this.stepReference, iT);
		return rnt;
	}
	
}